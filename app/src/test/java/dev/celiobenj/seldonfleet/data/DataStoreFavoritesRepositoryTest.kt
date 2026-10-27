package dev.celiobenj.seldonfleet.data

import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.PreferencesSerializer
import dev.celiobenj.seldonfleet.data.repository.DataStoreFavoritesRepository
import dev.celiobenj.seldonfleet.domain.repository.FavoritesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** DataStore real, gravando em um arquivo temporário na JVM (sem Android). */
class DataStoreFavoritesRepositoryTest : FavoritesRepositoryContractTest() {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val file: File by lazy { File(tempFolder.root, "favorites.preferences_pb") }

    // Cada DataStore vive em um escopo próprio; cancelar o escopo libera o arquivo.
    private val scopes = mutableListOf<Job>()

    private fun newRepository(): FavoritesRepository {
        val job = Job()
        scopes += job
        // Armazenamento Okio: substitui o arquivo de forma atômica também no Windows. O
        // armazenamento padrão usa File.renameTo, que no Windows falha quando o arquivo de
        // destino já existe (no Android e no Linux da CI, os dois funcionam).
        val storage = OkioStorage(FileSystem.SYSTEM, PreferencesSerializer) { file.toOkioPath() }
        val dataStore = PreferenceDataStoreFactory.create(
            storage = storage,
            scope = CoroutineScope(Dispatchers.IO + job),
        )
        return DataStoreFavoritesRepository(dataStore)
    }

    override fun createRepository(): FavoritesRepository = newRepository()

    @After
    fun closeDataStores() = runBlocking {
        scopes.forEach { it.cancelAndJoin() }
    }

    @Test
    fun `favoritos sobrevivem ao fechamento e reabertura do DataStore`() = runTest {
        newRepository().setFavorite("DEV-0522", true)

        // Simula o encerramento do app: o DataStore anterior deixa de existir.
        scopes.forEach { it.cancelAndJoin() }

        val reopened = newRepository()
        assertEquals(setOf("DEV-0522"), reopened.favoriteIds.first())
    }
}
