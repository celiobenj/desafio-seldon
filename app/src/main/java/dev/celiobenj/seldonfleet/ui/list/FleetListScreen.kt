package dev.celiobenj.seldonfleet.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
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
        onNextFilter = viewModel::selectNextFilter,
        onPreviousFilter = viewModel::selectPreviousFilter,
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
    onNextFilter: () -> Unit,
    onPreviousFilter: () -> Unit,
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
                is FleetListUiState.Content -> FleetContent(
                    state = state,
                    onSelectFilter = onSelectFilter,
                    onNextFilter = onNextFilter,
                    onPreviousFilter = onPreviousFilter,
                    onToggleFavorite = onToggleFavorite,
                    onOpenDevice = onOpenDevice,
                )
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
    onNextFilter: () -> Unit,
    onPreviousFilter: () -> Unit,
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

    // Ao trocar de filtro, a lista volta ao topo. O último filtro visto é salvo junto com a tela,
    // para que voltar do detalhe (mesmo filtro) preserve a posição de rolagem.
    val listState = rememberLazyListState()
    var lastFilter by rememberSaveable { mutableStateOf(state.selectedFilter?.name) }
    LaunchedEffect(state.selectedFilter) {
        val current = state.selectedFilter?.name
        if (current != lastFilter) {
            listState.scrollToItem(0)
            lastFilter = current
        }
    }

    // Atalho: arrastar na horizontal sobre a lista troca de filtro; os chips continuam sendo o controle principal.
    Box(Modifier.fillMaxSize().filterSwipe(onNext = onNextFilter, onPrevious = onPreviousFilter)) {
        if (state.items.isEmpty()) {
            MessageState(
                title = "Nenhum dispositivo com este status",
                message = "Escolha outro filtro ou limpe o filtro para ver todos os dispositivos.",
                actionLabel = "Limpar filtro",
                onAction = { onSelectFilter(null) },
            )
        } else {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
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
    }
}

/** Distância mínima do arraste horizontal para trocar de filtro. */
private val SWIPE_THRESHOLD = 80.dp

/** Faixa das bordas laterais ignorada pelo gesto (reservada ao voltar do sistema). */
private val EDGE_EXCLUSION = 24.dp

/**
 * Arrastar da direita para a esquerda chama [onNext]; da esquerda para a direita, [onPrevious].
 * Só reage depois que o movimento horizontal passa da tolerância de toque: arrastes verticais
 * ficam com a lista (rolagem), e toques simples continuam chegando às linhas e às estrelas.
 */
private fun Modifier.filterSwipe(onNext: () -> Unit, onPrevious: () -> Unit): Modifier =
    pointerInput(onNext, onPrevious) {
        val threshold = SWIPE_THRESHOLD.toPx()
        val edge = EDGE_EXCLUSION.toPx()
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            if (down.position.x < edge || down.position.x > size.width - edge) return@awaitEachGesture

            var total = 0f
            // null: o dedo subiu ou o movimento foi consumido pela rolagem vertical da lista.
            val drag = awaitHorizontalTouchSlopOrCancellation(down.id) { change, overSlop ->
                change.consume()
                total += overSlop
            } ?: return@awaitEachGesture

            val finished = horizontalDrag(drag.id) { change ->
                total += change.positionChange().x
                change.consume()
            }
            if (finished) {
                when {
                    total <= -threshold -> onNext()
                    total >= threshold -> onPrevious()
                }
            }
        }
    }

/** Filtros por status com a contagem de cada estado; o selecionado tem fundo, borda e check. */
@Composable
private fun FilterRow(state: FleetListUiState.Content, onSelectFilter: (DeviceStatus?) -> Unit) {
    val options: List<Pair<DeviceStatus?, String>> = FILTER_ORDER.map { status ->
        status to if (status == null) "TODOS ${state.totalCount}" else "${status.label()} ${state.counts[status] ?: 0}"
    }

    // Quando o filtro muda (toque ou gesto), rola a fileira até o chip selecionado, deixando o
    // anterior parcialmente visível como contexto; nas pontas, a rolagem para no limite.
    val rowState = rememberLazyListState()
    val selectedIndex = FILTER_ORDER.indexOf(state.selectedFilter)
    LaunchedEffect(selectedIndex) {
        rowState.animateScrollToItem((selectedIndex - 1).coerceAtLeast(0))
    }

    LazyRow(
        state = rowState,
        modifier = Modifier.selectableGroup(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(options, key = { it.first?.name ?: "ALL" }) { (status, text) ->
            val selected = state.selectedFilter == status
            FilterChip(
                selected = selected,
                onClick = { onSelectFilter(status) },
                // Papel de aba: o leitor de tela anuncia "selecionado" e a posição ("2 de 5").
                modifier = Modifier.semantics {
                    role = Role.Tab
                    this.selected = selected
                },
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
