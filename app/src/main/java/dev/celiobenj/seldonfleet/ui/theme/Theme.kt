package dev.celiobenj.seldonfleet.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Esquema de cores do Material 3 preenchido com a paleta do app.
 * Sempre claro: sem tema escuro e sem cor dinâmica do sistema (Material You).
 */
private val AppColorScheme = lightColorScheme(
    primary = Cerulean,
    onPrimary = White,
    // secundária: usada pelos chips de filtro selecionados
    secondary = Cerulean,
    onSecondary = White,
    secondaryContainer = SelectedChip,
    onSecondaryContainer = CharcoalBlue,
    background = White,
    onBackground = CharcoalBlue,
    surface = White,
    onSurface = CharcoalBlue,
    surfaceVariant = CardSurface,
    onSurfaceVariant = TaupeGrey,
    // contêineres de superfície do Material 3 (cartões, barras): mesma superfície de cartão
    surfaceContainerLowest = White,
    surfaceContainerLow = CardSurface,
    surfaceContainer = CardSurface,
    surfaceContainerHigh = CardSurface,
    surfaceContainerHighest = CardSurface,
    outline = TaupeGrey,
    outlineVariant = PaleSlate,
)

@Composable
fun SeldonFleetTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content,
    )
}
