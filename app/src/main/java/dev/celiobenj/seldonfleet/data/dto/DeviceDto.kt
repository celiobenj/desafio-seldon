package dev.celiobenj.seldonfleet.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Registro de dispositivo exatamente como está no frota.json, inclusive com os defeitos.
 *
 * Campos sem valor padrão são obrigatórios: se faltarem, a leitura do arquivo inteiro falha
 * (arquivo fora do esquema). Campos anuláveis com padrão null podem faltar ou vir nulos
 * (defeitos de registro, tratados na classificação). Ver PROTOCOLO.md, seção 3.3.
 */
@Serializable
data class DeviceDto(
    val id: String,
    val model: String,
    val soc: String,
    val tier: TierDto,
    val ramMb: Int,
    val androidApi: Int,
    val batteryCapacityMah: Int? = null,
    val batteryLevelPct: Int,
    val screenOnMinutes: Int? = null,
    // unidade definida por energyUnit ("mAh" ou "mWh"); a normalização é feita no DeviceMapper
    val energyConsumed: Double? = null,
    val energyUnit: String,
    val nominalVoltage: Double? = null,
    val jankFramesPct: Double,
    val crashCount7d: Int,
    // texto ISO-8601; convertido (ou descartado, se inválido) no DeviceMapper
    val lastSeen: String? = null,
)

/** Valores aceitos para `tier`. Qualquer outro valor torna o arquivo inválido. */
@Serializable
enum class TierDto {
    @SerialName("low") LOW,
    @SerialName("mid") MID,
    @SerialName("high") HIGH,
}
