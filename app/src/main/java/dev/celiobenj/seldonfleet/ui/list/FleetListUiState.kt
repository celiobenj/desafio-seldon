package dev.celiobenj.seldonfleet.ui.list

import dev.celiobenj.seldonfleet.domain.model.DeviceStatus
import dev.celiobenj.seldonfleet.domain.repository.FleetLoadException

/** Tudo o que a tela da lista precisa para se desenhar. A tela não calcula nada. */
sealed interface FleetListUiState {

    data object Loading : FleetListUiState

    /** Falha ao carregar o arquivo; [reason] null quando a causa não é conhecida. */
    data class Error(val reason: FleetLoadException.Reason?) : FleetListUiState

    data class Content(
        // itens já filtrados, na ordem do arquivo
        val items: List<DeviceListItem>,
        // contagem por status sobre todos os registros (não muda com o filtro)
        val counts: Map<DeviceStatus, Int>,
        val totalCount: Int,
        // null: "Todos"
        val selectedFilter: DeviceStatus?,
    ) : FleetListUiState
}

/** Ordem dos filtros, usada pelos chips e pelo gesto de arrastar: TODOS (null) e depois cada status. */
val FILTER_ORDER: List<DeviceStatus?> = listOf(null) + DeviceStatus.entries

/** Uma linha da lista. */
data class DeviceListItem(
    val id: String,
    val model: String,
    val soc: String,
    val batteryLevelPct: Int,
    val status: DeviceStatus,
    val isFavorite: Boolean,
)
