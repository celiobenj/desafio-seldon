package dev.celiobenj.seldonfleet.data.parser

import dev.celiobenj.seldonfleet.data.dto.FleetDto
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Converte o texto do frota.json em [FleetDto]. Kotlin puro (sem Android), testável na JVM.
 * Só lê: não normaliza unidades nem classifica.
 */
class FleetJsonParser {

    // Chaves que o app não conhece são ignoradas, para tolerar versões futuras do arquivo.
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * @throws SerializationException se o texto não for JSON válido ou estiver fora do esquema
     * (campo obrigatório ausente, tipo errado, tier desconhecido).
     */
    fun parse(text: String): FleetDto = json.decodeFromString<FleetDto>(text)
}
