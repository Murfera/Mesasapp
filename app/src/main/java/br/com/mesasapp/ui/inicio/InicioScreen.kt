package br.com.mesasapp.ui.inicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.mesasapp.di.AppContainer
import br.com.mesasapp.domain.model.LocacaoResumo
import br.com.mesasapp.util.Datas
import br.com.mesasapp.util.formatAsBrl
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Painel de abertura: o que esta livre agora, o que sai hoje e o que ainda falta receber. */
@Composable
fun InicioScreen(
    container: AppContainer,
    aoNovoPedido: () -> Unit,
    aoVerPedidos: () -> Unit,
    aoVerAgenda: () -> Unit,
    aoVerEstoque: () -> Unit
) {
    val viewModel: InicioViewModel = viewModel(
        factory = viewModelFactory {
            initializer { InicioViewModel(container.repository, container.configStore) }
        }
    )
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    val formatoDia = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale.forLanguageTag("pt-BR"))

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text(
                    estado.hoje.format(formatoDia).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(estado.nomeNegocio, style = MaterialTheme.typography.headlineMedium)
            }
        }

        item {
            Button(onClick = aoNovoPedido, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.height(0.dp))
                Text("  Novo pedido")
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CartaoNumero(
                    titulo = "Mesas livres agora",
                    valor = estado.mesasLivres.toString(),
                    apoio = "de ${estado.mesasTotal}",
                    modifier = Modifier.weight(1f),
                    aoClicar = aoVerEstoque
                )
                CartaoNumero(
                    titulo = "Cadeiras livres agora",
                    valor = estado.cadeirasLivres.toString(),
                    apoio = "de ${estado.cadeirasTotal}",
                    modifier = Modifier.weight(1f),
                    aoClicar = aoVerEstoque
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CartaoNumero(
                    titulo = "Entregas hoje",
                    valor = estado.entregasHoje.size.toString(),
                    modifier = Modifier.weight(1f),
                    aoClicar = aoVerAgenda
                )
                CartaoNumero(
                    titulo = "Retiradas hoje",
                    valor = estado.retiradasHoje.size.toString(),
                    modifier = Modifier.weight(1f),
                    aoClicar = aoVerAgenda
                )
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "A receber dos pedidos em aberto",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            estado.aReceberCentavos.formatAsBrl(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(onClick = aoVerPedidos) { Text("Ver pedidos") }
                }
            }
        }

        item { Text("Agenda de hoje", style = MaterialTheme.typography.titleMedium) }

        val hoje = (estado.entregasHoje.map { it to true } + estado.retiradasHoje.map { it to false })
            .sortedBy { (resumo, entrega) -> if (entrega) resumo.inicioEpochSeg else resumo.fimEpochSeg }

        if (hoje.isEmpty()) {
            item {
                Text(
                    "Nenhuma entrega ou retirada para hoje.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(hoje.size) { indice ->
                val (resumo, entrega) = hoje[indice]
                LinhaEvento(resumo, entrega)
            }
        }

        item {
            Text(
                "Proximos eventos",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (estado.proximos.isEmpty()) {
            item {
                Text(
                    "Nada marcado pela frente.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(estado.proximos.size) { indice ->
                LinhaEvento(estado.proximos[indice], entrega = true)
            }
        }
    }
}

@Composable
private fun CartaoNumero(
    titulo: String,
    valor: String,
    apoio: String? = null,
    modifier: Modifier = Modifier,
    aoClicar: () -> Unit
) {
    Card(onClick = aoClicar, modifier = modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(
                titulo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(valor, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            if (apoio != null) {
                Text(
                    apoio,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LinhaEvento(resumo: LocacaoResumo, entrega: Boolean) {
    val quando = if (entrega) resumo.inicioEpochSeg else resumo.fimEpochSeg
    Card {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${if (entrega) "Entrega" else "Retirada"} · ${Datas.formatarDataHora(quando)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    resumo.clienteNome.ifBlank { "Sem nome" },
                    style = MaterialTheme.typography.titleSmall
                )
                if (resumo.itens.isNotBlank()) {
                    Text(resumo.itens, style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(resumo.totalCentavos.formatAsBrl(), style = MaterialTheme.typography.titleSmall)
        }
    }
}
