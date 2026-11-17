package br.com.mesasapp.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.com.mesasapp.di.AppContainer
import br.com.mesasapp.ui.itens.ItensScreen
import br.com.mesasapp.ui.orcamento.OrcamentoScreen

object Rotas {
    const val ORCAMENTO = "orcamento"
    const val ITENS = "itens"
}

@Composable
fun MesasappApp(container: AppContainer) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Rotas.ORCAMENTO) {
        composable(Rotas.ORCAMENTO) {
            OrcamentoScreen(
                container = container,
                aoAbrirEstoque = { navController.navigate(Rotas.ITENS) }
            )
        }
        composable(Rotas.ITENS) {
            ItensScreen(
                container = container,
                aoVoltar = { navController.popBackStack() }
            )
        }
    }
}
