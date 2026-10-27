package dev.celiobenj.seldonfleet.fakes

import dev.celiobenj.seldonfleet.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Favoritos em memória, para testes que não precisam de persistência real. */
class FakeFavoritesRepository(initial: Set<String> = emptySet()) : FavoritesRepository {

    private val ids = MutableStateFlow(initial)

    override val favoriteIds: StateFlow<Set<String>> = ids

    override suspend fun setFavorite(id: String, favorite: Boolean) {
        ids.value = if (favorite) ids.value + id else ids.value - id
    }
}
