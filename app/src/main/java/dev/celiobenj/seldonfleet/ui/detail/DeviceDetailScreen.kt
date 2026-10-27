package dev.celiobenj.seldonfleet.ui.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.celiobenj.seldonfleet.domain.model.Classification
import dev.celiobenj.seldonfleet.domain.model.Device
import dev.celiobenj.seldonfleet.domain.repository.FleetLoadException
import dev.celiobenj.seldonfleet.ui.components.LoadingState
import dev.celiobenj.seldonfleet.ui.components.MessageState
import dev.celiobenj.seldonfleet.ui.presentation.formatDecimal
import dev.celiobenj.seldonfleet.ui.presentation.formatLastSeen
import dev.celiobenj.seldonfleet.ui.presentation.label
import dev.celiobenj.seldonfleet.ui.presentation.spokenDescription
import dev.celiobenj.seldonfleet.ui.presentation.presentation
import dev.celiobenj.seldonfleet.ui.theme.AppIcons
import dev.celiobenj.seldonfleet.ui.theme.Cerulean
import dev.celiobenj.seldonfleet.ui.theme.DeviceIdStyle
import dev.celiobenj.seldonfleet.ui.theme.MonoLabelStyle
import dev.celiobenj.seldonfleet.ui.theme.TaupeGrey

/** Liga a tela ao ViewModel: coleta o estado e repassa os eventos. */
@Composable
fun DeviceDetailRoute(viewModel: DeviceDetailViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DeviceDetailScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        onToggleFavorite = viewModel::setFavorite,
    )
}

/** Tela 2: detalhe com o status e a justificativa no topo, antes dos atributos. */
@Composable
fun DeviceDetailScreen(
    state: DeviceDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onToggleFavorite: (Boolean) -> Unit,
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(Modifier.padding(innerPadding).fillMaxSize()) {
            // Mesmo efeito do botão voltar do sistema.
            TextButton(onClick = onBack, modifier = Modifier.padding(start = 4.dp, top = 4.dp)) {
                Icon(AppIcons.ArrowBack, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(8.dp))
                Text("Frota", style = MaterialTheme.typography.titleMedium)
            }
            when (state) {
                DeviceDetailUiState.Loading -> LoadingState("Carregando o dispositivo…")
                is DeviceDetailUiState.NotFound -> MessageState(
                    title = "Dispositivo não encontrado",
                    message = "Não há dispositivo com o identificador ${state.deviceId} no arquivo frota.json.",
                    actionLabel = "Voltar para a frota",
                    onAction = onBack,
                )
                is DeviceDetailUiState.Error -> MessageState(
                    title = "Não foi possível carregar o dispositivo",
                    message = when (state.reason) {
                        FleetLoadException.Reason.FILE_UNAVAILABLE ->
                            "O arquivo frota.json não foi encontrado ou não pôde ser lido."
                        FleetLoadException.Reason.INVALID_CONTENT ->
                            "O arquivo frota.json é inválido ou está fora do esquema esperado."
                        null -> "Ocorreu um erro inesperado ao ler o arquivo frota.json."
                    },
                    actionLabel = "Tentar de novo",
                    actionIcon = AppIcons.Refresh,
                    onAction = onRetry,
                )
                is DeviceDetailUiState.Content -> DetailContent(state, onToggleFavorite)
            }
        }
    }
}

@Composable
private fun DetailContent(state: DeviceDetailUiState.Content, onToggleFavorite: (Boolean) -> Unit) {
    val device = state.device
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
            Text(
                text = device.id,
                style = DeviceIdStyle.copy(fontSize = 28.sp, lineHeight = 34.sp),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = "${device.model} · ${device.soc} · tier ${device.tier.name.lowercase()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        StatusCard(state.classification, state.justification)
        AttributesGrid(device, state.classification)
        FavoriteButton(device.id, state.isFavorite, onToggleFavorite)

        Text(
            text = "ÚLTIMO CONTATO · ${formatLastSeen(device.lastSeen)}",
            style = MonoLabelStyle,
            color = TaupeGrey,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
    }
}

/** Cartão com o status (ícone + rótulo) e a justificativa, na cor do estado. */
@Composable
private fun StatusCard(classification: Classification, justification: String) {
    val status = classification.status
    val style = status.presentation()
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = style.container),
        border = BorderStroke(1.dp, style.content),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.semantics(mergeDescendants = true) {
                    stateDescription = status.spokenDescription()
                },
            ) {
                Icon(style.icon, contentDescription = null, tint = style.content, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(8.dp))
                Text(status.label(), style = MonoLabelStyle.copy(fontWeight = FontWeight.Bold), color = style.content)
            }
            Text(justification, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

/** Atributos em grade de 2 colunas, separados por divisores de 1 dp. */
@Composable
private fun AttributesGrid(device: Device, classification: Classification) {
    val attributes = listOf(
        "BATERIA" to "${device.batteryLevelPct} %",
        "MEMÓRIA (RAM)" to "${device.ramMb} MB",
        "NÍVEL DE API" to "${device.androidApi}",
        "CAPACIDADE" to (device.batteryCapacityMah?.let { "$it mAh" } ?: "—"),
        "CONSUMO (NORMALIZADO)" to (device.energyConsumedMah?.let { "${formatDecimal(it)} mAh" } ?: "Indisponível"),
        "TELA LIGADA" to (device.screenOnMinutes?.let { "$it min" } ?: "—"),
        "DRENO ABSOLUTO" to (classification.absoluteDrainMahPerHour?.let { "${formatDecimal(it)} mAh/h" } ?: "—"),
        "DRENO RELATIVO" to (classification.relativeDrainPctPerHour?.let { "${formatDecimal(it)} %/h" } ?: "—"),
        "QUADROS PERDIDOS" to "${formatDecimal(device.jankFramesPct)} %",
        "FALHAS (7 DIAS)" to "${device.crashCount7d}",
    )
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        attributes.chunked(2).forEachIndexed { index, row ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.height(IntrinsicSize.Min)) {
                row.forEachIndexed { column, (label, value) ->
                    if (column > 0) {
                        VerticalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.fillMaxHeight(),
                        )
                    }
                    AttributeCell(label, value, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AttributeCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(12.dp)
            .semantics(mergeDescendants = true) {},
    ) {
        Text(label, style = MonoLabelStyle, color = TaupeGrey)
        Spacer(Modifier.size(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** Botão de favorito do detalhe: estrela cheia + "FAVORITO" ou contorno + "MARCAR COMO FAVORITO". */
@Composable
private fun FavoriteButton(deviceId: String, isFavorite: Boolean, onToggleFavorite: (Boolean) -> Unit) {
    OutlinedButton(
        onClick = { onToggleFavorite(!isFavorite) },
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (isFavorite) Cerulean else TaupeGrey),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .semantics {
                stateDescription = if (isFavorite) "$deviceId é favorito" else "$deviceId não é favorito"
            },
    ) {
        Icon(
            imageVector = if (isFavorite) AppIcons.StarFilled else AppIcons.StarOutline,
            contentDescription = null,
            tint = if (isFavorite) Cerulean else TaupeGrey,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = if (isFavorite) "FAVORITO" else "MARCAR COMO FAVORITO",
            style = MonoLabelStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
            color = if (isFavorite) Cerulean else MaterialTheme.colorScheme.onSurface,
        )
    }
}
