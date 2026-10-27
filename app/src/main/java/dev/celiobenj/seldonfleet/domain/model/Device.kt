package dev.celiobenj.seldonfleet.domain.model

import java.time.Instant

/**
 * Dispositivo da frota como o domínio o enxerga.
 *
 * A energia já chega normalizada para mAh: a conversão de mWh acontece na camada de dados.
 * Campos anuláveis representam dados ausentes ou impossíveis de obter; a classificação
 * os trata como dados insuficientes.
 */
data class Device(
    val id: String,
    val model: String,
    val soc: String,
    val tier: DeviceTier,
    val ramMb: Int,
    val androidApi: Int,
    // null ou <= 0: capacidade desconhecida
    val batteryCapacityMah: Int?,
    val batteryLevelPct: Int,
    // null ou <= 0: tempo de tela desconhecido
    val screenOnMinutes: Int?,
    // já em mAh; null: energia indisponível (ex.: mWh sem tensão nominal válida)
    val energyConsumedMah: Double?,
    val jankFramesPct: Double,
    val crashCount7d: Int,
    // null: ausente ou em formato inválido no arquivo
    val lastSeen: Instant?,
)
