package dev.celiobenj.seldonfleet.ui

import dev.celiobenj.seldonfleet.domain.model.DeviceStatus
import dev.celiobenj.seldonfleet.ui.presentation.formatDecimal
import dev.celiobenj.seldonfleet.ui.presentation.formatLastSeen
import dev.celiobenj.seldonfleet.ui.presentation.formatThreshold
import dev.celiobenj.seldonfleet.ui.presentation.label
import dev.celiobenj.seldonfleet.ui.presentation.spokenDescription
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class FormattingTest {

    @Test
    fun `decimal usa virgula e 1 casa`() {
        assertEquals("13,4", formatDecimal(13.3999))
        assertEquals("0,0", formatDecimal(0.0))
        assertEquals("1585,7", formatDecimal(6136.5 / 3.87))
    }

    @Test
    fun `limiar inteiro aparece sem casas e fracionario com virgula`() {
        assertEquals("12", formatThreshold(12.0))
        assertEquals("7,5", formatThreshold(7.5))
    }

    @Test
    fun `ultimo contato em UTC ou travessao quando ausente`() {
        assertEquals("20/10/2026 02:58 UTC", formatLastSeen(Instant.parse("2026-10-20T02:58:44Z")))
        assertEquals("—", formatLastSeen(null))
    }

    @Test
    fun `rotulos de status sao os da interface e INDETERMINADO aparece como SEM DADOS`() {
        assertEquals("OK", DeviceStatus.OK.label())
        assertEquals("ATENÇÃO", DeviceStatus.ATENCAO.label())
        assertEquals("CRÍTICO", DeviceStatus.CRITICO.label())
        assertEquals("SEM DADOS", DeviceStatus.INDETERMINADO.label())
    }

    @Test
    fun `descricao falada do status`() {
        assertEquals("Status: Crítico", DeviceStatus.CRITICO.spokenDescription())
        assertEquals("Status: Sem dados", DeviceStatus.INDETERMINADO.spokenDescription())
    }
}
