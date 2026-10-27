package dev.celiobenj.seldonfleet.data

import dev.celiobenj.seldonfleet.data.dto.TierDto
import dev.celiobenj.seldonfleet.data.parser.FleetJsonParser
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class FleetJsonParserTest {

    private val parser = FleetJsonParser()

    @Test
    fun `registro valido e lido com todos os campos`() {
        val device = parser.parse(fleetJson(validDeviceJson)).devices.single()
        assertEquals("DEV-0001", device.id)
        assertEquals(TierDto.MID, device.tier)
        assertEquals(5000, device.batteryCapacityMah)
        assertEquals(600.0, device.energyConsumed!!, 0.0)
        assertEquals("mAh", device.energyUnit)
        assertEquals("2026-10-20T02:58:44Z", device.lastSeen)
    }

    @Test
    fun `campos opcionais ausentes viram null sem invalidar o arquivo`() {
        val json = validDeviceJson
            .without("batteryCapacityMah")
            .without("screenOnMinutes")
            .without("energyConsumed")
            .without("nominalVoltage")
            .without("lastSeen")
        val device = parser.parse(fleetJson(json)).devices.single()
        assertNull(device.batteryCapacityMah)
        assertNull(device.screenOnMinutes)
        assertNull(device.energyConsumed)
        assertNull(device.nominalVoltage)
        assertNull(device.lastSeen)
    }

    @Test
    fun `campo opcional com null explicito vira null`() {
        val json = validDeviceJson.with("batteryCapacityMah", JsonNull)
        assertNull(parser.parse(fleetJson(json)).devices.single().batteryCapacityMah)
    }

    @Test
    fun `chaves extras na raiz e no registro sao ignoradas`() {
        val device = validDeviceJson.with("campoNovo", JsonPrimitive("x"))
        val text = fleetJson(device).replaceFirst("{", "{\"outraChave\":123,")
        assertEquals(1, parser.parse(text).devices.size)
    }

    @Test
    fun `cada campo obrigatorio ausente torna o arquivo invalido`() {
        requiredDeviceFields.forEach { field ->
            assertThrows("campo obrigatório: $field", SerializationException::class.java) {
                parser.parse(fleetJson(validDeviceJson.without(field)))
            }
        }
    }

    @Test
    fun `lista devices ausente torna o arquivo invalido`() {
        assertThrows(SerializationException::class.java) {
            parser.parse("""{"schemaVersion":"1.0"}""")
        }
    }

    @Test
    fun `tier desconhecido torna o arquivo invalido`() {
        assertThrows(SerializationException::class.java) {
            parser.parse(fleetJson(validDeviceJson.with("tier", JsonPrimitive("ultra"))))
        }
    }

    @Test
    fun `JSON invalido ou vazio lanca SerializationException`() {
        listOf("{ isto não é JSON", "", "[]").forEach { text ->
            assertThrows("entrada: '$text'", SerializationException::class.java) {
                parser.parse(text)
            }
        }
    }
}
