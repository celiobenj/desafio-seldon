package dev.celiobenj.seldonfleet.ui.detail

import dev.celiobenj.seldonfleet.domain.model.Classification
import dev.celiobenj.seldonfleet.domain.model.Device
import dev.celiobenj.seldonfleet.domain.repository.FleetLoadException

/** Estado da tela de detalhe. A classificação e o texto já chegam prontos para a tela. */
sealed interface DeviceDetailUiState {

    data object Loading : DeviceDetailUiState

    data class NotFound(val deviceId: String) : DeviceDetailUiState

    data class Error(val reason: FleetLoadException.Reason?) : DeviceDetailUiState

    data class Content(
        val device: Device,
        val classification: Classification,
        val justification: String,
        val isFavorite: Boolean,
    ) : DeviceDetailUiState
}
