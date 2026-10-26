package panattoni.simo.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import panattoni.simo.data.local.AppDatabase
import panattoni.simo.data.model.Item
import panattoni.simo.data.model.ItemType
import panattoni.simo.data.model.Recipe
import panattoni.simo.data.model.RecipeComponent
import panattoni.simo.data.repository.RecipeRepository
import panattoni.simo.util.NumberUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import panattoni.simo.data.remote.GeminiIngredient
import panattoni.simo.data.remote.GeminiRecipeService
import panattoni.simo.domain.Allergen
import panattoni.simo.domain.RecipeTags
import java.util.UUID

/**
 * Modello locale di un componente in corso di modifica nella schermata di composizione ricetta.
 */
data class EditableComponent(
    val tempId: String = UUID.randomUUID().toString(),
    val itemId: String,
    val itemName: String,
    val itemType: ItemType,
    val quantity: String,
    val unit: String,
    val pricePerUnit: Double? = null,
    val priceUnit: String? = null,
    val allergens: List<Allergen> = emptyList()
)

/**
 * Stato immutabile dell'interfaccia di inserimento e modifica ricetta.
 */
data class RecipeEditUiState(
    val recipeId: String,
    val isEditing: Boolean = false,
    val name: String = "",
    val description: String = "",
    val category: String = "Dolci",
    val availableTags: List<String> = RecipeTags.DEFAULT_TAGS,
    val isFavorite: Boolean = false,
    val baseYield: String = "1",
    val yieldUnit: String = "porzioni",
    val instructions: String = "",
    val components: List<EditableComponent> = emptyList(),
    val availableItems: List<Item> = emptyList(),
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val isSaved: Boolean = false,
    val isAiLoading: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * ViewModel per la creazione e modifica delle ricette.
 * Gestisce l'aggiunta di ingredienti, prevenzione cicli DAG, drag-and-drop e completamento AI.
 */
class RecipeEditViewModel(
    application: Application,
    private val initialRecipeId: String? = null
) : AndroidViewModel(application){

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = RecipeRepository(database)

    private val _uiState = MutableStateFlow(
        RecipeEditUiState(
            recipeId = initialRecipeId ?: UUID.randomUUID().toString(),
            isEditing = initialRecipeId != null
        )
    )
    val uiState: StateFlow<RecipeEditUiState> = _uiState

    // Categorie appartenenti alle altre ricette esistenti nel database
    private var otherRecipeCategories: List<String> = emptyList()

    init{
        loadData()
    }

    private fun loadData(){
        viewModelScope.launch(Dispatchers.IO){
            try {
                val allItems = repository.allItems.first()

                if (initialRecipeId != null){
                    val current = repository.getRecipeWithComponentsDirect(initialRecipeId)
                    if (current != null){
                        val compList = current.components.map{ detail ->
                            EditableComponent(
                                itemId = detail.item.id,
                                itemName = detail.item.name,
                                itemType = detail.item.type,
                                quantity = NumberUtils.formatQuantityForUnit(detail.component.baseQuantity, detail.component.unit),
                                unit = detail.component.unit,
                                pricePerUnit = detail.item.pricePerUnit,
                                priceUnit = detail.item.priceUnit,
                                allergens = detail.item.allergensList
                            )
                        }
                        _uiState.value = _uiState.value.copy(
                            name = current.recipe.name,
                            description = current.recipe.description,
                            category = current.recipe.category,
                            isFavorite = current.recipe.isFavorite,
                            baseYield = NumberUtils.formatAmount(current.recipe.baseYieldQuantity),
                            yieldUnit = current.recipe.yieldUnit,
                            instructions = current.recipe.instructions,
                            components = compList,
                            createdAt = current.recipe.createdAt
                        )
                    }
                }

                // Filtra gli items disponibili per prevenire cicli con il DAG
                val safeItems = mutableListOf<Item>()
                for (item in allItems){
                    if (item.id == _uiState.value.recipeId) continue
                    if (item.type == ItemType.RECIPE){
                        val createsCycle = repository.checkWouldCreateCycle(_uiState.value.recipeId, item.id)
                        if (!createsCycle){
                            safeItems.add(item)
                        }
                    } else {
                        safeItems.add(item)
                    }
                }
                val allRecipes = repository.allRecipes.first()
                val currentRecipeId = _uiState.value.recipeId
                otherRecipeCategories = allRecipes
                    .filter{ it.id != currentRecipeId }
                    .map{ it.category }
                val currentCat = _uiState.value.category
                val initialTags = if (currentCat.isNotBlank()){
                    otherRecipeCategories + currentCat.trim()
                } else{
                    otherRecipeCategories
                }
                val sortedTags = RecipeTags.getAllSortedTags(initialTags)
                _uiState.value = _uiState.value.copy(
                    availableItems = safeItems,
                    availableTags = sortedTags
                )
            } catch (e: Exception){
                _uiState.value = _uiState.value.copy(errorMessage = "Impossibile caricare i dati: ${e.message}")
            }
        }
    }

    fun onNameChange(value: String){ _uiState.value = _uiState.value.copy(name = value) }
    fun onDescriptionChange(value: String){ _uiState.value = _uiState.value.copy(description = value) }
    fun onCategoryChange(value: String){
        val tagsInput = if (value.isNotBlank()){
            otherRecipeCategories + value.trim()
        } else{
            otherRecipeCategories
        }
        val updatedTags = RecipeTags.getAllSortedTags(tagsInput)
        _uiState.value = _uiState.value.copy(
            category = value,
            availableTags = updatedTags
        )
    }
    fun onBaseYieldChange(value: String){ _uiState.value = _uiState.value.copy(baseYield = value) }
    fun onYieldUnitChange(value: String){ _uiState.value = _uiState.value.copy(yieldUnit = value) }
    fun onInstructionsChange(value: String){ _uiState.value = _uiState.value.copy(instructions = value) }

    fun addComponent(
        item: Item,
        quantity: String,
        unit: String,
        pricePerUnit: Double? = null,
        priceUnit: String? = null,
        allergens: List<Allergen>? = null
    ){
        val state = _uiState.value
        // Verifica duplicati
        if (state.components.any { it.itemId == item.id }){
            _uiState.value = state.copy(errorMessage = "\"${item.name}\" è già presente negli ingredienti di questa ricetta")
            return
        }

        val effectiveUnit = unit.trim().ifEmpty { item.defaultUnit }
        val isQb = panattoni.simo.domain.MeasurementUnit.isQb(effectiveUnit)
        val isInteger = panattoni.simo.domain.MeasurementUnit.isIntegerUnit(effectiveUnit)

        // Verifica validità quantità se presente (q.b. e campo vuoto ammessi per definizione parziale)
        if (!isQb && quantity.isNotBlank() && !NumberUtils.isValidPositiveNumber(quantity)){
            _uiState.value = state.copy(errorMessage = "Inserisci una quantità valida per \"${item.name}\" oppure lasciala vuota per completarla con l'AI")
            return
        }

        val sanitizedQty = if (isQb || quantity.isBlank()){
            ""
        } else if (isInteger){
            val parsed = NumberUtils.parseDoubleSafe(quantity, 1.0)
            panattoni.simo.domain.MeasurementUnit.roundIntegerAmount(parsed).toString()
        } else {
            quantity.trim()
        }

        val effectivePrice = pricePerUnit ?: item.pricePerUnit
        val effectivePriceUnit = priceUnit ?: item.priceUnit
        val effectiveAllergens = allergens ?: item.allergensList

        val newComp = EditableComponent(
            itemId = item.id,
            itemName = item.name,
            itemType = item.type,
            quantity = sanitizedQty,
            unit = effectiveUnit,
            pricePerUnit = effectivePrice,
            priceUnit = effectivePriceUnit,
            allergens = effectiveAllergens
        )
        _uiState.value = state.copy(
            components = state.components + newComp,
            errorMessage = null
        )

        // Se è stato specificato o aggiornato un prezzo o allergeni per materia prima, salvalo nell'Item sul DB
        val allergensStr = Allergen.formatAllergensString(effectiveAllergens)
        val shouldUpdateItem = (pricePerUnit != null && pricePerUnit > 0.0) || (allergens != null && item.type != ItemType.RECIPE)
        if (shouldUpdateItem){
            viewModelScope.launch {
                try {
                    repository.saveItem(item.copy(
                        pricePerUnit = effectivePrice,
                        priceUnit = effectivePriceUnit,
                        allergens = if (item.type != ItemType.RECIPE) allergensStr else item.allergens
                    ))
                } catch (_: Exception){}
            }
        }
    }

    fun createAndAddRawIngredient(
        name: String,
        quantity: String,
        unit: String,
        pricePerUnit: Double? = null,
        priceUnit: String? = null,
        allergens: List<Allergen> = emptyList()
    ){
        val trimmedName = name.trim()
        if (trimmedName.isBlank()){
            _uiState.value = _uiState.value.copy(errorMessage = "Il nome dell'ingrediente non può essere vuoto")
            return
        }
        val effectiveUnit = unit.trim().ifEmpty { "g" }
        val isQb = panattoni.simo.domain.MeasurementUnit.isQb(effectiveUnit)
        val isInteger = panattoni.simo.domain.MeasurementUnit.isIntegerUnit(effectiveUnit)

        if (!isQb && quantity.isNotBlank() && !NumberUtils.isValidPositiveNumber(quantity)){
            _uiState.value = _uiState.value.copy(errorMessage = "Inserisci una quantità valida oppure lasciala vuota per completarla con l'AI")
            return
        }

        val sanitizedQty = if (isQb || quantity.isBlank()){
            ""
        } else if (isInteger){
            val parsed = NumberUtils.parseDoubleSafe(quantity, 1.0)
            panattoni.simo.domain.MeasurementUnit.roundIntegerAmount(parsed).toString()
        } else {
            quantity.trim()
        }

        val allergensStr = Allergen.formatAllergensString(allergens)

        viewModelScope.launch {
            try {
                val newItem = Item(
                    id = "custom_" + UUID.randomUUID().toString().take(8),
                    name = trimmedName,
                    type = ItemType.RAW_INGREDIENT,
                    defaultUnit = effectiveUnit,
                    pricePerUnit = pricePerUnit,
                    priceUnit = priceUnit,
                    allergens = allergensStr
                )
                repository.saveItem(newItem)
                addComponent(newItem, sanitizedQty, effectiveUnit, pricePerUnit, priceUnit, allergens)
                // Aggiorna gli available items
                _uiState.value = _uiState.value.copy(
                    availableItems = _uiState.value.availableItems + newItem
                )
            } catch (e: Exception){
                _uiState.value = _uiState.value.copy(errorMessage = "Errore durante la creazione dell'ingrediente: ${e.message}")
            }
        }
    }

    fun updateComponentAllergens(tempId: String, newAllergens: List<Allergen>){
        val updated = _uiState.value.components.map { comp ->
            if (comp.tempId == tempId){
                comp.copy(allergens = newAllergens)
            } else comp
        }
        _uiState.value = _uiState.value.copy(components = updated)

        // Aggiorna anche l'Item nel database se materia prima
        val targetComp = _uiState.value.components.firstOrNull { it.tempId == tempId }
        if (targetComp != null && targetComp.itemType != ItemType.RECIPE){
            val allergensStr = Allergen.formatAllergensString(newAllergens)
            viewModelScope.launch {
                try {
                    val existingItem = repository.allItems.first().firstOrNull { it.id == targetComp.itemId }
                    if (existingItem != null){
                        repository.saveItem(existingItem.copy(allergens = allergensStr))
                    }
                } catch (_: Exception){}
            }
        }
    }

    fun updateComponentPrice(tempId: String, newPrice: Double?, newPriceUnit: String?){
        val updated = _uiState.value.components.map { comp ->
            if (comp.tempId == tempId){
                comp.copy(pricePerUnit = newPrice, priceUnit = newPriceUnit)
            } else comp
        }
        _uiState.value = _uiState.value.copy(components = updated)

        // Aggiorna anche l'Item nel database se applicabile
        val targetComp = _uiState.value.components.firstOrNull { it.tempId == tempId }
        if (targetComp != null){
            viewModelScope.launch {
                try {
                    val existingItem = repository.allItems.first().firstOrNull { it.id == targetComp.itemId }
                    if (existingItem != null){
                        repository.saveItem(existingItem.copy(pricePerUnit = newPrice, priceUnit = newPriceUnit))
                    }
                } catch (_: Exception){}
            }
        }
    }

    fun removeComponent(tempId: String){
        _uiState.value = _uiState.value.copy(
            components = _uiState.value.components.filterNot{ it.tempId == tempId }
        )
    }

    fun moveComponent(fromIndex: Int, toIndex: Int){
        val current = _uiState.value.components.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices && fromIndex != toIndex){
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            _uiState.value = _uiState.value.copy(components = current)
        }
    }

    fun moveComponentUp(index: Int){
        if (index > 0){
            moveComponent(index, index - 1)
        }
    }

    fun moveComponentDown(index: Int){
        if (index < _uiState.value.components.size - 1){
            moveComponent(index, index + 1)
        }
    }

    fun saveRecipe(onSuccess: () -> Unit){
        val state = _uiState.value
        if (state.name.isBlank()){
            _uiState.value = state.copy(errorMessage = "Inserisci il nome della ricetta")
            return
        }
        val yieldValue = NumberUtils.parseDoubleOrNull(state.baseYield)
        if (yieldValue == null || yieldValue <= 0.0){
            _uiState.value = state.copy(errorMessage = "La resa base deve essere un numero valido maggiore di zero (es. 1 o 500)")
            return
        }
        if (state.components.isEmpty()){
            _uiState.value = state.copy(errorMessage = "Aggiungi almeno un ingrediente o ricetta")
            return
        }

        // Verifica validità quantità di tutti i componenti (eccetto q.b.): non è possibile salvare senza aver determinato almeno il peso!
        for (comp in state.components){
            if (panattoni.simo.domain.MeasurementUnit.isQb(comp.unit)) continue
            val q = NumberUtils.parseDoubleOrNull(comp.quantity)
            if (q == null || q <= 0.0){
                _uiState.value = state.copy(
                    errorMessage = "Impossibile salvare la ricetta: devi prima determinare la dose/peso per \"${comp.itemName}\". Inseriscila o chiedi a Gemini di calcolarla."
                )
                return
            }
        }

        viewModelScope.launch {
            try {
                // Controllo cicli DAG per tutte le sotto-ricette
                for (comp in state.components){
                    if (comp.itemType == ItemType.RECIPE){
                        val wouldCycle = repository.checkWouldCreateCycle(state.recipeId, comp.itemId)
                        if (wouldCycle){
                            _uiState.value = state.copy(
                                errorMessage = "Impossibile salvare: il collegamento a \"${comp.itemName}\" crea un riferimento circolare infinito"
                            )
                            return@launch
                        }
                    }
                }

                val recipe = Recipe(
                    id = state.recipeId,
                    name = state.name.trim(),
                    description = state.description.trim(),
                    category = state.category.trim().ifEmpty { "Altro" },
                    isFavorite = state.isFavorite,
                    baseYieldQuantity = yieldValue,
                    yieldUnit = state.yieldUnit.trim().ifEmpty { "porzioni" },
                    instructions = state.instructions.trim(),
                    isDefault = false,
                    createdAt = state.createdAt
                )
                val comps = state.components.mapIndexed { index, comp ->
                    val rawQty = if (panattoni.simo.domain.MeasurementUnit.isQb(comp.unit)){
                        1.0
                    } else if (panattoni.simo.domain.MeasurementUnit.isIntegerUnit(comp.unit)){
                        panattoni.simo.domain.MeasurementUnit.roundIntegerAmount(NumberUtils.parseDoubleSafe(comp.quantity, 1.0)).toDouble()
                    } else {
                        NumberUtils.parseDoubleSafe(comp.quantity, 1.0)
                    }
                    RecipeComponent(
                        parentRecipeId = state.recipeId,
                        childItemId = comp.itemId,
                        baseQuantity = rawQty,
                        unit = comp.unit.trim().ifEmpty { "g" },
                        sortOrder = index
                    )
                }
                repository.saveRecipe(recipe, comps)
                _uiState.value = state.copy(isSaved = true, errorMessage = null)
                onSuccess()
            } catch (e: Exception){
                _uiState.value = state.copy(errorMessage = "Errore durante il salvataggio nel database: ${e.message}")
            }
        }
    }

    fun updateComponentQuantity(tempId: String, newQuantity: String){
        val updated = _uiState.value.components.map { comp ->
            if (comp.tempId == tempId){
                comp.copy(quantity = newQuantity)
            } else comp
        }
        _uiState.value = _uiState.value.copy(components = updated)
    }

    fun updateComponentDetails(
        tempId: String,
        newName: String,
        newQuantity: String,
        newUnit: String,
        newPrice: Double?,
        newPriceUnit: String?,
        newAllergens: List<Allergen>
    ){
        val trimmedName = newName.trim()
        val effectiveUnit = newUnit.trim().ifEmpty{ "g" }
        val isQb = panattoni.simo.domain.MeasurementUnit.isQb(effectiveUnit)
        val isInteger = panattoni.simo.domain.MeasurementUnit.isIntegerUnit(effectiveUnit)

        val sanitizedQty = if (isQb || newQuantity.isBlank()){
            ""
        } else if (isInteger){
            val parsed = NumberUtils.parseDoubleSafe(newQuantity, 1.0)
            panattoni.simo.domain.MeasurementUnit.roundIntegerAmount(parsed).toString()
        } else{
            newQuantity.trim()
        }

        val targetComp = _uiState.value.components.firstOrNull{ it.tempId == tempId } ?: return
        val finalName = if (trimmedName.isNotBlank()) trimmedName else targetComp.itemName

        val updated = _uiState.value.components.map{ comp ->
            if (comp.tempId == tempId){
                comp.copy(
                    itemName = finalName,
                    quantity = sanitizedQty,
                    unit = effectiveUnit,
                    pricePerUnit = newPrice,
                    priceUnit = newPriceUnit,
                    allergens = newAllergens
                )
            } else comp
        }
        _uiState.value = _uiState.value.copy(components = updated)

        if (targetComp.itemType != ItemType.RECIPE){
            viewModelScope.launch{
                try{
                    val existingItem = repository.allItems.first().firstOrNull{ it.id == targetComp.itemId }
                    val allergensStr = Allergen.formatAllergensString(newAllergens)
                    val updatedItem = if (existingItem != null){
                        existingItem.copy(
                            name = finalName,
                            defaultUnit = effectiveUnit,
                            pricePerUnit = newPrice,
                            priceUnit = newPriceUnit,
                            allergens = allergensStr
                        )
                    } else{
                        Item(
                            id = targetComp.itemId,
                            name = finalName,
                            type = ItemType.RAW_INGREDIENT,
                            defaultUnit = effectiveUnit,
                            pricePerUnit = newPrice,
                            priceUnit = newPriceUnit,
                            allergens = allergensStr
                        )
                    }
                    repository.saveItem(updatedItem)
                    _uiState.value = _uiState.value.copy(
                        availableItems = repository.allItems.first()
                    )
                } catch (_: Exception){}
            }
        }
    }

    // --- Gemini AI Assistant Integration ---

    private val geminiService = GeminiRecipeService()
    private val prefs = getApplication<Application>().getSharedPreferences("simo_prefs", android.content.Context.MODE_PRIVATE)

    fun getGeminiApiKey(): String {
        // 1. Prova a leggere la chiave crittografata con KeyStore hardware AES-256 GCM
        val encrypted = prefs.getString("gemini_api_key_enc", null)
        if (!encrypted.isNullOrBlank()){
            val decrypted = panattoni.simo.util.CryptoUtils.decrypt(encrypted)
            if (decrypted.isNotBlank()) return decrypted
        }

        // 2. Retrocompatibilità e Migrazione: se esiste una chiave memorizzata in chiaro, cifrala e rimuovi quella in chiaro
        val legacyPlainKey = prefs.getString("gemini_api_key", null)?.trim()
        if (!legacyPlainKey.isNullOrBlank()){
            saveGeminiApiKey(legacyPlainKey)
            prefs.edit().remove("gemini_api_key").apply()
            return legacyPlainKey
        }

        return ""
    }

    fun saveGeminiApiKey(key: String){
        val trimmed = key.trim()
        if (trimmed.isBlank()){
            prefs.edit()
                .remove("gemini_api_key_enc")
                .remove("gemini_api_key")
                .apply()
        } else {
            val encrypted = panattoni.simo.util.CryptoUtils.encrypt(trimmed)
            prefs.edit()
                .putString("gemini_api_key_enc", encrypted)
                .remove("gemini_api_key")
                .apply()
        }
    }

    fun getGeminiPreferredModel(): String =
        prefs.getString("gemini_preferred_model", panattoni.simo.data.remote.GeminiModel.DEFAULT.modelId)
            ?: panattoni.simo.data.remote.GeminiModel.DEFAULT.modelId

    fun saveGeminiPreferredModel(modelId: String){
        prefs.edit().putString("gemini_preferred_model", modelId.trim()).apply()
    }

    fun saveGeminiSettings(apiKey: String, modelId: String){
        saveGeminiApiKey(apiKey)
        saveGeminiPreferredModel(modelId)
    }

    private suspend fun applyGeminiIngredients(
        ingredients: List<GeminiIngredient>,
        clearExisting: Boolean
    ){
        val existingAllItems = repository.allItems.first().associateBy { it.name.trim().lowercase() }
        val newComponents = if (clearExisting) mutableListOf() else _uiState.value.components.toMutableList()

        for (gemIng in ingredients){
            val key = gemIng.name.trim().lowercase()
            val existingItem = existingAllItems[key]

            val targetItem = if (existingItem != null){
                val updatedItem = existingItem.copy(
                    pricePerUnit = gemIng.pricePerUnit ?: existingItem.pricePerUnit,
                    priceUnit = gemIng.priceUnit ?: existingItem.priceUnit,
                    allergens = if (gemIng.allergens.isNotEmpty()) Allergen.formatAllergensString(gemIng.allergens) else existingItem.allergens
                )
                repository.saveItem(updatedItem)
                updatedItem
            } else {
                val newItem = Item(
                    id = "custom_" + UUID.randomUUID().toString().take(8),
                    name = gemIng.name.trim(),
                    type = ItemType.RAW_INGREDIENT,
                    defaultUnit = gemIng.unit,
                    pricePerUnit = gemIng.pricePerUnit,
                    priceUnit = gemIng.priceUnit,
                    allergens = Allergen.formatAllergensString(gemIng.allergens)
                )
                repository.saveItem(newItem)
                newItem
            }

            val formattedQty = if (panattoni.simo.domain.MeasurementUnit.isQb(gemIng.unit)){
                ""
            } else if (panattoni.simo.domain.MeasurementUnit.isIntegerUnit(gemIng.unit)){
                panattoni.simo.domain.MeasurementUnit.roundIntegerAmount(gemIng.quantity).toString()
            } else {
                NumberUtils.formatAmount(gemIng.quantity)
            }

            val existingIndex = newComponents.indexOfFirst { it.itemId == targetItem.id }
            if (existingIndex >= 0){
                newComponents[existingIndex] = newComponents[existingIndex].copy(
                    quantity = if (formattedQty.isNotBlank()) formattedQty else newComponents[existingIndex].quantity,
                    unit = gemIng.unit.ifBlank { newComponents[existingIndex].unit },
                    pricePerUnit = gemIng.pricePerUnit ?: newComponents[existingIndex].pricePerUnit,
                    priceUnit = gemIng.priceUnit ?: newComponents[existingIndex].priceUnit,
                    allergens = if (gemIng.allergens.isNotEmpty()) gemIng.allergens else newComponents[existingIndex].allergens
                )
            } else {
                newComponents.add(
                    EditableComponent(
                        itemId = targetItem.id,
                        itemName = targetItem.name,
                        itemType = targetItem.type,
                        quantity = formattedQty,
                        unit = gemIng.unit,
                        pricePerUnit = targetItem.pricePerUnit,
                        priceUnit = targetItem.priceUnit,
                        allergens = targetItem.allergensList
                    )
                )
            }
        }

        _uiState.value = _uiState.value.copy(
            components = newComponents,
            availableItems = repository.allItems.first()
        )
    }

    fun generateRecipeWithGemini(userCustomPrompt: String? = null, onComplete: () -> Unit = {}){
        val state = _uiState.value
        if (state.name.isBlank()){
            _uiState.value = state.copy(errorMessage = "Inserisci prima il nome della ricetta per consentire a Gemini di elaborarla.")
            return
        }

        val apiKey = getGeminiApiKey()
        if (apiKey.isBlank()){
            _uiState.value = state.copy(errorMessage = "API_KEY_REQUIRED")
            return
        }

        _uiState.value = state.copy(isAiLoading = true, errorMessage = null, infoMessage = null)

        val preferredModel = getGeminiPreferredModel()
        viewModelScope.launch {
            val result = geminiService.generateFullRecipe(
                apiKey = apiKey,
                recipeName = state.name,
                description = state.description.takeIf { it.isNotBlank() },
                category = state.category,
                baseYield = state.baseYield,
                yieldUnit = state.yieldUnit,
                userCustomPrompt = userCustomPrompt,
                preferredModel = preferredModel
            )

            result.onSuccess { response ->
                applyGeminiIngredients(response.ingredients, clearExisting = true)

                val newDesc = if (state.description.isBlank()) response.description ?: state.description else state.description
                val newCategory = response.category?.let{ cat ->
                    if (cat.isNotBlank()) RecipeTags.sanitizeTag(cat) else state.category
                } ?: state.category
                val tagsInput = if (newCategory.isNotBlank()){
                    otherRecipeCategories + newCategory.trim()
                } else{
                    otherRecipeCategories
                }
                val updatedTags = RecipeTags.getAllSortedTags(tagsInput)
                val newYield = response.baseYield?.let{ NumberUtils.formatAmount(it) } ?: state.baseYield
                val newYieldUnit = response.yieldUnit ?: state.yieldUnit
                val newInstructions = response.instructions ?: state.instructions

                _uiState.value = _uiState.value.copy(
                    description = newDesc,
                    category = newCategory,
                    availableTags = updatedTags,
                    baseYield = newYield,
                    yieldUnit = newYieldUnit,
                    instructions = newInstructions,
                    isAiLoading = false,
                    infoMessage = "✨ Ricetta, dosi, costi, allergeni e passaggi generati con successo da Gemini!"
                )
                onComplete()
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isAiLoading = false,
                    errorMessage = "Errore Gemini: ${err.message}"
                )
            }
        }
    }

    fun completeMissingWithGemini(userCustomPrompt: String? = null, onComplete: () -> Unit = {}){
        val state = _uiState.value
        if (state.name.isBlank()){
            _uiState.value = state.copy(errorMessage = "Inserisci prima il nome della ricetta.")
            return
        }

        val apiKey = getGeminiApiKey()
        if (apiKey.isBlank()){
            _uiState.value = state.copy(errorMessage = "API_KEY_REQUIRED")
            return
        }

        _uiState.value = state.copy(isAiLoading = true, errorMessage = null, infoMessage = null)

        val preferredModel = getGeminiPreferredModel()
        viewModelScope.launch {
            val existingGeminiIngs = state.components.map { comp ->
                val parsedQty = NumberUtils.parseDoubleSafe(comp.quantity, 0.0)
                GeminiIngredient(
                    name = comp.itemName,
                    quantity = parsedQty,
                    unit = comp.unit,
                    pricePerUnit = comp.pricePerUnit,
                    priceUnit = comp.priceUnit,
                    allergens = comp.allergens
                )
            }

            val result = geminiService.completeMissingIngredients(
                apiKey = apiKey,
                recipeName = state.name,
                description = state.description.takeIf { it.isNotBlank() },
                baseYield = NumberUtils.parseDoubleSafe(state.baseYield, 1.0),
                yieldUnit = state.yieldUnit,
                existingIngredients = existingGeminiIngs,
                userCustomPrompt = userCustomPrompt,
                preferredModel = preferredModel
            )

            result.onSuccess { response ->
                applyGeminiIngredients(response.ingredients, clearExisting = false)

                val newInstructions = if (state.instructions.isBlank() && !response.instructions.isNullOrBlank()){
                    response.instructions
                } else state.instructions

                _uiState.value = _uiState.value.copy(
                    instructions = newInstructions,
                    isAiLoading = false,
                    infoMessage = "✨ Pesi, dosi, costi e allergeni mancanti completati da Gemini!"
                )
                onComplete()
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isAiLoading = false,
                    errorMessage = "Errore Gemini: ${err.message}"
                )
            }
        }
    }

    fun generateInstructionsWithGemini(userCustomPrompt: String? = null, onComplete: () -> Unit = {}){
        val state = _uiState.value
        if (state.name.isBlank()){
            _uiState.value = state.copy(errorMessage = "Inserisci il nome della ricetta.")
            return
        }
        if (state.components.isEmpty()){
            _uiState.value = state.copy(errorMessage = "Aggiungi prima gli ingredienti per poter generare i passaggi.")
            return
        }

        val apiKey = getGeminiApiKey()
        if (apiKey.isBlank()){
            _uiState.value = state.copy(errorMessage = "API_KEY_REQUIRED")
            return
        }

        _uiState.value = state.copy(isAiLoading = true, errorMessage = null, infoMessage = null)

        val preferredModel = getGeminiPreferredModel()
        viewModelScope.launch {
            val existingGeminiIngs = state.components.map { comp ->
                val parsedQty = NumberUtils.parseDoubleSafe(comp.quantity, 0.0)
                GeminiIngredient(
                    name = comp.itemName,
                    quantity = parsedQty,
                    unit = comp.unit,
                    pricePerUnit = comp.pricePerUnit,
                    priceUnit = comp.priceUnit,
                    allergens = comp.allergens
                )
            }

            val result = geminiService.generateInstructionsOnly(
                apiKey = apiKey,
                recipeName = state.name,
                ingredients = existingGeminiIngs,
                userCustomPrompt = userCustomPrompt,
                preferredModel = preferredModel
            )

            result.onSuccess { instructions ->
                _uiState.value = _uiState.value.copy(
                    instructions = instructions,
                    isAiLoading = false,
                    infoMessage = "✨ Passaggi della ricetta generati con successo!"
                )
                onComplete()
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isAiLoading = false,
                    errorMessage = "Errore Gemini: ${err.message}"
                )
            }
        }
    }

    fun clearError(){
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearInfoMessage(){
        _uiState.value = _uiState.value.copy(infoMessage = null)
    }
}
