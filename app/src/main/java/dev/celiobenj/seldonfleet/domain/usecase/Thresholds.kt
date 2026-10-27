package dev.celiobenj.seldonfleet.domain.usecase

/**
 * Limiares da regra de classificação (valores de referência do enunciado).
 * Todos são inclusivos: um valor igual ao limiar promove o status.
 */
data class Thresholds(
    // dreno relativo, em %/h
    val drainAttention: Double = 7.0,
    val drainCritical: Double = 12.0,
    // falhas nos últimos 7 dias
    val crashesAttention: Int = 1,
    val crashesCritical: Int = 3,
    // quadros perdidos, em %
    val jankAttention: Double = 8.0,
    val jankCritical: Double = 15.0,
)

/**
 * Tolerância (%/h) nas comparações do dreno relativo com os limiares.
 * Absorve o ruído de ponto flutuante (ex.: 6,999999999999999 onde o valor exato é 7).
 * Ver PROTOCOLO.md, seção 2.5.
 */
const val RELATIVE_DRAIN_TOLERANCE = 1e-9
