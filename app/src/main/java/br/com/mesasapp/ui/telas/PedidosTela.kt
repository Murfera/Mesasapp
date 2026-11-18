package br.com.mesasapp.ui.telas

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import br.com.mesasapp.ui.comum.PilulaStatus
import br.com.mesasapp.ui.comum.T
import br.com.mesasapp.ui.comum.Titulo
import br.com.mesasapp.ui.theme.Cores
import br.com.mesasapp.util.formatAsBrl
import java.time.LocalDate

@Composable
fun PedidosTela(vm: MesasViewModel) {
    val todos by vm.pedidos.collectAsState()
    val filtro by vm.filtro.collectAsState()
    val hoje = LocalDate.now()
    val linhas = todos
        .filter { it.temMaterial(filtro) }
        .filter { vm.filtroStatus == null || it.status == vm.filtroStatus }
        .sortedByDescending { it.inicioEpochSeg }

    LazyColumn(
        Modifier.fillMaxSize().testTag("lista_pedidos"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Titulo("Pedidos") }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (listOf<StatusPedido?>(null) + StatusPedido.entries).forEach { s ->
                    Chip(s?.rotulo ?: "Todos", vm.filtroStatus == s, { vm.filtroStatus = s }, altura = 36.dp, escuro = true)
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
            Cartao(
                Modifier.testTag("pedido_${o.id}"),
                padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp), espaco = 6.dp,
                aoTocar = { vm.abrirPedido(o.id) }
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    T("#${o.id} · ${periodo(o)}", 12.sp, 400, Cores.Suave)
                    PilulaStatus(o.status)
                }
                T(o.cliente, 16.sp, 600)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                    T(contagem(o), 13.sp, 400, Cores.Texto2, Modifier.weight(1f))
                    T((o.finalCentavos ?: o.conta.totalCentavos).formatAsBrl(), 15.sp, 600)
                }
                if (alerta != null) T(alerta, 12.sp, 600, Cores.Laranja)
            }
        }
        if (linhas.isEmpty()) item { T("Nenhum pedido nesse filtro.", 14.sp, 400, Cores.Suave) }
    }
}
