package panattoni.simo.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import panattoni.simo.data.model.Recipe
import panattoni.simo.data.model.RecipeSortOption
import panattoni.simo.ui.components.SimoTopBar
import panattoni.simo.ui.theme.MacaronBackground
import panattoni.simo.ui.theme.MacaronCard
import panattoni.simo.ui.theme.MacaronCardBorder
import panattoni.simo.ui.theme.MacaronPrimary
import panattoni.simo.ui.theme.MacaronSubtleText
import panattoni.simo.ui.theme.MacaronSurfaceVariant
import panattoni.simo.ui.theme.MacaronText
import panattoni.simo.ui.viewmodel.RecipeListViewModel
import panattoni.simo.util.ItalianGrammarUtils

/**
 * Schermata principale: elenco ricette con ricerca, filtri per tag, preferiti,
 * ordinamento alfabetico/data, gestione conflitti importazione ed esportazione backup JSON.
 */
@Composable
fun RecipeListScreen(
    viewModel: RecipeListViewModel,
    onRecipeClick: (String) -> Unit,
    onAddRecipeClick: () -> Unit,
    onEditRecipeClick: (String) -> Unit,
    onShoppingClick: () -> Unit
){
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember{ SnackbarHostState() }
    val deleteState by viewModel.deleteDialogState.collectAsState()
    var isSearchVisible by remember{ mutableStateOf(false) }

    val conflictState by viewModel.conflictDialogState.collectAsState()

    // Launcher per esportare backup JSON
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ){ uri: Uri? ->
        uri?.let{ viewModel.exportBackup(it) }
    }

    // Launcher per importare backup JSON
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ){ uri: Uri? ->
        uri?.let{ viewModel.startImport(it) }
    }

    val context = LocalContext.current

    // Richiesta non bloccante dei permessi di notifica su Android 13+ dopo il caricamento della schermata principale
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU){
        val notificationPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ){ /* Esito non bloccante */ }

        LaunchedEffect(Unit){
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ){
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(uiState.message){
        uiState.message?.let{
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        containerColor = MacaronBackground,
        snackbarHost ={ SnackbarHost(snackbarHostState) },
        topBar ={
            SimoTopBar(
                title = "SIMO",
                subtitle = "Scalable Ingredient Management Organizer",
                actions ={
                    var sortMenuExpanded by remember{ mutableStateOf(false) }
                    Box{
                        IconButton(onClick ={ sortMenuExpanded = true }){
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Ordina ricette",
                                tint = MacaronText
                            )
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest ={ sortMenuExpanded = false },
                            modifier = Modifier.background(MacaronCard)
                        ){
                            RecipeSortOption.entries.forEach{ option ->
                                DropdownMenuItem(
                                    text ={
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ){
                                            RadioButton(
                                                selected = uiState.sortOption == option,
                                                onClick = null,
                                                colors = RadioButtonDefaults.colors(
                                                    selectedColor = MacaronPrimary,
                                                    unselectedColor = MacaronSubtleText
                                                )
                                            )
                                            Text(
                                                text = option.displayName,
                                                color = MacaronText,
                                                fontWeight = if (uiState.sortOption == option) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    },
                                    onClick ={
                                        viewModel.onSortOptionChange(option)
                                        sortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick ={ isSearchVisible = !isSearchVisible }){
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Cerca e Filtra per Tag",
                            tint = if (isSearchVisible) MacaronPrimary else MacaronText
                        )
                    }
                    // Icona carrello 🛒: a destra dell'ordinamento e della ricerca, prima dell'export e dell'import
                    IconButton(onClick = onShoppingClick){
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Lista della Spesa",
                            tint = MacaronText
                        )
                    }
                    IconButton(onClick ={
                        val timestamp = System.currentTimeMillis()
                        exportLauncher.launch("simo_ricettario_$timestamp.json")
                    }){
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = "Esporta Backup JSON",
                            tint = MacaronText
                        )
                    }
                    IconButton(onClick ={
                        importLauncher.launch(arrayOf("application/json"))
                    }){
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Importa Backup JSON",
                            tint = MacaronText
                        )
                    }
                }
            )
        },
        floatingActionButton ={
            FloatingActionButton(
                onClick = onAddRecipeClick,
                containerColor = MacaronPrimary,
                contentColor = MacaronCard,
                shape = CircleShape
            ){
                Icon(imageVector = Icons.Default.Add, contentDescription = "Nuova Ricetta")
            }
        }
    ){ innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ){
            // Barra di ricerca con Tag selezionabili in ordine alfabetico (OR)
            if (isSearchVisible){
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MacaronCard),
                    border = BorderStroke(1.dp, MacaronCardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ){
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ){
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = viewModel::onSearchQueryChange,
                            placeholder ={ Text("Cerca ricetta...") },
                            leadingIcon ={
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MacaronPrimary
                                )
                            },
                            trailingIcon ={
                                if (uiState.searchQuery.isNotEmpty()){
                                    IconButton(onClick ={ viewModel.onSearchQueryChange("") }){
                                        Icon(Icons.Default.Close, contentDescription = "Pulisci")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MacaronBackground,
                                unfocusedContainerColor = MacaronBackground,
                                focusedBorderColor = MacaronPrimary,
                                unfocusedBorderColor = MacaronCardBorder
                            ),
                            singleLine = true
                        )

                        // Intestazione Tag e reset
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ){
                            Row(
                                modifier = Modifier.weight(1f, fill = false),
                                verticalAlignment = Alignment.CenterVertically
                            ){
                                Icon(
                                    imageVector = Icons.Default.LocalOffer,
                                    contentDescription = null,
                                    tint = MacaronPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Tag (selezione multipla):",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MacaronText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (uiState.selectedTags.isNotEmpty() || uiState.filterFavoritesOnly){
                                val activeFiltersCount = uiState.selectedTags.size + (if (uiState.filterFavoritesOnly) 1 else 0)
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MacaronPrimary.copy(alpha = 0.12f),
                                    modifier = Modifier.clickable{
                                        viewModel.clearSelectedTags()
                                        if (uiState.filterFavoritesOnly) viewModel.toggleFavoritesFilter()
                                    }
                                ){
                                    Text(
                                        text = "Azzera ($activeFiltersCount)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MacaronPrimary,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // Riga di Tag in ordine alfabetico + Preferiti
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ){
                            // Chip Preferiti
                            item{
                                val isFavSelected = uiState.filterFavoritesOnly
                                FilterChip(
                                    selected = isFavSelected,
                                    onClick ={ viewModel.toggleFavoritesFilter() },
                                    label ={
                                        Text(
                                            text = "⭐ Preferiti",
                                            fontSize = 12.sp,
                                            fontWeight = if (isFavSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFFFB300),
                                        selectedLabelColor = Color.White,
                                        containerColor = MacaronBackground,
                                        labelColor = MacaronText
                                    ),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isFavSelected) Color(0xFFFFB300) else MacaronCardBorder
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                )
                            }

                            // Tutti i tag disponibili in ordine alfabetico
                            items(uiState.availableTags, key ={ it }){ tag ->
                                val isSelected = uiState.selectedTags.any{ it.equals(tag, ignoreCase = true) }
                                FilterChip(
                                    selected = isSelected,
                                    onClick ={ viewModel.toggleTag(tag) },
                                    label ={
                                        Text(
                                            text = tag,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MacaronPrimary,
                                        selectedLabelColor = Color.White,
                                        containerColor = MacaronBackground,
                                        labelColor = MacaronText
                                    ),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isSelected) MacaronPrimary else MacaronCardBorder
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.isLoading){
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ){
                    CircularProgressIndicator(
                        color = MacaronPrimary,
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp
                    )
                }
            } else if (uiState.recipes.isEmpty()){
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ){
                    Column(horizontalAlignment = Alignment.CenterHorizontally){
                        Surface(
                            shape = CircleShape,
                            color = MacaronSurfaceVariant,
                            modifier = Modifier.size(72.dp)
                        ){
                            Box(contentAlignment = Alignment.Center){
                                Icon(
                                    imageVector = Icons.Default.BakeryDining,
                                    contentDescription = null,
                                    tint = MacaronPrimary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (uiState.searchQuery.isNotEmpty() || uiState.selectedTags.isNotEmpty() || uiState.filterFavoritesOnly) "Nessuna ricetta trovata" else "Il tuo ricettario è vuoto",
                            color = MacaronText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (uiState.selectedTags.isNotEmpty()) "Nessuna ricetta corrisponde ai tag selezionati." else if (uiState.filterFavoritesOnly) "Nessuna ricetta tra i preferiti." else "Tocca il pulsante + per creare la tua prima ricetta.",
                            color = MacaronSubtleText,
                            fontSize = 14.sp
                        )
                    }
                }
            } else{
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ){
                    items(uiState.recipes, key ={ it.id }){ recipe ->
                        RecipeItemCard(
                            recipe = recipe,
                            onClick ={ onRecipeClick(recipe.id) },
                            onEdit ={ onEditRecipeClick(recipe.id) },
                            onDelete ={ viewModel.requestDeleteRecipe(recipe) },
                            onToggleFavorite ={ viewModel.toggleFavorite(recipe) }
                        )
                    }
                }
            }
        }
    }

    // Dialog di conferma eliminazione
    if (deleteState != null){
        val recipe = deleteState!!.recipe
        val linked = deleteState!!.linkedRecipes

        if (linked.isNotEmpty()){
            // Ricetta collegata ad altre ricette: disclaimer e conversione in ingrediente normale
            AlertDialog(
                onDismissRequest ={ viewModel.dismissDeleteDialog() },
                containerColor = MacaronCard,
                shape = RoundedCornerShape(20.dp),
                title ={
                    Text(
                        text = "Ricetta Collegata",
                        color = MacaronPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text ={
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ){
                        Text(
                            text = "Questa ricetta è collegata a:",
                            color = MacaronText,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MacaronBackground)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ){
                            linked.forEach{ parentName ->
                                Row(verticalAlignment = Alignment.CenterVertically){
                                    Text(
                                        text = "• ",
                                        color = MacaronPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = parentName,
                                        color = MacaronText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Sei sicuro di volerla eliminare?\nNelle ricette collegate, la ricetta non sparirà ma diventerà un ingrediente normale (senza più il link alla preparazione).",
                            color = MacaronSubtleText,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                },
                confirmButton ={
                    Button(
                        onClick ={
                            viewModel.confirmDeleteRecipe(recipe, convertToRaw = true)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(10.dp)
                    ){
                        Text("Elimina e Converti", color = MacaronCard, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton ={
                    OutlinedButton(
                        onClick ={ viewModel.dismissDeleteDialog() },
                        shape = RoundedCornerShape(10.dp)
                    ){
                        Text("Annulla", color = MacaronSubtleText)
                    }
                }
            )
        } else{
            // Ricetta normale non collegata
            AlertDialog(
                onDismissRequest ={ viewModel.dismissDeleteDialog() },
                containerColor = MacaronCard,
                shape = RoundedCornerShape(20.dp),
                title ={
                    Text(
                        text = "Elimina Ricetta",
                        color = MacaronPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text ={
                    Text(
                        text = "Sei sicuro di voler eliminare \"${recipe.name}\"? Questa operazione non può essere annullata.",
                        color = MacaronText,
                        fontSize = 14.sp
                    )
                },
                confirmButton ={
                    Button(
                        onClick ={
                            viewModel.confirmDeleteRecipe(recipe, convertToRaw = false)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(10.dp)
                    ){
                        Text("Elimina", color = MacaronCard, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton ={
                    OutlinedButton(
                        onClick ={ viewModel.dismissDeleteDialog() },
                        shape = RoundedCornerShape(10.dp)
                    ){
                        Text("Annulla", color = MacaronSubtleText)
                    }
                }
            )
        }
    }

    // Dialog di risoluzione conflitti importazione
    if (conflictState != null){
        val conflicts = conflictState!!.conflicts
        var replacementMap by remember(conflictState){
            mutableStateOf(conflicts.associate{ it.importedRecipe.id to true })
        }

        AlertDialog(
            onDismissRequest ={ viewModel.cancelImport() },
            containerColor = MacaronCard,
            shape = RoundedCornerShape(20.dp),
            title ={
                Text(
                    text = "Ricette Già Esistenti",
                    color = MacaronPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text ={
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ){
                    Text(
                        text = "Nel file importato sono presenti ${conflicts.size} ricette già salvate nel ricettario. Le ricette nuove verranno sempre aggiunte.",
                        color = MacaronSubtleText,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Scegli l'azione per ciascuna ricetta:",
                        color = MacaronText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ){
                        items(conflicts){ item ->
                            val isReplacing = replacementMap[item.importedRecipe.id] ?: true
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MacaronBackground)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ){
                                Column(modifier = Modifier.weight(1f)){
                                    Text(
                                        text = item.importedRecipe.name,
                                        color = MacaronText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (isReplacing) "Verrà sostituita con la versione importata" else "Verrà conservata la versione esistente",
                                        color = if (isReplacing) MacaronPrimary else MacaronSubtleText,
                                        fontSize = 11.sp
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ){
                                    FilterChip(
                                        selected = isReplacing,
                                        onClick ={
                                            replacementMap = replacementMap + (item.importedRecipe.id to true)
                                        },
                                        label ={ Text("Sostituisci", fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MacaronPrimary,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                    FilterChip(
                                        selected = !isReplacing,
                                        onClick ={
                                            replacementMap = replacementMap + (item.importedRecipe.id to false)
                                        },
                                        label ={ Text("Mantieni", fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MacaronSurfaceVariant,
                                            selectedLabelColor = MacaronText
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Azioni rapide per tutte
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ){
                        OutlinedButton(
                            onClick ={
                                replacementMap = conflicts.associate{ it.importedRecipe.id to true }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp, horizontal = 4.dp)
                        ){
                            Text("Sostituisci tutte", fontSize = 11.sp, color = MacaronPrimary)
                        }

                        OutlinedButton(
                            onClick ={
                                replacementMap = conflicts.associate{ it.importedRecipe.id to false }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp, horizontal = 4.dp)
                        ){
                            Text("Mantieni tutte", fontSize = 11.sp, color = MacaronSubtleText)
                        }
                    }
                }
            },
            confirmButton ={
                Button(
                    onClick ={
                        viewModel.resolveImportConflictsCustom(replacementMap)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MacaronPrimary),
                    shape = RoundedCornerShape(10.dp)
                ){
                    Text("Conferma", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton ={
                OutlinedButton(
                    onClick ={ viewModel.cancelImport() },
                    shape = RoundedCornerShape(10.dp)
                ){
                    Text("Annulla", color = MacaronSubtleText)
                }
            }
        )
    }
}

@Composable
fun RecipeItemCard(
    recipe: Recipe,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit
){
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable{ onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MacaronCard),
        border = BorderStroke(1.dp, MacaronCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ){
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ){
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ){
                Column(modifier = Modifier.weight(1f)){
                    Text(
                        text = recipe.name,
                        color = MacaronText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    if (recipe.description.isNotBlank()){
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = recipe.description,
                            color = MacaronSubtleText,
                            fontSize = 13.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically){
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(32.dp)
                    ){
                        Icon(
                            imageVector = if (recipe.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = if (recipe.isFavorite) "Rimuovi dai preferiti" else "Aggiungi ai preferiti",
                            tint = if (recipe.isFavorite) Color(0xFFFFB300) else MacaronSubtleText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ){
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Modifica",
                            tint = MacaronSubtleText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ){
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Elimina",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Badge Categoria e Resa base
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ){
                if (recipe.category.isNotBlank()){
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MacaronPrimary.copy(alpha = 0.12f)
                    ){
                        Text(
                            text = recipe.category,
                            color = MacaronPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MacaronSurfaceVariant
                ){
                    val yieldText = ItalianGrammarUtils.formatQuantityWithUnit(recipe.baseYieldQuantity, recipe.yieldUnit)
                    Text(
                        text = "Resa base: $yieldText",
                        color = MacaronPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Azione "Tocca per ricalcolare ->" con layout regolare e uniforme per tutte le card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ){
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MacaronPrimary.copy(alpha = 0.08f)
                ){
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ){
                        Text(
                            text = "Tocca per ricalcolare",
                            color = MacaronPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "→",
                            color = MacaronPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
