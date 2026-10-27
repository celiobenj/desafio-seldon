package dev.celiobenj.seldonfleet.domain.repository

import dev.celiobenj.seldonfleet.domain.model.Device

/**
 * Fonte dos dispositivos da frota. Declarada no domínio e implementada na camada de dados
 * (inversão de dependência): o domínio não sabe se os dados vêm de arquivo, de rede ou de teste.
 */
interface DeviceRepository {
    /** Dispositivos com energia já normalizada em mAh; falha se o arquivo for inválido. */
    suspend fun getDevices(): Result<List<Device>>
}
