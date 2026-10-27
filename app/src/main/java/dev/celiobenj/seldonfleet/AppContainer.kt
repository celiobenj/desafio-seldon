package dev.celiobenj.seldonfleet

import android.content.Context
import dev.celiobenj.seldonfleet.data.mapper.DeviceMapper
import dev.celiobenj.seldonfleet.data.parser.FleetJsonParser
import dev.celiobenj.seldonfleet.data.repository.DataStoreFavoritesRepository
import dev.celiobenj.seldonfleet.data.repository.FleetRepositoryImpl
import dev.celiobenj.seldonfleet.data.repository.favoritesDataStore
import dev.celiobenj.seldonfleet.data.source.AssetFleetSource
import dev.celiobenj.seldonfleet.domain.repository.DeviceRepository
import dev.celiobenj.seldonfleet.domain.repository.FavoritesRepository
import dev.celiobenj.seldonfleet.domain.usecase.ClassifyDevice
import dev.celiobenj.seldonfleet.ui.presentation.JustificationFormatter

/**
 * Ponto de composição: o único lugar que conhece as implementações concretas (camada data)
 * e as entrega, já montadas, para quem as usa. Criado uma vez pela [SeldonFleetApp].
 * Quem consome recebe as interfaces do domínio por construtor (injeção manual).
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val deviceRepository: DeviceRepository = FleetRepositoryImpl(
        source = AssetFleetSource(appContext.assets),
        parser = FleetJsonParser(),
        mapper = DeviceMapper(),
    )

    val favoritesRepository: FavoritesRepository = DataStoreFavoritesRepository(appContext.favoritesDataStore)

    val classifyDevice: ClassifyDevice = ClassifyDevice()

    val justificationFormatter: JustificationFormatter = JustificationFormatter()
}
