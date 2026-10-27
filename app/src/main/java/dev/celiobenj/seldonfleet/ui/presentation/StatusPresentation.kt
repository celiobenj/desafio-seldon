package dev.celiobenj.seldonfleet.ui.presentation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import dev.celiobenj.seldonfleet.domain.model.DeviceStatus
import dev.celiobenj.seldonfleet.ui.theme.AppIcons

/**
 * Como cada estado de domínio aparece na tela: cores e ícone (o rótulo vem de [label]).
 * Fica na camada ui; o domínio só conhece os nomes OK, ATENCAO, CRITICO e INDETERMINADO.
 * A cor nunca é o único sinal: todo chip de status tem ícone e rótulo.
 */
data class StatusPresentation(
    // texto, ícone e borda (contraste >= 4,5:1 sobre o fundo do chip)
    val content: Color,
    val container: Color,
    val icon: ImageVector,
)

// Cores de status: texto/ícone e fundo do chip.
private val OkContent = Color(0xFF1B6B3A)
private val OkContainer = Color(0xFFE2F2E7)
private val AttentionContent = Color(0xFF7A4A00)
private val AttentionContainer = Color(0xFFFFF0C7)
private val CriticalContent = Color(0xFF9B1C1C)
private val CriticalContainer = Color(0xFFFCE4E2)
private val NoDataContent = Color(0xFF605856)
private val NoDataContainer = Color(0xFFECEAEC)

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

fun DeviceStatus.presentation(): StatusPresentation = when (this) {
    DeviceStatus.OK -> StatusPresentation(OkContent, OkContainer, AppIcons.CheckCircle)
    DeviceStatus.ATENCAO -> StatusPresentation(AttentionContent, AttentionContainer, AppIcons.AlertTriangle)
    DeviceStatus.CRITICO -> StatusPresentation(CriticalContent, CriticalContainer, AppIcons.AlertCircle)
    DeviceStatus.INDETERMINADO -> StatusPresentation(NoDataContent, NoDataContainer, AppIcons.HelpCircle)
}
