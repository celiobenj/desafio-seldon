package dev.celiobenj.seldonfleet.data.dto

import kotlinx.serialization.Serializable

/**
 * Raiz do frota.json, espelhando o arquivo.
 * Só `devices` é obrigatório; os metadados de exportação não são usados pelo app.
 */
@Serializable
data class FleetDto(
    val schemaVersion: String? = null,
    val exportedAt: String? = null,
    val fleetId: String? = null,
    val deviceCount: Int? = null,
    val devices: List<DeviceDto>,
)
