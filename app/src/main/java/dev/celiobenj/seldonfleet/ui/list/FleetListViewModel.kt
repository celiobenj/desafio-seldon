package dev.celiobenj.seldonfleet.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.celiobenj.seldonfleet.domain.model.Device
import dev.celiobenj.seldonfleet.domain.model.DeviceStatus
import dev.celiobenj.seldonfleet.domain.repository.DeviceRepository
import dev.celiobenj.seldonfleet.domain.repository.FavoritesRepository
import dev.celiobenj.seldonfleet.domain.repository.FleetLoadException
import dev.celiobenj.seldonfleet.domain.usecase.ClassifyDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Estado da tela da lista: carrega e classifica a frota, guarda o filtro selecionado e
 * combina tudo com os favoritos. O filtro vive aqui; como este ViewModel continua na pilha de
 * navegação enquanto o detalhe está aberto, o filtro é preservado ao voltar.
 */
class FleetListViewModel(
    private val deviceRepository: DeviceRepository,
    private val favoritesRepository: FavoritesRepository,
    private val classifyDevice: ClassifyDevice,
) : ViewModel() {

    /** Resultado da leitura do arquivo, já com o status de cada dispositivo. */
    private sealed interface LoadResult {
        data object Loading : LoadResult
        data class Failed(val reason: FleetLoadException.Reason?) : LoadResult
        data class Loaded(val devices: List<Pair<Device, DeviceStatus>>) : LoadResult
    }

    private val loadResult = MutableStateFlow<LoadResult>(LoadResult.Loading)
    private val selectedFilter = MutableStateFlow<DeviceStatus?>(null)

    val uiState: StateFlow<FleetListUiState> =
        combine(loadResult, selectedFilter, favoritesRepository.favoriteIds) { result, filter, favoriteIds ->
            when (result) {
                LoadResult.Loading -> FleetListUiState.Loading
                is LoadResult.Failed -> FleetListUiState.Error(result.reason)
                is LoadResult.Loaded -> buildContent(result.devices, filter, favoriteIds)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = FleetListUiState.Loading,
        )

    init {
        load()
    }

    fun selectFilter(status: DeviceStatus?) {
        selectedFilter.value = status
    }

    /** Avança um filtro na ordem de [FILTER_ORDER]; no último, não faz nada. */
    fun selectNextFilter() = moveFilter(+1)

    /** Volta um filtro na ordem de [FILTER_ORDER]; em TODOS, não faz nada. */
    fun selectPreviousFilter() = moveFilter(-1)

    private fun moveFilter(step: Int) {
        val target = FILTER_ORDER.indexOf(selectedFilter.value) + step
        if (target in FILTER_ORDER.indices) {
            selectedFilter.value = FILTER_ORDER[target]
        }
    }

    /** Relê o arquivo (botão "Tentar de novo"). */
    fun retry() = load()

    fun setFavorite(id: String, favorite: Boolean) {
        viewModelScope.launch { favoritesRepository.setFavorite(id, favorite) }
    }

    private fun load() {
        loadResult.value = LoadResult.Loading
        viewModelScope.launch {
            loadResult.value = deviceRepository.getDevices().fold(
                onSuccess = { devices -> LoadResult.Loaded(devices.map { it to classifyDevice(it).status }) },
                onFailure = { error -> LoadResult.Failed((error as? FleetLoadException)?.reason) },
            )
        }
    }

    private fun buildContent(
        devices: List<Pair<Device, DeviceStatus>>,
        filter: DeviceStatus?,
        favoriteIds: Set<String>,
    ): FleetListUiState.Content {
        val counts = DeviceStatus.entries.associateWith { status -> devices.count { it.second == status } }
        val visible = devices
            .filter { (_, status) -> filter == null || status == filter }
            .map { (device, status) ->
                DeviceListItem(
                    id = device.id,
                    model = device.model,
                    soc = device.soc,
                    batteryLevelPct = device.batteryLevelPct,
                    status = status,
                    isFavorite = device.id in favoriteIds,
                )
            }
        return FleetListUiState.Content(
            items = visible,
            counts = counts,
            totalCount = devices.size,
            selectedFilter = filter,
        )
    }

    companion object {
        // mantém o fluxo ativo por 5 s sem observadores (ex.: rotação de tela)
        private const val STOP_TIMEOUT_MS = 5_000L

        /** Fábrica que entrega as dependências pelo construtor (injeção manual). */
        fun factory(
            deviceRepository: DeviceRepository,
            favoritesRepository: FavoritesRepository,
            classifyDevice: ClassifyDevice,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { FleetListViewModel(deviceRepository, favoritesRepository, classifyDevice) }
        }
    }
}
