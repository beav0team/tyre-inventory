package com.yassine.inventory.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Brand — ember orange / amber "workshop" palette
val Ember = Color(0xFFC4540F)
val EmberContainer = Color(0xFFFFDCC7)
val OnEmber = Color(0xFFFFFFFF)
val OnEmberContainer = Color(0xFF3A1D00)

val Amber = Color(0xFF7A5900)
val AmberContainer = Color(0xFFFFDFA1)
val OnAmber = Color(0xFFFFFFFF)
val OnAmberContainer = Color(0xFF261900)

val Teal = Color(0xFF00687A)
val TealContainer = Color(0xFFA9ECFF)
val OnTeal = Color(0xFFFFFFFF)
val OnTealContainer = Color(0xFF001F27)

// Light neutrals — warm off-whites
val LightBg = Color(0xFFFEF7F3)
val LightOnBg = Color(0xFF211A17)
val LightSurfaceVariant = Color(0xFFF3DED2)
val LightOnSurfaceVariant = Color(0xFF52443E)
val LightOutline = Color(0xFF856F65)
val LightOutlineVariant = Color(0xFFD8C2B5)

// Dark neutrals — warm charcoal / night-workshop
val DarkBg = Color(0xFF1A120B)
val DarkOnBg = Color(0xFFF0DFD8)
val DarkSurfaceVariant = Color(0xFF53433C)
val DarkOnSurfaceVariant = Color(0xFFD7C3B9)
val DarkOutline = Color(0xFFA08D82)
val DarkOutlineVariant = Color(0xFF53433C)

val LightColors = lightColorScheme(
    primary = Ember,
    onPrimary = OnEmber,
    primaryContainer = EmberContainer,
    onPrimaryContainer = OnEmberContainer,
    secondary = Amber,
    onSecondary = OnAmber,
    secondaryContainer = AmberContainer,
    onSecondaryContainer = OnAmberContainer,
    tertiary = Teal,
    onTertiary = OnTeal,
    tertiaryContainer = TealContainer,
    onTertiaryContainer = OnTealContainer,
    background = LightBg,
    onBackground = LightOnBg,
    surface = LightBg,
    onSurface = LightOnBg,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
)

val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB77D),
    onPrimary = Color(0xFF502600),
    primaryContainer = Color(0xFF963B00),
    onPrimaryContainer = Color(0xFFFFDCC7),
    secondary = Color(0xFFE7BE70),
    onSecondary = Color(0xFF432D00),
    secondaryContainer = Color(0xFF5F4300),
    onSecondaryContainer = Color(0xFFFFDFA1),
    tertiary = Color(0xFF4CD3EC),
    onTertiary = Color(0xFF00363F),
    tertiaryContainer = Color(0xFF004E5C),
    onTertiaryContainer = Color(0xFFA9ECFF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = DarkBg,
    onBackground = DarkOnBg,
    surface = DarkBg,
    onSurface = DarkOnBg,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
)