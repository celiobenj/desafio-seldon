package dev.celiobenj.seldonfleet.data

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Registro válido em mAh, base dos testes de leitura; cada teste remove ou troca campos. */
internal val validDeviceJson: JsonObject = buildJsonObject {
    put("id", "DEV-0001")
    put("model", "Modelo")
    put("soc", "SoC")
    put("tier", "mid")
    put("ramMb", 4096)
    put("androidApi", 34)
    put("batteryCapacityMah", 5000)
    put("batteryLevelPct", 50)
    put("screenOnMinutes", 120)
    put("energyConsumed", 600.0)
    put("energyUnit", "mAh")
    put("nominalVoltage", 3.85)
    put("jankFramesPct", 2.0)
    put("crashCount7d", 0)
    put("lastSeen", "2026-10-20T02:58:44Z")
}

/** Campos que o esquema garante; a ausência de qualquer um torna o arquivo inválido. */
internal val requiredDeviceFields = listOf(
    "id", "model", "soc", "tier", "ramMb", "androidApi",
    "batteryLevelPct", "energyUnit", "jankFramesPct", "crashCount7d",
)

internal fun JsonObject.without(key: String) = JsonObject(this - key)

internal fun JsonObject.with(key: String, value: JsonElement) = JsonObject(this + (key to value))

/** Texto de um frota.json com os registros dados. */
internal fun fleetJson(vararg devices: JsonObject): String = buildJsonObject {
    put("schemaVersion", "1.0")
    put("devices", JsonArray(devices.toList()))
}.toString()
