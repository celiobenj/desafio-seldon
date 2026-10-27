package dev.celiobenj.seldonfleet.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.celiobenj.seldonfleet.domain.model.DeviceStatus
import dev.celiobenj.seldonfleet.domain.repository.FleetLoadException
import dev.celiobenj.seldonfleet.ui.components.FavoriteToggle
import dev.celiobenj.seldonfleet.ui.components.LoadingState
import dev.celiobenj.seldonfleet.ui.components.MessageState
import dev.celiobenj.seldonfleet.ui.components.StatusChip
import dev.celiobenj.seldonfleet.ui.presentation.label
import dev.celiobenj.seldonfleet.ui.theme.AppIcons
import dev.celiobenj.seldonfleet.ui.theme.Cerulean
import dev.celiobenj.seldonfleet.ui.theme.CharcoalBlue
import dev.celiobenj.seldonfleet.ui.theme.DeviceIdStyle
import dev.celiobenj.seldonfleet.ui.theme.MonoLabelStyle
import dev.celiobenj.seldonfleet.ui.theme.SelectedChip
import dev.celiobenj.seldonfleet.ui.theme.TaupeGrey

/** Liga a tela ao ViewModel: coleta o estado e repassa os eventos. */
@Composable
fun FleetListRoute(viewModel: FleetListViewModel, onOpenDevice: (String) -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    FleetListScreen(
        state = state,
        onSelectFilter = viewModel::selectFilter,
        onToggleFavorite = viewModel::setFavorite,
        onRetry = viewModel::retry,
        onOpenDevice = onOpenDevice,
    )
}

/** Tela 1: lista da frota. Só desenha o estado recebido; não classifica nada. */
@Composable
fun FleetListScreen(
    state: FleetListUiState,
    onSelectFilter: (DeviceStatus?) -> Unit,
    onToggleFavorite: (String, Boolean) -> Unit,
    onRetry: () -> Unit,
    onOpenDevice: (String) -> Unit,
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(Modifier.padding(innerPadding).fillMaxSize()) {
            Text(
                text = "Frota",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                    .semantics { heading() },
            )
            when (state) {
                FleetListUiState.Loading -> LoadingState("Lendo frota.json…")
                is FleetListUiState.Error -> MessageState(
                    title = "Não foi possível carregar a frota",
                    message = errorMessage(state.reason),
                    actionLabel = "Tentar de novo",
                    actionIcon = AppIcons.Refresh,
                    onAction = onRetry,
                )
                is FleetListUiState.Content -> FleetContent(state, onSelectFilter, onToggleFavorite, onOpenDevice)
            }
        }
    }
}

private fun errorMessage(reason: FleetLoadException.Reason?): String = when (reason) {
    FleetLoadException.Reason.FILE_UNAVAILABLE -> "O arquivo frota.json não foi encontrado ou não pôde ser lido."
    FleetLoadException.Reason.INVALID_CONTENT -> "O arquivo frota.json é inválido ou está fora do esquema esperado."
    null -> "Ocorreu um erro inesperado ao ler o arquivo frota.json."
}

@Composable
private fun FleetContent(
    state: FleetListUiState.Content,
    onSelectFilter: (DeviceStatus?) -> Unit,
    onToggleFavorite: (String, Boolean) -> Unit,
    onOpenDevice: (String) -> Unit,
) {
    Text(
        text = "${state.totalCount} DISPOSITIVOS · LIDOS DE ASSETS",
        style = MonoLabelStyle,
        color = TaupeGrey,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
    FilterRow(state, onSelectFilter)
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

    if (state.items.isEmpty()) {
        MessageState(
            title = "Nenhum dispositivo com este status",
            message = "Escolha outro filtro ou limpe o filtro para ver todos os dispositivos.",
            actionLabel = "Limpar filtro",
            onAction = { onSelectFilter(null) },
        )
        return
    }

    LazyColumn(Modifier.fillMaxSize()) {
        items(state.items, key = { it.id }) { item ->
            DeviceRow(
                item = item,
                onOpen = { onOpenDevice(item.id) },
                onToggleFavorite = { favorite -> onToggleFavorite(item.id, favorite) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

/** Filtros por status com a contagem de cada estado; o selecionado tem fundo, borda e check. */
@Composable
private fun FilterRow(state: FleetListUiState.Content, onSelectFilter: (DeviceStatus?) -> Unit) {
    val options: List<Pair<DeviceStatus?, String>> =
        listOf<Pair<DeviceStatus?, String>>(null to "TODOS ${state.totalCount}") +
            DeviceStatus.entries.map { it to "${it.label()} ${state.counts[it] ?: 0}" }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(options, key = { it.first?.name ?: "ALL" }) { (status, text) ->
            val selected = state.selectedFilter == status
            FilterChip(
                selected = selected,
                onClick = { onSelectFilter(status) },
                label = { Text(text, style = MonoLabelStyle) },
                leadingIcon = if (selected) {
                    { Icon(AppIcons.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else {
                    null
                },
                colors = FilterChipDefaults.filterChipColors(
                    labelColor = CharcoalBlue,
                    selectedContainerColor = SelectedChip,
                    selectedLabelColor = CharcoalBlue,
                    selectedLeadingIconColor = CharcoalBlue,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = TaupeGrey,
                    selectedBorderColor = Cerulean,
                    borderWidth = 1.dp,
                    selectedBorderWidth = 1.dp,
                ),
            )
        }
    }
}

/** Linha da lista: tocar abre o detalhe; a estrela tem área de toque própria e não navega. */
@Composable
private fun DeviceRow(item: DeviceListItem, onOpen: () -> Unit, onToggleFavorite: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Abrir detalhe de ${item.id}", onClick = onOpen)
            .heightIn(min = 72.dp)
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(item.id, style = DeviceIdStyle, color = MaterialTheme.colorScheme.onBackground)
            Text(
                text = "${item.model} · ${item.soc}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.size(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${item.batteryLevelPct}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { contentDescription = "Bateria ${item.batteryLevelPct} %" },
            )
            Spacer(Modifier.size(4.dp))
            StatusChip(item.status)
        }
        FavoriteToggle(deviceId = item.id, isFavorite = item.isFavorite, onToggle = onToggleFavorite)
    }
}
