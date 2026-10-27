package panattoni.simo.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import panattoni.simo.data.model.ScaledComponent
import panattoni.simo.data.model.ScaledRecipeTree
import panattoni.simo.domain.MeasurementUnit
import panattoni.simo.ui.theme.BubbleFontFamily
import panattoni.simo.ui.theme.MacaronBackground
import panattoni.simo.ui.theme.MacaronCard
import panattoni.simo.ui.theme.MacaronCardBorder
import panattoni.simo.ui.theme.MacaronPrimary
import panattoni.simo.ui.theme.MacaronSubtleText
import panattoni.simo.ui.theme.MacaronSurfaceVariant
import androidx.compose.runtime.collectAsState
import panattoni.simo.service.ActiveKitchenTimer
import panattoni.simo.service.CookingSessionManager
import panattoni.simo.ui.theme.MacaronText
import panattoni.simo.util.CookingTimerExtractor
import panattoni.simo.util.ItalianGrammarUtils
import panattoni.simo.util.NumberUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.UUID

@Composable
fun CookingModeScreen(
    tree: ScaledRecipeTree,
    onClose: () -> Unit
){
    // Gestione del tasto indietro di sistema: chiude Cooking Mode e torna alla vista ricetta
    BackHandler{
        onClose()
    }

    // 1. MODALITÀ SCHERMO SEMPRE ATTIVO ("Keep Screen On") DURANTE LA COOKING MODE
    val currentView = LocalView.current
    DisposableEffect(Unit){
        currentView.keepScreenOn = true
        onDispose{
            currentView.keepScreenOn = false
        }
    }

    val context = LocalContext.current
    val session by CookingSessionManager.sessionState.collectAsState()
    val selectedTab = session?.selectedTab ?: 0
    val checkedIngredients = session?.checkedIngredients ?: emptySet()
    val checkedSteps = session?.checkedSteps ?: emptySet()
    val activeTimers = session?.activeTimers ?: emptyList()

    var showCustomTimerDialog by remember{ mutableStateOf(false) }
    var selectedQuickAddTimer by remember{ mutableStateOf<ActiveKitchenTimer?>(null) }

    // Effetto conto alla rovescia continuo basato sul clock reale, sincronizzato con Foreground Service
    LaunchedEffect(Unit){
        while (isActive){
            delay(1000L)
            CookingSessionManager.tick(context)
        }
    }

    // Ingredienti unificati
    val allIngredients = remember(tree){
        val list = mutableListOf<Pair<String, ScaledComponent>>()
        fun collect(node: ScaledRecipeTree){
            for (comp in node.components){
                list.add(node.recipe.name to comp)
                comp.subRecipeTree?.let{ collect(it) }
            }
        }
        collect(tree)
        list
    }

    // Suddivisione istruzioni per passaggi
    val instructionSteps = remember(tree.recipe.instructions){
        val raw = tree.recipe.instructions.trim()
        if (raw.isBlank()) emptyList()
        else{
            raw.lines()
                .map{ it.trim() }
                .filter{ it.isNotEmpty() }
        }
    }

    Scaffold(
        containerColor = MacaronBackground,
        topBar ={
            Surface(
                color = MacaronCard,
                shadowElevation = 2.dp,
                modifier = Modifier.statusBarsPadding()
            ){
                Column(modifier = Modifier.fillMaxWidth()){
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ){
                        Row(verticalAlignment = Alignment.CenterVertically){
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MacaronPrimary.copy(alpha = 0.15f),
                                modifier = Modifier.size(36.dp)
                            ){
                                Box(contentAlignment = Alignment.Center){
                                    Text("🍳", fontSize = 18.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column{
                                Row(verticalAlignment = Alignment.CenterVertically){
                                    Text(
                                        text = "Cooking Mode",
                                        fontFamily = BubbleFontFamily,
                                        fontSize = 18.sp,
                                        color = MacaronPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFE8F5E9)
                                    ){
                                        Text(
                                            text = "☀️ Schermo Attivo",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32),
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${tree.recipe.name} (${ItalianGrammarUtils.formatQuantityWithUnit(tree.targetYieldQuantity, tree.recipe.yieldUnit)})",
                                    fontSize = 12.sp,
                                    color = MacaronSubtleText,
                                    maxLines = 1
                                )
                            }
                        }

                        IconButton(onClick = onClose){
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Esci dalla Cooking Mode",
                                tint = MacaronText
                            )
                        }
                    }

                    // Selettore Tab
                    PrimaryTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MacaronCard,
                        contentColor = MacaronPrimary
                    ){
                        Tab(
                            selected = selectedTab == 0,
                            onClick ={ CookingSessionManager.setSelectedTab(context, 0) },
                            text ={
                                val checkedCount = checkedIngredients.size
                                Text(
                                    text = "Ingredienti ($checkedCount/${allIngredients.size})",
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == 0) MacaronPrimary else MacaronSubtleText
                                )
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick ={ CookingSessionManager.setSelectedTab(context, 1) },
                            text ={
                                val checkedCount = checkedSteps.size
                                Text(
                                    text = "Istruzioni (${checkedCount}/${instructionSteps.size})",
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == 1) MacaronPrimary else MacaronSubtleText
                                )
                            }
                        )
                    }
                }
            }
        },
        bottomBar ={
            // Barra di controllo e panoramica dei Timer da Cucina Multipli
            MultiKitchenTimerBar(
                timers = activeTimers,
                onAddTimerClick ={ showCustomTimerDialog = true },
                onTimerClick ={ timer -> selectedQuickAddTimer = timer },
                onTogglePause ={ timerId -> CookingSessionManager.togglePauseTimer(context, timerId) },
                onResetTimer ={ timerId -> CookingSessionManager.resetTimer(context, timerId) },
                onDeleteTimer ={ timerId ->
                    CookingSessionManager.deleteTimer(context, timerId)
                    if (selectedQuickAddTimer?.id == timerId){
                        selectedQuickAddTimer = null
                    }
                },
                onDismissAlarm ={ timerId -> CookingSessionManager.dismissAlarm(context, timerId) }
            )
        }
    ){ innerPadding ->
        if (showCustomTimerDialog){
            SetTimerDialog(
                initialSeconds = 300,
                onConfirm ={ chosenSecs, label ->
                    CookingSessionManager.addTimer(context, label, chosenSecs)
                    showCustomTimerDialog = false
                },
                onDismiss ={ showCustomTimerDialog = false }
            )
        }

        if (selectedQuickAddTimer != null){
            val currentSelected = activeTimers.find{ it.id == selectedQuickAddTimer!!.id }
            if (currentSelected != null){
                QuickAddTimerDialog(
                    timer = currentSelected,
                    onAddSeconds ={ addedSecs ->
                        CookingSessionManager.addSecondsToTimer(context, currentSelected.id, addedSecs)
                        selectedQuickAddTimer = null
                    },
                    onDismiss ={ selectedQuickAddTimer = null }
                )
            } else{
                selectedQuickAddTimer = null
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ){
            if (selectedTab == 0){
                item{
                    Text(
                        text = "Tocca un ingrediente appena lo pesi per spuntarlo:",
                        fontSize = 13.sp,
                        color = MacaronSubtleText,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                itemsIndexed(allIngredients){ index, pair ->
                    val sourceName = pair.first
                    val comp = pair.second
                    val item = comp.componentDetail.item
                    val unit = comp.componentDetail.component.unit
                    val key = "${sourceName}_${item.id}_${index}"
                    val isChecked = checkedIngredients.contains(key)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable{ CookingSessionManager.toggleIngredient(context, key) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isChecked) MacaronSurfaceVariant.copy(alpha = 0.5f) else MacaronCard
                        ),
                        border = BorderStroke(1.dp, if (isChecked) Color.Transparent else MacaronCardBorder)
                    ){
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ){
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange ={ CookingSessionManager.toggleIngredient(context, key) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MacaronPrimary,
                                    uncheckedColor = MacaronSubtleText
                                )
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(modifier = Modifier.weight(1f)){
                                Text(
                                    text = item.name,
                                    fontWeight = if (isChecked) FontWeight.Normal else FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = if (isChecked) MacaronSubtleText else MacaronText,
                                    textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None
                                )

                                if (sourceName != tree.recipe.name){
                                    Text(
                                        text = "Per: $sourceName",
                                        fontSize = 11.sp,
                                        color = MacaronSubtleText
                                    )
                                }
                            }

                            // Quantità pesata
                            val displayQty = if (MeasurementUnit.isQb(unit)){
                                "q.b."
                            } else{
                                val qtyVal = if (MeasurementUnit.isIntegerUnit(unit)){
                                    MeasurementUnit.roundIntegerAmount(comp.scaledQuantity).toDouble()
                                } else{
                                    comp.scaledQuantity
                                }
                                val unitStr = ItalianGrammarUtils.formatUnitByQuantity(unit, qtyVal)
                                "${NumberUtils.formatQuantityForUnit(comp.scaledQuantity, unit)} $unitStr"
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isChecked) Color.Transparent else MacaronSurfaceVariant
                            ){
                                Text(
                                    text = displayQty,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isChecked) MacaronSubtleText else MacaronPrimary,
                                    textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            } else{
                if (instructionSteps.isEmpty()){
                    item{
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ){
                            Text(
                                text = "Nessun passaggio o istruzione registrata per questa ricetta.",
                                color = MacaronSubtleText,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else{
                    item{
                        Text(
                            text = "Segui la ricetta passo dopo passo e avvia i timer integrati:",
                            fontSize = 13.sp,
                            color = MacaronSubtleText,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    itemsIndexed(instructionSteps){ index, stepText ->
                        val isChecked = checkedSteps.contains(index)
                        val extractedTimers = remember(stepText){
                            CookingTimerExtractor.extractTimersFromStep(stepText)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable{ CookingSessionManager.toggleStep(context, index) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isChecked) MacaronSurfaceVariant.copy(alpha = 0.5f) else MacaronCard
                            ),
                            border = BorderStroke(1.dp, if (isChecked) Color.Transparent else MacaronCardBorder)
                        ){
                            Column(modifier = Modifier.padding(14.dp)){
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ){
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isChecked) MacaronSubtleText.copy(alpha = 0.2f) else MacaronPrimary,
                                        modifier = Modifier.size(28.dp)
                                    ){
                                        Box(contentAlignment = Alignment.Center){
                                            Text(
                                                text = "${index + 1}",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Text(
                                        text = stepText,
                                        modifier = Modifier.weight(1f),
                                        fontSize = 15.sp,
                                        color = if (isChecked) MacaronSubtleText else MacaronText,
                                        textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
                                        lineHeight = 22.sp
                                    )

                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange ={ CookingSessionManager.toggleStep(context, index) },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = MacaronPrimary,
                                            uncheckedColor = MacaronSubtleText
                                        )
                                    )
                                }

                                // Pulsanti Smart Timer rilevati automaticamente nel passaggio
                                if (extractedTimers.isNotEmpty()){
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 40.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ){
                                        extractedTimers.forEach{ timerPrompt ->
                                            Button(
                                                onClick ={
                                                    val label = "Passo ${index + 1} (${timerPrompt.displayLabel})"
                                                    CookingSessionManager.addTimer(context, label, timerPrompt.totalSeconds)
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3E5F5)),
                                                border = BorderStroke(1.dp, Color(0xFFCE93D8)),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ){
                                                Text("⏱️", fontSize = 13.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Avvia Timer ${timerPrompt.displayLabel}",
                                                    color = Color(0xFF6A1B9A),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
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
    }
}

@Composable
fun MultiKitchenTimerBar(
    timers: List<ActiveKitchenTimer>,
    onAddTimerClick: () -> Unit,
    onTimerClick: (ActiveKitchenTimer) -> Unit ={},
    onTogglePause: (String) -> Unit,
    onResetTimer: (String) -> Unit,
    onDeleteTimer: (String) -> Unit,
    onDismissAlarm: (String) -> Unit
){
    Surface(
        color = MacaronCard,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        border = BorderStroke(1.dp, MacaronCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ){
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ){
            // Se nessun timer è attivo
            if (timers.isEmpty()){
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ){
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = MacaronPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column{
                            Text(
                                text = "Timer da Cucina",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MacaronText
                            )
                            Text(
                                text = "Tocca i bottoni nei passaggi",
                                fontSize = 11.sp,
                                color = MacaronSubtleText,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onAddTimerClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MacaronPrimary),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ){
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Timer",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            } else{
                // Header barra con conteggio timer e tasto +
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ){
                    Text(
                        text = "⏱️ Timer Attivi (${timers.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MacaronPrimary
                    )
                    OutlinedButton(
                        onClick = onAddTimerClick,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ){
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Nuovo Timer", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Lista a scorrimento orizzontale dei timer contemporanei
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ){
                    timers.forEach{ timer ->
                        val rem = timer.currentRemainingSeconds()
                        val mins = rem / 60
                        val secs = rem % 60
                        val timeStr = String.format("%02d:%02d", mins, secs)

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (timer.isFinished) MaterialTheme.colorScheme.errorContainer else Color(0xFFFCE4EC),
                            border = BorderStroke(
                                1.dp,
                                if (timer.isFinished) MaterialTheme.colorScheme.error else MacaronPrimary.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.clickable{ onTimerClick(timer) }
                        ){
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ){
                                Column{
                                    Text(
                                        text = timer.label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (timer.isFinished) MaterialTheme.colorScheme.onErrorContainer else MacaronSubtleText,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = if (timer.isFinished) "⏰ SCADUTO!" else timeStr,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (timer.isFinished) MaterialTheme.colorScheme.onErrorContainer else MacaronPrimary
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                if (timer.isFinished){
                                    val errColor = MaterialTheme.colorScheme.error
                                    TextButton(
                                        onClick ={ onDismissAlarm(timer.id) },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ){
                                        Text("Stop", color = errColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                } else{
                                    IconButton(
                                        onClick ={ onTogglePause(timer.id) },
                                        modifier = Modifier.size(28.dp)
                                    ){
                                        Icon(
                                            imageVector = if (timer.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = MacaronPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick ={ onDeleteTimer(timer.id) },
                                    modifier = Modifier.size(28.dp)
                                ){
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Chiudi",
                                        tint = MacaronSubtleText,
                                        modifier = Modifier.size(16.dp)
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

private fun triggerVibration(context: Context){
    try{
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            val vibrator = vibratorManager?.defaultVibrator
            vibrator?.vibrate(VibrationEffect.createOneShot(800, VibrationEffect.DEFAULT_AMPLITUDE))
        } else{
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            @Suppress("DEPRECATION")
            vibrator?.vibrate(800)
        }
    } catch (_: Exception){}
}

@Composable
fun SetTimerDialog(
    initialSeconds: Int,
    onConfirm: (Int, String) -> Unit,
    onDismiss: () -> Unit
){
    var labelText by remember{ mutableStateOf("") }
    var minutesText by remember{ mutableStateOf((initialSeconds / 60).toString()) }
    var secondsText by remember{ mutableStateOf((initialSeconds % 60).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title ={
            Text(
                text = "⏱️ Imposta Timer da Cucina",
                fontFamily = BubbleFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MacaronText
            )
        },
        text ={
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ){
                OutlinedTextField(
                    value = labelText,
                    onValueChange ={ labelText = it },
                    label ={ Text("Nome timer (es. Cottura pasta, Lievitazione)") },
                    placeholder ={ Text("es. Forno") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    OutlinedTextField(
                        value = minutesText,
                        onValueChange ={ newVal ->
                            if (newVal.all{ it.isDigit() } && newVal.length <= 3){
                                minutesText = newVal
                            }
                        },
                        label ={ Text("Minuti") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MacaronPrimary,
                            unfocusedBorderColor = MacaronCardBorder
                        )
                    )

                    Text(":", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MacaronText)

                    OutlinedTextField(
                        value = secondsText,
                        onValueChange ={ newVal ->
                            if (newVal.all{ it.isDigit() } && newVal.length <= 2){
                                val num = newVal.toIntOrNull() ?: 0
                                if (num < 60){
                                    secondsText = newVal
                                }
                            }
                        },
                        label ={ Text("Secondi") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MacaronPrimary,
                            unfocusedBorderColor = MacaronCardBorder
                        )
                    )
                }

                // Preset rapidi
                Text(
                    text = "Preset rapidi:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MacaronSubtleText
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ){
                    listOf(1, 3, 5, 10, 15).forEach{ presetMins ->
                        OutlinedButton(
                            onClick ={
                                minutesText = presetMins.toString()
                                secondsText = "0"
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MacaronCardBorder)
                        ){
                            Text(
                                text = "${presetMins}m",
                                fontSize = 11.sp,
                                color = MacaronText
                            )
                        }
                    }
                }
            }
        },
        confirmButton ={
            Button(
                onClick ={
                    val m = minutesText.toIntOrNull() ?: 0
                    val s = secondsText.toIntOrNull() ?: 0
                    val total = (m * 60) + s
                    onConfirm(if (total > 0) total else 60, labelText.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = MacaronPrimary),
                shape = RoundedCornerShape(10.dp)
            ){
                Text("Avvia")
            }
        },
        dismissButton ={
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MacaronCardBorder)
            ){
                Text("Annulla", color = MacaronText)
            }
        },
        containerColor = MacaronCard,
        shape = RoundedCornerShape(16.dp)
    )
}

/**
 * Piccola finestra modale / popup compatta che compare al tocco su un timer attivo.
 * Mostra il timer attuale e permette con un tocco di aggiungere:
 * +30s, +1m, +5m o +10m al tempo rimanente.
 * Dotata di icona con croce in cima per chiuderla comodamente.
 */
@Composable
fun QuickAddTimerDialog(
    timer: ActiveKitchenTimer,
    onAddSeconds: (Int) -> Unit,
    onDismiss: () -> Unit
){
    val rem = timer.currentRemainingSeconds()
    val mins = rem / 60
    val secs = rem % 60
    val timeFormatted = String.format("%02d:%02d", mins, secs)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MacaronCard,
        shape = RoundedCornerShape(20.dp),
        title ={
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ){
                Row(verticalAlignment = Alignment.CenterVertically){
                    Text("⏱️", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column{
                        Text(
                            text = timer.label,
                            fontFamily = BubbleFontFamily,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MacaronText,
                            maxLines = 1
                        )
                        Text(
                            text = if (timer.isFinished) "Tempo scaduto" else "Rimanente: $timeFormatted",
                            fontSize = 12.sp,
                            color = if (timer.isFinished) MaterialTheme.colorScheme.error else MacaronPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ){
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Chiudi",
                        tint = MacaronText,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        text ={
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ){
                Text(
                    text = "Aggiungi tempo rapido al timer:",
                    fontSize = 13.sp,
                    color = MacaronSubtleText
                )

                // Bottoni a griglia 2x2 per i pulsanti di aggiunta tempo
                val increments = listOf(
                    30 to "+30 sec",
                    60 to "+1 min",
                    300 to "+5 min",
                    600 to "+10 min"
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ){
                    increments.chunked(2).forEach{ rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ){
                            rowItems.forEach{ (addedSecs, label) ->
                                Button(
                                    onClick ={
                                        onAddSeconds(addedSecs)
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MacaronPrimary.copy(alpha = 0.12f),
                                        contentColor = MacaronPrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.2.dp, MacaronPrimary.copy(alpha = 0.5f)),
                                    contentPadding = PaddingValues(vertical = 10.dp)
                                ){
                                    Text(
                                        text = label,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton ={}
    )
}

