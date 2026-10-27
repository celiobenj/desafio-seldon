package dev.celiobenj.seldonfleet.data.repository

import dev.celiobenj.seldonfleet.data.dto.FleetDto
import dev.celiobenj.seldonfleet.data.mapper.DeviceMapper
import dev.celiobenj.seldonfleet.data.parser.FleetJsonParser
import dev.celiobenj.seldonfleet.data.source.FleetSource
import dev.celiobenj.seldonfleet.domain.model.Device
import dev.celiobenj.seldonfleet.domain.repository.DeviceRepository
import dev.celiobenj.seldonfleet.domain.repository.FleetLoadException
import java.io.IOException

/**
 * Implementação de [DeviceRepository] que lê o frota.json: origem -> parser -> mapper.
 * Relê o arquivo a cada chamada (o arquivo é a fonte da verdade). Nunca lança exceção:
 * defeitos do arquivo voltam como [Result.failure] com [FleetLoadException].
 */
class FleetRepositoryImpl(
    private val source: FleetSource,
    private val parser: FleetJsonParser,
    private val mapper: DeviceMapper,
) : DeviceRepository {

    override suspend fun getDevices(): Result<List<Device>> {
        val text: String = try {
            source.readText()
        } catch (e: IOException) {
            return Result.failure(FleetLoadException(FleetLoadException.Reason.FILE_UNAVAILABLE, e))
        }

        val fleet: FleetDto = try {
            parser.parse(text)
        } catch (e: IllegalArgumentException) {
            // SerializationException (JSON inválido ou fora do esquema) é subclasse de IllegalArgumentException.
            return Result.failure(FleetLoadException(FleetLoadException.Reason.INVALID_CONTENT, e))
        }

        return Result.success(fleet.devices.map(mapper::toDomain))
    }
}
