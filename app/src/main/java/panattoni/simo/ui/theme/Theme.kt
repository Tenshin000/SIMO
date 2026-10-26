package panattoni.simo.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val MacaronColorScheme = lightColorScheme(
    primary = MacaronPrimary,
    onPrimary = MacaronCard,
    primaryContainer = MacaronSurfaceVariant,
    onPrimaryContainer = MacaronPrimary,
    secondary = MacaronSecondary,
    onSecondary = MacaronCard,
    background = MacaronBackground,
    onBackground = MacaronText,
    surface = MacaronCard,
    onSurface = MacaronText,
    surfaceVariant = MacaronSurfaceVariant,
    onSurfaceVariant = MacaronText,
    outline = MacaronCardBorder
)

@Composable
fun SIMOTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
){
    val colorScheme = MacaronColorScheme

    val view = LocalView.current
    if (!view.isInEditMode){
        SideEffect{
            val window = (view.context as? Activity)?.window
            if (window != null){
                window.statusBarColor = MacaronBackground.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}