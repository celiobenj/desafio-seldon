package dev.celiobenj.seldonfleet.data

import dev.celiobenj.seldonfleet.data.dto.DeviceDto
import dev.celiobenj.seldonfleet.data.dto.TierDto
import dev.celiobenj.seldonfleet.data.mapper.DeviceMapper
import dev.celiobenj.seldonfleet.domain.model.DeviceStatus
import dev.celiobenj.seldonfleet.domain.model.DeviceTier
import dev.celiobenj.seldonfleet.domain.usecase.ClassifyDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class DeviceMapperTest {

    private val mapper = DeviceMapper()
    private val classify = ClassifyDevice()

    private fun dto(
        energy: Double? = 980.0,
        unit: String = "mAh",
        voltage: Double? = 3.85,
        capacity: Int? = 5000,
        screenOnMinutes: Int? = 186,
        tier: TierDto = TierDto.MID,
        lastSeen: String? = "2026-10-20T02:50:44Z",
    ) = DeviceDto(
        id = "DEV-TEST",
        model = "Galaxy A54",
        soc = "Exynos 1380",
        tier = tier,
        ramMb = 8192,
        androidApi = 34,
        batteryCapacityMah = capacity,
        batteryLevelPct = 61,
        screenOnMinutes = screenOnMinutes,
        energyConsumed = energy,
        energyUnit = unit,
        nominalVoltage = voltage,
        jankFramesPct = 4.1,
        crashCount7d = 0,
        lastSeen = lastSeen,
    )

    // ---------- Normalização de unidade (REQ-02) ----------

    @Test
    fun `mWh e mAh com o mesmo consumo fisico geram a mesma energia, o mesmo dreno e o mesmo status`() {
        // 3773 mWh ÷ 3,85 V = 980 mAh
        val fromMwh = mapper.toDomain(dto(energy = 3773.0, unit = "mWh", voltage = 3.85))
        val fromMah = mapper.toDomain(dto(energy = 980.0, unit = "mAh"))

        assertEquals(980.0, fromMwh.energyConsumedMah!!, 1e-9)
        assertEquals(fromMah.energyConsumedMah!!, fromMwh.energyConsumedMah!!, 1e-9)

        val classifiedMwh = classify(fromMwh)
        val classifiedMah = classify(fromMah)
        assertEquals(6.32, classifiedMwh.relativeDrainPctPerHour!!, 0.01)
        assertEquals(classifiedMah.relativeDrainPctPerHour!!, classifiedMwh.relativeDrainPctPerHour!!, 1e-9)
        assertEquals(DeviceStatus.OK, classifiedMwh.status)
        assertEquals(classifiedMah.status, classifiedMwh.status)
    }

    @Test
    fun `sem a conversao o mesmo registro seria CRITICO`() {
        // Controle: tratar 3773 mWh como se fossem mAh dá 24,3 %/h.
        val wrong = classify(mapper.toDomain(dto(energy = 3773.0, unit = "mAh")))
        assertEquals(24.3, wrong.relativeDrainPctPerHour!!, 0.1)
        assertEquals(DeviceStatus.CRITICO, wrong.status)
    }

    @Test
    fun `energia em mAh passa sem alteracao`() {
        assertEquals(612.0, mapper.toDomain(dto(energy = 612.0, unit = "mAh")).energyConsumedMah!!, 0.0)
    }

    @Test
    fun `mWh sem tensao nominal valida fica sem energia e INDETERMINADO`() {
        listOf(null, 0.0, -3.85).forEach { voltage ->
            val device = mapper.toDomain(dto(energy = 3773.0, unit = "mWh", voltage = voltage))
            assertNull("tensão: $voltage", device.energyConsumedMah)
            assertEquals(DeviceStatus.INDETERMINADO, classify(device).status)
        }
    }

    @Test
    fun `unidade desconhecida fica sem energia`() {
        listOf("Wh", "mah", "").forEach { unit ->
            assertNull("unidade: '$unit'", mapper.toDomain(dto(unit = unit)).energyConsumedMah)
        }
    }

    @Test
    fun `energia negativa fica sem energia`() {
        assertNull(mapper.toDomain(dto(energy = -10.0, unit = "mAh")).energyConsumedMah)
        assertNull(mapper.toDomain(dto(energy = -10.0, unit = "mWh")).energyConsumedMah)
    }

    @Test
    fun `energia ausente fica sem energia`() {
        assertNull(mapper.toDomain(dto(energy = null)).energyConsumedMah)
    }

    // ---------- Demais campos ----------

    @Test
    fun `capacidade e tela ausentes continuam ausentes`() {
        val device = mapper.toDomain(dto(capacity = null, screenOnMinutes = null))
        assertNull(device.batteryCapacityMah)
        assertNull(device.screenOnMinutes)
    }

    @Test
    fun `tier e convertido para o enum do dominio`() {
        assertEquals(DeviceTier.LOW, mapper.toDomain(dto(tier = TierDto.LOW)).tier)
        assertEquals(DeviceTier.MID, mapper.toDomain(dto(tier = TierDto.MID)).tier)
        assertEquals(DeviceTier.HIGH, mapper.toDomain(dto(tier = TierDto.HIGH)).tier)
    }

    @Test
    fun `lastSeen valido vira Instant`() {
        assertEquals(Instant.parse("2026-10-20T02:50:44Z"), mapper.toDomain(dto()).lastSeen)
    }

    @Test
    fun `lastSeen invalido ou ausente vira null`() {
        assertNull(mapper.toDomain(dto(lastSeen = "20/10/2026 02:50")).lastSeen)
        assertNull(mapper.toDomain(dto(lastSeen = "")).lastSeen)
        assertNull(mapper.toDomain(dto(lastSeen = null)).lastSeen)
    }
}
