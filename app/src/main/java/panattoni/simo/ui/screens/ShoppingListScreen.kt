package panattoni.simo.ui.screens

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RemoveShoppingCart
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import panattoni.simo.data.model.ShoppingItem
import panattoni.simo.data.model.ShoppingSortOption
import panattoni.simo.domain.MeasurementUnit
import panattoni.simo.ui.components.SimoTopBar
import panattoni.simo.ui.theme.MacaronBackground
import panattoni.simo.ui.theme.MacaronCard
import panattoni.simo.ui.theme.MacaronCardBorder
import panattoni.simo.ui.theme.MacaronPrimary
import panattoni.simo.ui.theme.MacaronSubtleText
import panattoni.simo.ui.theme.MacaronSurfaceVariant
import panattoni.simo.ui.theme.MacaronText
import panattoni.simo.ui.viewmodel.ShoppingItemUiModel
import panattoni.simo.ui.viewmodel.ShoppingListViewModel
import panattoni.simo.util.NumberUtils
import java.util.Locale

/**
 * Schermata della Lista della Spesa Intelligente.
 * Supporta aggregazione automatica per unità, costo stimato spesa, ordinamento e articoli manuali.
 */
@Composable
fun ShoppingListScreen(
    viewModel: ShoppingListViewModel,
    onBackClick: () -> Unit
){
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showClearConfirmDialog by remember{ mutableStateOf(false) }
    var showAddManualDialog by remember{ mutableStateOf(false) }
    var itemToEditQuantity by remember{ mutableStateOf<ShoppingItem?>(null) }
    var topMenuExpanded by remember{ mutableStateOf(false) }
    var sortMenuExpanded by remember{ mutableStateOf(false) }

    LaunchedEffect(uiState.message){
        uiState.message?.let{
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    Scaffold(
        containerColor = MacaronBackground,
        topBar ={
            SimoTopBar(
                title = "Lista della Spesa",
                subtitle = "${uiState.activeCount} da comprare, ${uiState.checkedCount} presi",
                showBackButton = true,
                onBackClick = onBackClick,
                actions ={
                    // Ordinamento lista della spesa
                    Box{
                        IconButton(onClick ={ sortMenuExpanded = true }){
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Ordina lista spesa",
                                tint = MacaronText
                            )
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest ={ sortMenuExpanded = false },
                            modifier = Modifier.background(MacaronCard)
                        ){
                            ShoppingSortOption.entries.forEach{ option ->
                                DropdownMenuItem(
                                    text ={
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ){
                                            RadioButton(
                                                selected = uiState.sortOption == option,
                                                onClick = null
                                            )
                                            Text(
                                                text = option.displayName,
                                                color = MacaronText,
                                                fontSize = 14.sp
                                            )
                                        }
                                    },
                                    onClick ={
                                        viewModel.setSortOption(option)
                                        sortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Tasto rapido per eliminare in blocco tutti gli articoli acquistati
                    if (uiState.checkedCount > 0){
                        IconButton(
                            onClick ={ viewModel.deleteCheckedItems() }
                        ){
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Elimina acquistati",
                                tint = MacaronPrimary
                            )
                        }
                    }

                    // Menu opzioni (es. svuota intera lista)
                    Box{
                        IconButton(onClick ={ topMenuExpanded = true }){
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Altre opzioni",
                                tint = MacaronText
                            )
                        }
                        DropdownMenu(
                            expanded = topMenuExpanded,
                            onDismissRequest ={ topMenuExpanded = false },
                            modifier = Modifier.background(MacaronCard)
                        ){
                            DropdownMenuItem(
                                text ={
                                    Row(verticalAlignment = Alignment.CenterVertically){
                                        Icon(
                                            imageVector = Icons.Default.RemoveShoppingCart,
                                            contentDescription = null,
                                            tint = Color(0xFFD32F2F),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Svuota intera lista", color = Color(0xFFD32F2F))
                                    }
                                },
                                onClick ={
                                    topMenuExpanded = false
                                    showClearConfirmDialog = true
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton ={
            FloatingActionButton(
                onClick ={ showAddManualDialog = true },
                containerColor = MacaronPrimary,
                contentColor = Color.White,
                shape = CircleShape
            ){
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Aggiungi articolo manuale",
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    ){ innerPadding ->
        if (uiState.items.isEmpty()){
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MacaronBackground),
                contentAlignment = Alignment.Center
            ){
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(32.dp)
                ){
                    Surface(
                        shape = CircleShape,
                        color = MacaronSurfaceVariant,
                        modifier = Modifier.size(80.dp)
                    ){
                        Box(contentAlignment = Alignment.Center){
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = MacaronPrimary.copy(alpha = 0.6f),
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                    Text(
                        text = "La tua lista della spesa è vuota",
                        color = MacaronText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Aggiungi ingredienti direttamente dalla schermata di dettaglio di una ricetta, oppure usa il tasto (+) per inserire articoli manuali come carta da forno o pellicola.",
                        color = MacaronSubtleText,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        } else{
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ){
                // Contatore dinamico in alto e costo stimato
                item{
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MacaronCard,
                        border = BorderStroke(1.dp, MacaronCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ){
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ){
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ){
                                Row(verticalAlignment = Alignment.CenterVertically){
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFE8F5E9),
                                        modifier = Modifier.size(24.dp)
                                    ){
                                        Box(contentAlignment = Alignment.Center){
                                            Text(
                                                text = "${uiState.activeCount}",
                                                color = Color(0xFF2E7D32),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Articoli da comprare",
                                        color = MacaronText,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                }

                                if (uiState.checkedCount > 0){
                                    Text(
                                        text = "${uiState.checkedCount} già presi ✓",
                                        color = MacaronSubtleText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            if (uiState.hasCostInfo){
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = MacaronCardBorder.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ){
                                    Row(verticalAlignment = Alignment.CenterVertically){
                                        Text(
                                            text = "💰 Spesa stimata:",
                                            color = MacaronText,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = String.format(Locale.US, "€%.2f", uiState.estimatedActiveCost),
                                            color = Color(0xFF2E7D32),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp
                                        )
                                    }
                                    if (uiState.checkedCount > 0 && uiState.estimatedTotalCost > uiState.estimatedActiveCost){
                                        Text(
                                            text = "Totale lista: " + String.format(Locale.US, "€%.2f", uiState.estimatedTotalCost),
                                            color = MacaronSubtleText,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Lista ordinata: prima non spuntati, poi spuntati
                items(uiState.items, key ={ it.item.id }){ itemUi ->
                    ShoppingItemRow(
                        itemUi = itemUi,
                        onToggleChecked ={ viewModel.toggleChecked(itemUi.item) },
                        onEditQuantityClick ={ itemToEditQuantity = itemUi.item },
                        onDeleteClick ={ viewModel.deleteItem(itemUi.item) }
                    )
                }
            }
        }
    }

    // Dialog conferma svuota lista
    if (showClearConfirmDialog){
        AlertDialog(
            onDismissRequest ={ showClearConfirmDialog = false },
            containerColor = MacaronCard,
            shape = RoundedCornerShape(20.dp),
            title ={
                Text(
                    text = "Svuota lista della spesa?",
                    color = MacaronText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text ={
                Text(
                    text = "Sei sicuro di voler eliminare tutti gli elementi presenti nella lista della spesa? Questa azione non può essere annullata.",
                    color = MacaronSubtleText,
                    fontSize = 14.sp
                )
            },
            confirmButton ={
                Button(
                    onClick ={
                        viewModel.clearAllItems()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(10.dp)
                ){
                    Text("Svuota", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton ={
                OutlinedButton(
                    onClick ={ showClearConfirmDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ){
                    Text("Annulla", color = MacaronSubtleText)
                }
            }
        )
    }

    // Dialog modifica quantità e costo unitario
    if (itemToEditQuantity != null){
        val targetItem = itemToEditQuantity!!
        var editQtyInput by remember{ mutableStateOf(NumberUtils.formatAmount(targetItem.quantity)) }
        var editUnit by remember{ mutableStateOf(if (panattoni.simo.domain.MeasurementUnit.isQb(targetItem.unit)) "pz" else targetItem.unit) }
        var editPriceInput by remember{
            mutableStateOf(if (targetItem.unitPrice > 0.0) NumberUtils.formatAmount(targetItem.unitPrice) else "0")
        }
        var editPriceUnit by remember{
            mutableStateOf(
                targetItem.priceUnit ?: when (editUnit.lowercase()){
                    "g", "kg" -> "kg"
                    "ml", "l" -> "l"
                    else -> editUnit
                }
            )
        }
        var editUnitMenuExpanded by remember{ mutableStateOf(false) }
        var editPriceUnitMenuExpanded by remember{ mutableStateOf(false) }

        val allowedEditPriceUnits = when (editUnit.lowercase()){
            "g", "kg" -> listOf("kg", "g")
            "ml", "l" -> listOf("l", "ml")
            else -> listOf(editUnit)
        }

        LaunchedEffect(editUnit){
            if (editPriceUnit !in allowedEditPriceUnits){
                editPriceUnit = when (editUnit.lowercase()){
                    "g", "kg" -> "kg"
                    "ml", "l" -> "l"
                    else -> editUnit
                }
            }
        }

        AlertDialog(
            onDismissRequest ={ itemToEditQuantity = null },
            containerColor = MacaronCard,
            shape = RoundedCornerShape(20.dp),
            title ={
                Text(
                    text = "Modifica articolo",
                    color = MacaronPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text ={
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)){
                    Text(
                        text = targetItem.itemName,
                        color = MacaronText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        style = LocalTextStyle.current.copy(
                            hyphens = Hyphens.None,
                            lineBreak = LineBreak.Simple
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        OutlinedTextField(
                            value = editQtyInput,
                            onValueChange ={ editQtyInput = it },
                            label ={ Text("Dose") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Box(modifier = Modifier.weight(1.1f)){
                            OutlinedTextField(
                                value = editUnit,
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
                                    .clickable{ editUnitMenuExpanded = true }
                            )
                            DropdownMenu(
                                expanded = editUnitMenuExpanded,
                                onDismissRequest ={ editUnitMenuExpanded = false }
                            ){
                                listOf("pz", "unità", "g", "kg", "ml", "l", "rotolo", "pacco", "busta", "bottiglia", "confezione").forEach{ u ->
                                    DropdownMenuItem(
                                        text ={ Text(u) },
                                        onClick ={
                                            editUnit = u
                                            editUnitMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        OutlinedTextField(
                            value = editPriceInput,
                            onValueChange ={ editPriceInput = it },
                            label ={ Text("Costo (€)") },
                            placeholder ={ Text("0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Box(modifier = Modifier.weight(1.1f)){
                            OutlinedTextField(
                                value = "/ $editPriceUnit",
                                onValueChange ={},
                                readOnly = true,
                                label ={ Text("Per") },
                                trailingIcon ={
                                    if (allowedEditPriceUnits.size > 1){
                                        Icon(Icons.Default.ArrowDropDown, null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            if (allowedEditPriceUnits.size > 1){
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable{ editPriceUnitMenuExpanded = true }
                                )
                                DropdownMenu(
                                    expanded = editPriceUnitMenuExpanded,
                                    onDismissRequest ={ editPriceUnitMenuExpanded = false }
                                ){
                                    allowedEditPriceUnits.forEach{ pUnit ->
                                        DropdownMenuItem(
                                            text ={ Text("al $pUnit") },
                                            onClick ={
                                                editPriceUnit = pUnit
                                                editPriceUnitMenuExpanded = false
                                            }
                                        )
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
                        val parsedQty = NumberUtils.parseDoubleOrNull(editQtyInput)
                        val parsedPrice = NumberUtils.parseDoubleSafe(editPriceInput, 0.0)
                        if (parsedQty != null && parsedQty > 0.0){
                            viewModel.updateItemDetails(targetItem, parsedQty, editUnit, parsedPrice, editPriceUnit)
                        }
                        itemToEditQuantity = null
                    },
                    enabled = NumberUtils.isValidPositiveNumber(editQtyInput),
                    colors = ButtonDefaults.buttonColors(containerColor = MacaronPrimary),
                    shape = RoundedCornerShape(10.dp)
                ){
                    Text("Aggiorna", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton ={
                OutlinedButton(
                    onClick ={ itemToEditQuantity = null },
                    shape = RoundedCornerShape(10.dp)
                ){
                    Text("Annulla", color = MacaronSubtleText)
                }
            }
        )
    }

    // Dialog aggiunta articolo manuale
    if (showAddManualDialog){
        var manualName by remember{ mutableStateOf("") }
        var manualUnit by remember{ mutableStateOf("pz") }
        var manualQty by remember{ mutableStateOf("1") }
        var manualPrice by remember{ mutableStateOf("0") }
        var manualPriceUnit by remember{ mutableStateOf("pz") }
        var unitMenuExpanded by remember{ mutableStateOf(false) }
        var priceUnitMenuExpanded by remember{ mutableStateOf(false) }

        val allowedManualPriceUnits = when (manualUnit.lowercase()){
            "g", "kg" -> listOf("kg", "g")
            "ml", "l" -> listOf("l", "ml")
            else -> listOf(manualUnit)
        }

        LaunchedEffect(manualUnit){
            if (manualPriceUnit !in allowedManualPriceUnits){
                manualPriceUnit = when (manualUnit.lowercase()){
                    "g", "kg" -> "kg"
                    "ml", "l" -> "l"
                    else -> manualUnit
                }
            }
        }

        AlertDialog(
            onDismissRequest ={ showAddManualDialog = false },
            containerColor = MacaronCard,
            shape = RoundedCornerShape(20.dp),
            title ={
                Text(
                    text = "Aggiungi articolo",
                    color = MacaronPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text ={
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)){
                    OutlinedTextField(
                        value = manualName,
                        onValueChange ={ manualName = it },
                        label ={ Text("Nome articolo") },
                        placeholder ={ Text("es. Carta da forno, Pellicola, Farina...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        OutlinedTextField(
                            value = manualQty,
                            onValueChange ={ manualQty = it },
                            label ={ Text("Dose") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Box(modifier = Modifier.weight(1.2f)){
                            OutlinedTextField(
                                value = manualUnit,
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
                                listOf("pz", "unità", "g", "kg", "ml", "l", "rotolo", "pacco", "busta", "bottiglia", "confezione").forEach{ u ->
                                    DropdownMenuItem(
                                        text ={ Text(u) },
                                        onClick ={
                                            manualUnit = u
                                            unitMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        OutlinedTextField(
                            value = manualPrice,
                            onValueChange ={ manualPrice = it },
                            label ={ Text("Costo (€)") },
                            placeholder ={ Text("0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Box(modifier = Modifier.weight(1.1f)){
                            OutlinedTextField(
                                value = "/ $manualPriceUnit",
                                onValueChange ={},
                                readOnly = true,
                                label ={ Text("Per") },
                                trailingIcon ={
                                    if (allowedManualPriceUnits.size > 1){
                                        Icon(Icons.Default.ArrowDropDown, null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            if (allowedManualPriceUnits.size > 1){
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable{ priceUnitMenuExpanded = true }
                                )
                                DropdownMenu(
                                    expanded = priceUnitMenuExpanded,
                                    onDismissRequest ={ priceUnitMenuExpanded = false }
                                ){
                                    allowedManualPriceUnits.forEach{ pUnit ->
                                        DropdownMenuItem(
                                            text ={ Text("al $pUnit") },
                                            onClick ={
                                                manualPriceUnit = pUnit
                                                priceUnitMenuExpanded = false
                                            }
                                        )
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
                        val parsedQty = NumberUtils.parseDoubleSafe(manualQty, 1.0)
                        val parsedPrice = NumberUtils.parseDoubleSafe(manualPrice, 0.0)
                        if (manualName.isNotBlank() && parsedQty > 0.0){
                            viewModel.addManualItem(manualName, parsedQty, manualUnit, parsedPrice, manualPriceUnit)
                            showAddManualDialog = false
                        }
                    },
                    enabled = manualName.isNotBlank() && NumberUtils.isValidPositiveNumber(manualQty),
                    colors = ButtonDefaults.buttonColors(containerColor = MacaronPrimary),
                    shape = RoundedCornerShape(10.dp)
                ){
                    Text("Aggiungi", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton ={
                OutlinedButton(
                    onClick ={ showAddManualDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ){
                    Text("Annulla", color = MacaronSubtleText)
                }
            }
        )
    }
}

@Composable
fun ShoppingItemRow(
    itemUi: ShoppingItemUiModel,
    onToggleChecked: () -> Unit,
    onEditQuantityClick: () -> Unit,
    onDeleteClick: () -> Unit
){
    val item = itemUi.item
    val cardBg by animateColorAsState(
        targetValue = if (item.isChecked) MacaronSurfaceVariant.copy(alpha = 0.4f) else MacaronCard,
        animationSpec = tween(durationMillis = 250),
        label = "cardBg"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, if (item.isChecked) Color.Transparent else MacaronCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isChecked) 0.dp else 2.dp)
    ){
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ){
            // Checkbox per spuntare
            Checkbox(
                checked = item.isChecked,
                onCheckedChange ={ onToggleChecked() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MacaronPrimary,
                    uncheckedColor = MacaronSubtleText,
                    checkmarkColor = Color.White
                )
            )

            // Info Articolo (Nome + Badge Ricetta/Manuale)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = 100.dp)
                    .clickable{ onToggleChecked() }
                    .padding(start = 2.dp, end = 6.dp)
            ){
                Text(
                    text = item.itemName,
                    color = if (item.isChecked) MacaronSubtleText else MacaronText,
                    fontWeight = if (item.isChecked) FontWeight.Normal else FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                    style = LocalTextStyle.current.copy(
                        hyphens = Hyphens.None,
                        lineBreak = LineBreak.Paragraph
                    )
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Provenienza
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (item.source == "Manuale") MacaronSurfaceVariant else Color(0xFFE3F2FD)
                ){
                    Text(
                        text = if (item.source == "Manuale") "✏️ Manuale" else "📖 ${item.source}",
                        color = if (item.source == "Manuale") MacaronPrimary else Color(0xFF1976D2),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Badge Dose & Unità (es. 210 g, 150 ml, 2 unità)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (item.isChecked) MacaronSurfaceVariant.copy(alpha = 0.6f) else Color(0xFFFFF3E0),
                modifier = Modifier
                    .widthIn(max = 115.dp)
                    .clickable{ onEditQuantityClick() }
                    .padding(end = 4.dp)
            ){
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp)
                ){
                    val qtyFormatted = NumberUtils.formatQuantityForUnit(item.quantity, item.unit)
                    Text(
                        text = if (MeasurementUnit.isQb(item.unit)) "q.b." else "$qtyFormatted ${item.unit}",
                        color = if (item.isChecked) MacaronSubtleText else Color(0xFFE65100),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Modifica dose",
                        tint = if (item.isChecked) MacaronSubtleText else Color(0xFFE65100),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // Costo stimato dell'ingrediente se presente
            if (itemUi.estimatedCost != null && itemUi.estimatedCost > 0.0){
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (item.isChecked) MacaronSurfaceVariant.copy(alpha = 0.5f) else Color(0xFFE8F5E9),
                    modifier = Modifier
                        .widthIn(max = 95.dp)
                        .padding(end = 4.dp)
                ){
                    Text(
                        text = String.format(Locale.US, "€%.2f", itemUi.estimatedCost),
                        color = if (item.isChecked) MacaronSubtleText else Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp)
                    )
                }
            }

            // Tasto elimina singolo articolo
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(32.dp)
            ){
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Elimina articolo",
                    tint = MacaronSubtleText,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
