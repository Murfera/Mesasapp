package br.com.mesasapp.ui.telas

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.mesasapp.domain.Catalogo
import br.com.mesasapp.domain.reservado
import br.com.mesasapp.ui.MesasViewModel
import br.com.mesasapp.ui.comum.Cartao
import br.com.mesasapp.ui.comum.Pilula
import br.com.mesasapp.ui.comum.T
import br.com.mesasapp.ui.comum.Titulo
import br.com.mesasapp.ui.theme.Cores
import br.com.mesasapp.util.Datas
import java.time.LocalDate

@Composable
fun AgendaTela(vm: MesasViewModel) {
    val todos by vm.pedidos.collectAsState()
    val filtro by vm.filtro.collectAsState()
    val peds = todos.filter { it.temMaterial(filtro) }
    val hoje = LocalDate.now()
    val mes = vm.mesCalendario
    val sel = vm.diaCalendario

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Titulo("Agenda")
        Cartao(padding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 14.dp), espaco = 8.dp) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Seta(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Mês anterior") { vm.mudarMes(-1) }
                T(Datas.mes(mes.monthValue).replaceFirstChar { it.uppercase() } + " de " + mes.year, 16.sp, 600, modifier = Modifier.weight(1f), alinhar = TextAlign.Center)
                Seta(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Próximo mês") { vm.mudarMes(1) }
            }
            Row(Modifier.fillMaxWidth()) {
                listOf("D", "S", "T", "Q", "Q", "S", "S").forEach {
                    T(it, 11.sp, 600, Cores.Suave, Modifier.weight(1f).padding(vertical = 4.dp), alinhar = TextAlign.Center)
                }
            }
            // Domingo primeiro, como no prototipo.
            val vazios = mes.atDay(1).dayOfWeek.value % 7
            val celulas: List<LocalDate?> = List(vazios) { null } + (1..mes.lengthOfMonth()).map { mes.atDay(it) }
            celulas.chunked(7).forEach { semana ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    (0 until 7).forEach { i ->
                        val dia = semana.getOrNull(i)
                        Box(Modifier.weight(1f).height(48.dp)) {
                            if (dia != null) {
                                val ent = peds.count { it.entrega == dia }
                                val ret = peds.count { it.retirada == dia }
                                val selecionado = dia == sel
                                val ehHoje = dia == hoje
                                val forma = RoundedCornerShape(10.dp)
                                var desc = "${dia.dayOfMonth} de ${Datas.mes(dia.monthValue)}"
                                if (ent > 0) desc += ", $ent " + if (ent == 1) "entrega" else "entregas"
                                if (ret > 0) desc += ", $ret " + if (ret == 1) "retirada" else "retiradas"
                                Column(
                                    Modifier.fillMaxSize().clip(forma)
                                        .then(if (selecionado) Modifier.background(Cores.GradienteVerde) else Modifier)
                                        .border(1.dp, if (selecionado || ehHoje) Cores.Destaque else Color.Transparent, forma)
                                        .clickable(role = Role.Button) { vm.diaCalendario = dia }
                                        .semantics(mergeDescendants = true) { contentDescription = desc },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically)
                                ) {
                                    T(
                                        dia.dayOfMonth.toString(), 14.sp, if (selecionado || ehHoje) 600 else 400,
                                        when { selecionado -> Cores.SobreVerde; ehHoje -> Cores.VerdeEscuro; else -> Cores.Texto }
                                    )
                                    Row(Modifier.height(6.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        if (ent > 0) Ponto(Cores.PontoEntrega, 6)
                                        if (ret > 0) Ponto(Cores.PontoRetirada, 6)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
                Legenda(Cores.PontoEntrega, "Entrega")
                Legenda(Cores.PontoRetirada, "Retirada")
            }
        }

        val eventos = buildList {
            peds.forEach { o ->
                if (o.entrega == sel) add(true to o)
                if (o.retirada == sel) add(false to o)
            }
        }
        val res = reservado(todos, Datas.inicioDoDia(sel), Datas.fimDoDia(sel))
        val visiveis = Catalogo.modelos.filter { it.material in filtro.materiais() }
        val mOcup = visiveis.filter { it.ehMesa }.sumOf { res[it.id] ?: 0 }
        val cOcup = visiveis.filter { !it.ehMesa }.sumOf { res[it.id] ?: 0 }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column {
                T("${sel.dayOfMonth} de ${Datas.mes(sel.monthValue)}", 16.sp, 600)
                T("Na rua nesse dia: $mOcup mesas e $cOcup cadeiras (pedidos confirmados)", 13.sp, 400, Cores.Suave, Modifier.padding(top = 3.dp))
            }
            eventos.forEach { (entrega, o) ->
                Cartao(padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp), espaco = 6.dp, aoTocar = { vm.abrirPedido(o.id) }) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        if (entrega) Pilula("Entrega", Cores.AzulClaro, Cores.Azul) else Pilula("Retirada", Cores.LaranjaClaro, Cores.Laranja)
                        T("#${o.id} · ${o.status.rotulo}", 12.sp, 400, Cores.Suave)
                    }
                    T(o.cliente, 16.sp, 600)
                    T(o.endereco, 13.sp, 400, Cores.Texto2)
                    T(contagem(o), 13.sp, 400, Cores.Suave)
                }
            }
            if (eventos.isEmpty()) T("Nenhuma entrega ou retirada nesse dia.", 14.sp, 400, Cores.Suave)
        }
    }
}

@Composable
internal fun Seta(icone: androidx.compose.ui.graphics.vector.ImageVector, descricao: String, tamanho: androidx.compose.ui.unit.Dp = 44.dp, aoTocar: () -> Unit) {
    Box(
        Modifier.size(tamanho).clip(RoundedCornerShape(10.dp)).clickable(role = Role.Button, onClick = aoTocar)
            .semantics { contentDescription = descricao },
        contentAlignment = Alignment.Center
    ) { Icon(icone, null, tint = Cores.Texto, modifier = Modifier.size(24.dp)) }
}

@Composable
private fun Ponto(cor: Color, tamanho: Int) = Box(Modifier.size(tamanho.dp).clip(CircleShape).background(cor))

@Composable
private fun Legenda(cor: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Ponto(cor, 8)
        T(texto, 12.sp, 400, Cores.Texto2)
    }
}
