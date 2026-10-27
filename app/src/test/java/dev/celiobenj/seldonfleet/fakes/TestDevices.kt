package dev.celiobenj.seldonfleet.fakes

import dev.celiobenj.seldonfleet.domain.model.Device
import dev.celiobenj.seldonfleet.domain.model.DeviceTier

/**
 * Dispositivo sintético para os testes da interface. Base: 1000 mAh e 60 min de tela, logo
 * drenoRelativo = energia ÷ 10. Padrão: dreno 5 %/h, sem falhas, 1 % de quadros perdidos (OK).
 */
fun testDevice(
    id: String,
    capacityMah: Int? = 1000,
    energyMah: Double? = 50.0,
    crashes: Int = 0,
    jankPct: Double = 1.0,
) = Device(
    id = id,
    model = "Modelo $id",
    soc = "SoC",
    tier = DeviceTier.MID,
    ramMb = 4096,
    androidApi = 34,
    batteryCapacityMah = capacityMah,
    batteryLevelPct = 50,
    screenOnMinutes = 60,
    energyConsumedMah = energyMah,
    jankFramesPct = jankPct,
    crashCount7d = crashes,
    lastSeen = null,
)
