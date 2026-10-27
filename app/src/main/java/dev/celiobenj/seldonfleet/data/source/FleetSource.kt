package dev.celiobenj.seldonfleet.data.source

import java.io.IOException

/** Origem do texto bruto do frota.json. Separada para os testes usarem uma origem em memória. */
interface FleetSource {
    /** @throws IOException se o arquivo não existir ou não puder ser lido. */
    suspend fun readText(): String
}
