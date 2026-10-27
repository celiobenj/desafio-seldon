package dev.celiobenj.seldonfleet.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.celiobenj.seldonfleet.domain.model.Classification
import dev.celiobenj.seldonfleet.domain.model.Device
import dev.celiobenj.seldonfleet.domain.repository.DeviceRepository
import dev.celiobenj.seldonfleet.domain.repository.FavoritesRepository
import dev.celiobenj.seldonfleet.domain.repository.FleetLoadException
import dev.celiobenj.seldonfleet.domain.usecase.ClassifyDevice
import dev.celiobenj.seldonfleet.ui.presentation.JustificationFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Estado da tela de detalhe de um dispositivo: lê a frota, encontra o id, classifica e monta
 * a justificativa, e acompanha se o dispositivo é favorito.
 */
class DeviceDetailViewModel(
    private val deviceId: String,
    private val deviceRepository: DeviceRepository,
    private val favoritesRepository: FavoritesRepository,
    private val classifyDevice: ClassifyDevice,
    private val justificationFormatter: JustificationFormatter,
) : ViewModel() {

    private sealed interface LoadResult {
        data object Loading : LoadResult
        data object NotFound : LoadResult
        data class Failed(val reason: FleetLoadException.Reason?) : LoadResult
        data class Found(val device: Device, val classification: Classification) : LoadResult
    }

    private val loadResult = MutableStateFlow<LoadResult>(LoadResult.Loading)

    val uiState: StateFlow<DeviceDetailUiState> =
        combine(loadResult, favoritesRepository.favoriteIds) { result, favoriteIds ->
            when (result) {
                LoadResult.Loading -> DeviceDetailUiState.Loading
                LoadResult.NotFound -> DeviceDetailUiState.NotFound(deviceId)
                is LoadResult.Failed -> DeviceDetailUiState.Error(result.reason)
                is LoadResult.Found -> DeviceDetailUiState.Content(
                    device = result.device,
                    classification = result.classification,
                    justification = justificationFormatter.format(result.device, result.classification),
                    isFavorite = deviceId in favoriteIds,
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DeviceDetailUiState.Loading)

    init {
        load()
    }

    /** Relê o arquivo (botão "Tentar de novo"). */
    fun retry() = load()

    fun setFavorite(favorite: Boolean) {
        viewModelScope.launch { favoritesRepository.setFavorite(deviceId, favorite) }
    }

    private fun load() {
        loadResult.value = LoadResult.Loading
        viewModelScope.launch {
            loadResult.value = deviceRepository.getDevices().fold(
                onSuccess = { devices ->
                    val device = devices.firstOrNull { it.id == deviceId }
                    if (device == null) LoadResult.NotFound else LoadResult.Found(device, classifyDevice(device))
                },
                onFailure = { error -> LoadResult.Failed((error as? FleetLoadException)?.reason) },
            )
        }
    }

    companion object {
        /** Fábrica que entrega o id e as dependências pelo construtor (injeção manual). */
        fun factory(
            deviceId: String,
            deviceRepository: DeviceRepository,
            favoritesRepository: FavoritesRepository,
            classifyDevice: ClassifyDevice,
            justificationFormatter: JustificationFormatter,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                DeviceDetailViewModel(
                    deviceId,
                    deviceRepository,
                    favoritesRepository,
                    classifyDevice,
                    justificationFormatter,
                )
            }
        }
    }
}
