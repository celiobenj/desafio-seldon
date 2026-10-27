package dev.celiobenj.seldonfleet.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.celiobenj.seldonfleet.AppContainer
import dev.celiobenj.seldonfleet.ui.detail.DeviceDetailRoute
import dev.celiobenj.seldonfleet.ui.detail.DeviceDetailViewModel
import dev.celiobenj.seldonfleet.ui.list.FleetListRoute
import dev.celiobenj.seldonfleet.ui.list.FleetListViewModel

private const val ROUTE_LIST = "list"
private const val ARG_ID = "id"
private const val ROUTE_DETAIL = "detail/{$ARG_ID}"

/**
 * Duas telas: "list" e "detail/{id}". Cada destino cria o seu ViewModel, ligado à sua entrada
 * na pilha. Ao abrir o detalhe, a entrada da lista (com o FleetListViewModel e o filtro)
 * continua na pilha; o voltar do sistema remove só o detalhe, e o filtro é preservado.
 */
@Composable
fun AppNavHost(container: AppContainer) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = ROUTE_LIST) {
        composable(ROUTE_LIST) {
            val viewModel: FleetListViewModel = viewModel(
                factory = FleetListViewModel.factory(
                    container.deviceRepository,
                    container.favoritesRepository,
                    container.classifyDevice,
                ),
            )
            FleetListRoute(
                viewModel = viewModel,
                onOpenDevice = { id -> navController.navigate("detail/${Uri.encode(id)}") },
            )
        }

        composable(
            route = ROUTE_DETAIL,
            arguments = listOf(navArgument(ARG_ID) { type = NavType.StringType }),
        ) { entry ->
            val deviceId = entry.arguments?.getString(ARG_ID).orEmpty()
            val viewModel: DeviceDetailViewModel = viewModel(
                factory = DeviceDetailViewModel.factory(
                    deviceId,
                    container.deviceRepository,
                    container.favoritesRepository,
                    container.classifyDevice,
                    container.justificationFormatter,
                ),
            )
            // navigateUp não sai da lista (destino inicial), mesmo com toques repetidos.
            DeviceDetailRoute(viewModel = viewModel, onBack = { navController.navigateUp() })
        }
    }
}
