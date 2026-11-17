package br.com.mesasapp.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.com.mesasapp.di.AppContainer
import br.com.mesasapp.ui.agenda.AgendaScreen
import br.com.mesasapp.ui.config.ConfigScreen
import br.com.mesasapp.ui.inicio.InicioScreen
import br.com.mesasapp.ui.itens.ItensScreen
import br.com.mesasapp.ui.novo.NovoPedidoScreen
import br.com.mesasapp.ui.pedido.PedidoScreen
import br.com.mesasapp.ui.pedidos.PedidosScreen

/** As cinco abas do menu de baixo. */
enum class Aba(val rota: String, val titulo: String, val icone: ImageVector) {
    INICIO("inicio", "Inicio", Icons.Default.Home),
    PEDIDOS("pedidos", "Pedidos", Icons.AutoMirrored.Filled.ListAlt),
    NOVO("novo", "Novo", Icons.Default.AddCircleOutline),
    AGENDA("agenda", "Agenda", Icons.Default.CalendarMonth),
    ESTOQUE("estoque", "Estoque", Icons.Default.Inventory2)
}

/** Telas que nao sao abas: abrem por cima e voltam para onde estavam. */
private object Telas {
    const val PEDIDO = "pedido/{id}"
    const val CONFIG = "config"

    fun pedido(id: Long) = "pedido/$id"
}

@Composable
fun MesasappApp(container: AppContainer) {
    val navController = rememberNavController()
    val entradaAtual by navController.currentBackStackEntryAsState()
    val rotaAtual = entradaAtual?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                Aba.entries.forEach { aba ->
                    val selecionada = rotaAtual?.hierarchy?.any { it.route == aba.rota } == true
                    NavigationBarItem(
                        selected = selecionada,
                        onClick = {
                            navController.navigate(aba.rota) {
                                // Trocar de aba nao empilha telas: voltar sai do app,
                                // em vez de desfazer o caminho entre as abas.
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(aba.icone, contentDescription = null) },
                        label = { Text(aba.titulo) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Aba.INICIO.rota,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            composable(Aba.INICIO.rota) {
                InicioScreen(
                    container = container,
                    aoNovoPedido = { navController.navegarParaAba(Aba.NOVO) },
                    aoVerPedidos = { navController.navegarParaAba(Aba.PEDIDOS) },
                    aoVerAgenda = { navController.navegarParaAba(Aba.AGENDA) },
                    aoVerEstoque = { navController.navegarParaAba(Aba.ESTOQUE) },
                    aoAbrirConfig = { navController.navigate(Telas.CONFIG) },
                    aoAbrirPedido = { navController.navigate(Telas.pedido(it)) }
                )
            }
            composable(Aba.PEDIDOS.rota) {
                PedidosScreen(
                    container = container,
                    aoNovoPedido = { navController.navegarParaAba(Aba.NOVO) },
                    aoAbrirPedido = { navController.navigate(Telas.pedido(it)) }
                )
            }
            composable(Aba.NOVO.rota) {
                NovoPedidoScreen(
                    container = container,
                    aoAbrirEstoque = { navController.navegarParaAba(Aba.ESTOQUE) },
                    aoSalvar = { navController.navegarParaAba(Aba.PEDIDOS) }
                )
            }
            composable(Aba.AGENDA.rota) {
                AgendaScreen(
                    container = container,
                    aoAbrirPedido = { navController.navigate(Telas.pedido(it)) }
                )
            }
            composable(Aba.ESTOQUE.rota) {
                ItensScreen(container = container)
            }
            composable(
                route = Telas.PEDIDO,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entrada ->
                PedidoScreen(
                    container = container,
                    pedidoId = entrada.arguments?.getLong("id") ?: 0L,
                    aoVoltar = { navController.popBackStack() }
                )
            }
            composable(Telas.CONFIG) {
                ConfigScreen(container = container, aoVoltar = { navController.popBackStack() })
            }
        }
    }
}

private fun androidx.navigation.NavController.navegarParaAba(aba: Aba) {
    navigate(aba.rota) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
