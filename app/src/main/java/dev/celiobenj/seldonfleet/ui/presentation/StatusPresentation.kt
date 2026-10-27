package dev.celiobenj.seldonfleet.ui.presentation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import dev.celiobenj.seldonfleet.domain.model.DeviceStatus
import dev.celiobenj.seldonfleet.ui.theme.AppIcons

/**
 * Como cada estado de domínio aparece na tela: rótulo, cores e ícone.
 * Fica na camada ui; o domínio só conhece os nomes OK, ATENCAO, CRITICO e INDETERMINADO.
 * A cor nunca é o único sinal: todo chip de status tem ícone e rótulo.
 */
data class StatusStyle(
    // texto, ícone e borda (contraste >= 4,5:1 sobre o fundo do chip)
    val content: Color,
    val container: Color,
    val icon: ImageVector,
)

/** Rótulo exibido, o mesmo na lista, no filtro e no detalhe. */
fun DeviceStatus.label(): String = when (this) {
    DeviceStatus.OK -> "OK"
    DeviceStatus.ATENCAO -> "ATENÇÃO"
    DeviceStatus.CRITICO -> "CRÍTICO"
    DeviceStatus.INDETERMINADO -> "SEM DADOS"
}

/** Descrição lida pelo leitor de tela. */
fun DeviceStatus.spokenDescription(): String = "Status: " + when (this) {
    DeviceStatus.OK -> "OK"
    DeviceStatus.ATENCAO -> "Atenção"
    DeviceStatus.CRITICO -> "Crítico"
    DeviceStatus.INDETERMINADO -> "Sem dados"
}

fun DeviceStatus.style(): StatusStyle = when (this) {
    DeviceStatus.OK -> StatusStyle(Color(0xFF1B6B3A), Color(0xFFE2F2E7), AppIcons.CheckCircle)
    DeviceStatus.ATENCAO -> StatusStyle(Color(0xFF7A4A00), Color(0xFFFFF0C7), AppIcons.AlertTriangle)
    DeviceStatus.CRITICO -> StatusStyle(Color(0xFF9B1C1C), Color(0xFFFCE4E2), AppIcons.AlertCircle)
    DeviceStatus.INDETERMINADO -> StatusStyle(Color(0xFF605856), Color(0xFFECEAEC), AppIcons.HelpCircle)
}
