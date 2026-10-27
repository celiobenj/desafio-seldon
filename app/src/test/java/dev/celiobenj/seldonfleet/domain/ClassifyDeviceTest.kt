package dev.celiobenj.seldonfleet.domain

import dev.celiobenj.seldonfleet.domain.model.Classification
import dev.celiobenj.seldonfleet.domain.model.Device
import dev.celiobenj.seldonfleet.domain.model.DeviceStatus
import dev.celiobenj.seldonfleet.domain.model.DeviceTier
import dev.celiobenj.seldonfleet.domain.model.Signal
import dev.celiobenj.seldonfleet.domain.usecase.ClassifyDevice
import dev.celiobenj.seldonfleet.domain.usecase.Thresholds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes da regra de classificação.
 *
 * Os valores esperados (limiares e status) são LITERAIS, de propósito: se alguém alterar um
 * limiar no código de produção, estes testes falham.
 *
 * Base dos registros sintéticos: capacidade 1000 mAh e 60 min de tela. Assim,
 * drenoRelativo = energia ÷ 1 h ÷ 1000 × 100 = energia ÷ 10, e a energia controla o dreno.
 */
class ClassifyDeviceTest {

    private val classify = ClassifyDevice()

    /** Registro saudável (dreno 5 %/h, sem falhas, 1 % de quadros perdidos); cada teste altera só o que interessa. */
    private fun device(
        capacityMah: Int? = 1000,
        screenOnMinutes: Int? = 60,
        energyMah: Double? = 50.0,
        crashes: Int = 0,
        jankPct: Double = 1.0,
    ) = Device(
        id = "DEV-TEST",
        model = "Modelo",
        soc = "SoC",
        tier = DeviceTier.MID,
        ramMb = 4096,
        androidApi = 34,
        batteryCapacityMah = capacityMah,
        batteryLevelPct = 50,
        screenOnMinutes = screenOnMinutes,
        energyConsumedMah = energyMah,
        jankFramesPct = jankPct,
        crashCount7d = crashes,
        lastSeen = null,
    )

    /** Registro cuja energia produz o dreno relativo desejado na base de 1000 mAh e 60 min. */
    private fun drain(pctPerHour: Double) = device(energyMah = pctPerHour * 10)

    private fun assertNoNaNOrInfinity(result: Classification) {
        listOfNotNull(result.absoluteDrainMahPerHour, result.relativeDrainPctPerHour).forEach {
            assertTrue("derivada não finita: $it", it.isFinite())
        }
    }

    // ---------- Fronteiras do dreno relativo (7 e 12 %/h) ----------

    @Test
    fun `dreno de 6,99 por cento por hora fica OK`() {
        assertEquals(DeviceStatus.OK, classify(drain(6.99)).status)
    }

    @Test
    fun `dreno de 7,0 por cento por hora vira ATENCAO`() {
        assertEquals(DeviceStatus.ATENCAO, classify(drain(7.0)).status)
    }

    @Test
    fun `dreno de 11,99 por cento por hora fica ATENCAO`() {
        assertEquals(DeviceStatus.ATENCAO, classify(drain(11.99)).status)
    }

    @Test
    fun `dreno de 12,0 por cento por hora vira CRITICO`() {
        assertEquals(DeviceStatus.CRITICO, classify(drain(12.0)).status)
    }

    @Test
    fun `dreno exatamente no limiar com ruido de ponto flutuante vira ATENCAO`() {
        // 1452,5 ÷ (249 ÷ 60) ÷ 5000 × 100 = 6,999999999999999 em Double; o valor exato é 7.
        val result = classify(device(capacityMah = 5000, screenOnMinutes = 249, energyMah = 1452.5))
        assertEquals(DeviceStatus.ATENCAO, result.status)
    }

    // ---------- Fronteiras de falhas (1 e 3) ----------

    @Test
    fun `0 falhas fica OK`() {
        assertEquals(DeviceStatus.OK, classify(device(crashes = 0)).status)
    }

    @Test
    fun `1 falha vira ATENCAO`() {
        assertEquals(DeviceStatus.ATENCAO, classify(device(crashes = 1)).status)
    }

    @Test
    fun `2 falhas ficam ATENCAO`() {
        assertEquals(DeviceStatus.ATENCAO, classify(device(crashes = 2)).status)
    }

    @Test
    fun `3 falhas viram CRITICO`() {
        assertEquals(DeviceStatus.CRITICO, classify(device(crashes = 3)).status)
    }

    // ---------- Fronteiras de quadros perdidos (8 e 15 %) ----------

    @Test
    fun `7,99 por cento de quadros perdidos fica OK`() {
        assertEquals(DeviceStatus.OK, classify(device(jankPct = 7.99)).status)
    }

    @Test
    fun `8,0 por cento de quadros perdidos vira ATENCAO`() {
        assertEquals(DeviceStatus.ATENCAO, classify(device(jankPct = 8.0)).status)
    }

    @Test
    fun `14,99 por cento de quadros perdidos fica ATENCAO`() {
        assertEquals(DeviceStatus.ATENCAO, classify(device(jankPct = 14.99)).status)
    }

    @Test
    fun `15,0 por cento de quadros perdidos vira CRITICO`() {
        assertEquals(DeviceStatus.CRITICO, classify(device(jankPct = 15.0)).status)
    }

    // ---------- Precedência ----------

    @Test
    fun `sinal critico vence sinal de atencao simultaneo`() {
        // dreno 7,5 %/h (atenção) + 3 falhas (crítico)
        val result = classify(device(energyMah = 75.0, crashes = 3))
        assertEquals(DeviceStatus.CRITICO, result.status)
        assertEquals(listOf(Signal.RecentCrashes(count = 3, threshold = 3)), result.signals)
    }

    @Test
    fun `capacidade 0 com falhas altas e quadros perdidos altos fica INDETERMINADO`() {
        val result = classify(device(capacityMah = 0, crashes = 5, jankPct = 20.0))
        assertEquals(DeviceStatus.INDETERMINADO, result.status)
    }

    @Test
    fun `todos os sinais criticos sao listados em ordem dreno, falhas, quadros perdidos`() {
        val result = classify(device(energyMah = 130.0, crashes = 4, jankPct = 16.0))
        assertEquals(DeviceStatus.CRITICO, result.status)
        assertEquals(3, result.signals.size)
        val drainSignal = result.signals[0] as Signal.RelativeDrain
        assertEquals(13.0, drainSignal.value, 1e-9)
        assertEquals(12.0, drainSignal.threshold, 0.0)
        assertEquals(Signal.RecentCrashes(count = 4, threshold = 3), result.signals[1])
        assertEquals(Signal.DroppedFrames(percent = 16.0, threshold = 15.0), result.signals[2])
    }

    @Test
    fun `status OK nao tem sinais`() {
        val result = classify(device())
        assertEquals(DeviceStatus.OK, result.status)
        assertTrue(result.signals.isEmpty())
    }

    // ---------- Registros incompletos (sempre INDETERMINADO, sem NaN/Infinity) ----------

    @Test
    fun `capacidade 0 fica INDETERMINADO com motivo de capacidade invalida`() {
        val result = classify(device(capacityMah = 0))
        assertEquals(DeviceStatus.INDETERMINADO, result.status)
        assertEquals(listOf(Signal.InsufficientData(Signal.Reason.INVALID_CAPACITY)), result.signals)
        assertNull(result.relativeDrainPctPerHour)
        assertNoNaNOrInfinity(result)
    }

    @Test
    fun `capacidade nula fica INDETERMINADO`() {
        val result = classify(device(capacityMah = null))
        assertEquals(DeviceStatus.INDETERMINADO, result.status)
        assertNull(result.relativeDrainPctPerHour)
        assertNoNaNOrInfinity(result)
    }

    @Test
    fun `capacidade negativa fica INDETERMINADO`() {
        val result = classify(device(capacityMah = -100))
        assertEquals(DeviceStatus.INDETERMINADO, result.status)
        assertNull(result.relativeDrainPctPerHour)
        assertNoNaNOrInfinity(result)
    }

    @Test
    fun `tela 0 fica INDETERMINADO com motivo de tempo de tela invalido`() {
        val result = classify(device(screenOnMinutes = 0))
        assertEquals(DeviceStatus.INDETERMINADO, result.status)
        assertEquals(listOf(Signal.InsufficientData(Signal.Reason.INVALID_SCREEN_TIME)), result.signals)
        assertNull(result.absoluteDrainMahPerHour)
        assertNull(result.relativeDrainPctPerHour)
        assertNoNaNOrInfinity(result)
    }

    @Test
    fun `tela nula fica INDETERMINADO`() {
        val result = classify(device(screenOnMinutes = null))
        assertEquals(DeviceStatus.INDETERMINADO, result.status)
        assertNull(result.absoluteDrainMahPerHour)
        assertNull(result.relativeDrainPctPerHour)
        assertNoNaNOrInfinity(result)
    }

    @Test
    fun `energia nula, como mWh sem tensao nominal, fica INDETERMINADO`() {
        val result = classify(device(energyMah = null))
        assertEquals(DeviceStatus.INDETERMINADO, result.status)
        assertEquals(listOf(Signal.InsufficientData(Signal.Reason.ENERGY_UNAVAILABLE)), result.signals)
        assertNull(result.absoluteDrainMahPerHour)
        assertNull(result.relativeDrainPctPerHour)
    }

    @Test
    fun `energia negativa fica INDETERMINADO`() {
        val result = classify(device(energyMah = -10.0))
        assertEquals(DeviceStatus.INDETERMINADO, result.status)
        assertEquals(listOf(Signal.InsufficientData(Signal.Reason.ENERGY_UNAVAILABLE)), result.signals)
        assertNull(result.absoluteDrainMahPerHour)
        assertNull(result.relativeDrainPctPerHour)
    }

    @Test
    fun `energia NaN ou infinita fica INDETERMINADO sem propagar NaN`() {
        listOf(Double.NaN, Double.POSITIVE_INFINITY).forEach { energy ->
            val result = classify(device(energyMah = energy))
            assertEquals(DeviceStatus.INDETERMINADO, result.status)
            assertNull(result.absoluteDrainMahPerHour)
            assertNull(result.relativeDrainPctPerHour)
        }
    }

    @Test
    fun `varios dados invalidos listam todos os motivos em ordem`() {
        val result = classify(device(capacityMah = 0, screenOnMinutes = 0, energyMah = null))
        assertEquals(
            listOf(
                Signal.InsufficientData(Signal.Reason.INVALID_CAPACITY),
                Signal.InsufficientData(Signal.Reason.INVALID_SCREEN_TIME),
                Signal.InsufficientData(Signal.Reason.ENERGY_UNAVAILABLE),
            ),
            result.signals,
        )
    }

    // ---------- Registros do arquivo (energia já normalizada para mAh) ----------

    @Test
    fun `DEV-0137 tem dreno de 6,2 por cento por hora e fica ATENCAO por 2 falhas`() {
        val result = classify(
            device(capacityMah = 4410, screenOnMinutes = 214, energyMah = 980.0, crashes = 2, jankPct = 7.4),
        )
        assertEquals(274.77, result.absoluteDrainMahPerHour!!, 0.01)
        assertEquals(6.23, result.relativeDrainPctPerHour!!, 0.01)
        assertEquals(DeviceStatus.ATENCAO, result.status)
        assertEquals(listOf(Signal.RecentCrashes(count = 2, threshold = 1)), result.signals)
    }

    @Test
    fun `DEV-0204 com 980 mAh convertidos de 3773 mWh tem dreno de 6,3 por cento por hora e fica OK`() {
        val result = classify(device(capacityMah = 5000, screenOnMinutes = 186, energyMah = 980.0, jankPct = 4.1))
        assertEquals(6.32, result.relativeDrainPctPerHour!!, 0.01)
        assertEquals(DeviceStatus.OK, result.status)
    }

    @Test
    fun `DEV-0311 com capacidade 0 fica INDETERMINADO mesmo com sinais de atencao`() {
        val result = classify(
            device(capacityMah = 0, screenOnMinutes = 97, energyMah = 612.0, crashes = 1, jankPct = 11.2),
        )
        assertEquals(DeviceStatus.INDETERMINADO, result.status)
        assertNull(result.relativeDrainPctPerHour)
    }

    @Test
    fun `DEV-0522 com 6136,5 mWh a 3,87 V tem dreno de 13,4 por cento por hora e fica CRITICO`() {
        val result = classify(
            device(capacityMah = 5000, screenOnMinutes = 142, energyMah = 6136.5 / 3.87, crashes = 1, jankPct = 9.8),
        )
        assertEquals(670.0, result.absoluteDrainMahPerHour!!, 0.1)
        assertEquals(13.4, result.relativeDrainPctPerHour!!, 0.01)
        assertEquals(DeviceStatus.CRITICO, result.status)
        val signal = result.signals.single() as Signal.RelativeDrain
        assertEquals(12.0, signal.threshold, 0.0)
    }

    // ---------- Injeção dos limiares ----------

    @Test
    fun `limiares recebidos pelo construtor substituem os de referencia`() {
        val strict = ClassifyDevice(Thresholds(drainAttention = 5.0))
        assertEquals(DeviceStatus.ATENCAO, strict(drain(6.0)).status)
    }
}
