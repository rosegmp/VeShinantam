package app.veshinantam.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DeepBlue = Color(0xFF173B67)
private val DeepBlueDark = Color(0xFFA9C7F5)
private val WarmGoldDark = Color(0xFFE7C578)
private val AccessibleGold = Color(0xFF765A16)

private val LightColors = lightColorScheme(
    primary = DeepBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E4FF),
    onPrimaryContainer = Color(0xFF001B3D),
    secondary = Color(0xFF765A16),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE08A),
    onSecondaryContainer = Color(0xFF251A00),
    tertiary = AccessibleGold,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE08A),
    onTertiaryContainer = Color(0xFF251A00),
    surface = Color(0xFFF9F9FC),
    surfaceVariant = Color(0xFFE0E2E8),
)

private val DarkColors = darkColorScheme(
    primary = DeepBlueDark,
    onPrimary = Color(0xFF002F61),
    primaryContainer = Color(0xFF0B477C),
    onPrimaryContainer = Color(0xFFD6E4FF),
    secondary = WarmGoldDark,
    onSecondary = Color(0xFF3D2E00),
    secondaryContainer = Color(0xFF574500),
    onSecondaryContainer = Color(0xFFFFE08A),
    tertiary = WarmGoldDark,
    onTertiary = Color(0xFF3D2E00),
    tertiaryContainer = Color(0xFF574500),
    onTertiaryContainer = Color(0xFFFFE08A),
    surface = Color(0xFF111318),
    surfaceVariant = Color(0xFF44474E),
)

@Composable
fun VeShinantamTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content,
    )
}
