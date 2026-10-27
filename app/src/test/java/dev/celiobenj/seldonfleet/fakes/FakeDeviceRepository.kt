package dev.celiobenj.seldonfleet.fakes

import dev.celiobenj.seldonfleet.domain.model.Device
import dev.celiobenj.seldonfleet.domain.repository.DeviceRepository

/** Repositório em memória: devolve o resultado configurado e conta as leituras. */
class FakeDeviceRepository(var result: Result<List<Device>>) : DeviceRepository {

    var calls = 0
        private set

    override suspend fun getDevices(): Result<List<Device>> {
        calls++
        return result
    }
}
