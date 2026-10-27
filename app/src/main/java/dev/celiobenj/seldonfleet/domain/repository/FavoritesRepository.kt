package dev.celiobenj.seldonfleet.domain.repository

import kotlinx.coroutines.flow.Flow

/** Favoritos persistidos entre aberturas do app, identificados pelo id do dispositivo. */
interface FavoritesRepository {
    /** Conjunto atual de ids favoritos; emite de novo a cada alteração. */
    val favoriteIds: Flow<Set<String>>

    suspend fun setFavorite(id: String, favorite: Boolean)
}
