package dev.celiobenj.seldonfleet.domain.model

/**
 * Resultado da classificação de um dispositivo.
 *
 * As derivadas são null quando não podem ser calculadas; nunca NaN ou Infinity.
 * [signals] lista, em ordem (dreno, falhas, quadros perdidos), os sinais que definem o status;
 * para INDETERMINADO, lista os motivos da falta de dados. Vazia para OK.
 */
data class Classification(
    val status: DeviceStatus,
    val absoluteDrainMahPerHour: Double?,
    val relativeDrainPctPerHour: Double?,
    val signals: List<Signal>,
)
