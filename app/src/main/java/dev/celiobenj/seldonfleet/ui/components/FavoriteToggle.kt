package dev.celiobenj.seldonfleet.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.celiobenj.seldonfleet.ui.theme.AppIcons
import dev.celiobenj.seldonfleet.ui.theme.Cerulean
import dev.celiobenj.seldonfleet.ui.theme.TaupeGrey

/**
 * Estrela de favorito com alvo de toque de 48 dp. Preenchida em Cerulean quando favorito,
 * contorno em Taupe Grey quando não: muda a forma, além da cor.
 */
@Composable
fun FavoriteToggle(
    deviceId: String,
    isFavorite: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    IconToggleButton(checked = isFavorite, onCheckedChange = onToggle, modifier = modifier) {
        Icon(
            imageVector = if (isFavorite) AppIcons.StarFilled else AppIcons.StarOutline,
            contentDescription = if (isFavorite) {
                "Remover $deviceId dos favoritos"
            } else {
                "Marcar $deviceId como favorito"
            },
            tint = if (isFavorite) Cerulean else TaupeGrey,
        )
    }
}
