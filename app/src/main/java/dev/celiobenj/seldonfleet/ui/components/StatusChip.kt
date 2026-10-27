package dev.celiobenj.seldonfleet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.celiobenj.seldonfleet.domain.model.DeviceStatus
import dev.celiobenj.seldonfleet.ui.presentation.label
import dev.celiobenj.seldonfleet.ui.presentation.spokenDescription
import dev.celiobenj.seldonfleet.ui.presentation.style
import dev.celiobenj.seldonfleet.ui.theme.MonoLabelStyle

/** Chip de status: ícone + rótulo, com borda e fundo na cor do estado. Lido como "Status: Crítico". */
@Composable
fun StatusChip(status: DeviceStatus, modifier: Modifier = Modifier) {
    val style = status.style()
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = modifier
            .clearAndSetSemantics { contentDescription = status.spokenDescription() }
            .background(style.container, shape)
            .border(1.dp, style.content, shape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(imageVector = style.icon, contentDescription = null, tint = style.content, modifier = Modifier.size(16.dp))
        Text(text = status.label(), style = MonoLabelStyle.copy(fontWeight = FontWeight.Bold), color = style.content)
    }
}
