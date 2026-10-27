package dev.celiobenj.seldonfleet.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * Arquivo de preferências dos favoritos (files/datastore/favorites.preferences_pb).
 * O delegate garante uma única instância por processo, como o DataStore exige; ela é lida
 * só no ponto de composição das dependências e injetada no [DataStoreFavoritesRepository].
 */
val Context.favoritesDataStore: DataStore<Preferences> by preferencesDataStore(name = "favorites")
