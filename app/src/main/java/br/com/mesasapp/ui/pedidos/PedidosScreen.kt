package br.com.mesasapp.ui.pedidos

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.mesasapp.di.AppContainer
import br.com.mesasapp.domain.model.LocacaoResumo
import br.com.mesasapp.domain.model.StatusLocacao
import br.com.mesasapp.ui.inicio.rotulo
import br.com.mesasapp.util.Datas
import br.com.mesasapp.util.formatAsBrl

/** Lista de pedidos com busca e filtro por situacao. */
@Composable
fun PedidosScreen(
    container: AppContainer,
    aoNovoPedido: () -> Unit,
    aoAbrirPedido: (Long) -> Unit
) {
    val viewModel: PedidosViewModel = viewModel(
        factory = viewModelFactory { initializer { PedidosViewModel(container.repository) } }
    )
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(estado.mensagem) {
        estado.mensagem?.let {
            snackbar.showSnackbar(it)
            viewModel.mensagemLida()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).testTag("lista_pedidos"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Text("Pedidos", style = MaterialTheme.typography.headlineMedium) }

            item {
                OutlinedTextField(
                    value = estado.busca,
                    onValueChange = viewModel::buscar,
                    label = { Text("Buscar cliente ou numero") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = estado.filtro == null,
                        onClick = { viewModel.filtrar(null) },
                        label = { Text("Todos") }
                    )
                    StatusLocacao.entries.forEach { status ->
                        FilterChip(
                            selected = estado.filtro == status,
                            onClick = { viewModel.filtrar(status) },
                            label = { Text(status.rotulo()) }
                        )
                    }
                }
            }

            if (estado.pedidos.isEmpty()) {
                item {
                    Column {
                        Text(
                            "Nenhum pedido aqui.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = aoNovoPedido) { Text("Criar o primeiro pedido") }
                    }
                }
            }

            items(estado.pedidos, key = { it.id }) { pedido ->
                CartaoPedido(
                    pedido = pedido,
                    aoAbrir = { aoAbrirPedido(pedido.id) },
                    aoMudarStatus = { viewModel.mudarStatus(pedido.id, it) }
                )
            }
        }
    }
}

@Composable
private fun CartaoPedido(
    pedido: LocacaoResumo,
    aoAbrir: () -> Unit,
    aoMudarStatus: (StatusLocacao) -> Unit
) {
    var aberto by remember { mutableStateOf(false) }

    Card(onClick = aoAbrir, modifier = Modifier.testTag("pedido_${pedido.id}")) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    pedido.clienteNome.ifBlank { "Pedido ${pedido.id}" },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                AssistChip(onClick = { aberto = !aberto }, label = { Text(pedido.status.rotulo()) })
            }

            Text(
                "Entrega ${Datas.formatarDataHora(pedido.inicioEpochSeg)} · " +
                    "Retirada ${Datas.formatarDataHora(pedido.fimEpochSeg)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (pedido.itens.isNotBlank()) {
                Text(pedido.itens, style = MaterialTheme.typography.bodyMedium)
            }

            Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                Text(
                    pedido.totalCentavos.formatAsBrl(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (pedido.aReceberCentavos != pedido.totalCentavos) {
                    Text(
                        "a receber ${pedido.aReceberCentavos.formatAsBrl()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (aberto) {
                if (pedido.endereco.isNotBlank()) {
                    Text(pedido.endereco, style = MaterialTheme.typography.bodySmall)
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()).padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    proximosStatus(pedido.status).forEach { destino ->
                        TextButton(onClick = { aoMudarStatus(destino) }) {
                            Text(acaoDe(destino))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Para onde um pedido pode ir a partir do estado atual. A devolucao com quebra
 * tem tela propria, por isso aqui DEVOLVIDA so aparece depois de ENTREGUE.
 */
private fun proximosStatus(atual: StatusLocacao): List<StatusLocacao> = when (atual) {
    StatusLocacao.ORCAMENTO -> listOf(StatusLocacao.CONFIRMADA, StatusLocacao.CANCELADA)
    StatusLocacao.CONFIRMADA -> listOf(StatusLocacao.ENTREGUE, StatusLocacao.CANCELADA)
    StatusLocacao.ENTREGUE -> listOf(StatusLocacao.DEVOLVIDA)
    StatusLocacao.DEVOLVIDA, StatusLocacao.CANCELADA -> emptyList()
}

private fun acaoDe(status: StatusLocacao): String = when (status) {
    StatusLocacao.CONFIRMADA -> "Confirmar"
    StatusLocacao.ENTREGUE -> "Marcar entregue"
    StatusLocacao.DEVOLVIDA -> "Marcar devolvido"
    StatusLocacao.CANCELADA -> "Cancelar"
    StatusLocacao.ORCAMENTO -> "Voltar a orcamento"
}
