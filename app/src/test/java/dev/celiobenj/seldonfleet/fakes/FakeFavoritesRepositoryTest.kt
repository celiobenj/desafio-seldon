package dev.celiobenj.seldonfleet.fakes

import dev.celiobenj.seldonfleet.data.FavoritesRepositoryContractTest
import dev.celiobenj.seldonfleet.domain.repository.FavoritesRepository

/** Garante que o fake se comporta como o DataStore real. */
class FakeFavoritesRepositoryTest : FavoritesRepositoryContractTest() {
    override fun createRepository(): FavoritesRepository = FakeFavoritesRepository()
}
