package dev.celiobenj.seldonfleet.data

import dev.celiobenj.seldonfleet.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Comportamento que toda implementação de [FavoritesRepository] deve ter.
 * Rodado contra o DataStore real e contra o fake em memória, para o fake usado nos testes
 * da interface não divergir da implementação de produção.
 */
abstract class FavoritesRepositoryContractTest {

    /** Cria um repositório vazio. */
    protected abstract fun createRepository(): FavoritesRepository

    @Test
    fun `comeca sem favoritos`() = runTest {
        assertEquals(emptySet<String>(), createRepository().favoriteIds.first())
    }

    @Test
    fun `marcar adiciona o id`() = runTest {
        val repository = createRepository()
        repository.setFavorite("DEV-0522", true)
        repository.setFavorite("DEV-0137", true)
        assertEquals(setOf("DEV-0522", "DEV-0137"), repository.favoriteIds.first())
    }

    @Test
    fun `desmarcar remove apenas o id indicado`() = runTest {
        val repository = createRepository()
        repository.setFavorite("DEV-0522", true)
        repository.setFavorite("DEV-0137", true)
        repository.setFavorite("DEV-0522", false)
        assertEquals(setOf("DEV-0137"), repository.favoriteIds.first())
    }

    @Test
    fun `marcar duas vezes ou desmarcar id ausente nao altera o conjunto`() = runTest {
        val repository = createRepository()
        repository.setFavorite("DEV-0522", true)
        repository.setFavorite("DEV-0522", true)
        repository.setFavorite("DEV-9999", false)
        assertEquals(setOf("DEV-0522"), repository.favoriteIds.first())
    }
}
