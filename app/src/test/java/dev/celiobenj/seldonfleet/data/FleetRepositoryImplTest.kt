package dev.celiobenj.seldonfleet.data

import dev.celiobenj.seldonfleet.data.mapper.DeviceMapper
import dev.celiobenj.seldonfleet.data.parser.FleetJsonParser
import dev.celiobenj.seldonfleet.data.repository.FleetRepositoryImpl
import dev.celiobenj.seldonfleet.data.source.FleetSource
import dev.celiobenj.seldonfleet.domain.model.DeviceStatus
import dev.celiobenj.seldonfleet.domain.model.DeviceStatus.ATENCAO
import dev.celiobenj.seldonfleet.domain.model.DeviceStatus.CRITICO
import dev.celiobenj.seldonfleet.domain.model.DeviceStatus.INDETERMINADO
import dev.celiobenj.seldonfleet.domain.model.DeviceStatus.OK
import dev.celiobenj.seldonfleet.domain.repository.FleetLoadException
import dev.celiobenj.seldonfleet.domain.usecase.ClassifyDevice
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.FileNotFoundException

/** Origem em memória: devolve um texto fixo ou lança a exceção que o teste quiser. */
private class FakeFleetSource(private val read: () -> String) : FleetSource {
    override suspend fun readText(): String = read()
}

class FleetRepositoryImplTest {

    private fun repository(read: () -> String) =
        FleetRepositoryImpl(FakeFleetSource(read), FleetJsonParser(), DeviceMapper())

    private fun assertFailure(result: Result<*>, expected: FleetLoadException.Reason) {
        val error = result.exceptionOrNull()
        assertTrue("esperava falha, veio $result", error is FleetLoadException)
        assertEquals(expected, (error as FleetLoadException).reason)
    }

    // ---------- Falhas do arquivo ----------

    @Test
    fun `arquivo ausente devolve falha FILE_UNAVAILABLE sem lancar excecao`() = runTest {
        val result = repository { throw FileNotFoundException("frota.json") }.getDevices()
        assertFailure(result, FleetLoadException.Reason.FILE_UNAVAILABLE)
    }

    @Test
    fun `JSON invalido devolve falha INVALID_CONTENT`() = runTest {
        assertFailure(repository { "{ quebrado" }.getDevices(), FleetLoadException.Reason.INVALID_CONTENT)
    }

    @Test
    fun `campo obrigatorio ausente devolve falha INVALID_CONTENT`() = runTest {
        val text = fleetJson(validDeviceJson, validDeviceJson.without("crashCount7d"))
        assertFailure(repository { text }.getDevices(), FleetLoadException.Reason.INVALID_CONTENT)
    }

    @Test
    fun `tier desconhecido devolve falha INVALID_CONTENT`() = runTest {
        val text = fleetJson(validDeviceJson.with("tier", JsonPrimitive("ultra")))
        assertFailure(repository { text }.getDevices(), FleetLoadException.Reason.INVALID_CONTENT)
    }

    // ---------- Arquivo válido ----------

    @Test
    fun `registro em mWh chega ao dominio ja em mAh`() = runTest {
        val mwh = validDeviceJson
            .with("energyConsumed", JsonPrimitive(3773.0))
            .with("energyUnit", JsonPrimitive("mWh"))
            .with("nominalVoltage", JsonPrimitive(3.85))
        val device = repository { fleetJson(mwh) }.getDevices().getOrThrow().single()
        assertEquals(980.0, device.energyConsumedMah!!, 1e-9)
    }

    // ---------- Classificação esperada do arquivo real (cópia congelada) ----------

    /**
     * Classificação esperada dos 30 registros, calculada a partir da regra do enunciado.
     * Lida da cópia congelada em src/test/resources, e não dos assets do app: se o arquivo
     * dos assets for trocado, este teste continua válido.
     */
    private val expectedStatus: Map<String, DeviceStatus> = mapOf(
        "DEV-0137" to ATENCAO, "DEV-0204" to OK, "DEV-0311" to INDETERMINADO,
        "DEV-0418" to OK, "DEV-0522" to CRITICO, "DEV-0630" to OK,
        "DEV-0745" to INDETERMINADO, "DEV-0800" to OK, "DEV-0854" to OK,
        "DEV-0916" to OK, "DEV-0964" to OK, "DEV-1057" to OK,
        "DEV-1142" to OK, "DEV-1194" to OK, "DEV-1214" to OK,
        "DEV-1231" to OK, "DEV-1258" to OK, "DEV-1277" to OK,
        "DEV-1366" to OK, "DEV-1422" to OK, "DEV-1514" to ATENCAO,
        "DEV-1546" to ATENCAO, "DEV-1559" to ATENCAO, "DEV-1582" to ATENCAO,
        "DEV-1625" to ATENCAO, "DEV-1711" to ATENCAO, "DEV-1767" to ATENCAO,
        "DEV-1839" to CRITICO, "DEV-1924" to CRITICO, "DEV-1985" to CRITICO,
    )

    private fun frozenFleetText(): String {
        val loader = javaClass.classLoader!!
        // Hermético: só pode existir uma frota.json no classpath de teste, a de src/test/resources.
        assertEquals(1, loader.getResources("frota.json").toList().size)
        return loader.getResourceAsStream("frota.json")!!.bufferedReader().use { it.readText() }
    }

    @Test
    fun `os 30 registros do arquivo recebem a classificacao esperada`() = runTest {
        val classify = ClassifyDevice()
        val devices = repository { frozenFleetText() }.getDevices().getOrThrow()

        val actual = devices.associate { it.id to classify(it).status }
        assertEquals(30, devices.size)
        expectedStatus.forEach { (id, status) ->
            assertEquals("status de $id", status, actual[id])
        }
        assertEquals(expectedStatus, actual)
    }

    @Test
    fun `contagem por status e 16 OK, 8 ATENCAO, 4 CRITICO e 2 INDETERMINADO`() = runTest {
        val classify = ClassifyDevice()
        val counts = repository { frozenFleetText() }.getDevices().getOrThrow()
            .groupingBy { classify(it).status }
            .eachCount()
        assertEquals(mapOf(OK to 16, ATENCAO to 8, CRITICO to 4, INDETERMINADO to 2), counts)
    }
}
