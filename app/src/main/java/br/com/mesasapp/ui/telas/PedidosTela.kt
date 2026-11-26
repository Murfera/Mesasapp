package br.com.mesasapp.ui.telas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.mesasapp.domain.StatusPedido
import br.com.mesasapp.ui.MesasViewModel
import br.com.mesasapp.ui.comum.Cartao
import br.com.mesasapp.ui.comum.Chip
import br.com.mesasapp.ui.comum.Pilula
import br.com.mesasapp.ui.comum.corStatus
import br.com.mesasapp.ui.comum.fundoCartaoStatus
import br.com.mesasapp.ui.comum.T
import br.com.mesasapp.ui.comum.Titulo
import br.com.mesasapp.ui.theme.Cores
import br.com.mesasapp.util.formatAsBrl
import java.time.LocalDate
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import br.com.mesasapp.util.Datas

@Composable
fun PedidosTela(vm: MesasViewModel) {
    val todos by vm.pedidos.collectAsState()
    val filtro by vm.filtro.collectAsState()
    val hoje = LocalDate.now()
    val mes = vm.mesPedidos
    val semana = vm.semanaPedidos
    val (ini, fim) = if (vm.pedidosPorSemana) semana to semana.plusDays(6) else mes.atDay(1) to mes.atEndOfMonth()
    val linhas = todos
        .filter { it.temMaterial(filtro) }
        .filter { it.status in vm.filtroStatus }
        // Entra todo pedido cujo periodo toca a semana/o mes: um aluguel de 24/12 a 01/01
        // aparece em dezembro e em janeiro.
        .filter { !it.entrega.isAfter(fim) && !it.retirada.isBefore(ini) }
        // Do comeco para o fim do mes, como num calendario.
        .sortedBy { it.inicioEpochSeg }

    LazyColumn(
        Modifier.fillMaxSize().testTag("lista_pedidos"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Situacoes marcaveis juntas: aparece so o que esta marcado (o "Todos" saiu, pedido do usuario).
        // Titulo com o mes fixo ao lado; embaixo, Semana | Mes e as setas da semana.
        // (Tudo numa linha so foi testado e o usuario preferiu assim.)
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Titulo("Pedidos", modifier = Modifier.weight(1f))
                Seta(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Mês anterior") { vm.mudarMesPedidos(-1) }
                // Tocar no nome do mes volta para hoje.
                T(
                    Datas.mes(mes.monthValue).replaceFirstChar { it.uppercase() } + " " + mes.year, 15.sp, 600,
                    modifier = Modifier.clickable(role = Role.Button) { vm.pedidosDeHoje() }.testTag("mes_pedidos"),
                    alinhar = TextAlign.Center
                )
                Seta(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Próximo mês") { vm.mudarMesPedidos(1) }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Segmentos(
                    listOf("Semana", "Mês"), if (vm.pedidosPorSemana) 0 else 1, "periodo_pedidos",
                    Modifier.width(160.dp), altura = 34.dp
                ) { vm.pedidosPorSemana = it == 0 }
                if (vm.pedidosPorSemana) {
                    Seta(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Semana anterior") { vm.mudarSemanaPedidos(-1) }
                    T(
                        Datas.dm(semana) + " a " + Datas.dm(semana.plusDays(6)), 13.sp, 600,
                        modifier = Modifier.weight(1f).testTag("semana_pedidos"), alinhar = TextAlign.Center
                    )
                    Seta(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Próxima semana") { vm.mudarSemanaPedidos(1) }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                StatusPedido.entries.forEach { s ->
                    Chip(s.rotulo, s in vm.filtroStatus, { vm.alternarStatus(s) }, altura = 34.dp, status = s, paddingH = 10.dp, tamanho = 12.sp)
                }
            }
        }
        items(linhas, key = { it.id }) { o ->
            val alerta = when {
                o.status == StatusPedido.CONFIRMADO && o.entrega == hoje -> "Entregar hoje"
                o.status == StatusPedido.ENTREGUE && o.retirada == hoje -> "Retirar hoje"
                o.status == StatusPedido.ENTREGUE && o.retirada.isBefore(hoje) -> "Retirada atrasada"
                else -> null
            }
            // O cartao inteiro na cor da situacao (pedido do usuario); a etiqueta fica
            // em fundo branco para continuar legivel sobre a cor.
            val corTexto = corStatus(o.status).second
            Cartao(
                Modifier.testTag("pedido_${o.id}"),
                padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp), espaco = 6.dp,
                borda = BorderStroke(1.dp, corTexto.copy(alpha = 0.35f)),
                fundo = fundoCartaoStatus(o.status),
                aoTocar = { vm.abrirPedido(o.id) }
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    T("#${o.id} · ${periodo(o)}", 12.sp, 400, Cores.Texto2)
                    Pilula(o.status.rotulo, Cores.Superficie, corTexto)
                }
                T(o.cliente, 16.sp, 600)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                    T(contagem(o), 13.sp, 400, Cores.Texto2, Modifier.weight(1f))
                    T((o.finalCentavos ?: o.conta.totalCentavos).formatAsBrl(), 15.sp, 600)
                }
                if (alerta != null) T(alerta, 12.sp, 600, Cores.Laranja)
            }
        }
        if (vm.filtroStatus.isEmpty()) item { T("Marque uma situação acima para ver os pedidos.", 14.sp, 400, Cores.Suave) }
        else if (linhas.isEmpty()) item {
            T(if (vm.pedidosPorSemana) "Nenhum pedido nesta semana com as situações marcadas." else "Nenhum pedido em ${Datas.mes(mes.monthValue)} de ${mes.year} com as situações marcadas.", 14.sp, 400, Cores.Suave)
        }
    }
}

