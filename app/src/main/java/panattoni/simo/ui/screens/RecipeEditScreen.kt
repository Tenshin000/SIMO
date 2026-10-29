package panattoni.simo.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import panattoni.simo.data.remote.GeminiModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import panattoni.simo.domain.Allergen
import panattoni.simo.ui.viewmodel.EditableComponent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import panattoni.simo.data.model.Item
import panattoni.simo.data.model.ItemType
import panattoni.simo.data.remote.AiProvider
import panattoni.simo.data.remote.OpenAiModel
import panattoni.simo.domain.MeasurementUnit
import panattoni.simo.ui.components.SimoTopBar
import panattoni.simo.util.NumberUtils
import panattoni.simo.util.ItalianGrammarUtils
import panattoni.simo.ui.theme.MacaronBackground
import panattoni.simo.ui.theme.MacaronCard
import panattoni.simo.ui.theme.MacaronCardBorder
import panattoni.simo.ui.theme.MacaronPrimary
import panattoni.simo.ui.theme.MacaronRecipeBadge
import panattoni.simo.ui.theme.MacaronSubtleText
import panattoni.simo.ui.theme.MacaronSurfaceVariant
import panattoni.simo.ui.theme.MacaronText
import panattoni.simo.ui.viewmodel.RecipeEditViewModel

/**
 * Schermata di creazione e modifica ricetta.
 * Gestisce dati anagrafici, componenti e sotto-ricette con verifica cicli DAG,
 * calcolo automatico allergeni, bilanciamento impasti e Assistente Chef AI (Gemini).
 */
@Composable
fun RecipeEditScreen(
    viewModel: RecipeEditViewModel,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit
){
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember{ mutableStateOf(false) }
    var editingComponent by remember{ mutableStateOf<EditableComponent?>(null) }
    var showAiDialog by remember{ mutableStateOf(false) }
    var showApiKeyDialog by remember{ mutableStateOf(false) }
    var activeDragId by remember{ mutableStateOf<String?>(null) }
    var dragOffsetY by remember{ mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val itemSlotHeightPx = with(density){ 80.dp.toPx() }

    LaunchedEffect(uiState.errorMessage){
        if (uiState.errorMessage == "API_KEY_REQUIRED"){
            showApiKeyDialog = true
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = MacaronBackground,
        topBar ={
            SimoTopBar(
                title = if (uiState.isEditing) "Modifica Ricetta" else "Nuova Ricetta",
                showBackButton = true,
                onBackClick = onBackClick,
                actions ={
                    Button(
                        onClick ={ viewModel.saveRecipe(onSaveSuccess) },
                        colors = ButtonDefaults.buttonColors(containerColor = MacaronPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ){
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Salva", tint = MacaronCard)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Salva", color = MacaronCard, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ){ innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ){
            // Loading Gemini
            if (uiState.isAiLoading){
                item{
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5)),
                        border = BorderStroke(1.dp, Color(0xFFCE93D8))
                    ){
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ){
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ){
                                Text("✨", fontSize = 16.sp)
                                Text(
                                    text = "Elaborazione Assistente AI in corso...",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4A148C),
                                    fontSize = 14.sp
                                )
                            }
                            Text(
                                text = "Generazione ingredienti, bilanciamento, costi e procedimento.",
                                color = Color(0xFF6A1B9A),
                                fontSize = 12.sp
                            )
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = Color(0xFF8E24AA),
                                trackColor = Color(0xFFE1BEE7)
                            )
                        }
                    }
                }
            }

            // Messaggio di Successo / Info
            if (uiState.infoMessage != null){
                item{
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFA5D6A7)),
                        modifier = Modifier.fillMaxWidth()
                    ){
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ){
                            Text(
                                text = uiState.infoMessage!!,
                                color = Color(0xFF2E7D32),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick ={ viewModel.clearInfoMessage() }){
                                Text("OK", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Messaggio di errore
            if (uiState.errorMessage != null && uiState.errorMessage != "API_KEY_REQUIRED"){
                item{
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ){
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ){
                            Text(
                                text = uiState.errorMessage!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick ={ viewModel.clearError() }){
                                Text("Chiudi", color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Dati Generali
            item{
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MacaronCard),
                    border = BorderStroke(1.dp, MacaronCardBorder)
                ){
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ){
                        Text(
                            text = "Informazioni Principali",
                            color = MacaronText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        OutlinedTextField(
                            value = uiState.name,
                            onValueChange = viewModel::onNameChange,
                            label ={ Text("Nome della ricetta *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = uiState.description,
                            onValueChange = viewModel::onDescriptionChange,
                            label ={ Text("Breve descrizione (opzionale)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Text(
                            text = "Tag / Categoria * (scegli un tag o scrivine uno nuovo)",
                            color = MacaronText,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ){
                            uiState.availableTags.forEach{ tag ->
                                val isSelected = uiState.category.equals(tag, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick ={ viewModel.onCategoryChange(tag) },
                                    label ={ Text(tag) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MacaronPrimary,
                                        selectedLabelColor = Color.White,
                                        containerColor = MacaronBackground,
                                        labelColor = MacaronText
                                    ),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isSelected) MacaronPrimary else MacaronCardBorder
                                    )
                                )
                            }
                        }

                        OutlinedTextField(
                            value = uiState.category,
                            onValueChange = viewModel::onCategoryChange,
                            label ={ Text("Tag della ricetta (puoi crearne uno nuovo personalizzato)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ){
                            OutlinedTextField(
                                value = uiState.baseYield,
                                onValueChange = viewModel::onBaseYieldChange,
                                label ={ Text("Resa base *") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = uiState.yieldUnit,
                                onValueChange = viewModel::onYieldUnitChange,
                                label ={ Text("Unità resa (es. cornetti, porzioni) *") },
                                singleLine = true,
                                supportingText ={
                                    if (uiState.yieldUnit.isNotBlank()){
                                        val sing = ItalianGrammarUtils.toSingular(uiState.yieldUnit)
                                        val plur = ItalianGrammarUtils.toPlural(uiState.yieldUnit)
                                        Text("Sing: $sing • Plur: $plur", fontSize = 11.sp, color = MacaronSubtleText)
                                    }
                                },
                                modifier = Modifier.weight(1.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // Modificatore resa/peso in cottura (%)
                        OutlinedTextField(
                            value = uiState.cookingWeightModifierInput,
                            onValueChange = viewModel::onCookingWeightModifierChange,
                            label ={ Text("Modificatore resa in cottura (%)") },
                            placeholder ={ Text("es. -12 per calo forno, +120 per resa pasta") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            singleLine = true,
                            supportingText ={
                                val mod = uiState.cookingWeightModifier
                                val desc = when{
                                    mod < 0 -> "Calo peso / evaporazione in cottura: ${String.format(java.util.Locale.US, "%.1f", mod)}%"
                                    mod > 0 -> "Aumento peso / resa in cottura: +${String.format(java.util.Locale.US, "%.1f", mod)}%"
                                    else -> "Nessuna variazione di peso in cottura (0%)"
                                }
                                Text(desc, fontSize = 11.sp, color = MacaronSubtleText)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Assistente AI (Google Gemini & ChatGPT OpenAI)
            item{
                val activeProvider = viewModel.getPreferredAiProvider()
                val providerLabel = if (activeProvider == panattoni.simo.data.remote.AiProvider.OPENAI) "ChatGPT" else "Gemini"
                val hasAnyKey = viewModel.getGeminiApiKey().isNotBlank() || viewModel.getOpenAiApiKey().isNotBlank()

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5)),
                    border = BorderStroke(1.dp, Color(0xFFCE93D8))
                ){
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ){
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ){
                            Row(verticalAlignment = Alignment.CenterVertically){
                                Text("✨", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Assistente AI",
                                    color = Color(0xFF6A1B9A),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = Color(0xFF8E24AA).copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(6.dp)
                                ){
                                    Text(
                                        text = providerLabel,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF6A1B9A)
                                    )
                                }
                            }
                            TextButton(onClick ={ showApiKeyDialog = true }){
                                Text(
                                    text = if (hasAnyKey) "⚙️ Impostazioni" else "🔑 Configura API",
                                    fontSize = 12.sp,
                                    color = Color(0xFF6A1B9A),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = "Compila in automatico pesi e dosi bilanciate, costi di mercato, 14 allergeni ufficiali UE e passaggi numerati con l'Intelligenza Artificiale.",
                            color = Color(0xFF4A148C),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        Button(
                            onClick ={ showAiDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E24AA)),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !uiState.isAiLoading,
                            modifier = Modifier.fillMaxWidth()
                        ){
                            Text("🪄 Apri Assistente AI", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Sezione Ingredienti
            item{
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Text(
                        text = "Ingredienti e Ricette (${uiState.components.size})",
                        color = MacaronText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    OutlinedButton(
                        onClick ={ showAddDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MacaronPrimary)
                    ){
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = MacaronPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Aggiungi", color = MacaronPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Lista componenti aggiunti
            if (uiState.components.isEmpty()){
                item{
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MacaronSurfaceVariant,
                        shape = RoundedCornerShape(14.dp)
                    ){
                        Text(
                            text = "Nessun ingrediente ancora aggiunto. Tocca \"Aggiungi\" per inserire ingredienti semplici o collegare altre ricette.",
                            color = MacaronSubtleText,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else{
                if (uiState.components.size > 1){
                    item{
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 2.dp)
                        ){
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = null,
                                tint = MacaronPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Tieni premuto un ingrediente per trascinarlo su o giù, oppure usa le frecce.",
                                fontSize = 11.sp,
                                color = MacaronSubtleText
                            )
                        }
                    }
                }

                itemsIndexed(uiState.components, key ={ _, comp -> comp.tempId }){ index, comp ->
                    val isCurrentlyDragged = activeDragId == comp.tempId
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(if (isCurrentlyDragged) 10f else 1f)
                            .graphicsLayer{
                                if (isCurrentlyDragged){
                                    translationY = dragOffsetY
                                    scaleX = 1.02f
                                    scaleY = 1.02f
                                }
                            }
                            .pointerInput(comp.tempId){
                                detectDragGesturesAfterLongPress(
                                    onDragStart ={
                                        activeDragId = comp.tempId
                                        dragOffsetY = 0f
                                    },
                                    onDrag ={ change, dragAmount ->
                                        change.consume()
                                        dragOffsetY += dragAmount.y
                                        while (dragOffsetY > itemSlotHeightPx){
                                            val list = viewModel.uiState.value.components
                                            val curIdx = list.indexOfFirst{ it.tempId == activeDragId }
                                            if (curIdx != -1 && curIdx < list.size - 1){
                                                viewModel.moveComponent(curIdx, curIdx + 1)
                                                dragOffsetY -= itemSlotHeightPx
                                            } else{
                                                break
                                            }
                                        }
                                        while (dragOffsetY < -itemSlotHeightPx){
                                            val list = viewModel.uiState.value.components
                                            val curIdx = list.indexOfFirst{ it.tempId == activeDragId }
                                            if (curIdx != -1 && curIdx > 0){
                                                viewModel.moveComponent(curIdx, curIdx - 1)
                                                dragOffsetY += itemSlotHeightPx
                                            } else{
                                                break
                                            }
                                        }
                                    },
                                    onDragEnd ={
                                        activeDragId = null
                                        dragOffsetY = 0f
                                    },
                                    onDragCancel ={
                                        activeDragId = null
                                        dragOffsetY = 0f
                                    }
                                )
                            }
                            .clickable{ editingComponent = comp },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrentlyDragged) MacaronPrimary.copy(alpha = 0.15f) else MacaronCard
                        ),
                        border = BorderStroke(
                            width = if (isCurrentlyDragged) 2.dp else 1.dp,
                            color = if (isCurrentlyDragged) MacaronPrimary else MacaronCardBorder
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrentlyDragged) 8.dp else 2.dp)
                    ){
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ){
                            // Maniglia di trascinamento visiva laterale
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isCurrentlyDragged) MacaronPrimary.copy(alpha = 0.2f) else MacaronSurfaceVariant,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(32.dp)
                            ){
                                Box(contentAlignment = Alignment.Center){
                                    Icon(
                                        imageVector = Icons.Default.DragHandle,
                                        contentDescription = "Tieni premuto a lato per trascinare in qualsiasi posizione",
                                        tint = if (isCurrentlyDragged) MacaronPrimary else MacaronSubtleText,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)){
                                if (comp.itemType == ItemType.RECIPE){
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MacaronRecipeBadge.copy(alpha = 0.12f),
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    ){
                                        Text(
                                            text = "RICETTA",
                                            color = MacaronRecipeBadge,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = comp.itemName,
                                    color = MacaronText,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                                val isMissingDose = !MeasurementUnit.isQb(comp.unit) && (comp.quantity.isBlank() || comp.quantity.toDoubleOrNull() == null || comp.quantity.toDoubleOrNull()!! <= 0.0)
                                if (isMissingDose){
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFFFF3E0),
                                        border = BorderStroke(1.dp, Color(0xFFFFB74D)),
                                        modifier = Modifier.clickable{ editingComponent = comp }
                                    ){
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ){
                                            Text(
                                                text = "⚠️ Dose non definita (${comp.unit}) • Tocca qui",
                                                color = Color(0xFFE65100),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                } else{
                                    val displayText = if (MeasurementUnit.isQb(comp.unit)){
                                        "q.b."
                                    } else{
                                        "${comp.quantity} ${comp.unit}"
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable{ editingComponent = comp }
                                    ){
                                        Text(
                                            text = displayText,
                                            color = MacaronPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        if (comp.pricePerUnit != null && comp.pricePerUnit > 0.0){
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFE8F5E9)
                                            ){
                                                val priceFormatted = String.format(java.util.Locale.US, "€%.2f", comp.pricePerUnit)
                                                Text(
                                                    text = "$priceFormatted/${comp.priceUnit ?: comp.unit}",
                                                    color = Color(0xFF2E7D32),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (comp.allergens.isNotEmpty()){
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = if (comp.itemType != ItemType.RECIPE) Modifier.clickable{ editingComponent = comp } else Modifier
                                    ){
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFFFF3E0),
                                            border = BorderStroke(0.5.dp, Color(0xFFFFB74D))
                                        ){
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ){
                                                val emojis = comp.allergens.joinToString(" "){ it.emoji }
                                                val names = comp.allergens.joinToString(", "){ it.displayName }
                                                Text(
                                                    text = "$emojis $names",
                                                    color = Color(0xFFE65100),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                if (comp.itemType != ItemType.RECIPE){
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Modifica ingrediente",
                                                        tint = Color(0xFFE65100),
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else if (comp.itemType != ItemType.RECIPE){
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MacaronSurfaceVariant,
                                        modifier = Modifier.clickable{ editingComponent = comp }
                                    ){
                                        Text(
                                            text = "+ Allergeni",
                                            color = MacaronSubtleText,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ){
                                // Pulsanti rapidi Sposta Su e Sposta Giù
                                if (index > 0){
                                    IconButton(
                                        onClick ={ viewModel.moveComponentUp(index) },
                                        modifier = Modifier.size(28.dp)
                                    ){
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowUp,
                                            contentDescription = "Sposta sopra",
                                            tint = MacaronPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                if (index < uiState.components.size - 1){
                                    IconButton(
                                        onClick ={ viewModel.moveComponentDown(index) },
                                        modifier = Modifier.size(28.dp)
                                    ){
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Sposta sotto",
                                            tint = MacaronPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick ={ editingComponent = comp },
                                    modifier = Modifier.size(28.dp)
                                ){
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Modifica ingrediente",
                                        tint = MacaronPrimary,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                                IconButton(
                                    onClick ={ viewModel.removeComponent(comp.tempId) },
                                    modifier = Modifier.size(28.dp)
                                ){
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Rimuovi",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Istruzioni
            item{
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MacaronCard),
                    border = BorderStroke(1.dp, MacaronCardBorder)
                ){
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ){
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ){
                            Text(
                                text = "Procedimento di preparazione",
                                color = MacaronText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            if (uiState.components.isNotEmpty()){
                                TextButton(
                                    onClick ={ viewModel.generateInstructionsWithAi() },
                                    enabled = !uiState.isAiLoading
                                ){
                                    Text("✨ Genera con AI", color = MacaronPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        OutlinedTextField(
                            value = uiState.instructions,
                            onValueChange = viewModel::onInstructionsChange,
                            placeholder ={ Text("Descrivi i passaggi di preparazione...") },
                            minLines = 4,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }
    }

    // Dialog per modificare nome e valori dell'ingrediente
    if (editingComponent != null){
        val comp = editingComponent!!
        EditIngredientDetailsDialog(
            component = comp,
            onDismiss ={ editingComponent = null },
            onConfirm ={ newName, newQty, newUnit, newPrice, newPriceUnit, newAllergens, newCalories, newCarbs, newFats, newProteins, newWater, newSodium ->
                viewModel.updateComponentDetails(
                    tempId = comp.tempId,
                    newName = newName,
                    newQuantity = newQty,
                    newUnit = newUnit,
                    newPrice = newPrice,
                    newPriceUnit = newPriceUnit,
                    newAllergens = newAllergens,
                    newCalories = newCalories,
                    newCarbs = newCarbs,
                    newFats = newFats,
                    newProteins = newProteins,
                    newWater = newWater,
                    newSodium = newSodium
                )
                editingComponent = null
            }
        )
    }

    // Dialog Assistente AI (Google Gemini & ChatGPT)
    if (showAiDialog){
        GeminiAssistantDialog(
            recipeName = uiState.name,
            hasIngredients = uiState.components.isNotEmpty(),
            isLoading = uiState.isAiLoading,
            preferredProvider = viewModel.getPreferredAiProvider(),
            preferredGeminiModelId = viewModel.getGeminiPreferredModel(),
            preferredOpenAiModelId = viewModel.getOpenAiPreferredModel(),
            onDismiss ={ showAiDialog = false },
            onGenerateFullRecipe ={ customPrompt ->
                viewModel.generateRecipeWithAi(customPrompt, onComplete ={ showAiDialog = false })
            },
            onCompleteMissing ={ customPrompt ->
                viewModel.completeMissingWithAi(customPrompt, onComplete ={ showAiDialog = false })
            },
            onGenerateInstructions ={ customPrompt ->
                viewModel.generateInstructionsWithAi(customPrompt, onComplete ={ showAiDialog = false })
            },
            onEnrichNutrition ={
                viewModel.enrichNutritionalValuesWithAi(onComplete ={ showAiDialog = false })
            },
            onOpenApiKeyConfig ={
                showAiDialog = false
                showApiKeyDialog = true
            }
        )
    }

    // Dialog Configurazione Impostazioni e Modelli AI (Gemini & ChatGPT)
    if (showApiKeyDialog){
        GeminiApiKeyDialog(
            currentProvider = viewModel.getPreferredAiProvider(),
            currentGeminiApiKey = viewModel.getGeminiApiKey(),
            currentGeminiModelId = viewModel.getGeminiPreferredModel(),
            currentOpenAiApiKey = viewModel.getOpenAiApiKey(),
            currentOpenAiModelId = viewModel.getOpenAiPreferredModel(),
            onDismiss ={ showApiKeyDialog = false },
            onSave ={ provider, geminiKey, geminiModel, openAiKey, openAiModel ->
                viewModel.saveAiSettings(provider, geminiKey, geminiModel, openAiKey, openAiModel)
                showApiKeyDialog = false
            }
        )
    }

    // Dialog per aggiungere un ingrediente o sotto-ricetta
    if (showAddDialog){
        AddComponentDialog(
            availableItems = uiState.availableItems,
            onDismiss ={ showAddDialog = false },
            onAddExisting ={ item, quantity, unit, pricePerUnit, priceUnit, allergens ->
                viewModel.addComponent(item, quantity, unit, pricePerUnit, priceUnit, allergens)
                showAddDialog = false
            },
            onCreateNew ={ name, quantity, unit, pricePerUnit, priceUnit, allergens ->
                viewModel.createAndAddRawIngredient(name, quantity, unit, pricePerUnit, priceUnit, allergens)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddComponentDialog(
    availableItems: List<Item>,
    onDismiss: () -> Unit,
    onAddExisting: (Item, String, String, Double?, String?, List<Allergen>?) -> Unit,
    onCreateNew: (String, String, String, Double?, String?, List<Allergen>) -> Unit
){
    // "Nuovo Ingrediente" è l'opzione di default come richiesto
    var isNewItemMode by remember{ mutableStateOf(true) }
    var selectedAllergens by remember{ mutableStateOf<Set<Allergen>>(emptySet()) }
    var recipeSearchQuery by remember{ mutableStateOf("") }

    // Filtro per sole ricette: barra vuota non mostra nulla, digita per mostrare le prime 5 in ordine alfabetico
    val matchingRecipes = remember(availableItems, recipeSearchQuery){
        if (recipeSearchQuery.isBlank()){
            emptyList()
        } else{
            availableItems
                .filter{ it.type == ItemType.RECIPE && it.name.contains(recipeSearchQuery.trim(), ignoreCase = true) }
                .sortedBy{ it.name.lowercase() }
                .take(5)
        }
    }

    var selectedItem by remember{ mutableStateOf<Item?>(null) }
    var newIngredientName by remember{ mutableStateOf("") }
    var selectedUnit by remember{ mutableStateOf(MeasurementUnit.GRAM.code) }
    var quantityInput by remember{ mutableStateOf(MeasurementUnit.getDefaultQuantityForUnit(selectedUnit)) }
    var unitMenuExpanded by remember{ mutableStateOf(false) }

    // Campi Costo Unitario
    var priceInput by remember{ mutableStateOf("") }
    var priceUnitSelected by remember{ mutableStateOf("kg") }
    var priceUnitMenuExpanded by remember{ mutableStateOf(false) }

    val isQb = MeasurementUnit.isQb(selectedUnit)
    val isInteger = MeasurementUnit.isIntegerUnit(selectedUnit)

    val allowedPriceUnits = when {
        isQb -> listOf("kg", "g", "l", "ml", "pz", "unità")
        selectedUnit.lowercase() in listOf("g", "kg") -> listOf("kg", "g")
        selectedUnit.lowercase() in listOf("ml", "l") -> listOf("l", "ml")
        else -> listOf(selectedUnit)
    }

    LaunchedEffect(selectedUnit){
        if (priceUnitSelected !in allowedPriceUnits){
            priceUnitSelected = when {
                selectedUnit.lowercase() in listOf("g", "kg") -> "kg"
                selectedUnit.lowercase() in listOf("ml", "l") -> "l"
                isQb -> "kg"
                else -> selectedUnit
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MacaronCard,
        shape = RoundedCornerShape(20.dp),
        title ={
            Text(
                text = "Aggiungi Ingrediente o Ricetta",
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
                // Toggle: "Nuovo Ingrediente" (DEFAULT) vs "Da Ricetta"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ){
                    Button(
                        onClick ={ isNewItemMode = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isNewItemMode) MacaronPrimary else MacaronSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ){
                        Text(
                            "Nuovo Ingrediente",
                            color = if (isNewItemMode) MacaronCard else MacaronText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick ={
                            isNewItemMode = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isNewItemMode) MacaronPrimary else MacaronSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ){
                        Text(
                            "Da Ricetta",
                            color = if (!isNewItemMode) MacaronCard else MacaronText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (isNewItemMode){
                    OutlinedTextField(
                        value = newIngredientName,
                        onValueChange ={ newIngredientName = it },
                        label ={ Text("Nome ingrediente (qualsiasi)") },
                        placeholder ={ Text("es. Farina di mandorle, Tuorli, Zafferano...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else{
                    // Ricerca e selezione tra le ricette esistenti
                    OutlinedTextField(
                        value = recipeSearchQuery,
                        onValueChange ={ recipeSearchQuery = it },
                        label ={ Text("Cerca ricetta da collegare") },
                        placeholder ={ Text("Digita nome ricetta...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Quando la barra di ricerca è vuota non compare nulla
                    if (recipeSearchQuery.isNotBlank()){
                        if (matchingRecipes.isEmpty()){
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MacaronSurfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ){
                                Text(
                                    text = "Nessuna ricetta corrispondente a \"$recipeSearchQuery\"",
                                    color = MacaronSubtleText,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        } else{
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ){
                                matchingRecipes.forEach{ recipeItem ->
                                    val isSelected = selectedItem?.id == recipeItem.id
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) MacaronPrimary.copy(alpha = 0.15f) else MacaronSurfaceVariant,
                                        border = BorderStroke(1.dp, if (isSelected) MacaronPrimary else Color.Transparent),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable{
                                                selectedItem = recipeItem
                                                selectedUnit = recipeItem.defaultUnit
                                                quantityInput = MeasurementUnit.getDefaultQuantityForUnit(recipeItem.defaultUnit)
                                                selectedAllergens = recipeItem.allergensList.toSet()
                                            }
                                    ){
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ){
                                            Surface(
                                                color = MacaronRecipeBadge.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(4.dp),
                                                modifier = Modifier.padding(end = 6.dp)
                                            ){
                                                Text(
                                                    "RICETTA",
                                                    color = MacaronRecipeBadge,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text(
                                                text = recipeItem.name,
                                                color = MacaronText,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else if (selectedItem != null){
                        // Mostra la ricetta selezionata se la barra viene svuotata
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MacaronPrimary.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, MacaronPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ){
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ){
                                Surface(
                                    color = MacaronRecipeBadge.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.padding(end = 6.dp)
                                ){
                                    Text(
                                        "SELEZIONATA",
                                        color = MacaronRecipeBadge,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = selectedItem!!.name,
                                    color = MacaronText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                // Quantità e Selettore Unità di Misura
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    if (!isQb){
                        OutlinedTextField(
                            value = quantityInput,
                            onValueChange ={ input ->
                                if (isInteger){
                                    // Solo cifre intere, niente virgola o decimali
                                    quantityInput = input.filter{ it.isDigit() }
                                } else{
                                    quantityInput = input
                                }
                            },
                            label ={ Text("Dose") },
                            placeholder ={ Text(if (isInteger) "es. 2 (vuoto = AI)" else "es. 100 (vuoto = AI)") },
                            isError = quantityInput.isNotBlank() && !NumberUtils.isValidPositiveNumber(quantityInput),
                            keyboardOptions = KeyboardOptions(keyboardType = if (isInteger) KeyboardType.Number else KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Selettore Unità (g, ml, unità, pz, cucchiaio, pizzico, q.b., etc.)
                    Box(modifier = if (!isQb) Modifier.weight(1.2f) else Modifier.fillMaxWidth()){
                        OutlinedTextField(
                            value = selectedUnit,
                            onValueChange ={},
                            readOnly = true,
                            label ={ Text("Unità") },
                            trailingIcon ={ Icon(Icons.Default.ArrowDropDown, null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable{ unitMenuExpanded = true }
                        )
                        DropdownMenu(
                            expanded = unitMenuExpanded,
                            onDismissRequest ={ unitMenuExpanded = false }
                        ){
                            MeasurementUnit.ALL_UNITS.forEach{ unit ->
                                DropdownMenuItem(
                                    text ={ Text("${unit.code} - ${unit.displayName}") },
                                    onClick ={
                                        val prevUnit = selectedUnit
                                        selectedUnit = unit.code
                                        if (MeasurementUnit.isQb(unit.code)){
                                            quantityInput = ""
                                        } else if (unit.code.equals(MeasurementUnit.GRAM.code, ignoreCase = true) || unit.code.equals(MeasurementUnit.MILLILITER.code, ignoreCase = true)){
                                            if (quantityInput == "1" || quantityInput.isBlank() || MeasurementUnit.isQb(prevUnit)){
                                                quantityInput = "100"
                                            }
                                        } else if (MeasurementUnit.isIntegerUnit(unit.code)){
                                            if (quantityInput == "100" || quantityInput.isBlank() || MeasurementUnit.isQb(prevUnit)){
                                                quantityInput = "1"
                                            } else{
                                                val currentNum = NumberUtils.parseDoubleOrNull(quantityInput)
                                                quantityInput = if (currentNum != null && currentNum > 0.0){
                                                    MeasurementUnit.roundIntegerAmount(currentNum).toString()
                                                } else{
                                                    "1"
                                                }
                                            }
                                        } else{
                                            if (quantityInput == "100" || quantityInput.isBlank() || MeasurementUnit.isQb(prevUnit)){
                                                quantityInput = "1"
                                            }
                                        }
                                        unitMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (isQb){
                    Text(
                        text = "La dose 'quanto basta' (q.b.) non richiede l'inserimento di un valore numerico.",
                        color = MacaronSubtleText,
                        fontSize = 12.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }

                // Sezione Costo Unitario (Opzionale, ma fondamentale per il Food Cost)
                if (isNewItemMode || selectedItem?.type != ItemType.RECIPE){
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MacaronSurfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MacaronCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ){
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ){
                            Text(
                                text = "💰 Costo Unitario (Food Cost)",
                                color = MacaronText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ){
                                OutlinedTextField(
                                    value = priceInput,
                                    onValueChange ={ priceInput = it },
                                    label ={ Text("Prezzo (€)") },
                                    placeholder ={ Text("es. 1.20") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                Box(modifier = Modifier.weight(1f)){
                                    OutlinedTextField(
                                        value = "/ $priceUnitSelected",
                                        onValueChange ={},
                                        readOnly = true,
                                        label ={ Text("Per") },
                                        trailingIcon ={
                                            if (allowedPriceUnits.size > 1){
                                                Icon(Icons.Default.ArrowDropDown, null)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    if (allowedPriceUnits.size > 1){
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .clickable{ priceUnitMenuExpanded = true }
                                        )
                                        DropdownMenu(
                                            expanded = priceUnitMenuExpanded,
                                            onDismissRequest ={ priceUnitMenuExpanded = false }
                                        ){
                                            allowedPriceUnits.forEach{ pUnit ->
                                                DropdownMenuItem(
                                                    text ={ Text("al $pUnit") },
                                                    onClick ={
                                                        priceUnitSelected = pUnit
                                                        priceUnitMenuExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Sezione Allergeni
                if (isNewItemMode || selectedItem?.type != ItemType.RECIPE){
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MacaronSurfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MacaronCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ){
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ){
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ){
                                Text(
                                    text = "⚠️ Allergeni (Reg. UE 1169/2011)",
                                    color = MacaronText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                if (selectedAllergens.isNotEmpty()){
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFFFF3E0)
                                    ){
                                        Text(
                                            text = "${selectedAllergens.size} selezionati",
                                            color = Color(0xFFE65100),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Tocca per contrassegnare gli allergeni contenuti:",
                                color = MacaronSubtleText,
                                fontSize = 11.sp
                            )
                            AllergenSelector(
                                selectedAllergens = selectedAllergens,
                                onToggleAllergen ={ allergen ->
                                    selectedAllergens = if (selectedAllergens.contains(allergen)){
                                        selectedAllergens - allergen
                                    } else{
                                        selectedAllergens + allergen
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton ={
            val canConfirm = if (isNewItemMode){
                newIngredientName.isNotBlank() && (isQb || quantityInput.isBlank() || NumberUtils.isValidPositiveNumber(quantityInput))
            } else{
                selectedItem != null && (isQb || quantityInput.isBlank() || NumberUtils.isValidPositiveNumber(quantityInput))
            }

            Button(
                onClick ={
                    val finalQty = if (isQb || quantityInput.isBlank()){
                        ""
                    } else if (isInteger){
                        val parsed = NumberUtils.parseDoubleSafe(quantityInput, 1.0)
                        MeasurementUnit.roundIntegerAmount(parsed).toString()
                    } else{
                        quantityInput.trim()
                    }

                    val parsedPrice = NumberUtils.parseDoubleOrNull(priceInput)
                    val effectivePrice = if (parsedPrice != null && parsedPrice > 0.0) parsedPrice else null
                    val effectivePriceUnit = if (effectivePrice != null) priceUnitSelected else null
                    val allergensList = Allergen.entries.filter{ selectedAllergens.contains(it) }

                    if (isNewItemMode){
                        if (newIngredientName.isNotBlank()){
                            onCreateNew(newIngredientName.trim(), finalQty, selectedUnit, effectivePrice, effectivePriceUnit, allergensList)
                        }
                    } else{
                        selectedItem?.let{
                            val itemPrice = effectivePrice ?: it.pricePerUnit
                            val itemPUnit = effectivePriceUnit ?: it.priceUnit
                            val finalAllergens = if (it.type == ItemType.RECIPE) null else allergensList
                            onAddExisting(it, finalQty, selectedUnit, itemPrice, itemPUnit, finalAllergens)
                        }
                    }
                },
                enabled = canConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MacaronPrimary),
                shape = RoundedCornerShape(10.dp)
            ){
                Text("Inserisci", color = MacaronCard, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton ={
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ){
                Text("Annulla", color = MacaronSubtleText)
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AllergenSelector(
    selectedAllergens: Set<Allergen>,
    onToggleAllergen: (Allergen) -> Unit,
    modifier: Modifier = Modifier
){
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ){
        Allergen.entries.forEach{ allergen ->
            val isSelected = selectedAllergens.contains(allergen)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) MacaronPrimary else MacaronSurfaceVariant,
                border = BorderStroke(1.dp, if (isSelected) MacaronPrimary else MacaronCardBorder),
                modifier = Modifier.clickable{ onToggleAllergen(allergen) }
            ){
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Text(
                        text = allergen.emoji,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = allergen.displayName,
                        color = if (isSelected) MacaronCard else MacaronText,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun EditIngredientDetailsDialog(
    component: EditableComponent,
    onDismiss: () -> Unit,
    onConfirm: (newName: String, newQuantity: String, newUnit: String, newPrice: Double?, newPriceUnit: String?, newAllergens: List<Allergen>, newCalories: Double?, newCarbs: Double?, newFats: Double?, newProteins: Double?, newWater: Double?, newSodium: Double?) -> Unit
){
    var nameInput by remember{ mutableStateOf(component.itemName) }
    var selectedUnit by remember{ mutableStateOf(component.unit.ifBlank{ MeasurementUnit.GRAM.code }) }
    var quantityInput by remember{ mutableStateOf(component.quantity) }
    var unitMenuExpanded by remember{ mutableStateOf(false) }

    var priceInput by remember{
        mutableStateOf(component.pricePerUnit?.let{ if (it == 0.0) "" else String.format(java.util.Locale.US, "%.2f", it) } ?: "")
    }
    var priceUnitSelected by remember{
        mutableStateOf(
            component.priceUnit ?: when{
                component.unit.lowercase() in listOf("g", "kg") -> "kg"
                component.unit.lowercase() in listOf("ml", "l") -> "l"
                else -> component.unit.ifBlank{ "kg" }
            }
        )
    }
    var priceUnitMenuExpanded by remember{ mutableStateOf(false) }
    var selectedAllergens by remember{ mutableStateOf<Set<Allergen>>(component.allergens.toSet()) }

    var caloriesInput by remember{
        mutableStateOf(component.caloriesKcal?.let{ if (it == 0.0) "" else String.format(java.util.Locale.US, "%.1f", it).replace(".0", "") } ?: "")
    }
    var carbsInput by remember{
        mutableStateOf(component.carbohydratesG?.let{ if (it == 0.0) "" else String.format(java.util.Locale.US, "%.1f", it).replace(".0", "") } ?: "")
    }
    var fatsInput by remember{
        mutableStateOf(component.fatsG?.let{ if (it == 0.0) "" else String.format(java.util.Locale.US, "%.1f", it).replace(".0", "") } ?: "")
    }
    var proteinsInput by remember{
        mutableStateOf(component.proteinsG?.let{ if (it == 0.0) "" else String.format(java.util.Locale.US, "%.1f", it).replace(".0", "") } ?: "")
    }
    var waterInput by remember{
        mutableStateOf(component.waterG?.let{ if (it == 0.0) "" else String.format(java.util.Locale.US, "%.1f", it).replace(".0", "") } ?: "")
    }
    var sodiumInput by remember{
        mutableStateOf(component.sodiumMg?.let{ if (it == 0.0) "" else String.format(java.util.Locale.US, "%.1f", it).replace(".0", "") } ?: "")
    }

    val isQb = MeasurementUnit.isQb(selectedUnit)
    val isInteger = MeasurementUnit.isIntegerUnit(selectedUnit)

    val allowedPriceUnits = when{
        isQb -> listOf("kg", "g", "l", "ml", "pz", "unità")
        selectedUnit.lowercase() in listOf("g", "kg") -> listOf("kg", "g")
        selectedUnit.lowercase() in listOf("ml", "l") -> listOf("l", "ml")
        else -> listOf(selectedUnit)
    }

    LaunchedEffect(selectedUnit){
        if (priceUnitSelected !in allowedPriceUnits){
            priceUnitSelected = when{
                selectedUnit.lowercase() in listOf("g", "kg") -> "kg"
                selectedUnit.lowercase() in listOf("ml", "l") -> "l"
                isQb -> "kg"
                else -> selectedUnit
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MacaronCard,
        shape = RoundedCornerShape(20.dp),
        title ={
            Text(
                text = if (component.itemType == ItemType.RECIPE) "✏️ Modifica Ricetta" else "✏️ Modifica Ingrediente",
                color = MacaronPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text ={
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ){
                // Nome Ingrediente
                OutlinedTextField(
                    value = nameInput,
                    onValueChange ={ nameInput = it },
                    label ={ Text("Nome ingrediente") },
                    placeholder ={ Text("es. Farina, Zucchero...") },
                    enabled = component.itemType != ItemType.RECIPE,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Quantità e Unità di Misura
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    if (!isQb){
                        OutlinedTextField(
                            value = quantityInput,
                            onValueChange ={ input ->
                                if (isInteger){
                                    quantityInput = input.filter{ it.isDigit() }
                                } else{
                                    quantityInput = input
                                }
                            },
                            label ={ Text("Dose") },
                            placeholder ={ Text(if (isInteger) "es. 2" else "es. 100") },
                            isError = quantityInput.isNotBlank() && !NumberUtils.isValidPositiveNumber(quantityInput),
                            keyboardOptions = KeyboardOptions(keyboardType = if (isInteger) KeyboardType.Number else KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Box(modifier = if (!isQb) Modifier.weight(1.2f) else Modifier.fillMaxWidth()){
                        OutlinedTextField(
                            value = selectedUnit,
                            onValueChange ={},
                            readOnly = true,
                            label ={ Text("Unità") },
                            trailingIcon ={ Icon(Icons.Default.ArrowDropDown, null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable{ unitMenuExpanded = true }
                        )
                        DropdownMenu(
                            expanded = unitMenuExpanded,
                            onDismissRequest ={ unitMenuExpanded = false }
                        ){
                            MeasurementUnit.ALL_UNITS.forEach{ unit ->
                                DropdownMenuItem(
                                    text ={ Text("${unit.code} - ${unit.displayName}") },
                                    onClick ={
                                        val prevUnit = selectedUnit
                                        selectedUnit = unit.code
                                        if (MeasurementUnit.isQb(unit.code)){
                                            quantityInput = ""
                                        } else if (unit.code.equals(MeasurementUnit.GRAM.code, ignoreCase = true) || unit.code.equals(MeasurementUnit.MILLILITER.code, ignoreCase = true)){
                                            if (quantityInput == "1" || quantityInput.isBlank() || MeasurementUnit.isQb(prevUnit)){
                                                quantityInput = "100"
                                            }
                                        } else if (MeasurementUnit.isIntegerUnit(unit.code)){
                                            if (quantityInput == "100" || quantityInput.isBlank() || MeasurementUnit.isQb(prevUnit)){
                                                quantityInput = "1"
                                            }
                                        }
                                        unitMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (isQb){
                    Text(
                        text = "La dose 'quanto basta' (q.b.) non richiede l'inserimento di un valore numerico.",
                        color = MacaronSubtleText,
                        fontSize = 12.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }

                // Costo Unitario (Food Cost)
                if (component.itemType != ItemType.RECIPE){
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MacaronSurfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MacaronCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ){
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ){
                            Text(
                                text = "💰 Costo Unitario (Food Cost)",
                                color = MacaronText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ){
                                OutlinedTextField(
                                    value = priceInput,
                                    onValueChange ={ priceInput = it },
                                    label ={ Text("Prezzo (€)") },
                                    placeholder ={ Text("es. 1.20") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                Box(modifier = Modifier.weight(1f)){
                                    OutlinedTextField(
                                        value = "/ $priceUnitSelected",
                                        onValueChange ={},
                                        readOnly = true,
                                        label ={ Text("Per") },
                                        trailingIcon ={
                                            if (allowedPriceUnits.size > 1){
                                                Icon(Icons.Default.ArrowDropDown, null)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    if (allowedPriceUnits.size > 1){
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .clickable{ priceUnitMenuExpanded = true }
                                        )
                                        DropdownMenu(
                                            expanded = priceUnitMenuExpanded,
                                            onDismissRequest ={ priceUnitMenuExpanded = false }
                                        ){
                                            allowedPriceUnits.forEach{ pUnit ->
                                                DropdownMenuItem(
                                                    text ={ Text("al $pUnit") },
                                                    onClick ={
                                                        priceUnitSelected = pUnit
                                                        priceUnitMenuExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Sezione Allergeni
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MacaronSurfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MacaronCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ){
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ){
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ){
                                Text(
                                    text = "⚠️ Allergeni (Reg. UE 1169/2011)",
                                    color = MacaronText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                if (selectedAllergens.isNotEmpty()){
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFFFF3E0)
                                    ){
                                        Text(
                                            text = "${selectedAllergens.size} selezionati",
                                            color = Color(0xFFE65100),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Tocca per contrassegnare gli allergeni contenuti:",
                                color = MacaronSubtleText,
                                fontSize = 11.sp
                            )
                            AllergenSelector(
                                selectedAllergens = selectedAllergens,
                                onToggleAllergen ={ allergen ->
                                    selectedAllergens = if (selectedAllergens.contains(allergen)){
                                        selectedAllergens - allergen
                                    } else{
                                        selectedAllergens + allergen
                                    }
                                }
                            )
                        }
                    }

                    // Sezione Valori Nutrizionali per 100g (Reg. UE 1169/2011)
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFE8F5E9).copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, Color(0xFFA5D6A7)),
                        modifier = Modifier.fillMaxWidth()
                    ){
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ){
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ){
                                Text(
                                    text = "🥗 Valori Nutrizionali (per 100g)",
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Reg. UE 1169/2011",
                                    color = Color(0xFF388E3C),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ){
                                OutlinedTextField(
                                    value = caloriesInput,
                                    onValueChange = { caloriesInput = it },
                                    label = { Text("Calorie (kcal)", fontSize = 11.sp) },
                                    placeholder = { Text("es. 364", fontSize = 11.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                OutlinedTextField(
                                    value = carbsInput,
                                    onValueChange = { carbsInput = it },
                                    label = { Text("Carboidrati (g)", fontSize = 11.sp) },
                                    placeholder = { Text("es. 72", fontSize = 11.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ){
                                OutlinedTextField(
                                    value = fatsInput,
                                    onValueChange = { fatsInput = it },
                                    label = { Text("Grassi (g)", fontSize = 11.sp) },
                                    placeholder = { Text("es. 1.5", fontSize = 11.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                OutlinedTextField(
                                    value = proteinsInput,
                                    onValueChange = { proteinsInput = it },
                                    label = { Text("Proteine (g)", fontSize = 11.sp) },
                                    placeholder = { Text("es. 12", fontSize = 11.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ){
                                OutlinedTextField(
                                    value = waterInput,
                                    onValueChange = { waterInput = it },
                                    label = { Text("Acqua (g)", fontSize = 11.sp) },
                                    placeholder = { Text("es. 14", fontSize = 11.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                OutlinedTextField(
                                    value = sodiumInput,
                                    onValueChange = { sodiumInput = it },
                                    label = { Text("Sodio (mg)", fontSize = 11.sp) },
                                    placeholder = { Text("es. 5", fontSize = 11.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton ={
            val canConfirm = (component.itemType == ItemType.RECIPE || nameInput.isNotBlank()) &&
                (isQb || quantityInput.isBlank() || NumberUtils.isValidPositiveNumber(quantityInput))
            Button(
                onClick ={
                    val finalPrice = priceInput.replace(',', '.').toDoubleOrNull()
                    val effectivePriceUnit = if (finalPrice != null && finalPrice > 0.0) priceUnitSelected else null
                    val cal = caloriesInput.replace(',', '.').toDoubleOrNull()
                    val carb = carbsInput.replace(',', '.').toDoubleOrNull()
                    val fat = fatsInput.replace(',', '.').toDoubleOrNull()
                    val prot = proteinsInput.replace(',', '.').toDoubleOrNull()
                    val wat = waterInput.replace(',', '.').toDoubleOrNull()
                    val sod = sodiumInput.replace(',', '.').toDoubleOrNull()
                    onConfirm(
                        nameInput.trim(),
                        quantityInput.trim(),
                        selectedUnit,
                        finalPrice,
                        effectivePriceUnit,
                        selectedAllergens.toList(),
                        cal,
                        carb,
                        fat,
                        prot,
                        wat,
                        sod
                    )
                },
                enabled = canConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MacaronPrimary),
                shape = RoundedCornerShape(10.dp)
            ){
                Text("Salva Modifiche", color = MacaronCard, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton ={
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ){
                Text("Annulla", color = MacaronSubtleText)
            }
        }
    )
}

@Composable
fun GeminiAssistantDialog(
    recipeName: String,
    hasIngredients: Boolean,
    isLoading: Boolean,
    preferredProvider: AiProvider = AiProvider.DEFAULT,
    preferredGeminiModelId: String = GeminiModel.DEFAULT.modelId,
    preferredOpenAiModelId: String = OpenAiModel.DEFAULT.modelId,
    onDismiss: () -> Unit,
    onGenerateFullRecipe: (customPrompt: String?) -> Unit,
    onCompleteMissing: (customPrompt: String?) -> Unit,
    onGenerateInstructions: (customPrompt: String?) -> Unit,
    onEnrichNutrition: () -> Unit,
    onOpenApiKeyConfig: () -> Unit
){
    var customPrompt by remember{ mutableStateOf("") }
    val geminiModel = GeminiModel.fromModelId(preferredGeminiModelId)
    val openAiModel = OpenAiModel.fromModelId(preferredOpenAiModelId)
    val isChatGpt = preferredProvider == AiProvider.OPENAI
    val activeDisplayName = if (isChatGpt) "ChatGPT (${openAiModel.displayName})" else "Gemini (${geminiModel.displayName})"

    AlertDialog(
        onDismissRequest ={ if (!isLoading) onDismiss() },
        containerColor = MacaronCard,
        shape = RoundedCornerShape(20.dp),
        title ={
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ){
                Row(verticalAlignment = Alignment.CenterVertically){
                    Text("✨", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Assistente AI",
                        color = Color(0xFF6A1B9A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                TextButton(onClick = onOpenApiKeyConfig){
                    Text("⚙️ Impostazioni", color = Color(0xFF6A1B9A), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        text ={
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ){
                if (isLoading){
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5)),
                        border = BorderStroke(1.dp, Color(0xFFCE93D8))
                    ){
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ){
                            Text(
                                text = if (isChatGpt) "✨ Elaborazione con ChatGPT in corso..." else "✨ Elaborazione con Gemini in corso...",
                                color = Color(0xFF6A1B9A),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = Color(0xFF8E24AA),
                                trackColor = Color(0xFFE1BEE7)
                            )
                            Text(
                                text = "Generazione ricetta e bilanciamento dosi...",
                                color = Color(0xFF7B1FA2),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                if (recipeName.isBlank()){
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ){
                        Text(
                            text = "ℹ️ Scrivi prima il nome della ricetta in alto per consentire all'AI di elaborarla.",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                } else{
                    Text(
                        text = "Ricetta attuale: \"$recipeName\"",
                        color = MacaronText,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                // Badge Provider e Modello AI attivo
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEDE7F6),
                    border = BorderStroke(1.dp, Color(0xFFD1C4E9)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isLoading){ onOpenApiKeyConfig() }
                ){
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ){
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ){
                            Text(if (isChatGpt) "🤖" else "⚡", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Column{
                                Text(
                                    text = "Provider attivo: $activeDisplayName",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4A148C)
                                )
                                Text(
                                    text = "Fallback automatico tra modelli e provider",
                                    fontSize = 10.sp,
                                    color = Color(0xFF6A1B9A)
                                )
                            }
                        }
                        Text(
                            text = "Modifica",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6A1B9A)
                        )
                    }
                }

                OutlinedTextField(
                    value = customPrompt,
                    onValueChange ={ customPrompt = it },
                    label ={ Text("Istruzioni speciali per l'AI (opzionale)") },
                    placeholder ={ Text("es. per 6 persone, senza glutine, frolla montata...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2
                )

                Text(
                    text = "Scegli un'azione:",
                    color = MacaronSubtleText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                // Opzione 1: Genera Ricetta Completa
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF3E5F5),
                    border = BorderStroke(1.dp, Color(0xFFCE93D8)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isLoading && recipeName.isNotBlank()){
                            onGenerateFullRecipe(customPrompt.takeIf{ it.isNotBlank() })
                        }
                ){
                    Column(modifier = Modifier.padding(12.dp)){
                        Text(
                            text = "🪄 Genera Ricetta Completa",
                            color = Color(0xFF6A1B9A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Crea da zero gli ingredienti bilanciati, prezzi unitari, allergeni ufficiali UE e procedimento numerato.",
                            color = Color(0xFF4A148C),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                // Opzione 2: Completa Dosi e Dati Mancanti
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEDE7F6),
                    border = BorderStroke(1.dp, Color(0xFFB39DDB)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isLoading && recipeName.isNotBlank()){
                            onCompleteMissing(customPrompt.takeIf{ it.isNotBlank() })
                        }
                ){
                    Column(modifier = Modifier.padding(12.dp)){
                        Text(
                            text = "⚖️ Completa Dosi e Dati Mancanti",
                            color = Color(0xFF4527A0),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Calcola i pesi degli ingredienti senza dose, stima i costi mancanti e compila gli allergeni.",
                            color = Color(0xFF311B92),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                // Opzione 3: Genera Procedimento Numerato
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE8EAF6),
                    border = BorderStroke(1.dp, Color(0xFF9FA8DA)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isLoading && recipeName.isNotBlank() && hasIngredients){
                            onGenerateInstructions(customPrompt.takeIf{ it.isNotBlank() })
                        }
                ){
                    Column(modifier = Modifier.padding(12.dp)){
                        Text(
                            text = "📝 Genera Solo Procedimento Numerato",
                            color = Color(0xFF283593),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (hasIngredients){
                                "Genera passaggi numerati (1., 2...) strettamente coerenti con gli ingredienti presenti."
                            } else{
                                "Richiede almeno un ingrediente già inserito nella lista."
                            },
                            color = Color(0xFF1A237E),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                // Opzione 4: Stima Valori Nutrizionali e Calo Peso Cottura
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE0F2F1),
                    border = BorderStroke(1.dp, Color(0xFF80CBC4)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isLoading && hasIngredients){
                            onEnrichNutrition()
                        }
                ){
                    Column(modifier = Modifier.padding(12.dp)){
                        Text(
                            text = "🥗 Calcola Valori Nutrizionali & Calo Peso",
                            color = Color(0xFF00695C),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (hasIngredients){
                                "Stima i 6 nutrienti ufficiali (calorie, carbo, grassi, proteine, acqua, sodio) e calcola la resa di cottura."
                            } else {
                                "Richiede almeno un ingrediente inserito nella ricetta."
                            },
                            color = Color(0xFF004D40),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        },
        confirmButton ={},
        dismissButton ={
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isLoading,
                shape = RoundedCornerShape(10.dp)
            ){
                Text("Chiudi", color = MacaronSubtleText)
            }
        }
    )
}

/**
 * Dialog unificato per la configurazione e crittografia hardware delle chiavi API (Gemini & ChatGPT),
 * con selezione del provider predefinito e modelli supportati.
 */
@Composable
fun GeminiApiKeyDialog(
    currentProvider: AiProvider = AiProvider.DEFAULT,
    currentGeminiApiKey: String = "",
    currentGeminiModelId: String = GeminiModel.DEFAULT.modelId,
    currentOpenAiApiKey: String = "",
    currentOpenAiModelId: String = OpenAiModel.DEFAULT.modelId,
    onDismiss: () -> Unit,
    onSave: (provider: AiProvider, geminiKey: String, geminiModelId: String, openAiKey: String, openAiModelId: String) -> Unit
){
    var selectedProvider by remember{ mutableStateOf(currentProvider) }
    var geminiKeyInput by remember{ mutableStateOf(currentGeminiApiKey) }
    var geminiSelectedModelId by remember{ mutableStateOf(currentGeminiModelId) }
    var openAiKeyInput by remember{ mutableStateOf(currentOpenAiApiKey) }
    var openAiSelectedModelId by remember{ mutableStateOf(currentOpenAiModelId) }
    var selectedTab by remember{ mutableIntStateOf(if (currentProvider == AiProvider.OPENAI) 1 else 0) }
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MacaronCard,
        shape = RoundedCornerShape(20.dp),
        title ={
            Row(verticalAlignment = Alignment.CenterVertically){
                Text("⚙️", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Impostazioni Intelligenza Artificiale",
                    color = MacaronPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text ={
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ){
                // Selettore Provider Predefinito
                Text(
                    text = "1. Provider AI Predefinito",
                    fontWeight = FontWeight.Bold,
                    color = MacaronText,
                    fontSize = 13.sp
                )
                Text(
                    text = "Scegli il motore AI principale. Se il provider è momentaneamente occupato o senza crediti, l'app effettua automaticamente il fallback sull'altro se configurato:",
                    fontSize = 11.sp,
                    color = MacaronSubtleText,
                    lineHeight = 15.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ){
                    // Opzione Google Gemini
                    val isGeminiSelected = selectedProvider == AiProvider.GEMINI
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isGeminiSelected) Color(0xFFEDE7F6) else Color.Transparent,
                        border = BorderStroke(
                            width = if (isGeminiSelected) 2.dp else 1.dp,
                            color = if (isGeminiSelected) Color(0xFF6A1B9A) else Color(0xFFE0E0E0)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable{
                                selectedProvider = AiProvider.GEMINI
                                selectedTab = 0
                            }
                    ){
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ){
                            RadioButton(
                                selected = isGeminiSelected,
                                onClick ={
                                    selectedProvider = AiProvider.GEMINI
                                    selectedTab = 0
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF6A1B9A))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Gemini",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isGeminiSelected) Color(0xFF4A148C) else MacaronText
                            )
                        }
                    }

                    // Opzione ChatGPT (OpenAI)
                    val isOpenAiSelected = selectedProvider == AiProvider.OPENAI
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isOpenAiSelected) Color(0xFFEDE7F6) else Color.Transparent,
                        border = BorderStroke(
                            width = if (isOpenAiSelected) 2.dp else 1.dp,
                            color = if (isOpenAiSelected) Color(0xFF6A1B9A) else Color(0xFFE0E0E0)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable{
                                selectedProvider = AiProvider.OPENAI
                                selectedTab = 1
                            }
                    ){
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ){
                            RadioButton(
                                selected = isOpenAiSelected,
                                onClick ={
                                    selectedProvider = AiProvider.OPENAI
                                    selectedTab = 1
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF6A1B9A))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ChatGPT",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isOpenAiSelected) Color(0xFF4A148C) else MacaronText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Tab per la configurazione dei singoli provider
                Text(
                    text = "2. Configurazione Chiavi e Modelli",
                    fontWeight = FontWeight.Bold,
                    color = MacaronText,
                    fontSize = 13.sp
                )

                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFFF5F5F5),
                    contentColor = MacaronPrimary
                ){
                    Tab(
                        selected = selectedTab == 0,
                        onClick ={ selectedTab = 0 },
                        text ={ Text("Google Gemini", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick ={ selectedTab = 1 },
                        text ={ Text("ChatGPT (OpenAI)", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                }

                if (selectedTab == 0){
                    // Configurazione Google Gemini
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFE8F5E9),
                        border = BorderStroke(1.dp, Color(0xFFA5D6A7)),
                        modifier = Modifier.fillMaxWidth()
                    ){
                        Column(modifier = Modifier.padding(10.dp)){
                            Text(
                                text = "• 100% Gratuito su Google AI Studio\n• Cifratura hardware AES-256 GCM con Android KeyStore\n• Modello leggero: Gemini Flash Lite • Modello avanzato: Gemini Flash",
                                color = Color(0xFF2E7D32),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    OutlinedButton(
                        onClick ={ uriHandler.openUri("https://aistudio.google.com/app/apikey") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ){
                        Text("🌐 Ottieni Chiave Gratis su Google AI Studio", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(
                        value = geminiKeyInput,
                        onValueChange ={ geminiKeyInput = it },
                        label ={ Text("Incolla la tua Gemini API Key") },
                        placeholder ={ Text("AIzaSy...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (currentGeminiApiKey.isNotBlank()){
                        Text(
                            text = "✓ Chiave Gemini salvata (${currentGeminiApiKey.take(6)}...${currentGeminiApiKey.takeLast(4)})",
                            color = Color(0xFF2E7D32),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = "Scelta Modello Gemini:",
                        fontWeight = FontWeight.SemiBold,
                        color = MacaronText,
                        fontSize = 12.sp
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)){
                        GeminiModel.entries.forEach{ model ->
                            val isSelected = model.modelId == geminiSelectedModelId
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFFEDE7F6) else Color.Transparent,
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF6A1B9A) else Color(0xFFE0E0E0)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable{ geminiSelectedModelId = model.modelId }
                            ){
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ){
                                    RadioButton(
                                        selected = isSelected,
                                        onClick ={ geminiSelectedModelId = model.modelId },
                                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF6A1B9A))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)){
                                        Text(
                                            text = model.displayName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isSelected) Color(0xFF4A148C) else MacaronText
                                        )
                                        Text(
                                            text = model.description,
                                            fontSize = 10.sp,
                                            color = MacaronSubtleText,
                                            lineHeight = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else{
                    // Configurazione ChatGPT (OpenAI)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFE3F2FD),
                        border = BorderStroke(1.dp, Color(0xFF90CAF9)),
                        modifier = Modifier.fillMaxWidth()
                    ){
                        Column(modifier = Modifier.padding(10.dp)){
                            Text(
                                text = "• API ufficiale OpenAI per ChatGPT\n• Cifratura hardware AES-256 GCM con Android KeyStore\n• Modello leggero: GPT-4o Mini • Modello avanzato: GPT-4o",
                                color = Color(0xFF1565C0),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    OutlinedButton(
                        onClick ={ uriHandler.openUri("https://platform.openai.com/api-keys") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ){
                        Text("🌐 Ottieni Chiave su OpenAI Platform", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(
                        value = openAiKeyInput,
                        onValueChange ={ openAiKeyInput = it },
                        label ={ Text("Incolla la tua OpenAI API Key") },
                        placeholder ={ Text("sk-proj-...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (currentOpenAiApiKey.isNotBlank()){
                        Text(
                            text = "✓ Chiave OpenAI salvata (${currentOpenAiApiKey.take(6)}...${currentOpenAiApiKey.takeLast(4)})",
                            color = Color(0xFF1565C0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = "Scelta Modello ChatGPT:",
                        fontWeight = FontWeight.SemiBold,
                        color = MacaronText,
                        fontSize = 12.sp
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)){
                        OpenAiModel.entries.forEach{ model ->
                            val isSelected = model.modelId == openAiSelectedModelId
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFFEDE7F6) else Color.Transparent,
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF6A1B9A) else Color(0xFFE0E0E0)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable{ openAiSelectedModelId = model.modelId }
                            ){
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ){
                                    RadioButton(
                                        selected = isSelected,
                                        onClick ={ openAiSelectedModelId = model.modelId },
                                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF6A1B9A))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)){
                                        Text(
                                            text = model.displayName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isSelected) Color(0xFF4A148C) else MacaronText
                                        )
                                        Text(
                                            text = model.description,
                                            fontSize = 10.sp,
                                            color = MacaronSubtleText,
                                            lineHeight = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton ={
            Button(
                onClick ={
                    onSave(
                        selectedProvider,
                        geminiKeyInput.trim(),
                        geminiSelectedModelId,
                        openAiKeyInput.trim(),
                        openAiSelectedModelId
                    )
                },
                enabled = geminiKeyInput.isNotBlank() || openAiKeyInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MacaronPrimary),
                shape = RoundedCornerShape(10.dp)
            ){
                Text("Salva Impostazioni", color = MacaronCard, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton ={
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)){
                Text("Annulla", color = MacaronSubtleText)
            }
        }
    )
}

/**
 * Overload per retrocompatibilità con chiamate legacy.
 */
@Composable
fun GeminiApiKeyDialog(
    currentApiKey: String,
    currentModelId: String = GeminiModel.DEFAULT.modelId,
    onDismiss: () -> Unit,
    onSave: (apiKey: String, modelId: String) -> Unit
){
    GeminiApiKeyDialog(
        currentProvider = AiProvider.GEMINI,
        currentGeminiApiKey = currentApiKey,
        currentGeminiModelId = currentModelId,
        currentOpenAiApiKey = "",
        currentOpenAiModelId = OpenAiModel.DEFAULT.modelId,
        onDismiss = onDismiss,
        onSave ={ _, geminiKey, geminiModel, _, _ ->
            onSave(geminiKey, geminiModel)
        }
    )
}




