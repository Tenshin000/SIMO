package panattoni.simo.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import panattoni.simo.data.local.AppDatabase
import panattoni.simo.data.model.ShoppingItem
import panattoni.simo.data.model.ShoppingSortOption
import panattoni.simo.data.repository.RecipeRepository
import panattoni.simo.domain.MathEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Modello UI per un articolo della spesa arricchito con la stima del costo calcolato.
 */
data class ShoppingItemUiModel(
    val item: ShoppingItem,
    val estimatedCost: Double? = null
)

/**
 * Stato immutabile per la schermata della lista della spesa.
 */
data class ShoppingListUiState(
    val items: List<ShoppingItemUiModel> = emptyList(),
    val activeCount: Int = 0,
    val checkedCount: Int = 0,
    val estimatedTotalCost: Double = 0.0,
    val estimatedActiveCost: Double = 0.0,
    val hasCostInfo: Boolean = false,
    val sortOption: ShoppingSortOption = ShoppingSortOption.DATE_DESC,
    val isLoading: Boolean = false,
    val message: String? = null
)

/**
 * ViewModel per la gestione del carrello della spesa.
 * Coordina l'aggregazione per unità uguali, il calcolo della spesa stimata e l'ordinamento.
 */
class ShoppingListViewModel(application: Application) : AndroidViewModel(application){

    private val prefs = application.getSharedPreferences("simo_prefs", Context.MODE_PRIVATE)
    private val PREF_KEY_SORT = "shopping_sort_option"

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = RecipeRepository(database)

    private val _message = MutableStateFlow<String?>(null)
    private val _sortOption = MutableStateFlow(loadInitialSortOption())

    init {
        viewModelScope.launch(Dispatchers.IO){
            repository.sanitizeQbShoppingItems()
        }
    }

    private fun loadInitialSortOption(): ShoppingSortOption {
        val saved = prefs.getString(PREF_KEY_SORT, null) ?: return ShoppingSortOption.DATE_DESC
        return try {
            ShoppingSortOption.valueOf(saved)
        } catch (_: Exception){
            ShoppingSortOption.DATE_DESC
        }
    }

    val uiState: StateFlow<ShoppingListUiState> = combine(
        repository.shoppingItems,
        repository.allItems,
        _sortOption,
        _message
    ){ items, allItems, sortOption, msg ->
        val active = items.count { !it.isChecked }
        val checked = items.count { it.isChecked }

        // Lookup rapido per nome ingrediente (case-insensitive)
        val itemLookup = allItems.associateBy { it.name.trim().lowercase() }

        var totalCost = 0.0
        var activeCost = 0.0
        var hasAnyCost = false

        val modelsWithCost = items.map { item ->
            val matched = itemLookup[item.itemName.trim().lowercase()]

            val pPrice = if (item.unitPrice > 0.0) item.unitPrice else matched?.pricePerUnit
            val pUnit = if (!item.priceUnit.isNullOrBlank()) item.priceUnit else (matched?.priceUnit ?: item.unit)

            val cost = MathEngine.calculateComponentCost(
                quantity = item.quantity,
                recipeUnit = item.unit,
                pricePerUnit = pPrice,
                priceUnit = pUnit,
                gramsPerUnit = matched?.gramsPerUnit
            )

            if (cost != null && cost > 0.0){
                hasAnyCost = true
                totalCost += cost
                if (!item.isChecked){
                    activeCost += cost
                }
            }

            ShoppingItemUiModel(item = item, estimatedCost = cost)
        }

        // Ordinamento: prima gli articoli non spuntati (in cima), poi quelli spuntati (in fondo).
        // All'interno di ciascun gruppo, ordina secondo sortOption.
        val sortedItems = modelsWithCost.sortedWith { a, b ->
            val checkedCompare = a.item.isChecked.compareTo(b.item.isChecked)
            if (checkedCompare != 0){
                checkedCompare
            } else {
                when (sortOption){
                    ShoppingSortOption.NAME_ASC ->
                        a.item.itemName.compareTo(b.item.itemName, ignoreCase = true)
                    ShoppingSortOption.NAME_DESC ->
                        b.item.itemName.compareTo(a.item.itemName, ignoreCase = true)
                    ShoppingSortOption.DATE_DESC ->
                        b.item.createdAt.compareTo(a.item.createdAt).let {
                            if (it != 0) it else b.item.id.compareTo(a.item.id)
                         }
                    ShoppingSortOption.DATE_ASC ->
                        a.item.createdAt.compareTo(b.item.createdAt).let {
                            if (it != 0) it else a.item.id.compareTo(b.item.id)
                         }
                }
            }
        }

        ShoppingListUiState(
            items = sortedItems,
            activeCount = active,
            checkedCount = checked,
            estimatedTotalCost = totalCost,
            estimatedActiveCost = activeCost,
            hasCostInfo = hasAnyCost,
            sortOption = sortOption,
            isLoading = false,
            message = msg
        )
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ShoppingListUiState(isLoading = true)
    )

    fun setSortOption(option: ShoppingSortOption){
        _sortOption.value = option
        prefs.edit().putString(PREF_KEY_SORT, option.name).apply()
    }

    fun toggleChecked(item: ShoppingItem){
        viewModelScope.launch {
            repository.setShoppingItemChecked(item.id, !item.isChecked)
        }
    }

    fun updateQuantity(item: ShoppingItem, newQuantity: Double){
        viewModelScope.launch {
            repository.updateShoppingItemQuantity(item.id, newQuantity)
        }
    }

    fun updateItem(item: ShoppingItem, newQuantity: Double, newUnitPrice: Double){
        viewModelScope.launch {
            repository.updateShoppingItem(item.id, newQuantity, newUnitPrice)
        }
    }

    fun updateItemDetails(item: ShoppingItem, newQuantity: Double, newUnit: String, newUnitPrice: Double, newPriceUnit: String? = null){
        viewModelScope.launch {
            repository.updateShoppingItemDetails(item.id, newQuantity, newUnit, newUnitPrice, newPriceUnit)
        }
    }

    fun deleteItem(item: ShoppingItem){
        viewModelScope.launch {
            repository.deleteShoppingItem(item.id)
        }
    }

    fun deleteCheckedItems(){
        viewModelScope.launch {
            val count = repository.deleteCheckedShoppingItems()
            _message.value = "Rimossi $count articoli acquistati"
        }
    }

    fun clearAllItems(){
        viewModelScope.launch {
            repository.clearShoppingList()
            _message.value = "Lista della spesa svuotata"
        }
    }

    fun addManualItem(name: String, quantity: Double, unit: String, unitPrice: Double = 0.0, priceUnit: String? = null){
        if (name.isBlank() || quantity <= 0.0) return
        viewModelScope.launch {
            repository.addManualShoppingItem(name.trim(), quantity, unit.trim(), unitPrice, priceUnit)
            _message.value = "\"${name.trim()}\" aggiunto alla spesa"
        }
    }

    fun clearMessage(){
        _message.value = null
    }
}
