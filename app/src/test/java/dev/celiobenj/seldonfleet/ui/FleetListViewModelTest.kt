package dev.celiobenj.seldonfleet.ui

import dev.celiobenj.seldonfleet.data.mapper.DeviceMapper
import dev.celiobenj.seldonfleet.data.parser.FleetJsonParser
import dev.celiobenj.seldonfleet.data.repository.FleetRepositoryImpl
import dev.celiobenj.seldonfleet.data.source.FleetSource
import dev.celiobenj.seldonfleet.domain.model.DeviceStatus
import dev.celiobenj.seldonfleet.domain.repository.DeviceRepository
import dev.celiobenj.seldonfleet.domain.repository.FleetLoadException
import dev.celiobenj.seldonfleet.domain.usecase.ClassifyDevice
import dev.celiobenj.seldonfleet.fakes.FakeDeviceRepository
import dev.celiobenj.seldonfleet.fakes.FakeFavoritesRepository
import dev.celiobenj.seldonfleet.fakes.testDevice
import dev.celiobenj.seldonfleet.ui.list.FleetListUiState
import dev.celiobenj.seldonfleet.ui.list.FleetListViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.FileNotFoundException

class FleetListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // 2 OK, 1 ATENCAO, 1 CRITICO, 1 INDETERMINADO
    private val devices = listOf(
        testDevice("DEV-A"),
        testDevice("DEV-B"),
        testDevice("DEV-C", crashes = 1),
        testDevice("DEV-D", energyMah = 130.0),
        testDevice("DEV-E", capacityMah = 0),
    )

    private val favorites = FakeFavoritesRepository()

    private fun viewModel(repository: DeviceRepository = FakeDeviceRepository(Result.success(devices))) =
        FleetListViewModel(repository, favorites, ClassifyDevice())

    /** Mantém um coletor ativo (como a tela faria) e devolve o estado atual. */
    private fun TestScope.observe(viewModel: FleetListViewModel): () -> FleetListUiState {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        return { viewModel.uiState.value }
    }

    private fun FleetListUiState.content() = this as FleetListUiState.Content

    // ---------- Contagens (REQ-05) ----------

    @Test
    fun `contagem por status soma o total de registros`() = runTest {
        val state = observe(viewModel())().content()
        assertEquals(
            mapOf(
                DeviceStatus.OK to 2,
                DeviceStatus.ATENCAO to 1,
                DeviceStatus.CRITICO to 1,
                DeviceStatus.INDETERMINADO to 1,
            ),
            state.counts,
        )
        assertEquals(5, state.totalCount)
        assertEquals(state.totalCount, state.counts.values.sum())
    }

    @Test
    fun `com o arquivo real a contagem e 16, 8, 4 e 2, somando 30`() = runTest {
        val text = javaClass.classLoader!!.getResourceAsStream("frota.json")!!.bufferedReader().use { it.readText() }
        val source = object : FleetSource {
            override suspend fun readText() = text
        }
        val repository = FleetRepositoryImpl(source, FleetJsonParser(), DeviceMapper())

        val state = observe(viewModel(repository))().content()
        assertEquals(16, state.counts[DeviceStatus.OK])
        assertEquals(8, state.counts[DeviceStatus.ATENCAO])
        assertEquals(4, state.counts[DeviceStatus.CRITICO])
        assertEquals(2, state.counts[DeviceStatus.INDETERMINADO])
        assertEquals(30, state.totalCount)
        assertEquals(30, state.counts.values.sum())
        assertEquals(30, state.items.size)
    }

    // ---------- Filtro ----------

    @Test
    fun `sem filtro a lista mostra todos`() = runTest {
        val state = observe(viewModel())().content()
        assertEquals(null, state.selectedFilter)
        assertEquals(listOf("DEV-A", "DEV-B", "DEV-C", "DEV-D", "DEV-E"), state.items.map { it.id })
    }

    @Test
    fun `filtro reduz a lista aos dispositivos daquele status`() = runTest {
        val viewModel = viewModel()
        val state = observe(viewModel)

        viewModel.selectFilter(DeviceStatus.OK)
        assertEquals(listOf("DEV-A", "DEV-B"), state().content().items.map { it.id })
        assertEquals(DeviceStatus.OK, state().content().selectedFilter)

        viewModel.selectFilter(DeviceStatus.INDETERMINADO)
        assertEquals(listOf("DEV-E"), state().content().items.map { it.id })
        assertTrue(state().content().items.all { it.status == DeviceStatus.INDETERMINADO })
    }

    @Test
    fun `contagens nao mudam quando o filtro muda`() = runTest {
        val viewModel = viewModel()
        val state = observe(viewModel)
        val before = state().content().counts

        viewModel.selectFilter(DeviceStatus.CRITICO)
        assertEquals(before, state().content().counts)
        assertEquals(5, state().content().totalCount)
    }

    @Test
    fun `limpar o filtro volta a mostrar todos`() = runTest {
        val viewModel = viewModel()
        val state = observe(viewModel)
        viewModel.selectFilter(DeviceStatus.CRITICO)
        viewModel.selectFilter(null)
        assertEquals(5, state().content().items.size)
    }

    @Test
    fun `filtro sem dispositivos resulta em lista vazia, nao em erro`() = runTest {
        val viewModel = viewModel(FakeDeviceRepository(Result.success(listOf(testDevice("DEV-A")))))
        val state = observe(viewModel)
        viewModel.selectFilter(DeviceStatus.CRITICO)
        assertTrue(state().content().items.isEmpty())
    }

    // ---------- Próximo / anterior (gesto de arrastar) ----------

    @Test
    fun `proximo percorre TODOS, OK, ATENCAO, CRITICO e SEM DADOS e para na ultima ponta`() = runTest {
        val viewModel = viewModel()
        val state = observe(viewModel)
        val visited = mutableListOf(state().content().selectedFilter)
        repeat(5) {
            viewModel.selectNextFilter()
            visited += state().content().selectedFilter
        }
        assertEquals(
            listOf(
                null,
                DeviceStatus.OK,
                DeviceStatus.ATENCAO,
                DeviceStatus.CRITICO,
                DeviceStatus.INDETERMINADO,
                // não dá a volta: continua no último
                DeviceStatus.INDETERMINADO,
            ),
            visited,
        )
    }

    @Test
    fun `anterior volta um filtro e para em TODOS`() = runTest {
        val viewModel = viewModel()
        val state = observe(viewModel)

        viewModel.selectPreviousFilter()
        assertEquals(null, state().content().selectedFilter)

        viewModel.selectFilter(DeviceStatus.CRITICO)
        viewModel.selectPreviousFilter()
        assertEquals(DeviceStatus.ATENCAO, state().content().selectedFilter)
        viewModel.selectPreviousFilter()
        viewModel.selectPreviousFilter()
        viewModel.selectPreviousFilter()
        assertEquals(null, state().content().selectedFilter)
    }

    @Test
    fun `ao trocar de filtro pelo gesto a lista e as contagens continuam coerentes`() = runTest {
        val viewModel = viewModel()
        val state = observe(viewModel)
        val counts = state().content().counts

        repeat(4) {
            viewModel.selectNextFilter()
            val content = state().content()
            val filter = content.selectedFilter!!
            assertTrue(content.items.all { it.status == filter })
            assertEquals(counts[filter], content.items.size)
            assertEquals(counts, content.counts)
            assertEquals(content.totalCount, content.counts.values.sum())
        }
    }

    // ---------- Erro ----------

    @Test
    fun `falha ao ler o arquivo vira estado de erro com o motivo`() = runTest {
        val failure = FleetLoadException(FleetLoadException.Reason.FILE_UNAVAILABLE, FileNotFoundException())
        val state = observe(viewModel(FakeDeviceRepository(Result.failure(failure))))()
        assertEquals(FleetListUiState.Error(FleetLoadException.Reason.FILE_UNAVAILABLE), state)
    }

    @Test
    fun `tentar de novo rele o arquivo e sai do erro`() = runTest {
        val failure = FleetLoadException(FleetLoadException.Reason.INVALID_CONTENT, IllegalArgumentException())
        val repository = FakeDeviceRepository(Result.failure(failure))
        val viewModel = viewModel(repository)
        val state = observe(viewModel)
        assertTrue(state() is FleetListUiState.Error)

        repository.result = Result.success(devices)
        viewModel.retry()
        assertEquals(5, state().content().totalCount)
        assertEquals(2, repository.calls)
    }

    // ---------- Favoritos ----------

    @Test
    fun `favoritar atualiza o estado do item e desfavoritar remove`() = runTest {
        val viewModel = viewModel()
        val state = observe(viewModel)
        assertTrue(state().content().items.none { it.isFavorite })

        viewModel.setFavorite("DEV-C", true)
        assertEquals(listOf("DEV-C"), state().content().items.filter { it.isFavorite }.map { it.id })
        assertEquals(setOf("DEV-C"), favorites.favoriteIds.value)

        viewModel.setFavorite("DEV-C", false)
        assertTrue(state().content().items.none { it.isFavorite })
    }

    @Test
    fun `favoritar nao altera o filtro selecionado`() = runTest {
        val viewModel = viewModel()
        val state = observe(viewModel)
        viewModel.selectFilter(DeviceStatus.OK)
        viewModel.setFavorite("DEV-A", true)
        assertEquals(DeviceStatus.OK, state().content().selectedFilter)
        assertEquals(listOf("DEV-A", "DEV-B"), state().content().items.map { it.id })
    }
}
