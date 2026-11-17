package br.com.mesasapp.ui.agenda

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.mesasapp.di.AppContainer
import br.com.mesasapp.domain.model.LocacaoResumo
import br.com.mesasapp.ui.theme.MesasappTheme
import br.com.mesasapp.util.Datas
import br.com.mesasapp.util.formatAsBrl
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Agenda interna: quais dias tem entrega ou retirada e o que acontece no dia
 * escolhido. Nao conversa com o Google Agenda — isso ficou para depois.
 */
@Composable
fun AgendaScreen(container: AppContainer, aoAbrirPedido: (Long) -> Unit) {
    val viewModel: AgendaViewModel = viewModel(
        factory = viewModelFactory { initializer { AgendaViewModel(container.repository) } }
    )
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    val tituloMes = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", Locale.forLanguageTag("pt-BR"))
    val tituloDia = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale.forLanguageTag("pt-BR"))

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Agenda", style = MaterialTheme.typography.headlineMedium) }

        item {
            Card {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = viewModel::mesAnterior) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Mes anterior")
                        }
                        Text(
                            estado.mes.format(tituloMes).replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = viewModel::proximoMes) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Proximo mes")
                        }
                    }

                    Row(Modifier.fillMaxWidth()) {
                        listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sab").forEach { dia ->
                            Text(
                                dia,
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    estado.semanas.forEach { semana ->
                        Row(Modifier.fillMaxWidth()) {
                            semana.forEach { dia ->
                                DiaDoMes(
                                    dia = dia,
                                    selecionado = dia?.data == estado.diaSelecionado,
                                    modifier = Modifier.weight(1f),
                                    aoClicar = { dia?.let { viewModel.selecionarDia(it.data) } }
                                )
                            }
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Legenda(cor = MaterialTheme.colorScheme.primary, texto = "Entrega")
                        Legenda(cor = MesasappTheme.estado.alerta, texto = "Retirada")
                    }
                }
            }
        }

        item {
            Column {
                Text(
                    estado.diaSelecionado.format(tituloDia).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "Na rua nesse dia: ${estado.mesasNaRua} mesas e ${estado.cadeirasNaRua} cadeiras",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (estado.eventosDoDia.isEmpty()) {
            item {
                Text(
                    "Nenhuma entrega ou retirada nesse dia.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(estado.eventosDoDia.size) { indice ->
                val evento = estado.eventosDoDia[indice]
                LinhaEvento(evento.pedido, evento.entrega) { aoAbrirPedido(evento.pedido.id) }
            }
        }
    }
}

@Composable
private fun DiaDoMes(
    dia: DiaAgenda?,
    selecionado: Boolean,
    modifier: Modifier = Modifier,
    aoClicar: () -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selecionado) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .clickable(enabled = dia != null, onClick = aoClicar),
        contentAlignment = Alignment.Center
    ) {
        if (dia != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    dia.data.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (dia.hoje) FontWeight.Bold else FontWeight.Normal
                )
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (dia.entregas > 0) Ponto(MaterialTheme.colorScheme.primary)
                    if (dia.retiradas > 0) Ponto(MesasappTheme.estado.alerta)
                }
            }
        }
    }
}

@Composable
private fun Ponto(cor: androidx.compose.ui.graphics.Color) {
    Box(Modifier.size(6.dp).clip(CircleShape).background(cor))
}

@Composable
private fun Legenda(cor: androidx.compose.ui.graphics.Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
        Ponto(cor)
        Text("  $texto", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun LinhaEvento(pedido: LocacaoResumo, entrega: Boolean, aoAbrir: () -> Unit) {
    val quando = if (entrega) pedido.inicioEpochSeg else pedido.fimEpochSeg
    Card(onClick = aoAbrir) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${if (entrega) "Entrega" else "Retirada"} as ${Datas.formatarHora(quando)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(pedido.clienteNome.ifBlank { "Pedido ${pedido.id}" }, style = MaterialTheme.typography.titleSmall)
                if (pedido.itens.isNotBlank()) {
                    Text(pedido.itens, style = MaterialTheme.typography.bodySmall)
                }
                if (pedido.endereco.isNotBlank()) {
                    Text(
                        pedido.endereco,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(pedido.totalCentavos.formatAsBrl(), style = MaterialTheme.typography.titleSmall)
        }
    }
}

/** Um dia da grade do mes. */
data class DiaAgenda(
    val data: LocalDate,
    val entregas: Int,
    val retiradas: Int,
    val hoje: Boolean
)
