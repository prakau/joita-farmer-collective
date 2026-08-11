package ai.joita.biosoil.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val JoitaGreen = Color(0xFF238B2A)
val JoitaGreenDark = Color(0xFF125C2A)
val LeafLight = Color(0xFFDDF3DD)
val Cream = Color(0xFFFFF9ED)
val Ink = Color(0xFF111817)
val InkMuted = Color(0xFF59645E)
val Line = Color(0xFFD9E2DA)
val Turmeric = Color(0xFFF4A622)
val SoilBrown = Color(0xFF7A4E2D)
val InfoBlue = Color(0xFF1769AA)

private val JoitaColors = lightColorScheme(
    primary = JoitaGreen,
    onPrimary = Color.White,
    primaryContainer = LeafLight,
    onPrimaryContainer = JoitaGreenDark,
    secondary = SoilBrown,
    onSecondary = Color.White,
    tertiary = Turmeric,
    onTertiary = Ink,
    background = Cream,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF1F5F0),
    onSurfaceVariant = InkMuted,
    outline = Line,
    error = Color(0xFFBA1A1A),
)

private val JoitaTypography = Typography(
    bodySmall = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    labelLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    headlineMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.SemiBold,
    ),
)

private val JoitaShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun JoitaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = JoitaColors,
        typography = JoitaTypography,
        shapes = JoitaShapes,
        content = content,
    )
}

