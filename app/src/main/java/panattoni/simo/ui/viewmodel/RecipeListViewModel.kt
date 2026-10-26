package panattoni.simo.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import panattoni.simo.data.local.AppDatabase
import panattoni.simo.data.model.Recipe
import panattoni.simo.data.model.RecipeSortOption
import panattoni.simo.data.repository.BackupManager
import panattoni.simo.data.repository.RecipeRepository
import panattoni.simo.domain.RecipeTags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecipeListUiState(
    val recipes: List<Recipe> = emptyList(),
    val searchQuery: String = "",
    val selectedTags: Set<String> = emptySet(),
    val availableTags: List<String> = emptyList(),
    val filterFavoritesOnly: Boolean = false,
    val sortOption: RecipeSortOption = RecipeSortOption.NAME_ASC,
    val isLoading: Boolean = false,
    val message: String? = null
)

data class ConflictDialogState(
    val backup: BackupManager.ParsedBackup,
    val conflicts: List<BackupManager.ConflictItem>
)

data class DeleteDialogState(
    val recipe: Recipe,
    val linkedRecipes: List<String>
)

class RecipeListViewModel(application: Application) : AndroidViewModel(application){

    private val prefs = application.getSharedPreferences("simo_prefs", Context.MODE_PRIVATE)
    private val PREF_KEY_SORT = "recipe_sort_option"

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    val repository = RecipeRepository(database)
    private val backupManager = BackupManager(database)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedTags = MutableStateFlow<Set<String>>(emptySet())
    val selectedTags: StateFlow<Set<String>> = _selectedTags

    private val _filterFavoritesOnly = MutableStateFlow(false)
    val filterFavoritesOnly: StateFlow<Boolean> = _filterFavoritesOnly

    private val _sortOption = MutableStateFlow(loadInitialSortOption())
    val sortOption: StateFlow<RecipeSortOption> = _sortOption

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _conflictDialogState = MutableStateFlow<ConflictDialogState?>(null)
    val conflictDialogState: StateFlow<ConflictDialogState?> = _conflictDialogState

    private val _deleteDialogState = MutableStateFlow<DeleteDialogState?>(null)
    val deleteDialogState: StateFlow<DeleteDialogState?> = _deleteDialogState

    private data class FilterState(
        val query: String,
        val selectedTags: Set<String>,
        val favoritesOnly: Boolean,
        val sort: RecipeSortOption,
        val msg: String?
    )

    private val _availableTags = repository.allRecipes.map{ recipes ->
        RecipeTags.getAllSortedTags(recipes.map{ it.category })
    }.distinctUntilChanged()

    private val _filterState = combine(
        _searchQuery,
        _selectedTags,
        _filterFavoritesOnly,
        _sortOption,
        _message
    ){ query, tags, favs, sort, msg ->
        FilterState(query, tags, favs, sort, msg)
    }

    val uiState: StateFlow<RecipeListUiState> = combine(
        repository.allRecipes,
        _availableTags,
        _filterState
    ){ recipes, availableTags, filter ->
        val query = filter.query
        val rawSelectedTags = filter.selectedTags
        // Rimuove dai tag selezionati quelli che non esistono più tra le ricette disponibili
        val selectedTags = rawSelectedTags.filter{ selTag ->
            availableTags.any{ it.equals(selTag, ignoreCase = true) }
        }.toSet()
        if (selectedTags.size != rawSelectedTags.size){
            _selectedTags.value = selectedTags
        }
        val favoritesOnly = filter.favoritesOnly
        val sort = filter.sort
        val msg = filter.msg

        // 1. Filtro Tag con logica OR: una ricetta compare se il suo tag è tra quelli selezionati
        val byTags = if (selectedTags.isEmpty()){
            recipes
        } else {
            recipes.filter{ recipe ->
                selectedTags.any{ it.equals(recipe.category.trim(), ignoreCase = true) }
            }
        }

        // 2. Filtro Preferiti
        val byFavorites = if (favoritesOnly){
            byTags.filter{ it.isFavorite }
        } else {
            byTags
        }

        // 3. Filtro di ricerca testuale
        val filtered = if (query.isBlank()){
            byFavorites
        } else {
            byFavorites.filter{
                it.name.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true)
            }
        }

        // 4. Ordinamento
        val sorted = when (sort){
            RecipeSortOption.NAME_ASC -> filtered.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER){ it.name })
            RecipeSortOption.NAME_DESC -> filtered.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER){ it.name })
            RecipeSortOption.DATE_DESC -> filtered.sortedByDescending{ it.createdAt }
            RecipeSortOption.DATE_ASC -> filtered.sortedBy{ it.createdAt }
        }

        RecipeListUiState(
            recipes = sorted,
            searchQuery = query,
            selectedTags = selectedTags,
            availableTags = availableTags,
            filterFavoritesOnly = favoritesOnly,
            sortOption = sort,
            isLoading = false,
            message = msg
        )
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = RecipeListUiState(sortOption = _sortOption.value, isLoading = true)
    )

    private fun loadInitialSortOption(): RecipeSortOption {
        val savedName = prefs.getString(PREF_KEY_SORT, null) ?: return RecipeSortOption.NAME_ASC
        return try {
            RecipeSortOption.valueOf(savedName)
        } catch (_: Exception){
            RecipeSortOption.NAME_ASC
        }
    }

    fun onSortOptionChange(newOption: RecipeSortOption){
        _sortOption.value = newOption
        prefs.edit().putString(PREF_KEY_SORT, newOption.name).apply()
    }

    fun onSearchQueryChange(query: String){
        _searchQuery.value = query
    }

    fun toggleTag(tag: String){
        _selectedTags.update { current ->
            val match = current.firstOrNull { it.equals(tag, ignoreCase = true) }
            if (match != null){
                current - match
            } else {
                current + tag
            }
        }
    }

    fun clearSelectedTags(){
        _selectedTags.value = emptySet()
    }

    fun toggleFavoritesFilter(){
        _filterFavoritesOnly.update { !it }
    }

    fun toggleFavorite(recipe: Recipe){
        viewModelScope.launch {
            try {
                repository.toggleFavorite(recipe.id, recipe.isFavorite)
            } catch (e: Exception){
                _message.value = "Errore durante l'aggiornamento dei preferiti: ${e.message}"
            }
        }
    }

    fun requestDeleteRecipe(recipe: Recipe){
        viewModelScope.launch {
            try {
                val linked = repository.getLinkedRecipes(recipe.id)
                _deleteDialogState.value = DeleteDialogState(
                    recipe = recipe,
                    linkedRecipes = linked
                )
            } catch (e: Exception){
                _message.value = "Errore durante la verifica dei collegamenti: ${e.message}"
            }
        }
    }

    fun dismissDeleteDialog(){
        _deleteDialogState.value = null
    }

    fun confirmDeleteRecipe(recipe: Recipe, convertToRaw: Boolean){
        _deleteDialogState.value = null
        viewModelScope.launch {
            try {
                when (val result = repository.deleteRecipe(recipe.id, convertToRaw = convertToRaw)){
                    is RecipeRepository.DeleteResult.Success -> {
                        if (convertToRaw){
                            _message.value = "\"${recipe.name}\" eliminata: è stata convertita in ingrediente normale nelle ricette collegate"
                        } else {
                            _message.value = "\"${recipe.name}\" eliminata con successo"
                        }
                    }
                    is RecipeRepository.DeleteResult.InUse -> {
                        val usedIn = result.usedInRecipes.joinToString(", ")
                        _message.value = "Impossibile eliminare: \"${recipe.name}\" è utilizzata in: $usedIn"
                    }
                    is RecipeRepository.DeleteResult.Error -> {
                        _message.value = "Errore durante l'eliminazione: ${result.message}"
                    }
                }
            } catch (e: Exception){
                _message.value = "Errore imprevisto durante l'eliminazione: ${e.message}"
            }
        }
    }

    fun exportBackup(uri: Uri){
        viewModelScope.launch {
            try {
                val result = backupManager.exportToJson(getApplication(), uri)
                if (result.isSuccess){
                    _message.value = "Backup esportato con successo (${result.getOrNull()} ricette salvate)"
                } else {
                    _message.value = "Errore durante l'esportazione: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception){
                _message.value = "Impossibile completare l'esportazione: ${e.message}"
            }
        }
    }

    fun startImport(uri: Uri){
        viewModelScope.launch {
            try {
                val parseRes = backupManager.parseBackup(getApplication(), uri)
                if (parseRes.isFailure){
                    _message.value = "Errore durante la lettura del file: ${parseRes.exceptionOrNull()?.message}"
                    return@launch
                }
                val parsed = parseRes.getOrThrow()
                if (parsed.conflicts.isEmpty()){
                    executeImport(parsed, replaceAll = false)
                } else {
                    _conflictDialogState.value = ConflictDialogState(
                        backup = parsed,
                        conflicts = parsed.conflicts
                    )
                }
            } catch (e: Exception){
                _message.value = "Errore durante l'apertura del backup: ${e.message}"
            }
        }
    }

    fun resolveImportConflicts(replaceAll: Boolean){
        val state = _conflictDialogState.value ?: return
        _conflictDialogState.value = null
        viewModelScope.launch {
            try {
                executeImport(state.backup, replaceAll = replaceAll)
            } catch (e: Exception){
                _message.value = "Errore durante la risoluzione dei conflitti: ${e.message}"
            }
        }
    }

    fun resolveImportConflictsCustom(replacements: Map<String, Boolean>){
        val state = _conflictDialogState.value ?: return
        _conflictDialogState.value = null
        viewModelScope.launch {
            try {
                val result = backupManager.applyImport(state.backup, specificReplacements = replacements)
                if (result.isSuccess){
                    _message.value = "Importate con successo ${result.getOrNull()} ricette"
                } else {
                    _message.value = "Errore durante l'importazione: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception){
                _message.value = "Errore durante l'importazione personalizzata: ${e.message}"
            }
        }
    }

    fun cancelImport(){
        _conflictDialogState.value = null
    }

    private suspend fun executeImport(backup: BackupManager.ParsedBackup, replaceAll: Boolean){
        try {
            val result = backupManager.applyImport(backup, replaceAllConflicts = replaceAll)
            if (result.isSuccess){
                val count = result.getOrNull() ?: 0
                _message.value = "Importate con successo $count ricette"
            } else {
                _message.value = "Errore durante l'importazione: ${result.exceptionOrNull()?.message}"
            }
        } catch (e: Exception){
            _message.value = "Errore critico durante l'importazione: ${e.message}"
        }
    }

    fun clearMessage(){
        _message.value = null
    }
}
