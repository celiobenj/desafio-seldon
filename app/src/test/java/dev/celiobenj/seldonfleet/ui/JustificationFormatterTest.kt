package dev.celiobenj.seldonfleet.ui

import dev.celiobenj.seldonfleet.domain.model.Device
import dev.celiobenj.seldonfleet.domain.model.DeviceTier
import dev.celiobenj.seldonfleet.domain.usecase.ClassifyDevice
import dev.celiobenj.seldonfleet.ui.presentation.JustificationFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Texto da justificativa exibido no detalhe. Cada frase nomeia o sinal e cita o valor observado.
 * Base sintética: capacidade 1000 mAh e 60 min de tela, logo drenoRelativo = energia ÷ 10.
 */
class JustificationFormatterTest {

    private val classify = ClassifyDevice()
    private val formatter = JustificationFormatter()

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
        tier = DeviceTier.LOW,
        ramMb = 4096,
        androidApi = 33,
        batteryCapacityMah = capacityMah,
        batteryLevelPct = 24,
        screenOnMinutes = screenOnMinutes,
        energyConsumedMah = energyMah,
        jankFramesPct = jankPct,
        crashCount7d = crashes,
        lastSeen = null,
    )

    private fun justify(device: Device) = formatter.format(device, classify(device))

    // ---------- CRÍTICO ----------

    @Test
    fun `DEV-0522 cita o dreno relativo e o limiar critico`() {
        val dev0522 = device(
            capacityMah = 5000,
            screenOnMinutes = 142,
            energyMah = 6136.5 / 3.87,
            crashes = 1,
            jankPct = 9.8,
        )
        assertEquals("Crítico: dreno relativo de 13,4 %/h, acima do limiar de 12 %/h.", justify(dev0522))
    }

    @Test
    fun `critico por falhas no limiar`() {
        assertEquals("Crítico: 3 falhas em 7 dias, no limiar de 3.", justify(device(crashes = 3)))
    }

    @Test
    fun `critico por quadros perdidos no limiar`() {
        assertEquals("Crítico: 15,0 % de quadros perdidos, no limiar de 15 %.", justify(device(jankPct = 15.0)))
    }

    @Test
    fun `varios sinais criticos aparecem todos, na ordem dreno, falhas, quadros perdidos`() {
        assertEquals(
            "Crítico: dreno relativo de 13,0 %/h, acima do limiar de 12 %/h; " +
                "4 falhas em 7 dias, acima do limiar de 3; " +
                "16,0 % de quadros perdidos, acima do limiar de 15 %.",
            justify(device(energyMah = 130.0, crashes = 4, jankPct = 16.0)),
        )
    }

    // ---------- ATENÇÃO ----------

    @Test
    fun `DEV-0137 cita as 2 falhas`() {
        val dev0137 = device(capacityMah = 4410, screenOnMinutes = 214, energyMah = 980.0, crashes = 2, jankPct = 7.4)
        assertEquals("Atenção: 2 falhas em 7 dias, acima do limiar de 1.", justify(dev0137))
    }

    @Test
    fun `atencao com 1 falha usa o singular`() {
        assertEquals("Atenção: 1 falha em 7 dias, no limiar de 1.", justify(device(crashes = 1)))
    }

    @Test
    fun `atencao por dreno exatamente no limiar`() {
        assertEquals("Atenção: dreno relativo de 7,0 %/h, no limiar de 7 %/h.", justify(device(energyMah = 70.0)))
    }

    @Test
    fun `dreno no limiar com ruido de ponto flutuante continua no limiar`() {
        // 6,999999999999999 em Double; o valor exato é 7.
        val noisy = device(capacityMah = 5000, screenOnMinutes = 249, energyMah = 1452.5)
        assertEquals("Atenção: dreno relativo de 7,0 %/h, no limiar de 7 %/h.", justify(noisy))
    }

    @Test
    fun `atencao por quadros perdidos acima do limiar`() {
        assertEquals("Atenção: 9,8 % de quadros perdidos, acima do limiar de 8 %.", justify(device(jankPct = 9.8)))
    }

    // ---------- OK ----------

    @Test
    fun `DEV-0204 OK cita o dreno e o limiar de atencao`() {
        val dev0204 = device(capacityMah = 5000, screenOnMinutes = 186, energyMah = 980.0, jankPct = 4.1)
        assertEquals(
            "Nenhum sinal acima dos limiares: dreno relativo de 6,3 %/h (limiar de atenção 7 %/h).",
            justify(dev0204),
        )
    }

    // ---------- SEM DADOS (INDETERMINADO) ----------

    @Test
    fun `capacidade 0 cita o valor invalido`() {
        assertEquals("Sem dados suficientes: capacidade da bateria inválida (0 mAh).", justify(device(capacityMah = 0)))
    }

    @Test
    fun `capacidade ausente`() {
        assertEquals("Sem dados suficientes: capacidade da bateria ausente.", justify(device(capacityMah = null)))
    }

    @Test
    fun `tela 0 cita o valor invalido`() {
        assertEquals(
            "Sem dados suficientes: tempo de tela ligada inválido (0 min).",
            justify(device(screenOnMinutes = 0)),
        )
    }

    @Test
    fun `energia indisponivel`() {
        assertEquals(
            "Sem dados suficientes: consumo de energia indisponível (ausente, negativo ou sem conversão para mAh).",
            justify(device(energyMah = null)),
        )
    }

    @Test
    fun `varios motivos aparecem todos`() {
        assertEquals(
            "Sem dados suficientes: capacidade da bateria inválida (0 mAh); tempo de tela ligada inválido (0 min).",
            justify(device(capacityMah = 0, screenOnMinutes = 0)),
        )
    }

    @Test
    fun `DEV-0311 mostra so o motivo da falta de dados, mesmo com quadros perdidos de atencao`() {
        val dev0311 = device(capacityMah = 0, screenOnMinutes = 97, energyMah = 612.0, crashes = 1, jankPct = 11.2)
        assertEquals("Sem dados suficientes: capacidade da bateria inválida (0 mAh).", justify(dev0311))
    }
}
