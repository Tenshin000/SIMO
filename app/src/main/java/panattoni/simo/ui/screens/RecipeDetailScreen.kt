package panattoni.simo.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import panattoni.simo.data.model.ScaledComponent
import panattoni.simo.domain.Allergen
import panattoni.simo.data.model.RecipeNutritionSummary
import panattoni.simo.data.model.NutritionalValues
import panattoni.simo.service.CookingSessionManager
import panattoni.simo.util.RecipePdfExporter
import panattoni.simo.ui.components.ReverseScaleDialog
import panattoni.simo.ui.components.ScaledIngredientCard
import panattoni.simo.ui.components.SimoTopBar
import panattoni.simo.ui.theme.MacaronBackground
import panattoni.simo.ui.theme.MacaronCard
import panattoni.simo.ui.theme.MacaronCardBorder
import panattoni.simo.ui.theme.MacaronPrimary
import panattoni.simo.ui.theme.MacaronSubtleText
import panattoni.simo.ui.theme.MacaronSurfaceVariant
import panattoni.simo.ui.theme.MacaronText
import panattoni.simo.ui.viewmodel.RecipeDetailViewModel
import panattoni.simo.util.ItalianGrammarUtils
import panattoni.simo.util.NumberUtils

/**
 * Schermata di dettaglio e ridimensionamento (scaling) delle ricette.
 * Permette di calcolare dosi proporzionate, food cost, esportare PDF ed entrare in Cooking Mode.
 */
@Composable
fun RecipeDetailScreen(
    viewModel: RecipeDetailViewModel,
    onBackClick: () -> Unit,
    onDrillDownClick: (String) -> Unit
){
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var activeComponentForReverseScale by remember{ mutableStateOf<ScaledComponent?>(null) }

    if (uiState.isLoading){
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MacaronBackground),
            contentAlignment = Alignment.Center
        ){
            CircularProgressIndicator(color = MacaronPrimary)
        }
        return
    }

    val tree = uiState.scaledTree
    val recipe = uiState.recipeWithComponents?.recipe
    val currentSession by CookingSessionManager.sessionState.collectAsState()
    val isCookingModeActive = currentSession != null && currentSession?.recipeId == recipe?.id

    if (isCookingModeActive && tree != null){
        CookingModeScreen(
            tree = tree,
            onClose ={ CookingSessionManager.stopSession(context) }
        )
        return
    }

    if (tree == null || recipe == null){
        Scaffold(
            containerColor = MacaronBackground,
            topBar ={
                SimoTopBar(
                    title = "Ricetta non trovata",
                    subtitle = "L'elemento selezionato non è disponibile",
                    showBackButton = true,
                    onBackClick = onBackClick
                )
            }
        ){ innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MacaronBackground),
                contentAlignment = Alignment.Center
            ){
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(24.dp)
                ){
                    Text(
                        text = "La ricetta richiesta non è presente o potrebbe essere stata eliminata.",
                        color = MacaronText,
                        fontSize = 16.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Button(
                        onClick = onBackClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MacaronPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ){
                        Text("Torna alle Ricette", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        return
    }

    Scaffold(
        containerColor = MacaronBackground,
        topBar ={
            SimoTopBar(
                title = recipe.name,
                subtitle = "Resa base: ${ItalianGrammarUtils.formatQuantityWithUnit(recipe.baseYieldQuantity, recipe.yieldUnit)}",
                showBackButton = true,
                onBackClick = onBackClick,
                actions ={
                    // Condividi / Stampa PDF Professionale
                    IconButton(
                        onClick ={
                            RecipePdfExporter.shareRecipePdf(context, tree)
                        }
                    ){
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Stampa / Condividi Scheda PDF",
                            tint = MacaronPrimary
                        )
                    }

                    if (uiState.scalingFactor != 1.0){
                        IconButton(onClick = viewModel::resetToOriginal){
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Ripristina Dosi Base",
                                tint = MacaronPrimary
                            )
                        }
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
            // Sezione Descrizione & Badge
            if (recipe.description.isNotBlank()){
                item{
                    Text(
                        text = recipe.description,
                        color = MacaronSubtleText,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }

            // Box di Controllo Ricalcolo (Resa / Reverse Scaling)
            item{
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MacaronCard),
                    border = BorderStroke(1.5.dp, MacaronPrimary.copy(alpha = 0.4f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ){
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ){
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ){
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ){
                                Surface(
                                    shape = CircleShape,
                                    color = MacaronSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                ){
                                    Box(contentAlignment = Alignment.Center){
                                        Icon(
                                            imageVector = Icons.Default.Scale,
                                            contentDescription = null,
                                            tint = MacaronPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Ricalcolo Dinamico",
                                    color = MacaronText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Moltiplicatore k visibile
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (uiState.scalingFactor != 1.0) MacaronPrimary else MacaronSurfaceVariant
                            ){
                                val factorFormatted = String.format(java.util.Locale.US, "%.2fx", uiState.scalingFactor)
                                Text(
                                    text = factorFormatted,
                                    color = if (uiState.scalingFactor != 1.0) MacaronCard else MacaronPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Banner se attivo reverse scaling
                        if (uiState.activeScalingIngredientName != null){
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MacaronSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ){
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ){
                                    Text(
                                        text = "Bilanciato su: ${uiState.activeScalingIngredientName}",
                                        color = MacaronPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Reset",
                                        color = MacaronPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }

                        // Controller Resa / Porzioni
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ){
                            IconButton(
                                onClick ={ viewModel.stepYield(-1.0) },
                                modifier = Modifier
                                    .background(MacaronSurfaceVariant, CircleShape)
                                    .size(42.dp)
                            ){
                                Icon(Icons.Default.Remove, contentDescription = "Diminuisci", tint = MacaronPrimary)
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ){
                                OutlinedTextField(
                                    value = uiState.yieldInputValue,
                                    onValueChange = viewModel::onYieldInputChange,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.width(90.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MacaronBackground,
                                        unfocusedContainerColor = MacaronBackground,
                                        focusedBorderColor = MacaronPrimary,
                                        unfocusedBorderColor = MacaronCardBorder
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                val currentYieldQty = NumberUtils.parseDoubleOrNull(uiState.yieldInputValue) ?: tree.targetYieldQuantity
                                val displayYieldUnit = ItalianGrammarUtils.formatUnitByQuantity(recipe.yieldUnit, currentYieldQty)
                                Text(
                                    text = displayYieldUnit,
                                    color = MacaronText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }

                            IconButton(
                                onClick ={ viewModel.stepYield(1.0) },
                                modifier = Modifier
                                    .background(MacaronSurfaceVariant, CircleShape)
                                    .size(42.dp)
                            ){
                                Icon(Icons.Default.Add, contentDescription = "Aumenta", tint = MacaronPrimary)
                            }
                        }

                        // Resa e Modificatore Peso in Cottura
                        val nutrition = tree.nutrition
                        val cookingMod = recipe.cookingWeightModifier
                        if (cookingMod != 0.0 || (nutrition != null && nutrition.totalRawWeightG > 0.0)){
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (cookingMod < 0.0) Color(0xFFFFF3E0) else if (cookingMod > 0.0) Color(0xFFE1F5FE) else MacaronSurfaceVariant,
                                border = BorderStroke(1.dp, if (cookingMod < 0.0) Color(0xFFFFB74D) else if (cookingMod > 0.0) Color(0xFF81D4FA) else MacaronCardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ){
                                Column(modifier = Modifier.padding(10.dp)){
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ){
                                        Text(
                                            text = if (cookingMod < 0.0) "🔥 Calo Peso in Cottura" else if (cookingMod > 0.0) "💧 Aumento Peso in Cottura" else "⚖️ Resa Peso Finito",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (cookingMod < 0.0) Color(0xFFE65100) else if (cookingMod > 0.0) Color(0xFF0277BD) else MacaronText
                                        )
                                        if (cookingMod != 0.0){
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (cookingMod < 0.0) Color(0xFFFFE0B2) else Color(0xFFB3E5FC)
                                            ){
                                                Text(
                                                    text = if (cookingMod > 0) "+${String.format(java.util.Locale.US, "%.1f", cookingMod)}%" else "${String.format(java.util.Locale.US, "%.1f", cookingMod)}%",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 12.sp,
                                                    color = if (cookingMod < 0.0) Color(0xFFBF360C) else Color(0xFF01579B),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (nutrition != null && nutrition.totalRawWeightG > 0.0){
                                        Spacer(modifier = Modifier.height(6.dp))
                                        val rawWeightStr = NumberUtils.formatWeight(nutrition.totalRawWeightG)
                                        val cookedWeightStr = NumberUtils.formatWeight(nutrition.totalCookedWeightG)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ){
                                            Text(
                                                text = "Impasto crudo: $rawWeightStr",
                                                fontSize = 11.sp,
                                                color = MacaronSubtleText
                                            )
                                            Text(
                                                text = "➔",
                                                fontSize = 11.sp,
                                                color = MacaronSubtleText
                                            )
                                            Text(
                                                text = "Cotto finito: $cookedWeightStr",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MacaronText
                                            )
                                        }
                                        if (nutrition.portionWeightG > 0.0 && tree.targetYieldQuantity > 0.0){
                                            val portionWeightStr = NumberUtils.formatWeight(nutrition.portionWeightG)
                                            val singUnit = ItalianGrammarUtils.toSingular(recipe.yieldUnit)
                                            Text(
                                                text = "Resa cotta finita: ~$portionWeightStr a $singUnit",
                                                fontSize = 10.5.sp,
                                                color = MacaronSubtleText,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "💡 Tocca la quantità di qualsiasi ingrediente per fare il Reverse Scaling con la quantità che hai in dispensa.",
                            color = MacaronSubtleText,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // Card Food Cost (Calcolo in tempo reale del costo totale impasto e per porzione)
            if (tree.hasCostData && tree.totalCost > 0.0){
                item{
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MacaronCard),
                        border = BorderStroke(1.5.dp, Color(0xFF4CAF50).copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ){
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ){
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ){
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                ){
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFE8F5E9),
                                        modifier = Modifier.size(34.dp)
                                    ){
                                        Box(contentAlignment = Alignment.Center){
                                            Text(
                                                text = "€",
                                                color = Color(0xFF2E7D32),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f, fill = false)){
                                        Text(
                                            text = "Food Cost (Costo Ricetta)",
                                            color = MacaronText,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            maxLines = 2,
                                            softWrap = true
                                        )
                                        Text(
                                            text = "Calcolato in tempo reale su dosi e resa",
                                            color = MacaronSubtleText,
                                            fontSize = 11.sp,
                                            maxLines = 2,
                                            softWrap = true
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFE8F5E9)
                                ){
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    ){
                                        Text(
                                            text = "STIMA",
                                            color = Color(0xFF2E7D32),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Text(
                                            text = "LIVE",
                                            color = Color(0xFF2E7D32),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ){
                                // Costo Totale Impasto
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF1F8E9)
                                ){
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.Start
                                    ){
                                        Text(
                                            text = "Costo Totale Impasto",
                                            color = MacaronSubtleText,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val totalFormatted = String.format(java.util.Locale.US, "€%.2f", tree.totalCost)
                                        Text(
                                            text = totalFormatted,
                                            color = Color(0xFF1B5E20),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 19.sp
                                        )
                                    }
                                }

                                // Costo al Pezzo / Porzione
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFE8F5E9)
                                ){
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.Start
                                    ){
                                        val portionLabel = ItalianGrammarUtils.formatYieldCostLabel(recipe.yieldUnit)
                                        Text(
                                            text = portionLabel,
                                            color = MacaronSubtleText,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val portionFormatted = String.format(java.util.Locale.US, "€%.2f", tree.costPerPortion)
                                        Text(
                                            text = portionFormatted,
                                            color = Color(0xFF2E7D32),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 19.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Azioni rapide: Cooking Mode & Aggiungi a Spesa
            item{
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ){
                    // Cooking Mode
                    Button(
                        onClick ={
                            if (tree != null){
                                CookingSessionManager.startSession(context, tree, uiState.scalingFactor)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MacaronPrimary),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ){
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Cooking Mode",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    // Aggiungi a Spesa
                    Button(
                        onClick ={
                            viewModel.addToShoppingList{ count ->
                                Toast.makeText(
                                    context,
                                    "🛒 $count ingredienti aggiunti alla Lista della Spesa!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier.weight(1.1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ){
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Aggiungi a Spesa",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Intestazione Sezione Ingredienti
            item{
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Text(
                        text = "Ingredienti e Ricette",
                        color = MacaronText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "${tree.components.size} elementi",
                        color = MacaronSubtleText,
                        fontSize = 13.sp
                    )
                }
            }

            // Lista Ingredienti proporzionati in tempo reale
            items(tree.components, key ={ it.componentDetail.component.id }){ scaledComp ->
                ScaledIngredientCard(
                    scaledComponent = scaledComp,
                    onReverseScaleClick ={ activeComponentForReverseScale = scaledComp },
                    onDrillDownClick = onDrillDownClick
                )
            }

            // Card Istruzioni di preparazione
            if (recipe.instructions.isNotBlank()){
                item{
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
                            Row(verticalAlignment = Alignment.CenterVertically){
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = MacaronPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Procedimento",
                                    color = MacaronText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = recipe.instructions,
                                color = MacaronText,
                                fontSize = 14.sp,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }
            }

            // Card Allergeni (Regolamento UE n. 1169/2011)
            item{
                Spacer(modifier = Modifier.height(10.dp))
                RecipeAllergensCard(
                    allergens = tree.allergens,
                    components = tree.components
                )
            }

            // Card Dichiarazione Nutrizionale Grafica UE (Regolamento UE n. 1169/2011)
            item{
                Spacer(modifier = Modifier.height(10.dp))
                RecipeNutritionCard(
                    nutrition = tree.nutrition,
                    yieldUnit = recipe.yieldUnit
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Dialog Reverse Scaling
    if (activeComponentForReverseScale != null){
        val comp = activeComponentForReverseScale!!
        ReverseScaleDialog(
            scaledComponent = comp,
            onDismiss ={ activeComponentForReverseScale = null },
            onConfirm ={ newAmount ->
                viewModel.onReverseScaleIngredient(
                    baseAmount = comp.componentDetail.component.baseQuantity,
                    newAmount = newAmount,
                    ingredientName = comp.componentDetail.item.name
                )
                activeComponentForReverseScale = null
            }
        )
    }
}

@Composable
fun RecipeAllergensCard(
    allergens: List<Allergen>,
    components: List<ScaledComponent>,
    modifier: Modifier = Modifier
){
    val hasAllergens = allergens.isNotEmpty()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MacaronCard),
        border = BorderStroke(
            1.5.dp,
            if (hasAllergens) Color(0xFFFF9800).copy(alpha = 0.5f) else Color(0xFF4CAF50).copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ){
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ){
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ){
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ){
                    Surface(
                        shape = CircleShape,
                        color = if (hasAllergens) Color(0xFFFFF3E0) else Color(0xFFE8F5E9),
                        modifier = Modifier.size(36.dp)
                    ){
                        Box(contentAlignment = Alignment.Center){
                            Text(
                                text = if (hasAllergens) "⚠️" else "🛡️",
                                fontSize = 18.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f, fill = false)){
                        Text(
                            text = if (hasAllergens) "Allergeni Presenti (${allergens.size})" else "Allergeni Alimentari",
                            color = MacaronText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 2,
                            softWrap = true
                        )
                        Text(
                            text = "Regolamento UE n. 1169/2011",
                            color = MacaronSubtleText,
                            fontSize = 11.sp,
                            maxLines = 2,
                            softWrap = true
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (hasAllergens) Color(0xFFFFF3E0) else Color(0xFFE8F5E9)
                ){
                    Text(
                        text = if (hasAllergens) "ALLERGENI" else "NESSUN ALLERGENE",
                        color = if (hasAllergens) Color(0xFFE65100) else Color(0xFF2E7D32),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (!hasAllergens){
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF1F8E9),
                    modifier = Modifier.fillMaxWidth()
                ){
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        Text(
                            text = "✅",
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Nessun allergene dichiarato tra le materie prime e le sotto-ricette che compongono questa preparazione.",
                            color = Color(0xFF2E7D32),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else{
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ){
                    allergens.forEach{ allergen ->
                        // Trova in quali componenti/sotto-ricette è presente questo allergene
                        val sources = components.filter{ comp ->
                            val compAllergens = comp.subRecipeTree?.allergens ?: comp.componentDetail.item.allergensList
                            compAllergens.contains(allergen)
                        }.map{ it.componentDetail.item.name }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFAFAFA),
                            border = BorderStroke(1.dp, MacaronCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ){
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ){
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFFF3E0),
                                    modifier = Modifier.size(36.dp)
                                ){
                                    Box(contentAlignment = Alignment.Center){
                                        Text(
                                            text = allergen.emoji,
                                            fontSize = 18.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)){
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ){
                                        Text(
                                            text = allergen.displayName,
                                            color = MacaronText,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFFBE9E7)
                                        ){
                                            Text(
                                                text = "Allergene #${allergen.ordinal + 1}",
                                                color = Color(0xFFD84315),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = allergen.description,
                                        color = MacaronSubtleText,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )

                                    if (sources.isNotEmpty()){
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Presente in: " + sources.joinToString(", "),
                                            color = Color(0xFFE65100),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Riquadro grafico "Dichiarazione Nutrizionale (Stima)" conforme al Regolamento UE n. 1169/2011.
 * Mostra valori nutrizionali per 100g e a porzione, calo peso/resa di cottura ed equivalenza sale/sodio.
 */
@Composable
fun RecipeNutritionCard(
    nutrition: RecipeNutritionSummary?,
    yieldUnit: String,
    modifier: Modifier = Modifier
){
    val hasNutrition = nutrition != null && nutrition.hasNutritionData

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MacaronCard),
        border = BorderStroke(
            1.5.dp,
            if (hasNutrition) Color(0xFF2E7D32).copy(alpha = 0.5f) else MacaronCardBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ){
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp) // Spaziatura ottimizzata per garantire la massima larghezza alla tabella
        ){
            // Intestazione con chiara indicazione "(Stima)"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ){
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ){
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFE8F5E9),
                        modifier = Modifier.size(34.dp)
                    ){
                        Box(contentAlignment = Alignment.Center){
                            Text("🥗", fontSize = 18.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column{
                        Text(
                            text = "Dichiarazione Nutrizionale (Stima)",
                            color = MacaronText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Regolamento (UE) n. 1169/2011 • Stima indicativa",
                            color = MacaronSubtleText,
                            fontSize = 11.sp
                        )
                    }
                }

                if (hasNutrition){
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE8F5E9)
                    ){
                        Text(
                            text = "${nutrition.perPortion.caloriesKcal.toInt()} kcal",
                            color = Color(0xFF2E7D32),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (!hasNutrition){
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MacaronSurfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MacaronCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ){
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        Text("ℹ️", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Valori nutrizionali non ancora impostati. Tocca 'Modifica' in alto per calcolarli o arricchirli automaticamente con l'Assistente AI.",
                            color = MacaronSubtleText,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            } else {
                val singUnit = ItalianGrammarUtils.toSingular(yieldUnit)
                val portionLabel = if (nutrition.portionWeightG > 0.0){
                    "Porz. (${NumberUtils.formatWeight(nutrition.portionWeightG)})"
                } else{
                    "1 $singUnit"
                }

                // Tabella Nutrizionale Stile Etichetta UE con larghezze ribilanciate per non mandare a capo i valori energetici
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MacaronSurfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.2.dp, Color(0xFF81C784)),
                    modifier = Modifier.fillMaxWidth()
                ){
                    Column(modifier = Modifier.fillMaxWidth()){
                        // Intestazione Tabella: pesi colonne ottimizzati per le cifre kJ/kcal
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF2E7D32))
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ){
                            Text(
                                text = "VALORI MEDI",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp,
                                modifier = Modifier.weight(1.15f)
                            )
                            Text(
                                text = "Per 100 g",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp,
                                modifier = Modifier.weight(1.425f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                            )
                            Text(
                                text = portionLabel,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp,
                                modifier = Modifier.weight(1.425f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                            )
                        }

                        // Righe Nutrienti
                        val rows = listOf(
                            NutrientRowData(
                                name = "Energia",
                                isBold = true,
                                per100g = "${nutrition.per100g.energyKj.toInt()} kJ / ${nutrition.per100g.caloriesKcal.toInt()} kcal",
                                perPortion = "${nutrition.perPortion.energyKj.toInt()} kJ / ${nutrition.perPortion.caloriesKcal.toInt()} kcal"
                            ),
                            NutrientRowData(
                                name = "Grassi",
                                isBold = true,
                                per100g = "${String.format(java.util.Locale.US, "%.1f", nutrition.per100g.fatsG)} g",
                                perPortion = "${String.format(java.util.Locale.US, "%.1f", nutrition.perPortion.fatsG)} g"
                            ),
                            NutrientRowData(
                                name = "Carboidrati",
                                isBold = true,
                                per100g = "${String.format(java.util.Locale.US, "%.1f", nutrition.per100g.carbohydratesG)} g",
                                perPortion = "${String.format(java.util.Locale.US, "%.1f", nutrition.perPortion.carbohydratesG)} g"
                            ),
                            NutrientRowData(
                                name = "Proteine",
                                isBold = true,
                                per100g = "${String.format(java.util.Locale.US, "%.1f", nutrition.per100g.proteinsG)} g",
                                perPortion = "${String.format(java.util.Locale.US, "%.1f", nutrition.perPortion.proteinsG)} g"
                            ),
                            NutrientRowData(
                                name = "Sale (equivalente)",
                                isBold = false,
                                per100g = "${String.format(java.util.Locale.US, "%.2f", nutrition.per100g.saltEquivalentG)} g",
                                perPortion = "${String.format(java.util.Locale.US, "%.2f", nutrition.perPortion.saltEquivalentG)} g"
                            ),
                            NutrientRowData(
                                name = "Sodio",
                                isBold = false,
                                per100g = "${nutrition.per100g.sodiumMg.toInt()} mg",
                                perPortion = "${nutrition.perPortion.sodiumMg.toInt()} mg"
                            ),
                            NutrientRowData(
                                name = "Acqua (umidità)",
                                isBold = false,
                                per100g = "${String.format(java.util.Locale.US, "%.1f", nutrition.per100g.waterG)} g",
                                perPortion = "${String.format(java.util.Locale.US, "%.1f", nutrition.perPortion.waterG)} g"
                            )
                        )

                        rows.forEachIndexed { index, row ->
                            val bg = if (index % 2 == 0) Color.Transparent else Color(0xFFF1F8E9).copy(alpha = 0.5f)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(bg)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ){
                                Text(
                                    text = row.name,
                                    fontWeight = if (row.isBold) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 10.5.sp,
                                    color = MacaronText,
                                    modifier = Modifier.weight(1.15f)
                                )
                                Text(
                                    text = row.per100g,
                                    fontWeight = if (row.isBold) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 10.sp,
                                    color = MacaronText,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.weight(1.425f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                                )
                                Text(
                                    text = row.perPortion,
                                    fontWeight = if (row.isBold) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 10.sp,
                                    color = MacaronText,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.weight(1.425f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                                )
                            }
                        }
                    }
                }

                // Dettagli cottura e resa
                if (nutrition.cookingWeightModifier != 0.0){
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Valori nutrizionali calcolati sulla massa cotta finita (Modificatore cottura: ${String.format(java.util.Locale.US, "%.1f", nutrition.cookingWeightModifier)}%).",
                        fontSize = 10.5.sp,
                        color = MacaronSubtleText,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

private data class NutrientRowData(
    val name: String,
    val isBold: Boolean,
    val per100g: String,
    val perPortion: String
)
