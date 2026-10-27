package dev.celiobenj.seldonfleet.domain.usecase

import dev.celiobenj.seldonfleet.domain.model.Classification
import dev.celiobenj.seldonfleet.domain.model.Device
import dev.celiobenj.seldonfleet.domain.model.DeviceStatus
import dev.celiobenj.seldonfleet.domain.model.Signal

/**
 * Regra de classificação de um dispositivo (PROTOCOLO.md, seção 2).
 *
 * Avalia em ordem e para no primeiro estado que casar:
 * INDETERMINADO -> CRITICO -> ATENCAO -> OK.
 * Os limiares são recebidos pelo construtor; a classe não guarda estado.
 */
class ClassifyDevice(private val thresholds: Thresholds = Thresholds()) {

    operator fun invoke(device: Device): Classification {
        // Dados inválidos viram null, para nunca entrarem em uma divisão.
        val capacityMah = device.batteryCapacityMah?.takeIf { it > 0 }
        val screenOnMinutes = device.screenOnMinutes?.takeIf { it > 0 }
        val energyMah = device.energyConsumedMah?.takeIf { it.isFinite() && it >= 0.0 }

        // Derivadas: só calculadas quando os divisores são válidos (nunca NaN/Infinity).
        val absoluteDrain = if (energyMah != null && screenOnMinutes != null) {
            energyMah / (screenOnMinutes / 60.0)
        } else {
            null
        }
        val relativeDrain = if (absoluteDrain != null && capacityMah != null) {
            absoluteDrain / capacityMah * 100
        } else {
            null
        }

        // 1. INDETERMINADO: o dreno relativo não pode ser calculado.
        if (relativeDrain == null) {
            val reasons = buildList {
                if (capacityMah == null) add(Signal.InsufficientData(Signal.Reason.INVALID_CAPACITY))
                if (screenOnMinutes == null) add(Signal.InsufficientData(Signal.Reason.INVALID_SCREEN_TIME))
                if (energyMah == null) add(Signal.InsufficientData(Signal.Reason.ENERGY_UNAVAILABLE))
            }
            return Classification(DeviceStatus.INDETERMINADO, absoluteDrain, null, reasons)
        }

        // 2. CRITICO
        val criticalSignals = signalsAtOrAbove(
            device,
            relativeDrain,
            drainLimit = thresholds.drainCritical,
            crashLimit = thresholds.crashesCritical,
            jankLimit = thresholds.jankCritical,
        )
        if (criticalSignals.isNotEmpty()) {
            return Classification(DeviceStatus.CRITICO, absoluteDrain, relativeDrain, criticalSignals)
        }

        // 3. ATENCAO
        val attentionSignals = signalsAtOrAbove(
            device,
            relativeDrain,
            drainLimit = thresholds.drainAttention,
            crashLimit = thresholds.crashesAttention,
            jankLimit = thresholds.jankAttention,
        )
        if (attentionSignals.isNotEmpty()) {
            return Classification(DeviceStatus.ATENCAO, absoluteDrain, relativeDrain, attentionSignals)
        }

        // 4. OK
        return Classification(DeviceStatus.OK, absoluteDrain, relativeDrain, emptyList())
    }

    /** Sinais que atingem os limiares dados (inclusivos), na ordem dreno, falhas, quadros perdidos. */
    private fun signalsAtOrAbove(
        device: Device,
        relativeDrain: Double,
        drainLimit: Double,
        crashLimit: Int,
        jankLimit: Double,
    ): List<Signal> = buildList {
        if (relativeDrain >= drainLimit - RELATIVE_DRAIN_TOLERANCE) {
            add(Signal.RelativeDrain(relativeDrain, drainLimit))
        }
        if (device.crashCount7d >= crashLimit) {
            add(Signal.RecentCrashes(device.crashCount7d, crashLimit))
        }
        if (device.jankFramesPct >= jankLimit) {
            add(Signal.DroppedFrames(device.jankFramesPct, jankLimit))
        }
    }
}
