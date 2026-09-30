package com.example.onefifty.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryOrange,
    onPrimary = TextOnPrimary,
    secondary = SecondarySlate,
    onSecondary = TextPrimary,
    tertiary = TertiaryIndigo,
    background = NeutralDark,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceChip,
    onSurfaceVariant = TextSecondary
)

@Composable
fun OneFiftyTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
