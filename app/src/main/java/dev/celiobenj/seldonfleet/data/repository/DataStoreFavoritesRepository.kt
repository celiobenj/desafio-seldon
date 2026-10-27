package dev.celiobenj.seldonfleet.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import dev.celiobenj.seldonfleet.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Favoritos persistidos com DataStore Preferences, como um conjunto de ids sob uma única chave.
 * O [DataStore] é recebido por construtor: no app, vem do arquivo do app; nos testes, de um
 * arquivo temporário.
 */
class DataStoreFavoritesRepository(
    private val dataStore: DataStore<Preferences>,
) : FavoritesRepository {

    override val favoriteIds: Flow<Set<String>> = dataStore.data
        // Arquivo ilegível: segue sem favoritos em vez de encerrar o app.
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { preferences -> preferences[FAVORITE_IDS] ?: emptySet() }

    override suspend fun setFavorite(id: String, favorite: Boolean) {
        // edit é atômico: lê o conjunto atual e grava o novo numa única transação.
        dataStore.edit { preferences ->
            val current = preferences[FAVORITE_IDS] ?: emptySet()
            preferences[FAVORITE_IDS] = if (favorite) current + id else current - id
        }
    }

    private companion object {
        val FAVORITE_IDS = stringSetPreferencesKey("favorite_ids")
    }
}
