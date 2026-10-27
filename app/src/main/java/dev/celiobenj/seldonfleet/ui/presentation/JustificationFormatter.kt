package dev.celiobenj.seldonfleet.ui.presentation

import dev.celiobenj.seldonfleet.domain.model.Classification
import dev.celiobenj.seldonfleet.domain.model.Device
import dev.celiobenj.seldonfleet.domain.model.DeviceStatus
import dev.celiobenj.seldonfleet.domain.model.Signal
import dev.celiobenj.seldonfleet.domain.usecase.RELATIVE_DRAIN_TOLERANCE
import dev.celiobenj.seldonfleet.domain.usecase.Thresholds
import kotlin.math.abs

/**
 * Monta a frase de justificativa do detalhe a partir da classificação já calculada.
 * Não classifica nada: só transforma os sinais (dados) em texto, nomeando o sinal e o valor.
 * Kotlin puro, testável na JVM.
 */
class JustificationFormatter(private val thresholds: Thresholds = Thresholds()) {

    fun format(device: Device, classification: Classification): String = when (classification.status) {
        DeviceStatus.CRITICO -> "Crítico: " + describeSignals(classification.signals) + "."
        DeviceStatus.ATENCAO -> "Atenção: " + describeSignals(classification.signals) + "."
        DeviceStatus.OK -> okText(classification)
        DeviceStatus.INDETERMINADO -> "Sem dados suficientes: " + describeSignals(classification.signals, device) + "."
    }

    private fun okText(classification: Classification): String {
        val drain = classification.relativeDrainPctPerHour
            ?: return "Nenhum sinal acima dos limiares."
        return "Nenhum sinal acima dos limiares: dreno relativo de ${formatDecimal(drain)} %/h " +
            "(limiar de atenção ${formatThreshold(thresholds.drainAttention)} %/h)."
    }

    private fun describeSignals(signals: List<Signal>, device: Device? = null): String =
        signals.joinToString(separator = "; ") { describe(it, device) }

    private fun describe(signal: Signal, device: Device?): String = when (signal) {
        is Signal.RelativeDrain -> {
            val position = if (abs(signal.value - signal.threshold) <= RELATIVE_DRAIN_TOLERANCE) AT else ABOVE
            "dreno relativo de ${formatDecimal(signal.value)} %/h, $position ${formatThreshold(signal.threshold)} %/h"
        }
        is Signal.RecentCrashes -> {
            val position = if (signal.count == signal.threshold) AT else ABOVE
            val noun = if (signal.count == 1) "falha" else "falhas"
            "${signal.count} $noun em 7 dias, $position ${signal.threshold}"
        }
        is Signal.DroppedFrames -> {
            val position = if (signal.percent == signal.threshold) AT else ABOVE
            "${formatDecimal(signal.percent)} % de quadros perdidos, $position ${formatThreshold(signal.threshold)} %"
        }
        is Signal.InsufficientData -> describeMissing(signal.reason, device)
    }

    private fun describeMissing(reason: Signal.Reason, device: Device?): String = when (reason) {
        Signal.Reason.INVALID_CAPACITY -> device?.batteryCapacityMah
            ?.let { "capacidade da bateria inválida ($it mAh)" }
            ?: "capacidade da bateria ausente"
        Signal.Reason.INVALID_SCREEN_TIME -> device?.screenOnMinutes
            ?.let { "tempo de tela ligada inválido ($it min)" }
            ?: "tempo de tela ligada ausente"
        Signal.Reason.ENERGY_UNAVAILABLE ->
            "consumo de energia indisponível (ausente, negativo ou sem conversão para mAh)"
    }

    private companion object {
        const val AT = "no limiar de"
        const val ABOVE = "acima do limiar de"
    }
}
