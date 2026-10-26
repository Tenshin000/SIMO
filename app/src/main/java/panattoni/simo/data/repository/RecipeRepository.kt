package panattoni.simo.data.repository

import panattoni.simo.data.local.dao.ItemDao
import panattoni.simo.data.local.dao.RecipeDao
import panattoni.simo.data.local.entity.ItemEntity
import panattoni.simo.data.local.entity.RecipeComponentEntity
import panattoni.simo.data.local.entity.RecipeEntity
import panattoni.simo.data.model.Item
import panattoni.simo.data.model.ItemType
import panattoni.simo.data.model.Recipe
import panattoni.simo.data.model.RecipeComponent
import panattoni.simo.data.model.RecipeComponentDetail
import panattoni.simo.data.model.RecipeWithComponents
import panattoni.simo.domain.MathEngine
import androidx.room.withTransaction
import panattoni.simo.data.local.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

import panattoni.simo.data.local.dao.ShoppingDao
import panattoni.simo.data.local.entity.ShoppingItemEntity
import panattoni.simo.data.model.ShoppingItem
import panattoni.simo.data.model.ScaledComponent

/**
 * Repository centrale dell'applicazione SIMO.
 * Coordina l'accesso a ricette, ingredienti, grafo orientato delle sotto-ricette e lista della spesa.
 */
class RecipeRepository(
    private val database: AppDatabase,
    private val itemDao: ItemDao = database.itemDao(),
    private val recipeDao: RecipeDao = database.recipeDao(),
    private val shoppingDao: ShoppingDao = database.shoppingDao()
){
    // Esito delle operazioni di eliminazione ricetta
    sealed class DeleteResult{
        object Success : DeleteResult()
        data class InUse(val usedInRecipes: List<String>) : DeleteResult()
        data class Error(val message: String) : DeleteResult()
    }

    val allRecipes: Flow<List<Recipe>> = recipeDao.getAllRecipes().map{ entities ->
        entities.map{ it.toDomain() }
    }

    val allItems: Flow<List<Item>> = itemDao.getAllItems().map{ entities ->
        entities.map{ it.toDomain() }
    }

    val allRawIngredients: Flow<List<Item>> = itemDao.getItemsByType("RAW_INGREDIENT").map{ entities ->
        entities.map{ it.toDomain() }
    }

    /**
     * Mappa reattiva di tutte le ricette con i relativi componenti e sotto-ricette.
     * Garantisce l'aggiornamento a cascata in tempo reale: se la ricetta base della Crema viene modificata,
     * questo Flow emette il nuovo stato e chiunque stia osservando i Cornetti riceve immediatamente
     * le nuove dosi dei sotto-ingredienti!
     */
    val allRecipesWithComponentsMap: Flow<Map<String, RecipeWithComponents>> = combine(
        recipeDao.getAllRecipes(),
        itemDao.getAllItems(),
        recipeDao.getAllComponentsFlow()
    ){ recipes, items, components ->
        val itemMap = items.associateBy{ it.id }
        val recipeMap = recipes.associateBy{ it.id }
        val componentsByParent = components.groupBy{ it.parentRecipeId }

        recipes.associate{ recipeEntity ->
            val recipe = recipeEntity.toDomain()
            val recipeComps = (componentsByParent[recipe.id] ?: emptyList())
                .distinctBy{ it.childItemId }
                .sortedBy{ it.sortOrder }
                .map{ compEntity ->
                    val item = itemMap[compEntity.childItemId]?.toDomain()
                        ?: Item(
                            id = compEntity.childItemId,
                            name = "Ingrediente sconosciuto",
                            type = ItemType.RAW_INGREDIENT
                        )
                    val subRecipe = if (item.type == ItemType.RECIPE){
                        recipeMap[item.id]?.toDomain()
                    } else null

                    RecipeComponentDetail(
                        component = compEntity.toDomain(),
                        item = item,
                        subRecipe = subRecipe
                    )
                }

            recipe.id to RecipeWithComponents(recipe, recipeComps)
        }
    }.flowOn(kotlinx.coroutines.Dispatchers.Default)

    fun getRecipeWithComponents(recipeId: String): Flow<RecipeWithComponents?>{
        return allRecipesWithComponentsMap.map{ map ->
            map[recipeId]
        }
    }

    suspend fun getRecipeWithComponentsDirect(recipeId: String): RecipeWithComponents?{
        val recipeEntity = recipeDao.getRecipeById(recipeId) ?: return null
        val compEntities = recipeDao.getComponentsForRecipeDirect(recipeId)
        val allItemsMap = itemDao.getAllItemsDirect().associateBy{ it.id }
        val allRecipesMap = recipeDao.getAllRecipesDirect().associateBy{ it.id }

        val recipeComps = compEntities
            .distinctBy{ it.childItemId }
            .sortedBy{ it.sortOrder }
            .map{ compEntity ->
                val item = allItemsMap[compEntity.childItemId]?.toDomain()
                    ?: Item(
                        id = compEntity.childItemId,
                        name = "Ingrediente sconosciuto",
                        type = ItemType.RAW_INGREDIENT
                    )
                val subRecipe = if (item.type == ItemType.RECIPE){
                    allRecipesMap[item.id]?.toDomain()
                } else null

                RecipeComponentDetail(
                    component = compEntity.toDomain(),
                    item = item,
                    subRecipe = subRecipe
                )
            }

        return RecipeWithComponents(recipeEntity.toDomain(), recipeComps)
    }

    suspend fun saveRecipe(recipe: Recipe, components: List<RecipeComponent>){
        // 1. Salva/aggiorna l'entità Recipe
        val recipeEntity = RecipeEntity.fromDomain(recipe)

        // 2. Assicura che esista un Item di tipo RECIPE corrispondente
        // in modo che questa ricetta possa essere usata come ingrediente in altre ricette
        val recipeItem = ItemEntity(
            id = recipe.id,
            name = recipe.name,
            type = "RECIPE",
            defaultUnit = recipe.yieldUnit
        )
        itemDao.upsertItem(recipeItem)

        // 3. Salva la ricetta e i componenti in transazione
        val componentEntities = components.mapIndexed{ index, comp ->
            RecipeComponentEntity.fromDomain(comp.copy(parentRecipeId = recipe.id, sortOrder = index))
        }
        recipeDao.replaceRecipeWithComponents(recipeEntity, componentEntities)
    }

    suspend fun saveItem(item: Item){
        itemDao.upsertItem(ItemEntity.fromDomain(item))
    }

    suspend fun getLinkedRecipes(recipeId: String): List<String>{
        return recipeDao.getParentRecipeNamesUsingChild(recipeId)
    }

    suspend fun deleteRecipe(recipeId: String, convertToRaw: Boolean = false): DeleteResult{
        return try{
            val referencing = recipeDao.getParentRecipeNamesUsingChild(recipeId)
            database.withTransaction{
                // 1. Rimuove i componenti di cui questa ricetta è genitore
                recipeDao.deleteComponentsForRecipe(recipeId)
                // 2. Rimuove la ricetta stessa dalla tabella recipes
                recipeDao.deleteRecipeById(recipeId)
                // 3. Se convertToRaw == true o se la ricetta è usata in altre ricette:
                // l'Item non viene eliminato, ma convertito in RAW_INGREDIENT!
                if (convertToRaw || referencing.isNotEmpty()){
                    itemDao.updateItemType(recipeId, "RAW_INGREDIENT")
                } else{
                    itemDao.deleteItemById(recipeId)
                }
            }
            DeleteResult.Success
        } catch (e: Exception){
            DeleteResult.Error(e.message ?: "Errore durante l'eliminazione")
        }
    }

    suspend fun checkWouldCreateCycle(parentRecipeId: String, candidateChildId: String): Boolean{
        val allComponents = recipeDao.getAllComponentsDirect()
        val existingEdges = allComponents.map{ it.parentRecipeId to it.childItemId }
        return MathEngine.wouldCreateCycle(parentRecipeId, candidateChildId, existingEdges)
    }

    suspend fun toggleFavorite(recipeId: String, currentStatus: Boolean){
        recipeDao.setFavorite(recipeId, !currentStatus)
    }

    // --- Shopping List Operations ---

    val shoppingItems: Flow<List<ShoppingItem>> = shoppingDao.getAllShoppingItems().map{ entities ->
        entities.map{ it.toDomain() }
    }

    /**
     * Aggiunge componenti scalate alla lista della spesa con Aggregazione Intelligente:
     * Se un ingrediente con la stessa unità è già presente tra quelli da comprare (isChecked == 0),
     * la quantità viene automaticamente sommata senza creare duplicati.
     */
    suspend fun addScaledComponentsToShoppingList(
        recipeName: String,
        components: List<ScaledComponent>
    ): Int{
        var addedOrUpdatedCount = 0
        database.withTransaction{
            for (comp in components){
                // Se è una sotto-ricetta, possiamo decidere di aggiungere le sue materie prime o la sotto-ricetta stessa.
                // Se ha un subRecipeTree, aggiungiamo ricorsivamente le foglie/materie prime per permettere di fare la spesa reale!
                if (comp.subRecipeTree != null && comp.subRecipeTree.components.isNotEmpty()){
                    addedOrUpdatedCount += addScaledComponentsToShoppingList(recipeName, comp.subRecipeTree.components)
                } else{
                    val rawUnit = comp.componentDetail.component.unit.trim()
                    val itemDetail = comp.componentDetail.item
                    val itemName = itemDetail.name.trim()

                    // Se l'ingrediente in ricetta ha unità 'q.b.', nella lista della spesa utilizziamo
                    // l'unità di misura scelta quando è stato deciso il prezzo unitario (priceUnit o defaultUnit).
                    // Come misura di default impostiamo 100 per grammi/millilitri e 1 in tutti gli altri casi.
                    val isQb = panattoni.simo.domain.MeasurementUnit.isQb(rawUnit)
                    val effectiveUnit = if (isQb){
                        itemDetail.priceUnit?.trim()?.ifEmpty { null }
                            ?: itemDetail.defaultUnit.trim().ifEmpty { null }
                            ?: "pz"
                    } else {
                        rawUnit
                    }

                    val effectiveQty = if (isQb){
                        when (effectiveUnit.lowercase()){
                            "g", "ml" -> 100.0
                            else -> 1.0
                        }
                    } else {
                        comp.scaledQuantity
                    }

                    val itemUnitPrice = if (itemDetail.pricePerUnit != null && itemDetail.pricePerUnit > 0.0){
                        itemDetail.pricePerUnit
                    } else 0.0

                    val itemPriceUnit = if (itemUnitPrice > 0.0){
                        itemDetail.priceUnit ?: itemDetail.defaultUnit
                    } else null

                    val existing = shoppingDao.findActiveItemByNameAndUnit(itemName, effectiveUnit)
                    if (existing != null){
                        val newQty = existing.quantity + effectiveQty
                        val updatedSource = if (existing.source == recipeName || existing.source == "Manuale"){
                            recipeName
                        } else if (!existing.source.contains(recipeName)){
                            "${existing.source}, $recipeName"
                        } else existing.source

                        val updatedPrice = if (existing.unitPrice <= 0.0 && itemUnitPrice > 0.0) itemUnitPrice else existing.unitPrice
                        val updatedPriceUnit = if (existing.priceUnit.isNullOrBlank() && itemPriceUnit != null) itemPriceUnit else existing.priceUnit

                        shoppingDao.updateShoppingItem(
                            existing.copy(
                                quantity = newQty,
                                source = updatedSource,
                                unitPrice = updatedPrice,
                                priceUnit = updatedPriceUnit
                            )
                        )
                    } else{
                        shoppingDao.insertShoppingItem(
                            ShoppingItemEntity(
                                itemName = itemName,
                                quantity = effectiveQty,
                                unit = effectiveUnit,
                                source = recipeName,
                                isChecked = false,
                                unitPrice = itemUnitPrice,
                                priceUnit = itemPriceUnit
                            )
                        )
                    }
                    addedOrUpdatedCount++
                }
            }
        }
        return addedOrUpdatedCount
    }

    suspend fun addManualShoppingItem(itemName: String, quantity: Double, unit: String, unitPrice: Double = 0.0, priceUnit: String? = null){
        val trimmedName = itemName.trim()
        val trimmedUnit = unit.trim()
        val safePrice = if (unitPrice > 0.0) unitPrice else 0.0
        val safePriceUnit = priceUnit?.trim()?.ifEmpty { null } ?: trimmedUnit
        database.withTransaction{
            val existing = shoppingDao.findActiveItemByNameAndUnit(trimmedName, trimmedUnit)
            if (existing != null){
                val updatedPrice = if (safePrice > 0.0) safePrice else existing.unitPrice
                val updatedPriceUnit = if (existing.priceUnit.isNullOrBlank()) safePriceUnit else existing.priceUnit
                shoppingDao.updateShoppingItem(existing.copy(
                    quantity = existing.quantity + quantity,
                    unitPrice = updatedPrice,
                    priceUnit = updatedPriceUnit
                ))
            } else{
                shoppingDao.insertShoppingItem(
                    ShoppingItemEntity(
                        itemName = trimmedName,
                        quantity = quantity,
                        unit = trimmedUnit,
                        source = "Manuale",
                        isChecked = false,
                        unitPrice = safePrice,
                        priceUnit = safePriceUnit
                    )
                )
            }
        }
    }

    suspend fun setShoppingItemChecked(id: Long, isChecked: Boolean){
        shoppingDao.setChecked(id, isChecked)
    }

    suspend fun updateShoppingItemQuantity(id: Long, newQuantity: Double){
        if (newQuantity > 0.0){
            shoppingDao.updateQuantity(id, newQuantity)
        } else{
            shoppingDao.deleteShoppingItemById(id)
        }
    }

    suspend fun updateShoppingItem(id: Long, newQuantity: Double, newUnitPrice: Double){
        if (newQuantity <= 0.0){
            shoppingDao.deleteShoppingItemById(id)
            return
        }
        val safePrice = if (newUnitPrice > 0.0) newUnitPrice else 0.0
        shoppingDao.updateQuantity(id, newQuantity)
        val current = shoppingDao.getItemById(id)
        if (current != null){
            shoppingDao.updateQuantityAndPrice(id, newQuantity, safePrice, current.priceUnit)
        }
    }

    suspend fun updateShoppingItemDetails(id: Long, newQuantity: Double, newUnit: String, newUnitPrice: Double, newPriceUnit: String? = null){
        if (newQuantity <= 0.0){
            shoppingDao.deleteShoppingItemById(id)
            return
        }
        val safePrice = if (newUnitPrice > 0.0) newUnitPrice else 0.0
        val safeUnit = newUnit.trim().ifEmpty { "pz" }
        val effectivePriceUnit = newPriceUnit?.trim()?.ifEmpty { null } ?: safeUnit
        shoppingDao.updateItemDetails(id, newQuantity, safeUnit, safePrice, effectivePriceUnit)
    }

    suspend fun sanitizeQbShoppingItems(){
        database.withTransaction{
            val items = shoppingDao.getActiveShoppingItems()
            val allItems = itemDao.getAllItemsDirect().associateBy { it.name.trim().lowercase() }
            for (item in items){
                val matched = allItems[item.itemName.trim().lowercase()]
                var changed = false
                var currentUnit = item.unit
                var currentQty = item.quantity
                var currentPrice = item.unitPrice
                var currentPriceUnit = item.priceUnit

                if (panattoni.simo.domain.MeasurementUnit.isQb(currentUnit)){
                    val resolvedUnit = matched?.priceUnit?.trim()?.ifEmpty { null }
                        ?: matched?.defaultUnit?.trim()?.ifEmpty { null }
                        ?: "pz"
                    currentUnit = resolvedUnit
                    if (currentQty <= 0.0){
                        currentQty = when (resolvedUnit.lowercase()){
                            "g", "ml" -> 100.0
                            else -> 1.0
                        }
                    }
                    changed = true
                }

                if (currentPrice <= 0.0 && matched?.pricePerUnit != null && matched.pricePerUnit > 0.0){
                    currentPrice = matched.pricePerUnit
                    changed = true
                }

                if (currentPriceUnit.isNullOrBlank() && matched?.priceUnit != null){
                    currentPriceUnit = matched.priceUnit
                    changed = true
                }

                if (changed){
                    shoppingDao.updateItemDetails(item.id, currentQty, currentUnit, currentPrice, currentPriceUnit)
                }
            }
        }
    }

    suspend fun deleteShoppingItem(id: Long){
        shoppingDao.deleteShoppingItemById(id)
    }

    suspend fun deleteCheckedShoppingItems(): Int{
        return shoppingDao.deleteCheckedItems()
    }

    suspend fun clearShoppingList(){
        shoppingDao.clearAll()
    }
}
