package panattoni.simo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import panattoni.simo.R

// Autentico Bubble Font per il branding SIMO (Modak - lettere palloncino ultra-gonfiate)
val BubbleFontFamily = FontFamily(
    Font(R.font.modak, FontWeight.Normal),
    Font(R.font.modak, FontWeight.Bold)
)

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)