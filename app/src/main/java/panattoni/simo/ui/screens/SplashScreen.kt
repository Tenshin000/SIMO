package panattoni.simo.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import panattoni.simo.ui.theme.BubbleFontFamily
import panattoni.simo.ui.theme.MacaronBackground
import panattoni.simo.ui.theme.MacaronPrimary
import panattoni.simo.ui.theme.MacaronSubtleText
import panattoni.simo.ui.theme.MacaronSurfaceVariant
import panattoni.simo.ui.theme.MacaronTitleSimo
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Schermata di avvio (Splash Screen) con animazione della fragola e barra di caricamento.
 */
@Composable
fun SplashScreen(
    isReady: () -> Boolean = { true },
    onSplashFinished: () -> Unit
){
    // Animazione di avanzamento barra
    val progress = remember{ Animatable(0f) }

    // Animazione rimbalzo/pulsazione fragola
    val infiniteTransition = rememberInfiniteTransition(label = "strawberry_bounce")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit){
        // Avvia l'inizializzazione del database e il popolamento ricette in background
        val dbInitDeferred = async(Dispatchers.IO){
            try {
                val db = panattoni.simo.data.local.AppDatabase.getDatabase(context, this)
                panattoni.simo.data.local.AppDatabase.ensurePrepopulated(db, context)
            } catch (_: Exception){}
        }

        // Animazione barra di caricamento
        val animJob = launch {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
            )
        }

        // Assicura che l'inizializzazione del DB e l'animazione siano completate con timeout di sicurezza
        try {
            withTimeoutOrNull(2500){
                dbInitDeferred.await()
            }
        } catch (_: Exception){}

        try {
            withTimeoutOrNull(1200){
                animJob.join()
            }
        } catch (_: Exception){}

        // Attende che le ricette siano completamente caricate in memoria prima di mostrare il ricettario
        try {
            withTimeoutOrNull(1500){
                snapshotFlow { isReady() }.first { it }
            }
        } catch (_: Exception){}

        delay(80)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MacaronBackground),
        contentAlignment = Alignment.Center
    ){
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ){
            // Illustrazione vettoriale della fragola con animazione fluida
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .scale(scale),
                contentAlignment = Alignment.Center
            ){
                StrawberryIllustration()
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Titolo SIMO con Bubble Font
            Text(
                text = "SIMO",
                color = MacaronTitleSimo,
                fontFamily = BubbleFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 54.sp,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Scalable Ingredient Management Organizer",
                color = MacaronSubtleText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Barra di caricamento elegante
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MacaronSurfaceVariant)
            ){
                LinearProgressIndicator(
                    progress ={ progress.value },
                    modifier = Modifier.fillMaxSize(),
                    color = MacaronPrimary,
                    trackColor = Color.Transparent,
                )
            }
        }
    }
}

@Composable
fun StrawberryIllustration(){
    Canvas(modifier = Modifier.size(110.dp)){
        val w = size.width
        val h = size.height

        // Corpo fragola (rosso-lampone)
        val bodyPath = Path().apply{
            moveTo(w * 0.5f, h * 0.92f)
            cubicTo(w * 0.35f, h * 0.90f, w * 0.16f, h * 0.68f, w * 0.16f, h * 0.44f)
            cubicTo(w * 0.16f, h * 0.28f, w * 0.35f, h * 0.24f, w * 0.5f, h * 0.32f)
            cubicTo(w * 0.65f, h * 0.24f, w * 0.84f, h * 0.28f, w * 0.84f, h * 0.44f)
            cubicTo(w * 0.84f, h * 0.68f, w * 0.65f, h * 0.90f, w * 0.5f, h * 0.92f)
            close()
        }
        drawPath(bodyPath, color = Color(0xFFE24B6A))

        // Riflesso di luce morbido
        val highlightPath = Path().apply{
            moveTo(w * 0.28f, h * 0.38f)
            cubicTo(w * 0.32f, h * 0.30f, w * 0.42f, h * 0.30f, w * 0.46f, h * 0.34f)
            cubicTo(w * 0.38f, h * 0.34f, w * 0.30f, h * 0.40f, w * 0.28f, h * 0.48f)
            close()
        }
        drawPath(highlightPath, color = Color(0xFFF0728D))

        // Foglioline verdi della fragola
        val leafCenter = Path().apply{
            moveTo(w * 0.5f, h * 0.30f)
            cubicTo(w * 0.48f, h * 0.18f, w * 0.52f, h * 0.12f, w * 0.5f, h * 0.10f)
            cubicTo(w * 0.54f, h * 0.15f, w * 0.54f, h * 0.22f, w * 0.5f, h * 0.30f)
            close()
        }
        drawPath(leafCenter, color = Color(0xFF5CB87A))

        val leafLeft = Path().apply{
            moveTo(w * 0.5f, h * 0.30f)
            cubicTo(w * 0.44f, h * 0.20f, w * 0.30f, h * 0.16f, w * 0.25f, h * 0.22f)
            cubicTo(w * 0.32f, h * 0.26f, w * 0.42f, h * 0.28f, w * 0.5f, h * 0.30f)
            close()
        }
        drawPath(leafLeft, color = Color(0xFF48A868))

        val leafRight = Path().apply{
            moveTo(w * 0.5f, h * 0.30f)
            cubicTo(w * 0.56f, h * 0.20f, w * 0.70f, h * 0.16f, w * 0.75f, h * 0.22f)
            cubicTo(w * 0.68f, h * 0.26f, w * 0.58f, h * 0.28f, w * 0.5f, h * 0.30f)
            close()
        }
        drawPath(leafRight, color = Color(0xFF48A868))

        // Picciolo
        val stem = Path().apply{
            moveTo(w * 0.49f, h * 0.14f)
            cubicTo(w * 0.48f, h * 0.08f, w * 0.54f, h * 0.05f, w * 0.56f, h * 0.04f)
            cubicTo(w * 0.54f, h * 0.08f, w * 0.52f, h * 0.11f, w * 0.51f, h * 0.14f)
            close()
        }
        drawPath(stem, color = Color(0xFF2D6A40))

        // Semini dorati (piccoli ovali)
        val seedColor = Color(0xFFFFF2A3)
        val seeds = listOf(
            Offset(w * 0.38f, h * 0.45f),
            Offset(w * 0.50f, h * 0.43f),
            Offset(w * 0.62f, h * 0.45f),
            Offset(w * 0.30f, h * 0.55f),
            Offset(w * 0.44f, h * 0.54f),
            Offset(w * 0.56f, h * 0.54f),
            Offset(w * 0.70f, h * 0.55f),
            Offset(w * 0.38f, h * 0.66f),
            Offset(w * 0.50f, h * 0.65f),
            Offset(w * 0.62f, h * 0.66f),
            Offset(w * 0.44f, h * 0.76f),
            Offset(w * 0.56f, h * 0.76f),
            Offset(w * 0.50f, h * 0.83f)
        )
        seeds.forEach{ pt ->
            drawOval(
                color = seedColor,
                topLeft = Offset(pt.x - 2f, pt.y - 3f),
                size = Size(4f, 6f)
            )
        }
    }
}
