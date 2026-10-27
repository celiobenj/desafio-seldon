package dev.celiobenj.seldonfleet.data.source

import android.content.res.AssetManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Lê o frota.json empacotado em assets/, sem rede.
 * A leitura roda em [ioDispatcher] (por padrão, Dispatchers.IO), recebido por construtor
 * para poder ser trocado em testes.
 */
class AssetFleetSource(
    private val assets: AssetManager,
    private val fileName: String = "frota.json",
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : FleetSource {

    override suspend fun readText(): String = withContext(ioDispatcher) {
        // AssetManager.open lança IOException (FileNotFoundException) se o arquivo não existir.
        assets.open(fileName).bufferedReader().use { it.readText() }
    }
}
