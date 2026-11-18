package br.com.mesasapp.ui.telas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.mesasapp.domain.Catalogo
import br.com.mesasapp.domain.Pedido
import br.com.mesasapp.domain.StatusPedido
import br.com.mesasapp.domain.reservado
import br.com.mesasapp.ui.MesasViewModel
import br.com.mesasapp.ui.Tela
import br.com.mesasapp.ui.comum.Botao
import br.com.mesasapp.ui.comum.Cartao
import br.com.mesasapp.ui.comum.EstiloBotao
import br.com.mesasapp.ui.comum.Numero
import br.com.mesasapp.ui.comum.Pilula
import br.com.mesasapp.ui.comum.PilulaStatus
import br.com.mesasapp.ui.comum.T
import br.com.mesasapp.ui.comum.Titulo
import br.com.mesasapp.ui.theme.Cores
import br.com.mesasapp.util.Datas
import br.com.mesasapp.util.Links
import br.com.mesasapp.util.formatAsBrl
import java.time.LocalDate
import java.time.LocalTime

fun contagem(p: Pedido) = "${p.conta.mesas} mesas · ${p.conta.cadeiras} cadeiras"
fun periodo(p: Pedido) = Datas.dm(p.entrega) + " a " + Datas.dm(p.retirada)

private fun saudacao(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Bom dia"
    in 12..17 -> "Boa tarde"
    else -> "Boa noite"
}

@Composable
fun InicioTela(vm: MesasViewModel) {
    val pedidosTodos by vm.pedidos.collectAsState()
    val estoque by vm.estoque.collectAsState()
    val filtro by vm.filtro.collectAsState()
    val contexto = LocalContext.current
    val hoje = LocalDate.now()

    val peds = pedidosTodos.filter { it.temMaterial(filtro) }
    val visiveis = Catalogo.modelos.filter { it.material in filtro.materiais() }
    val res = reservado(pedidosTodos, Datas.inicioDoDia(hoje), Datas.fimDoDia(hoje))
    fun livres(mesa: Boolean) = visiveis.filter { it.ehMesa == mesa }.sumOf { maxOf(0, (estoque[it.id] ?: 0) - (res[it.id] ?: 0)) }
    fun total(mesa: Boolean) = visiveis.filter { it.ehMesa == mesa }.sumOf { estoque[it.id] ?: 0 }

    val agenda = buildList {
        peds.forEach { o ->
            if (o.entrega == hoje && o.status == StatusPedido.CONFIRMADO) add(true to o)
            if (o.retirada == hoje && o.status == StatusPedido.ENTREGUE) add(false to o)
        }
    }
    val proximos = peds.filter { it.entrega.isAfter(hoje) && it.status != StatusPedido.DEVOLVIDO }.sortedBy { it.entrega }
    val aReceber = peds.filter { it.status.ocupaEstoque }.sumOf { it.conta.saldoCentavos }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                T(Datas.extenso(hoje), 13.sp, 400, Cores.Suave)
                Titulo(saudacao(), 28.sp, Modifier.padding(top = 4.dp))
            }
            Botao("Novo pedido", { vm.irPara(Tela.NOVO) }, altura = 44.dp, raio = 22.dp) {
                Icon(Icons.Filled.Add, null, tint = Cores.Branco, modifier = Modifier.size(18.dp))
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Indicador("Mesas livres hoje", livres(true).toString(), "de ${total(true)} no estoque", Modifier.weight(1f))
                Indicador("Cadeiras livres hoje", livres(false).toString(), "de ${total(false)} no estoque", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Indicador("Entregas hoje", agenda.count { it.first }.toString(), null, Modifier.weight(1f))
                Indicador("Retiradas hoje", agenda.count { !it.first }.toString(), null, Modifier.weight(1f))
            }
        }

        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Cores.Verde).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                T("A receber dos pedidos em aberto", 12.sp, 400, Cores.VerdeTexto)
                Numero(aReceber.formatAsBrl(), 24.sp, Cores.Branco, Modifier.padding(top = 4.dp))
            }
            Box(
                Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).border(1.dp, Cores.VerdeBorda, RoundedCornerShape(20.dp))
                    .clickable(role = Role.Button) { vm.irPara(Tela.PEDIDOS) }.padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) { T("Ver pedidos", 13.sp, 600, Cores.Branco) }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            T("Agenda de hoje", 16.sp, 600)
            agenda.forEach { (entrega, o) ->
                Cartao(padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp), espaco = 6.dp) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        if (entrega) Pilula("Entregar", Cores.AzulClaro, Cores.Azul) else Pilula("Retirar", Cores.LaranjaClaro, Cores.Laranja)
                        T("Pedido #${o.id}", 12.sp, 400, Cores.Suave)
                    }
                    T(o.cliente, 16.sp, 600, modifier = Modifier.padding(top = 2.dp))
                    T(o.endereco, 13.sp, 400, Cores.Texto2)
                    T(contagem(o), 13.sp, 400, Cores.Suave)
                    Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Botao("Ligar", { Links.ligar(contexto, o.telefone) }, Modifier.weight(1f), 44.dp, EstiloBotao.NEUTRO, raio = 10.dp) {
                            Icon(Icons.Outlined.Call, null, tint = Cores.Texto, modifier = Modifier.size(16.dp))
                        }
                        Botao("Abrir pedido", { vm.abrirPedido(o.id) }, Modifier.weight(1f), 44.dp, EstiloBotao.SUAVE_SEM_BORDA, raio = 10.dp)
                    }
                }
            }
            if (agenda.isEmpty()) T("Nenhuma entrega ou retirada para hoje.", 14.sp, 400, Cores.Suave)
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            T("Próximos eventos", 16.sp, 600)
            proximos.forEach { o ->
                Cartao(padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp), aoTocar = { vm.abrirPedido(o.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f)) {
                            T(o.cliente, 15.sp, 600)
                            T(periodo(o) + " · " + contagem(o), 13.sp, 400, Cores.Suave, Modifier.padding(top = 3.dp))
                        }
                        PilulaStatus(o.status)
                    }
                }
            }
        }
    }
}

@Composable
private fun Indicador(rotulo: String, valor: String, rodape: String?, modifier: Modifier) {
    Cartao(modifier, PaddingValues(horizontal = 16.dp, vertical = 14.dp), espaco = 0.dp, borda = BorderStroke(1.dp, Cores.Borda)) {
        T(rotulo, 12.sp, 400, Cores.Suave)
        Numero(valor, 30.sp, modifier = Modifier.padding(top = 6.dp))
        if (rodape != null) T(rodape, 12.sp, 400, Cores.Suave, Modifier.padding(top = 2.dp))
    }
}
