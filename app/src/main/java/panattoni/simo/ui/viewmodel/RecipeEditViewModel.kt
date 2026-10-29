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
import panattoni.simo.data.remote.AiProvider
import panattoni.simo.data.remote.OpenAiModel
import panattoni.simo.data.remote.OpenAiRecipeService
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
    val allergens: List<Allergen> = emptyList(),
    // Valori nutrizionali per 100g di ingrediente
    val caloriesKcal: Double? = null,
    val carbohydratesG: Double? = null,
    val fatsG: Double? = null,
    val proteinsG: Double? = null,
    val waterG: Double? = null,
    val sodiumMg: Double? = null
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
    val createdAt: Long = System.currentTimeMillis(),
    // Modificatore resa/peso in cottura (%: es. -12.0 per evaporazione o +120.0 per resa pasta/riso)
    val cookingWeightModifier: Double = 0.0,
    val cookingWeightModifierInput: String = "0"
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
                                allergens = detail.item.allergensList,
                                caloriesKcal = detail.item.caloriesKcal,
                                carbohydratesG = detail.item.carbohydratesG,
                                fatsG = detail.item.fatsG,
                                proteinsG = detail.item.proteinsG,
                                waterG = detail.item.waterG,
                                sodiumMg = detail.item.sodiumMg
                            )
                        }
                        val modVal = current.recipe.cookingWeightModifier
                        val modInput = if (modVal == 0.0) "0" else String.format(java.util.Locale.US, "%.1f", modVal).replace(".0", "")
                        _uiState.value = _uiState.value.copy(
                            name = current.recipe.name,
                            description = current.recipe.description,
                            category = current.recipe.category,
                            isFavorite = current.recipe.isFavorite,
                            baseYield = NumberUtils.formatAmount(current.recipe.baseYieldQuantity),
                            yieldUnit = current.recipe.yieldUnit,
                            instructions = current.recipe.instructions,
                            components = compList,
                            createdAt = current.recipe.createdAt,
                            cookingWeightModifier = modVal,
                            cookingWeightModifierInput = modInput
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

    // Aggiorna il modificatore di peso in cottura (% es. -12.0 per evaporazione o +120.0 per pasta)
    fun onCookingWeightModifierChange(input: String){
        val parsed = NumberUtils.parseDoubleSafe(input, 0.0)
        _uiState.value = _uiState.value.copy(
            cookingWeightModifier = parsed,
            cookingWeightModifierInput = input
        )
    }

    /**
     * Armonizza l'unità di prezzo con l'unità di misura per evitare incongruenze
     * tra massa e volume (es. se la dose è in ml ma il prezzo è al kg, normalizza l'unità di prezzo a 'l' a parità di valore 1:1).
     */
    private fun harmonizePriceUnit(unit: String, priceUnit: String?): String?{
        if (priceUnit.isNullOrBlank()) return priceUnit
        val cleanUnit = unit.trim().lowercase()
        val cleanPriceUnit = priceUnit.trim().lowercase()
        return when{
            cleanUnit in setOf("ml", "l") && cleanPriceUnit in setOf("kg", "chilogrammi", "chilo", "chili") -> "l"
            cleanUnit in setOf("ml", "l") && cleanPriceUnit in setOf("g", "grammi", "grammo") -> "ml"
            cleanUnit in setOf("g", "kg") && cleanPriceUnit in setOf("l", "litri", "litro") -> "kg"
            cleanUnit in setOf("g", "kg") && cleanPriceUnit in setOf("ml", "millilitri", "millilitro") -> "g"
            else -> priceUnit
        }
    }

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
        val rawPriceUnit = priceUnit ?: item.priceUnit
        val effectivePriceUnit = harmonizePriceUnit(effectiveUnit, rawPriceUnit)
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
                    createdAt = state.createdAt,
                    cookingWeightModifier = state.cookingWeightModifier
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
        newAllergens: List<Allergen>,
        newCalories: Double? = null,
        newCarbs: Double? = null,
        newFats: Double? = null,
        newProteins: Double? = null,
        newWater: Double? = null,
        newSodium: Double? = null
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
                    allergens = newAllergens,
                    caloriesKcal = newCalories,
                    carbohydratesG = newCarbs,
                    fatsG = newFats,
                    proteinsG = newProteins,
                    waterG = newWater,
                    sodiumMg = newSodium
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
                            allergens = allergensStr,
                            caloriesKcal = newCalories,
                            carbohydratesG = newCarbs,
                            fatsG = newFats,
                            proteinsG = newProteins,
                            waterG = newWater,
                            sodiumMg = newSodium
                        )
                    } else{
                        Item(
                            id = targetComp.itemId,
                            name = finalName,
                            type = ItemType.RAW_INGREDIENT,
                            defaultUnit = effectiveUnit,
                            pricePerUnit = newPrice,
                            priceUnit = newPriceUnit,
                            allergens = allergensStr,
                            caloriesKcal = newCalories,
                            carbohydratesG = newCarbs,
                            fatsG = newFats,
                            proteinsG = newProteins,
                            waterG = newWater,
                            sodiumMg = newSodium
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

    // --- Gestione Provider AI (Gemini & OpenAI ChatGPT) ---

    private val geminiService = GeminiRecipeService()
    private val openAiService = OpenAiRecipeService()
    private val prefs = getApplication<Application>().getSharedPreferences("simo_prefs", android.content.Context.MODE_PRIVATE)

    /**
     * Ritorna il provider AI preferito dall'utente (Google Gemini o OpenAI ChatGPT).
     */
    fun getPreferredAiProvider(): AiProvider{
        val id = prefs.getString("ai_preferred_provider", AiProvider.DEFAULT.id)
        return AiProvider.fromId(id)
    }

    /**
     * Salva il provider AI preferito.
     */
    fun savePreferredAiProvider(provider: AiProvider){
        prefs.edit().putString("ai_preferred_provider", provider.id).apply()
    }

    /**
     * Recupera la chiave API di Gemini decifrandola tramite Android KeyStore hardware (AES-256 GCM).
     */
    fun getGeminiApiKey(): String{
        val encrypted = prefs.getString("gemini_api_key_enc", null)
        if (!encrypted.isNullOrBlank()){
            val decrypted = panattoni.simo.util.CryptoUtils.decrypt(encrypted)
            if (decrypted.isNotBlank()) return decrypted
        }

        // Retrocompatibilità con vecchie installazioni
        val legacyPlainKey = prefs.getString("gemini_api_key", null)?.trim()
        if (!legacyPlainKey.isNullOrBlank()){
            saveGeminiApiKey(legacyPlainKey)
            prefs.edit().remove("gemini_api_key").apply()
            return legacyPlainKey
        }
        return ""
    }

    /**
     * Cifra la chiave API di Gemini e la salva in modo sicuro.
     */
    fun saveGeminiApiKey(key: String){
        val trimmed = key.trim()
        if (trimmed.isBlank()){
            prefs.edit()
                .remove("gemini_api_key_enc")
                .remove("gemini_api_key")
                .apply()
        } else{
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

    /**
     * Recupera la chiave API di OpenAI (ChatGPT) decifrandola con Android KeyStore hardware (AES-256 GCM).
     */
    fun getOpenAiApiKey(): String{
        val encrypted = prefs.getString("openai_api_key_enc", null)
        if (!encrypted.isNullOrBlank()){
            val decrypted = panattoni.simo.util.CryptoUtils.decrypt(encrypted)
            if (decrypted.isNotBlank()) return decrypted
        }

        val legacyPlainKey = prefs.getString("openai_api_key", null)?.trim()
        if (!legacyPlainKey.isNullOrBlank()){
            saveOpenAiApiKey(legacyPlainKey)
            prefs.edit().remove("openai_api_key").apply()
            return legacyPlainKey
        }
        return ""
    }

    /**
     * Cifra la chiave API di OpenAI e la salva in modo sicuro.
     */
    fun saveOpenAiApiKey(key: String){
        val trimmed = key.trim()
        if (trimmed.isBlank()){
            prefs.edit()
                .remove("openai_api_key_enc")
                .remove("openai_api_key")
                .apply()
        } else{
            val encrypted = panattoni.simo.util.CryptoUtils.encrypt(trimmed)
            prefs.edit()
                .putString("openai_api_key_enc", encrypted)
                .remove("openai_api_key")
                .apply()
        }
    }

    fun getOpenAiPreferredModel(): String =
        prefs.getString("openai_preferred_model", panattoni.simo.data.remote.OpenAiModel.DEFAULT.modelId)
            ?: panattoni.simo.data.remote.OpenAiModel.DEFAULT.modelId

    fun saveOpenAiPreferredModel(modelId: String){
        prefs.edit().putString("openai_preferred_model", modelId.trim()).apply()
    }

    /**
     * Salva simultaneamente tutte le impostazioni AI (Provider, chiavi cifrate e modelli selezionati).
     */
    fun saveAiSettings(
        provider: AiProvider,
        geminiKey: String,
        geminiModelId: String,
        openAiKey: String,
        openAiModelId: String
    ){
        savePreferredAiProvider(provider)
        saveGeminiApiKey(geminiKey)
        saveGeminiPreferredModel(geminiModelId)
        saveOpenAiApiKey(openAiKey)
        saveOpenAiPreferredModel(openAiModelId)
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
            val resolvedPriceUnit = harmonizePriceUnit(gemIng.unit, gemIng.priceUnit)

            val targetItem = if (existingItem != null){
                val updatedItem = existingItem.copy(
                    pricePerUnit = gemIng.pricePerUnit ?: existingItem.pricePerUnit,
                    priceUnit = resolvedPriceUnit ?: existingItem.priceUnit,
                    allergens = if (gemIng.allergens.isNotEmpty()) Allergen.formatAllergensString(gemIng.allergens) else existingItem.allergens,
                    caloriesKcal = gemIng.caloriesKcal ?: existingItem.caloriesKcal,
                    carbohydratesG = gemIng.carbohydratesG ?: existingItem.carbohydratesG,
                    fatsG = gemIng.fatsG ?: existingItem.fatsG,
                    proteinsG = gemIng.proteinsG ?: existingItem.proteinsG,
                    waterG = gemIng.waterG ?: existingItem.waterG,
                    sodiumMg = gemIng.sodiumMg ?: existingItem.sodiumMg
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
                    priceUnit = resolvedPriceUnit,
                    allergens = Allergen.formatAllergensString(gemIng.allergens),
                    caloriesKcal = gemIng.caloriesKcal,
                    carbohydratesG = gemIng.carbohydratesG,
                    fatsG = gemIng.fatsG,
                    proteinsG = gemIng.proteinsG,
                    waterG = gemIng.waterG,
                    sodiumMg = gemIng.sodiumMg
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
                    priceUnit = resolvedPriceUnit ?: newComponents[existingIndex].priceUnit,
                    allergens = if (gemIng.allergens.isNotEmpty()) gemIng.allergens else newComponents[existingIndex].allergens,
                    caloriesKcal = gemIng.caloriesKcal ?: newComponents[existingIndex].caloriesKcal,
                    carbohydratesG = gemIng.carbohydratesG ?: newComponents[existingIndex].carbohydratesG,
                    fatsG = gemIng.fatsG ?: newComponents[existingIndex].fatsG,
                    proteinsG = gemIng.proteinsG ?: newComponents[existingIndex].proteinsG,
                    waterG = gemIng.waterG ?: newComponents[existingIndex].waterG,
                    sodiumMg = gemIng.sodiumMg ?: newComponents[existingIndex].sodiumMg
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
                        allergens = targetItem.allergensList,
                        caloriesKcal = targetItem.caloriesKcal,
                        carbohydratesG = targetItem.carbohydratesG,
                        fatsG = targetItem.fatsG,
                        proteinsG = targetItem.proteinsG,
                        waterG = targetItem.waterG,
                        sodiumMg = targetItem.sodiumMg
                    )
                )
            }
        }

        _uiState.value = _uiState.value.copy(
            components = newComponents,
            availableItems = repository.allItems.first()
        )
    }

    /**
     * Applica ESCLUSIVAMENTE i 6 valori nutrizionali restituiti dall'AI agli ingredienti esistenti.
     * Preserva in modo assoluto pesature (quantity), unità (unit), costi unitari (pricePerUnit/priceUnit) ed allergeni.
     */
    private suspend fun applyNutritionalValuesOnly(
        ingredients: List<GeminiIngredient>
    ){
        val allItemsMap = repository.allItems.first().associateBy{ it.name.trim().lowercase() }
        val currentComponents = _uiState.value.components.toMutableList()

        for (i in currentComponents.indices){
            val comp = currentComponents[i]
            val compNameClean = comp.itemName.trim().lowercase()

            // Trova l'ingrediente corrispondente restituito dall'AI
            val match = ingredients.find{ it.name.trim().lowercase() == compNameClean }
                ?: ingredients.find{ compNameClean.contains(it.name.trim().lowercase()) || it.name.trim().lowercase().contains(compNameClean) }

            if (match != null){
                // Aggiorna solo i valori nutrizionali, preservando quantità, unità, costi e allergeni esistenti
                val updatedComp = comp.copy(
                    caloriesKcal = match.caloriesKcal ?: comp.caloriesKcal,
                    carbohydratesG = match.carbohydratesG ?: comp.carbohydratesG,
                    fatsG = match.fatsG ?: comp.fatsG,
                    proteinsG = match.proteinsG ?: comp.proteinsG,
                    waterG = match.waterG ?: comp.waterG,
                    sodiumMg = match.sodiumMg ?: comp.sodiumMg
                )
                currentComponents[i] = updatedComp

                // Aggiorna anche l'entità Item nel database Room
                val existingItem = allItemsMap[compNameClean] ?: repository.allItems.first().find{ it.id == comp.itemId }
                if (existingItem != null){
                    val updatedItem = existingItem.copy(
                        caloriesKcal = match.caloriesKcal ?: existingItem.caloriesKcal,
                        carbohydratesG = match.carbohydratesG ?: existingItem.carbohydratesG,
                        fatsG = match.fatsG ?: existingItem.fatsG,
                        proteinsG = match.proteinsG ?: existingItem.proteinsG,
                        waterG = match.waterG ?: existingItem.waterG,
                        sodiumMg = match.sodiumMg ?: existingItem.sodiumMg
                    )
                    repository.saveItem(updatedItem)
                }
            }
        }

        _uiState.value = _uiState.value.copy(
            components = currentComponents,
            availableItems = repository.allItems.first()
        )
    }

    /**
     * Completa gli ingredienti esistenti SENZA aggiungerne di nuovi né rimuoverne.
     * Completa: pesature (solo se mancanti o <= 0), costi (se mancanti), allergeni e tutti i 6 valori nutrizionali.
     */
    private suspend fun applyCompletedMissingIngredients(
        ingredients: List<GeminiIngredient>
    ){
        val allItemsMap = repository.allItems.first().associateBy{ it.name.trim().lowercase() }
        val currentComponents = _uiState.value.components.toMutableList()

        for (i in currentComponents.indices){
            val comp = currentComponents[i]
            val compNameClean = comp.itemName.trim().lowercase()

            val match = ingredients.find{ it.name.trim().lowercase() == compNameClean }
                ?: ingredients.find{ compNameClean.contains(it.name.trim().lowercase()) || it.name.trim().lowercase().contains(compNameClean) }

            if (match != null){
                val existingQtyParsed = NumberUtils.parseDoubleSafe(comp.quantity, 0.0)

                // Preserva la quantità se già definita dall'utente (> 0)
                val finalQuantity = if (existingQtyParsed > 0.0){
                    comp.quantity
                } else if (panattoni.simo.domain.MeasurementUnit.isQb(match.unit)){
                    ""
                } else if (panattoni.simo.domain.MeasurementUnit.isIntegerUnit(match.unit)){
                    panattoni.simo.domain.MeasurementUnit.roundIntegerAmount(match.quantity).toString()
                } else{
                    NumberUtils.formatAmount(match.quantity)
                }

                val finalUnit = if (comp.unit.isNotBlank() && existingQtyParsed > 0.0){
                    comp.unit
                } else{
                    match.unit.ifBlank{ comp.unit }
                }

                val finalPrice = comp.pricePerUnit ?: match.pricePerUnit
                val rawPriceUnit = comp.priceUnit ?: match.priceUnit
                val finalPriceUnit = harmonizePriceUnit(finalUnit, rawPriceUnit)
                val finalAllergens = if (comp.allergens.isNotEmpty()) comp.allergens else match.allergens

                val updatedComp = comp.copy(
                    quantity = finalQuantity,
                    unit = finalUnit,
                    pricePerUnit = finalPrice,
                    priceUnit = finalPriceUnit,
                    allergens = finalAllergens,
                    caloriesKcal = match.caloriesKcal ?: comp.caloriesKcal,
                    carbohydratesG = match.carbohydratesG ?: comp.carbohydratesG,
                    fatsG = match.fatsG ?: comp.fatsG,
                    proteinsG = match.proteinsG ?: comp.proteinsG,
                    waterG = match.waterG ?: comp.waterG,
                    sodiumMg = match.sodiumMg ?: comp.sodiumMg
                )
                currentComponents[i] = updatedComp

                // Aggiorna l'Item nel database Room
                val existingItem = allItemsMap[compNameClean] ?: repository.allItems.first().find{ it.id == comp.itemId }
                if (existingItem != null){
                    val updatedItem = existingItem.copy(
                        pricePerUnit = finalPrice ?: existingItem.pricePerUnit,
                        priceUnit = finalPriceUnit ?: existingItem.priceUnit,
                        allergens = if (finalAllergens.isNotEmpty()) Allergen.formatAllergensString(finalAllergens) else existingItem.allergens,
                        caloriesKcal = match.caloriesKcal ?: existingItem.caloriesKcal,
                        carbohydratesG = match.carbohydratesG ?: existingItem.carbohydratesG,
                        fatsG = match.fatsG ?: existingItem.fatsG,
                        proteinsG = match.proteinsG ?: existingItem.proteinsG,
                        waterG = match.waterG ?: existingItem.waterG,
                        sodiumMg = match.sodiumMg ?: existingItem.sodiumMg
                    )
                    repository.saveItem(updatedItem)
                }
            }
        }

        _uiState.value = _uiState.value.copy(
            components = currentComponents,
            availableItems = repository.allItems.first()
        )
    }

    /**
     * Genera una ricetta completa invocando il provider AI selezionato (Gemini o ChatGPT),
     * con fallback automatico all'altro provider in caso di esaurimento quota o errore transitorio.
     */
    fun generateRecipeWithAi(userCustomPrompt: String? = null, onComplete: () -> Unit = {}){
        val state = _uiState.value
        if (state.name.isBlank()){
            _uiState.value = state.copy(errorMessage = "Inserisci prima il nome della ricetta per consentire all'AI di elaborarla.")
            return
        }

        val preferredProvider = getPreferredAiProvider()
        val geminiKey = getGeminiApiKey()
        val openAiKey = getOpenAiApiKey()

        val primaryKey = if (preferredProvider == AiProvider.OPENAI) openAiKey else geminiKey
        val secondaryKey = if (preferredProvider == AiProvider.OPENAI) geminiKey else openAiKey

        if (primaryKey.isBlank() && secondaryKey.isBlank()){
            _uiState.value = state.copy(errorMessage = "API_KEY_REQUIRED")
            return
        }

        _uiState.value = state.copy(isAiLoading = true, errorMessage = null, infoMessage = null)

        viewModelScope.launch{
            val useOpenAiPrimary = (preferredProvider == AiProvider.OPENAI && openAiKey.isNotBlank()) || geminiKey.isBlank()

            val primaryResult = if (useOpenAiPrimary){
                openAiService.generateRecipe(
                    apiKey = openAiKey,
                    recipeName = state.name,
                    baseYield = state.baseYield,
                    yieldUnit = state.yieldUnit,
                    customPrompt = userCustomPrompt,
                    preferredModel = getOpenAiPreferredModel()
                )
            } else{
                geminiService.generateFullRecipe(
                    apiKey = geminiKey,
                    recipeName = state.name,
                    description = state.description.takeIf{ it.isNotBlank() },
                    category = state.category,
                    baseYield = state.baseYield,
                    yieldUnit = state.yieldUnit,
                    userCustomPrompt = userCustomPrompt,
                    preferredModel = getGeminiPreferredModel()
                )
            }

            var finalResult = primaryResult
            var providerUsed = if (useOpenAiPrimary) "ChatGPT (${getOpenAiPreferredModel()})" else "Gemini (${getGeminiPreferredModel()})"

            // Fallback automatico al provider secondario se il primario ha fallito ed è presente l'altra chiave
            if (primaryResult.isFailure){
                val canFallback = if (useOpenAiPrimary) geminiKey.isNotBlank() else openAiKey.isNotBlank()
                if (canFallback){
                    val fallbackResult = if (useOpenAiPrimary){
                        geminiService.generateFullRecipe(
                            apiKey = geminiKey,
                            recipeName = state.name,
                            description = state.description.takeIf{ it.isNotBlank() },
                            category = state.category,
                            baseYield = state.baseYield,
                            yieldUnit = state.yieldUnit,
                            userCustomPrompt = userCustomPrompt,
                            preferredModel = getGeminiPreferredModel()
                        )
                    } else{
                        openAiService.generateRecipe(
                            apiKey = openAiKey,
                            recipeName = state.name,
                            baseYield = state.baseYield,
                            yieldUnit = state.yieldUnit,
                            customPrompt = userCustomPrompt,
                            preferredModel = getOpenAiPreferredModel()
                        )
                    }
                    if (fallbackResult.isSuccess){
                        finalResult = fallbackResult
                        providerUsed = if (useOpenAiPrimary) "Gemini (fallback automatico)" else "ChatGPT (fallback automatico)"
                    }
                }
            }

            finalResult.onSuccess{ response ->
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
                // Unità di resa: l'AI mantiene l'unità scelta dall'utente se idonea alla ricetta, altrimenti torna a porzioni
                val newYieldUnit = response.yieldUnit?.trim()?.takeIf{ it.isNotBlank() } ?: state.yieldUnit
                val newInstructions = response.instructions ?: state.instructions
                val newModifier = response.cookingWeightModifier ?: state.cookingWeightModifier
                val newModInput = if (newModifier == 0.0) "0" else String.format(java.util.Locale.US, "%.1f", newModifier).replace(".0", "")

                _uiState.value = _uiState.value.copy(
                    description = newDesc,
                    category = newCategory,
                    availableTags = updatedTags,
                    baseYield = newYield,
                    yieldUnit = newYieldUnit,
                    instructions = newInstructions,
                    cookingWeightModifier = newModifier,
                    cookingWeightModifierInput = newModInput,
                    isAiLoading = false,
                    infoMessage = "✨ Ricetta, dosi, costi, allergeni, valori nutrizionali e passaggi generati con successo da $providerUsed!"
                )
                onComplete()
            }.onFailure{ err ->
                _uiState.value = _uiState.value.copy(
                    isAiLoading = false,
                    errorMessage = "Errore AI ($providerUsed): ${err.message}"
                )
            }
        }
    }

    /**
     * Completa ingredienti mancanti o senza dose bilanciandoli con l'AI.
     */
    fun completeMissingWithAi(userCustomPrompt: String? = null, onComplete: () -> Unit = {}){
        val state = _uiState.value
        if (state.name.isBlank()){
            _uiState.value = state.copy(errorMessage = "Inserisci prima il nome della ricetta.")
            return
        }

        val preferredProvider = getPreferredAiProvider()
        val geminiKey = getGeminiApiKey()
        val openAiKey = getOpenAiApiKey()

        val primaryKey = if (preferredProvider == AiProvider.OPENAI) openAiKey else geminiKey
        val secondaryKey = if (preferredProvider == AiProvider.OPENAI) geminiKey else openAiKey

        if (primaryKey.isBlank() && secondaryKey.isBlank()){
            _uiState.value = state.copy(errorMessage = "API_KEY_REQUIRED")
            return
        }

        _uiState.value = state.copy(isAiLoading = true, errorMessage = null, infoMessage = null)

        viewModelScope.launch{
            val existingGeminiIngs = state.components.map{ comp ->
                val parsedQty = NumberUtils.parseDoubleSafe(comp.quantity, 0.0)
                GeminiIngredient(
                    name = comp.itemName,
                    quantity = parsedQty,
                    unit = comp.unit,
                    pricePerUnit = comp.pricePerUnit,
                    priceUnit = comp.priceUnit,
                    allergens = comp.allergens,
                    caloriesKcal = comp.caloriesKcal,
                    carbohydratesG = comp.carbohydratesG,
                    fatsG = comp.fatsG,
                    proteinsG = comp.proteinsG,
                    waterG = comp.waterG,
                    sodiumMg = comp.sodiumMg
                )
            }

            val useOpenAiPrimary = (preferredProvider == AiProvider.OPENAI && openAiKey.isNotBlank()) || geminiKey.isBlank()

            val primaryResult = if (useOpenAiPrimary){
                openAiService.completeMissingIngredients(
                    apiKey = openAiKey,
                    recipeName = state.name,
                    baseYield = state.baseYield,
                    yieldUnit = state.yieldUnit,
                    existingIngredients = existingGeminiIngs,
                    customPrompt = userCustomPrompt,
                    preferredModel = getOpenAiPreferredModel()
                )
            } else{
                geminiService.completeMissingIngredients(
                    apiKey = geminiKey,
                    recipeName = state.name,
                    description = state.description.takeIf{ it.isNotBlank() },
                    baseYield = NumberUtils.parseDoubleSafe(state.baseYield, 1.0),
                    yieldUnit = state.yieldUnit,
                    existingIngredients = existingGeminiIngs,
                    userCustomPrompt = userCustomPrompt,
                    preferredModel = getGeminiPreferredModel()
                )
            }

            var finalResult = primaryResult
            var providerUsed = if (useOpenAiPrimary) "ChatGPT (${getOpenAiPreferredModel()})" else "Gemini (${getGeminiPreferredModel()})"

            if (primaryResult.isFailure){
                val canFallback = if (useOpenAiPrimary) geminiKey.isNotBlank() else openAiKey.isNotBlank()
                if (canFallback){
                    val fallbackResult = if (useOpenAiPrimary){
                        geminiService.completeMissingIngredients(
                            apiKey = geminiKey,
                            recipeName = state.name,
                            description = state.description.takeIf{ it.isNotBlank() },
                            baseYield = NumberUtils.parseDoubleSafe(state.baseYield, 1.0),
                            yieldUnit = state.yieldUnit,
                            existingIngredients = existingGeminiIngs,
                            userCustomPrompt = userCustomPrompt,
                            preferredModel = getGeminiPreferredModel()
                        )
                    } else{
                        openAiService.completeMissingIngredients(
                            apiKey = openAiKey,
                            recipeName = state.name,
                            baseYield = state.baseYield,
                            yieldUnit = state.yieldUnit,
                            existingIngredients = existingGeminiIngs,
                            customPrompt = userCustomPrompt,
                            preferredModel = getOpenAiPreferredModel()
                        )
                    }
                    if (fallbackResult.isSuccess){
                        finalResult = fallbackResult
                        providerUsed = if (useOpenAiPrimary) "Gemini (fallback automatico)" else "ChatGPT (fallback automatico)"
                    }
                }
            }

            finalResult.onSuccess{ response ->
                applyCompletedMissingIngredients(response.ingredients)

                val newInstructions = if (state.instructions.isBlank() && !response.instructions.isNullOrBlank()){
                    response.instructions
                } else state.instructions

                _uiState.value = _uiState.value.copy(
                    instructions = newInstructions,
                    isAiLoading = false,
                    infoMessage = "✨ Dosi mancanti, costi, allergeni e valori nutrizionali completati da $providerUsed!"
                )
                onComplete()
            }.onFailure{ err ->
                _uiState.value = _uiState.value.copy(
                    isAiLoading = false,
                    errorMessage = "Errore AI ($providerUsed): ${err.message}"
                )
            }
        }
    }

    /**
     * Genera esclusivamente il procedimento numerato passo-passo con l'AI.
     */
    fun generateInstructionsWithAi(userCustomPrompt: String? = null, onComplete: () -> Unit = {}){
        val state = _uiState.value
        if (state.name.isBlank()){
            _uiState.value = state.copy(errorMessage = "Inserisci il nome della ricetta.")
            return
        }
        if (state.components.isEmpty()){
            _uiState.value = state.copy(errorMessage = "Aggiungi prima gli ingredienti per poter generare i passaggi.")
            return
        }

        val preferredProvider = getPreferredAiProvider()
        val geminiKey = getGeminiApiKey()
        val openAiKey = getOpenAiApiKey()

        val primaryKey = if (preferredProvider == AiProvider.OPENAI) openAiKey else geminiKey
        val secondaryKey = if (preferredProvider == AiProvider.OPENAI) geminiKey else openAiKey

        if (primaryKey.isBlank() && secondaryKey.isBlank()){
            _uiState.value = state.copy(errorMessage = "API_KEY_REQUIRED")
            return
        }

        _uiState.value = state.copy(isAiLoading = true, errorMessage = null, infoMessage = null)

        viewModelScope.launch{
            val existingGeminiIngs = state.components.map{ comp ->
                val parsedQty = NumberUtils.parseDoubleSafe(comp.quantity, 0.0)
                GeminiIngredient(
                    name = comp.itemName,
                    quantity = parsedQty,
                    unit = comp.unit,
                    pricePerUnit = comp.pricePerUnit,
                    priceUnit = comp.priceUnit,
                    allergens = comp.allergens,
                    caloriesKcal = comp.caloriesKcal,
                    carbohydratesG = comp.carbohydratesG,
                    fatsG = comp.fatsG,
                    proteinsG = comp.proteinsG,
                    waterG = comp.waterG,
                    sodiumMg = comp.sodiumMg
                )
            }

            val useOpenAiPrimary = (preferredProvider == AiProvider.OPENAI && openAiKey.isNotBlank()) || geminiKey.isBlank()

            val primaryResult: Result<String> = if (useOpenAiPrimary){
                openAiService.generateInstructions(
                    apiKey = openAiKey,
                    recipeName = state.name,
                    baseYield = state.baseYield,
                    yieldUnit = state.yieldUnit,
                    ingredients = existingGeminiIngs,
                    customPrompt = userCustomPrompt,
                    preferredModel = getOpenAiPreferredModel()
                ).map{ it.instructions ?: "" }
            } else{
                geminiService.generateInstructionsOnly(
                    apiKey = geminiKey,
                    recipeName = state.name,
                    ingredients = existingGeminiIngs,
                    userCustomPrompt = userCustomPrompt,
                    preferredModel = getGeminiPreferredModel()
                )
            }

            var finalResult = primaryResult
            var providerUsed = if (useOpenAiPrimary) "ChatGPT (${getOpenAiPreferredModel()})" else "Gemini (${getGeminiPreferredModel()})"

            if (primaryResult.isFailure){
                val canFallback = if (useOpenAiPrimary) geminiKey.isNotBlank() else openAiKey.isNotBlank()
                if (canFallback){
                    val fallbackResult: Result<String> = if (useOpenAiPrimary){
                        geminiService.generateInstructionsOnly(
                            apiKey = geminiKey,
                            recipeName = state.name,
                            ingredients = existingGeminiIngs,
                            userCustomPrompt = userCustomPrompt,
                            preferredModel = getGeminiPreferredModel()
                        )
                    } else{
                        openAiService.generateInstructions(
                            apiKey = openAiKey,
                            recipeName = state.name,
                            baseYield = state.baseYield,
                            yieldUnit = state.yieldUnit,
                            ingredients = existingGeminiIngs,
                            customPrompt = userCustomPrompt,
                            preferredModel = getOpenAiPreferredModel()
                        ).map{ it.instructions ?: "" }
                    }
                    if (fallbackResult.isSuccess){
                        finalResult = fallbackResult
                        providerUsed = if (useOpenAiPrimary) "Gemini (fallback automatico)" else "ChatGPT (fallback automatico)"
                    }
                }
            }

            finalResult.onSuccess{ instructions ->
                _uiState.value = _uiState.value.copy(
                    instructions = instructions,
                    isAiLoading = false,
                    infoMessage = "✨ Passaggi della ricetta generati con successo da $providerUsed!"
                )
                onComplete()
            }.onFailure{ err ->
                _uiState.value = _uiState.value.copy(
                    isAiLoading = false,
                    errorMessage = "Errore AI ($providerUsed): ${err.message}"
                )
            }
        }
    }

    /**
     * Stima o arricchisce i 6 valori nutrizionali degli ingredienti e il modificatore peso in cottura con l'AI.
     */
    fun enrichNutritionalValuesWithAi(onComplete: () -> Unit = {}){
        val state = _uiState.value
        if (state.components.isEmpty()){
            _uiState.value = state.copy(errorMessage = "Aggiungi prima degli ingredienti alla ricetta per poterne arricchire i valori nutrizionali.")
            return
        }

        val preferredProvider = getPreferredAiProvider()
        val geminiKey = getGeminiApiKey()
        val openAiKey = getOpenAiApiKey()

        val primaryKey = if (preferredProvider == AiProvider.OPENAI) openAiKey else geminiKey
        val secondaryKey = if (preferredProvider == AiProvider.OPENAI) geminiKey else openAiKey

        if (primaryKey.isBlank() && secondaryKey.isBlank()){
            _uiState.value = state.copy(errorMessage = "API_KEY_REQUIRED")
            return
        }

        _uiState.value = state.copy(isAiLoading = true, errorMessage = null, infoMessage = null)

        viewModelScope.launch{
            val existingGeminiIngs = state.components.map{ comp ->
                val parsedQty = NumberUtils.parseDoubleSafe(comp.quantity, 0.0)
                GeminiIngredient(
                    name = comp.itemName,
                    quantity = parsedQty,
                    unit = comp.unit,
                    pricePerUnit = comp.pricePerUnit,
                    priceUnit = comp.priceUnit,
                    allergens = comp.allergens,
                    caloriesKcal = comp.caloriesKcal,
                    carbohydratesG = comp.carbohydratesG,
                    fatsG = comp.fatsG,
                    proteinsG = comp.proteinsG,
                    waterG = comp.waterG,
                    sodiumMg = comp.sodiumMg
                )
            }

            val useOpenAiPrimary = (preferredProvider == AiProvider.OPENAI && openAiKey.isNotBlank()) || geminiKey.isBlank()

            val primaryResult = if (useOpenAiPrimary){
                openAiService.enrichNutritionalValues(
                    apiKey = openAiKey,
                    recipeName = state.name.ifBlank{ "Ricetta" },
                    ingredients = existingGeminiIngs,
                    preferredModel = getOpenAiPreferredModel()
                )
            } else{
                geminiService.enrichNutritionalValues(
                    apiKey = geminiKey,
                    recipeName = state.name.ifBlank{ "Ricetta" },
                    ingredients = existingGeminiIngs,
                    preferredModel = getGeminiPreferredModel()
                )
            }

            var finalResult = primaryResult
            var providerUsed = if (useOpenAiPrimary) "ChatGPT (${getOpenAiPreferredModel()})" else "Gemini (${getGeminiPreferredModel()})"

            if (primaryResult.isFailure){
                val canFallback = if (useOpenAiPrimary) geminiKey.isNotBlank() else openAiKey.isNotBlank()
                if (canFallback){
                    val fallbackResult = if (useOpenAiPrimary){
                        geminiService.enrichNutritionalValues(
                            apiKey = geminiKey,
                            recipeName = state.name.ifBlank{ "Ricetta" },
                            ingredients = existingGeminiIngs,
                            preferredModel = getGeminiPreferredModel()
                        )
                    } else{
                        openAiService.enrichNutritionalValues(
                            apiKey = openAiKey,
                            recipeName = state.name.ifBlank{ "Ricetta" },
                            ingredients = existingGeminiIngs,
                            preferredModel = getOpenAiPreferredModel()
                        )
                    }
                    if (fallbackResult.isSuccess){
                        finalResult = fallbackResult
                        providerUsed = if (useOpenAiPrimary) "Gemini (fallback automatico)" else "ChatGPT (fallback automatico)"
                    }
                }
            }

            finalResult.onSuccess{ response ->
                // Applica esclusivamente i valori nutrizionali senza toccare pesi, costi, allergeni o calo peso
                applyNutritionalValuesOnly(response.ingredients)

                _uiState.value = _uiState.value.copy(
                    isAiLoading = false,
                    infoMessage = "✨ Valori nutrizionali calcolati con successo da $providerUsed!"
                )
                onComplete()
            }.onFailure{ err ->
                _uiState.value = _uiState.value.copy(
                    isAiLoading = false,
                    errorMessage = "Errore AI ($providerUsed): ${err.message}"
                )
            }
        }
    }

    // Alias per retrocompatibilità
    fun generateRecipeWithGemini(userCustomPrompt: String? = null, onComplete: () -> Unit = {}){
        generateRecipeWithAi(userCustomPrompt, onComplete)
    }

    fun completeMissingWithGemini(userCustomPrompt: String? = null, onComplete: () -> Unit = {}){
        completeMissingWithAi(userCustomPrompt, onComplete)
    }

    fun generateInstructionsWithGemini(userCustomPrompt: String? = null, onComplete: () -> Unit = {}){
        generateInstructionsWithAi(userCustomPrompt, onComplete)
    }

    fun clearError(){
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearInfoMessage(){
        _uiState.value = _uiState.value.copy(infoMessage = null)
    }
}
