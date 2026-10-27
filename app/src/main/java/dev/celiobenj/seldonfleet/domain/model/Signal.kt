package dev.celiobenj.seldonfleet.domain.model

/**
 * Sinal que justifica um status. Carrega dados (valor observado e limiar), nunca texto:
 * a frase exibida ao usuário é montada na camada ui.
 */
sealed interface Signal {
    /** Dreno relativo (%/h) que atingiu o limiar. */
    data class RelativeDrain(val value: Double, val threshold: Double) : Signal

    /** Falhas nos últimos 7 dias que atingiram o limiar. */
    data class RecentCrashes(val count: Int, val threshold: Int) : Signal

    /** Percentual de quadros perdidos que atingiu o limiar. */
    data class DroppedFrames(val percent: Double, val threshold: Double) : Signal

    /** Dado ausente ou inválido que impede a classificação. */
    data class InsufficientData(val reason: Reason) : Signal

    enum class Reason { INVALID_CAPACITY, INVALID_SCREEN_TIME, ENERGY_UNAVAILABLE }
}
