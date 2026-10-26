package panattoni.simo.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import panattoni.simo.data.local.AppDatabase
import panattoni.simo.data.model.RecipeWithComponents
import panattoni.simo.data.model.ScaledRecipeTree
import panattoni.simo.data.repository.RecipeRepository
import panattoni.simo.domain.MathEngine
import panattoni.simo.util.NumberUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RecipeDetailUiState(
    val recipeWithComponents: RecipeWithComponents? = null,
    val scaledTree: ScaledRecipeTree? = null,
    val scalingFactor: Double = 1.0,
    val yieldInputValue: String = "",
    val activeScalingIngredientName: String? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

/**
 * ViewModel per la schermata di dettaglio ricetta.
 * Gestisce lo stato di scaling reattivo, reverse scaling e trasferimento ingredienti alla spesa.
 */
class RecipeDetailViewModel(
    application: Application,
    private val recipeId: String
) : AndroidViewModel(application){

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = RecipeRepository(database)

    private data class ScaleParams(
        val factor: Double = 1.0,
        val yieldInputValue: String = "",
        val activeIngredientName: String? = null
    )

    private val _scaleParams = MutableStateFlow(ScaleParams())
    private val _errorMessage = MutableStateFlow<String?>(null)

    // Emissione reattiva dello stato UI combinando il grafo aggiornato e i parametri di scala
    val uiState: StateFlow<RecipeDetailUiState> = combine(
        repository.allRecipesWithComponentsMap,
        _scaleParams,
        _errorMessage
    ){ allRecipesMap, scaleParams, errorMsg ->
        val currentRecipe = allRecipesMap[recipeId]
        if (currentRecipe == null){
            RecipeDetailUiState(
                isLoading = false,
                errorMessage = errorMsg ?: "Ricetta non trovata nel database"
            )
        } else {
            val safeFactor = if (!scaleParams.factor.isFinite() || scaleParams.factor <= 0.0) 1.0 else scaleParams.factor
            val tree = MathEngine.buildScaledTree(
                recipeWithComponents = currentRecipe,
                scalingFactor = safeFactor,
                allRecipesMap = allRecipesMap
            )
            val currentYieldFormatted = if (scaleParams.yieldInputValue.isEmpty()){
                val target = currentRecipe.recipe.baseYieldQuantity * safeFactor
                NumberUtils.formatAmount(target)
            } else {
                scaleParams.yieldInputValue
            }

            RecipeDetailUiState(
                recipeWithComponents = currentRecipe,
                scaledTree = tree,
                scalingFactor = safeFactor,
                yieldInputValue = currentYieldFormatted,
                activeScalingIngredientName = scaleParams.activeIngredientName,
                isLoading = false,
                errorMessage = errorMsg
            )
        }
    }.flowOn(kotlinx.coroutines.Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RecipeDetailUiState()
    )

    /**
     * Ricalcola il fattore di scala in base al valore digitato manualmente nel campo della resa.
     */
    fun onYieldInputChange(newInput: String){
        val parsed = NumberUtils.parseDoubleOrNull(newInput)
        val currentRecipe = uiState.value.recipeWithComponents?.recipe
        val factor = if (parsed != null && parsed > 0.0 && currentRecipe != null){
            MathEngine.calculateScaleFactorByYield(currentRecipe.baseYieldQuantity, parsed)
        } else {
            _scaleParams.value.factor
        }
        _scaleParams.value = ScaleParams(
            factor = factor,
            yieldInputValue = newInput,
            activeIngredientName = null
        )
        _errorMessage.value = null
    }

    /**
     * Incrementa o decrementa la resa di una quantità fissa (es. +1 o -1).
     */
    fun stepYield(delta: Double){
        val currentRecipe = uiState.value.recipeWithComponents?.recipe ?: return
        val currentYield = currentRecipe.baseYieldQuantity * _scaleParams.value.factor
        val newYield = Math.max(1.0, currentYield + delta)
        val factor = MathEngine.calculateScaleFactorByYield(currentRecipe.baseYieldQuantity, newYield)
        _scaleParams.value = ScaleParams(
            factor = factor,
            yieldInputValue = "",
            activeIngredientName = null
        )
        _errorMessage.value = null
    }

    /**
     * Reverse Scaling: ricalcola proporzioni in base alla quantità inserita per un ingrediente.
     */
    fun onReverseScaleIngredient(baseAmount: Double, newAmount: Double, ingredientName: String){
        if (!baseAmount.isFinite() || !newAmount.isFinite() || baseAmount <= 0.0 || newAmount <= 0.0){
            _errorMessage.value = "Quantità non valida per il ricalcolo"
            return
        }
        val factor = MathEngine.calculateScaleFactorByIngredient(baseAmount, newAmount)
        _scaleParams.value = ScaleParams(
            factor = factor,
            yieldInputValue = "",
            activeIngredientName = ingredientName
        )
        _errorMessage.value = null
    }

    /**
     * Ripristina la ricetta alla resa e alle dosi base originali.
     */
    fun resetToOriginal(){
        _scaleParams.value = ScaleParams()
        _errorMessage.value = null
    }

    /**
     * Rimuove il messaggio di errore corrente.
     */
    fun clearError(){
        _errorMessage.value = null
    }

    /**
     * Aggiunge alla lista della spesa le quantità scalate al momento con il moltiplicatore corrente.
     */
    fun addToShoppingList(onSuccess: (Int) -> Unit){
        val tree = uiState.value.scaledTree ?: return
        viewModelScope.launch {
            try {
                val count = repository.addScaledComponentsToShoppingList(
                    recipeName = tree.recipe.name,
                    components = tree.components
                )
                onSuccess(count)
            } catch (e: Exception){
                _errorMessage.value = "Errore durante l'aggiunta alla spesa: ${e.message}"
            }
        }
    }
}
