package dev.celiobenj.seldonfleet.data.mapper

import dev.celiobenj.seldonfleet.data.dto.DeviceDto
import dev.celiobenj.seldonfleet.data.dto.TierDto
import dev.celiobenj.seldonfleet.domain.model.Device
import dev.celiobenj.seldonfleet.domain.model.DeviceTier
import java.time.Instant
import java.time.format.DateTimeParseException

/**
 * Converte o registro lido do arquivo no modelo de domínio.
 *
 * É aqui que a energia é normalizada para mAh (PROTOCOLO.md, seção 2.2), para o domínio
 * receber só mAh. Valores que não podem ser normalizados viram null; quem decide o que null
 * significa é a regra de classificação.
 */
class DeviceMapper {

    fun toDomain(dto: DeviceDto): Device = Device(
        id = dto.id,
        model = dto.model,
        soc = dto.soc,
        tier = dto.tier.toDomain(),
        ramMb = dto.ramMb,
        androidApi = dto.androidApi,
        batteryCapacityMah = dto.batteryCapacityMah,
        batteryLevelPct = dto.batteryLevelPct,
        screenOnMinutes = dto.screenOnMinutes,
        energyConsumedMah = normalizedEnergyMah(dto),
        jankFramesPct = dto.jankFramesPct,
        crashCount7d = dto.crashCount7d,
        lastSeen = parseInstantOrNull(dto.lastSeen),
    )

    /** Energia em mAh, ou null se ausente, negativa, em mWh sem tensão válida ou em unidade desconhecida. */
    private fun normalizedEnergyMah(dto: DeviceDto): Double? {
        val energy = dto.energyConsumed ?: return null
        if (energy < 0.0) return null
        return when (dto.energyUnit) {
            UNIT_MAH -> energy
            UNIT_MWH -> {
                val voltage = dto.nominalVoltage
                if (voltage != null && voltage > 0.0) energy / voltage else null
            }
            // Unidade desconhecida: não adivinha o fator de conversão.
            else -> null
        }
    }

    private fun parseInstantOrNull(text: String?): Instant? {
        if (text == null) return null
        return try {
            Instant.parse(text)
        } catch (e: DateTimeParseException) {
            null
        }
    }

    private fun TierDto.toDomain(): DeviceTier = when (this) {
        TierDto.LOW -> DeviceTier.LOW
        TierDto.MID -> DeviceTier.MID
        TierDto.HIGH -> DeviceTier.HIGH
    }

    private companion object {
        const val UNIT_MAH = "mAh"
        const val UNIT_MWH = "mWh"
    }
}
