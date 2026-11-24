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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import br.com.mesasapp.domain.Cor
import br.com.mesasapp.domain.Material
import br.com.mesasapp.domain.Modelo
import br.com.mesasapp.domain.Peca
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

    // Separado por tipo de cadeira: tem cliente que so quer jogo sem braco, outro so com
    // braco. Metal nao tem cadeira com braco. So aparecem os tipos com cadeira no estoque.
    val precos by vm.precos.collectAsState()
    val materiais = filtro.materiais()
    val doisMateriais = materiais.size > 1
    val tipos = buildList {
        if (Material.PLASTICO in materiais) {
            add(TipoCadeira(if (doisMateriais) "Plástico sem braço" else "Sem braço", Material.PLASTICO, Peca.CADEIRA))
            add(TipoCadeira(if (doisMateriais) "Plástico com braço" else "Com braço", Material.PLASTICO, Peca.CADEIRA_BRACO))
        }
        if (Material.METAL in materiais) add(TipoCadeira("Metal", Material.METAL, Peca.CADEIRA))
    }.filter { t -> Cor.entries.any { (estoque[Modelo(t.material, t.cadeira, it).id] ?: 0) > 0 } }

    fun qtd(id: String, soLivres: Boolean): Int {
        val tem = estoque[id] ?: 0
        return if (soLivres) maxOf(0, tem - (res[id] ?: 0)) else tem
    }
    val tiposMesa = materiais.filter { m -> Cor.entries.any { (estoque[Modelo(m, Peca.MESA, it).id] ?: 0) > 0 } }
    fun cadeiras(t: TipoCadeira, soLivres: Boolean) = Cor.entries.sumOf { qtd(Modelo(t.material, t.cadeira, it).id, soLivres) }
    // Jogo = 1 mesa + N cadeiras do mesmo material e cor (N do Estoque, por cor).
    fun jogos(t: TipoCadeira, soLivres: Boolean) = Cor.entries.sumOf { cor ->
        val cpm = precos.cadeirasPorMesa(t.material, cor).coerceAtLeast(1)
        minOf(qtd(Modelo(t.material, Peca.MESA, cor).id, soLivres), qtd(Modelo(t.material, t.cadeira, cor).id, soLivres) / cpm)
    }

    val agenda = buildList {
        peds.forEach { o ->
            if (o.entrega == hoje && o.status == StatusPedido.CONFIRMADO) add(true to o)
            if (o.retirada == hoje && o.status == StatusPedido.ENTREGUE) add(false to o)
        }
    }
    val proximos = peds.filter { it.entrega.isAfter(hoje) && it.status != StatusPedido.DEVOLVIDO }.sortedBy { it.entrega }
    val aReceber = peds.filter { it.status.ocupaEstoque }.sumOf { it.conta.saldoCentavos }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                T(Datas.extenso(hoje), 13.sp, 400, Cores.Suave)
                Titulo(saudacao(), modifier = Modifier.padding(top = 2.dp))
            }
            Botao("Novo pedido", { vm.irPara(Tela.NOVO) }, altura = 44.dp, raio = 22.dp) {
                Icon(Icons.Filled.Add, null, tint = Cores.SobreVerde, modifier = Modifier.size(18.dp))
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Jogo precisa de mesa e cadeira do mesmo material no estoque.
            val tiposJogo = tipos.filter { it.material in tiposMesa }
            if (tiposJogo.isNotEmpty()) {
                QuadroJogos(tiposJogo.map { Triple(it.rotulo, jogos(it, true), jogos(it, false)) })
            }
            // Mesma altura nos dois quadros, mesmo com a divisao por tipo so no das cadeiras.
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Indicador(
                    "Mesas livres hoje", livres(true).toString(), "de ${total(true)} no estoque", Modifier.weight(1f).fillMaxHeight(),
                    // Mesa nao tem braco: divide so por material, e so se houver mais de um.
                    detalhes = if (tiposMesa.size > 1) tiposMesa.map { m ->
                        m.rotulo to Cor.entries.sumOf { qtd(Modelo(m, Peca.MESA, it).id, true) }.toString()
                    } else emptyList()
                )
                Indicador(
                    "Cadeiras livres hoje", livres(false).toString(), "de ${total(false)} no estoque", Modifier.weight(1f).fillMaxHeight(),
                    // Com um tipo so, a divisao repetiria o numero de cima.
                    detalhes = if (tipos.size > 1) tipos.map { it.rotulo to cadeiras(it, true).toString() } else emptyList()
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Indicador("Entregas hoje", agenda.count { it.first }.toString(), null, Modifier.weight(1f))
                Indicador("Retiradas hoje", agenda.count { !it.first }.toString(), null, Modifier.weight(1f))
            }
        }

        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Cores.GradienteVerde).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                T("A receber dos pedidos em aberto", 12.sp, 400, Cores.VerdeTexto)
                Numero(aReceber.formatAsBrl(), 24.sp, Cores.SobreVerde, Modifier.padding(top = 4.dp))
            }
            Box(
                Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).border(1.dp, Cores.VerdeBorda, RoundedCornerShape(20.dp))
                    .clickable(role = Role.Button) { vm.irPara(Tela.PEDIDOS) }.padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) { T("Ver pedidos", 13.sp, 600, Cores.SobreVerde) }
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

private data class TipoCadeira(val rotulo: String, val material: Material, val cadeira: Peca)

/**
 * Largura inteira: uma linha por tipo de cadeira (livres hoje / no estoque). As linhas
 * nao se somam: as mesas sao as mesmas, cada uma diz quantos jogos daquele tipo cabem.
 */
@Composable
private fun QuadroJogos(linhas: List<Triple<String, Int, Int>>) {
    Cartao(Modifier.fillMaxWidth(), PaddingValues(horizontal = 16.dp, vertical = 14.dp), espaco = 0.dp, borda = BorderStroke(1.dp, Cores.Borda)) {
        T("Jogos disponíveis hoje", 12.sp, 400, Cores.Suave)
        linhas.forEach { (nome, livres, total) ->
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                T(nome, 14.sp, 600, modifier = Modifier.weight(1f))
                Numero(livres.toString(), 24.sp)
                T(" de $total", 12.sp, 400, Cores.Suave, Modifier.padding(start = 4.dp))
            }
        }
        if (linhas.size > 1) {
            T("As mesas são as mesmas para todos os tipos: os números não se somam.", 11.sp, 400, Cores.Suave, Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun Indicador(rotulo: String, valor: String, rodape: String?, modifier: Modifier, detalhes: List<Pair<String, String>> = emptyList()) {
    Cartao(modifier, PaddingValues(horizontal = 16.dp, vertical = 14.dp), espaco = 0.dp, borda = BorderStroke(1.dp, Cores.Borda)) {
        T(rotulo, 12.sp, 400, Cores.Suave)
        Numero(valor, 30.sp, modifier = Modifier.padding(top = 6.dp))
        if (rodape != null) T(rodape, 12.sp, 400, Cores.Suave, Modifier.padding(top = 2.dp))
        if (detalhes.isNotEmpty()) {
            Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                detalhes.forEach { (nome, n) ->
                    Row {
                        T(nome, 12.sp, 400, Cores.Suave, Modifier.weight(1f), umaLinha = true)
                        T(n, 12.sp, 600, Cores.Texto2)
                    }
                }
            }
        }
    }
}
