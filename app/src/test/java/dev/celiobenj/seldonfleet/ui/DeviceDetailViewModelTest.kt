package dev.celiobenj.seldonfleet.ui

import dev.celiobenj.seldonfleet.domain.model.DeviceStatus
import dev.celiobenj.seldonfleet.domain.repository.FleetLoadException
import dev.celiobenj.seldonfleet.domain.usecase.ClassifyDevice
import dev.celiobenj.seldonfleet.fakes.FakeDeviceRepository
import dev.celiobenj.seldonfleet.fakes.FakeFavoritesRepository
import dev.celiobenj.seldonfleet.fakes.testDevice
import dev.celiobenj.seldonfleet.ui.detail.DeviceDetailUiState
import dev.celiobenj.seldonfleet.ui.detail.DeviceDetailViewModel
import dev.celiobenj.seldonfleet.ui.presentation.JustificationFormatter
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.FileNotFoundException

class DeviceDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val devices = listOf(testDevice("DEV-A"), testDevice("DEV-D", energyMah = 130.0))
    private val favorites = FakeFavoritesRepository()

    private fun create(id: String, repository: FakeDeviceRepository = FakeDeviceRepository(Result.success(devices))) =
        DeviceDetailViewModel(id, repository, favorites, ClassifyDevice(), JustificationFormatter())

    private fun TestScope.observe(viewModel: DeviceDetailViewModel): () -> DeviceDetailUiState {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        return { viewModel.uiState.value }
    }

    @Test
    fun `mostra o dispositivo com status e justificativa`() = runTest {
        val state = observe(create("DEV-D"))() as DeviceDetailUiState.Content
        assertEquals("DEV-D", state.device.id)
        assertEquals(DeviceStatus.CRITICO, state.classification.status)
        assertEquals("Crítico: dreno relativo de 13,0 %/h, acima do limiar de 12 %/h.", state.justification)
        assertFalse(state.isFavorite)
    }

    @Test
    fun `id inexistente vira estado de nao encontrado`() = runTest {
        assertEquals(DeviceDetailUiState.NotFound("DEV-X"), observe(create("DEV-X"))())
    }

    @Test
    fun `falha ao ler o arquivo vira estado de erro`() = runTest {
        val failure = FleetLoadException(FleetLoadException.Reason.FILE_UNAVAILABLE, FileNotFoundException())
        val state = observe(create("DEV-A", FakeDeviceRepository(Result.failure(failure))))()
        assertEquals(DeviceDetailUiState.Error(FleetLoadException.Reason.FILE_UNAVAILABLE), state)
    }

    @Test
    fun `alternar favorito atualiza o estado e o repositorio`() = runTest {
        val viewModel = create("DEV-A")
        val state = observe(viewModel)

        viewModel.setFavorite(true)
        assertTrue((state() as DeviceDetailUiState.Content).isFavorite)
        assertEquals(setOf("DEV-A"), favorites.favoriteIds.value)

        viewModel.setFavorite(false)
        assertFalse((state() as DeviceDetailUiState.Content).isFavorite)
    }
}
