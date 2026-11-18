package br.com.mesasapp.ui.telas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.mesasapp.domain.Catalogo
import br.com.mesasapp.domain.FORMAS_PAGAMENTO
import br.com.mesasapp.domain.MULTIPLICAR_POR_DIAS
import br.com.mesasapp.domain.Modelo
import br.com.mesasapp.domain.StatusPedido
import br.com.mesasapp.domain.TipoLinha
import br.com.mesasapp.domain.reservado
import br.com.mesasapp.ui.MesasViewModel
import br.com.mesasapp.ui.centavos
import br.com.mesasapp.ui.comum.Aviso
import br.com.mesasapp.ui.comum.Botao
import br.com.mesasapp.ui.comum.BotaoPasso
import br.com.mesasapp.ui.comum.Bolinha
import br.com.mesasapp.ui.comum.Campo
import br.com.mesasapp.ui.comum.CampoBase
import br.com.mesasapp.ui.comum.Cartao
import br.com.mesasapp.ui.comum.Chip
import br.com.mesasapp.ui.comum.Divisor
import br.com.mesasapp.ui.comum.EstiloBotao
import br.com.mesasapp.ui.comum.LinhaValor
import br.com.mesasapp.ui.comum.Numero
import br.com.mesasapp.ui.comum.Nota
import br.com.mesasapp.ui.comum.Rotulo
import br.com.mesasapp.ui.comum.T
import br.com.mesasapp.ui.comum.Titulo
import br.com.mesasapp.ui.comum.Tracejado
import br.com.mesasapp.ui.theme.Cores
import br.com.mesasapp.util.Datas
import br.com.mesasapp.util.formatAsBrl
import java.time.LocalDate

fun diariasTexto(d: Int) = "$d " + if (d == 1) "diária" else "diárias"

/**
 * Enquanto a regra das diarias estiver em aberto (MULTIPLICAR_POR_DIAS falso),
 * o rotulo nao pode dizer "2 diarias x R$ 10" com o valor de uma diaria so.
 */
fun rotuloAluguel(dias: Int, diaria: Long) =
    if (MULTIPLICAR_POR_DIAS) "Aluguel (" + diariasTexto(dias) + " × " + diaria.formatAsBrl() + ")"
    else "Aluguel (valor de 1 diária)"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NovoTela(vm: MesasViewModel) {
    val precos by vm.precos.collectAsState()
    val estoque by vm.estoque.collectAsState()
    val pedidos by vm.pedidos.collectAsState()
    val filtro by vm.filtro.collectAsState()
    val f = vm.form
    val item = vm.itemForm
    val c = f.conta(precos)
    val res = reservado(pedidos, f.inicioEpochSeg, f.fimEpochSeg)
    val visiveis = Catalogo.modelos.filter { it.material in filtro.materiais() }
    fun livre(id: String) = (estoque[id] ?: 0) - (res[id] ?: 0)
    val cpm = precos.cadeirasPorMesa(item.mesaId)
    val qtd = vm.qtdItem.toIntOrNull()?.coerceAtLeast(1) ?: 1

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).testTag("lista_novo")
                .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Titulo("Novo pedido")

            Cartao {
                T("Cliente", 15.sp, 600)
                Campo("Nome", f.cliente, { vm.mudarForm(f.copy(cliente = it)) }, placeholder = "Nome completo", tag = "campo_cliente")
                Campo("Telefone / WhatsApp", f.telefone, { vm.mudarForm(f.copy(telefone = it)) }, placeholder = "(00) 00000-0000", teclado = KeyboardType.Phone, tag = "campo_telefone")
                Campo("CPF ou CNPJ (para o contrato)", f.documento, { vm.mudarForm(f.copy(documento = it)) }, placeholder = "Opcional")
            }

            Cartao {
                T("Local da entrega", 15.sp, 600)
                Campo("Endereço", f.endereco, { vm.mudarForm(f.copy(endereco = it)) }, placeholder = "Rua, número, bairro", tag = "campo_endereco")
                Campo("Ponto de referência / observações", f.referencia, { vm.mudarForm(f.copy(referencia = it)) }, placeholder = "Ex.: portão lateral, 2º andar sem elevador")
            }

            Cartao {
                T("Datas", 15.sp, 600)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CampoData("Entrega", f.entrega, { vm.mudarForm(f.copy(entrega = it)) }, Modifier.weight(1f))
                    CampoData("Retirada", f.retirada, { vm.mudarForm(f.copy(retirada = it)) }, Modifier.weight(1f))
                }
                Nota("Calculado: " + diariasTexto(c.dias))
            }

            Cartao {
                Column {
                    T("Itens", 15.sp, 600)
                    T("Escolha o tipo, o modelo (material, braço, cor) e a quantidade.", 13.sp, 400, Cores.Suave, Modifier.padding(top = 4.dp))
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        TipoLinha.JOGO to "Jogo (mesa + $cpm cadeiras)",
                        TipoLinha.MESA to "Mesa avulsa",
                        TipoLinha.CADEIRA to "Cadeira avulsa"
                    ).forEach { (tipo, nome) ->
                        Chip(nome, item.tipo == tipo, { vm.itemForm = item.copy(tipo = tipo) }, Modifier.testTag("tipo_${tipo.name}"))
                    }
                }
                if (item.tipo != TipoLinha.CADEIRA) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Rotulo("Modelo da mesa")
                        visiveis.filter { it.ehMesa }.forEach { m ->
                            OpcaoModelo(m, item.mesaId == m.id, precos.peca(m.id).diariaCentavos, livre(m.id) - (c.uso[m.id] ?: 0)) {
                                vm.itemForm = item.copy(mesaId = m.id)
                            }
                        }
                    }
                }
                if (item.tipo != TipoLinha.MESA) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Rotulo(if (item.tipo == TipoLinha.JOGO) "Modelo das cadeiras ($cpm por mesa)" else "Modelo da cadeira")
                        visiveis.filter { !it.ehMesa }.forEach { m ->
                            OpcaoModelo(m, item.cadId == m.id, precos.peca(m.id).diariaCentavos, livre(m.id) - (c.uso[m.id] ?: 0)) {
                                vm.itemForm = item.copy(cadId = m.id)
                            }
                        }
                    }
                }

                val unit = when (item.tipo) {
                    TipoLinha.JOGO -> precos.precoJogo(item.mesaId, item.cadId)
                    TipoLinha.MESA -> precos.peca(item.mesaId).diariaCentavos
                    TipoLinha.CADEIRA -> precos.peca(item.cadId).diariaCentavos
                }
                Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        T("Quantidade", 14.sp, 600)
                        T(
                            (if (item.tipo == TipoLinha.JOGO) "Jogo sai a " else "Unidade a ") + unit.formatAsBrl() + "/dia" +
                                (if (item.tipo == TipoLinha.JOGO) " · usa $qtd mesas e ${qtd * cpm} cadeiras" else ""),
                            12.sp, 400, Cores.Suave, Modifier.padding(top = 2.dp)
                        )
                    }
                    BotaoPasso("−", "Diminuir quantidade", { vm.qtdItem = (qtd - 1).coerceAtLeast(1).toString() })
                    CampoBase(
                        vm.qtdItem, { vm.qtdItem = it.filter { ch -> ch.isDigit() } },
                        Modifier.width(60.dp).testTag("campo_qtd"), teclado = KeyboardType.Number, altura = 44.dp,
                        tamanho = 17.sp, peso = 600, alinhar = TextAlign.Center, paddingH = 4.dp
                    )
                    BotaoPasso("+", "Aumentar quantidade", { vm.qtdItem = (qtd + 1).toString() }, Modifier.testTag("mais_qtd"))
                }
                Botao("Adicionar ao pedido", { vm.adicionarItem() }, Modifier.fillMaxWidth().testTag("botao_adicionar"), estilo = EstiloBotao.SUAVE)

                val linhas = f.linhas(precos)
                if (linhas.isNotEmpty()) {
                    Column {
                        Divisor()
                        linhas.forEachIndexed { i, l ->
                            Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Column(Modifier.weight(1f)) {
                                    T(l.titulo, 14.sp, 600)
                                    T(l.descricao, 12.sp, 400, Cores.Suave, Modifier.padding(top = 2.dp))
                                    T(l.diariaCentavos.formatAsBrl() + "/dia", 12.sp, 400, Cores.Texto2, Modifier.padding(top = 2.dp))
                                }
                                Box(
                                    Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(Cores.Branco)
                                        .border(1.dp, Cores.Borda, RoundedCornerShape(10.dp))
                                        .clickable(role = Role.Button) { vm.removerItem(i) }
                                        .semantics { contentDescription = "Remover " + l.titulo },
                                    contentAlignment = Alignment.Center
                                ) { Icon(Icons.Outlined.DeleteOutline, null, tint = Cores.Laranja, modifier = Modifier.size(18.dp)) }
                            }
                            Divisor()
                        }
                    }

                    var ok = true
                    val disp = c.uso.map { (id, n) ->
                        val liv = livre(id)
                        if (n > liv) ok = false
                        (Catalogo.modelo(id)?.nome ?: id) to "usa $n de ${maxOf(0, liv)} livres"
                    }
                    val (titulo, bom) = when {
                        !f.datasOk -> "Confira as datas" to false
                        ok -> "Tudo disponível de ${Datas.dm(f.entrega)} a ${Datas.dm(f.retirada)}" to true
                        else -> "Falta estoque de algum modelo nessas datas" to false
                    }
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (bom) Cores.VerdeClaro else Cores.LaranjaClaro).padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val cor = if (bom) Cores.VerdeEscuro else Cores.Laranja
                        T(titulo, 13.sp, 600, cor)
                        disp.forEach { (nome, txt) -> LinhaValor(nome, txt, tamanho = 12.sp, cor = cor, pesoValor = 600) }
                    }
                }
            }

            Cartao {
                T("Valores e pagamento", 15.sp, 600)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Campo("Frete (R$)", f.frete, { vm.mudarForm(f.copy(frete = dinheiro(it))) }, Modifier.weight(1f), teclado = KeyboardType.Decimal)
                        Campo("Desconto (R$)", f.desconto, { vm.mudarForm(f.copy(desconto = dinheiro(it))) }, Modifier.weight(1f), teclado = KeyboardType.Decimal)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Campo("Sinal pago (R$)", f.sinal, { vm.mudarForm(f.copy(sinal = dinheiro(it))) }, Modifier.weight(1f), teclado = KeyboardType.Decimal)
                        Campo("Caução (R$)", f.caucao, { vm.mudarForm(f.copy(caucao = dinheiro(it))) }, Modifier.weight(1f), teclado = KeyboardType.Decimal)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Rotulo("Forma de pagamento")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FORMAS_PAGAMENTO.forEach { nome -> Chip(nome, f.pagamento == nome, { vm.mudarForm(f.copy(pagamento = nome)) }) }
                    }
                }
            }
        }

        // Resumo do dinheiro, sempre visivel no rodape.
        Column(
            Modifier.fillMaxWidth().shadow(8.dp).background(Cores.Branco).padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LinhaValor(rotuloAluguel(c.dias, c.diariaCentavos), c.aluguelCentavos.formatAsBrl())
            LinhaValor("Frete", centavos(f.frete).formatAsBrl())
            LinhaValor("Desconto", "− " + centavos(f.desconto).formatAsBrl())
            Column {
                Tracejado()
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    T("Total", 14.sp, 600)
                    Numero(c.totalCentavos.formatAsBrl(), 24.sp, modifier = Modifier.testTag("total_novo"))
                }
            }
            LinhaValor("Saldo a cobrar na entrega (total − sinal)", c.saldoCentavos.formatAsBrl(), pesoValor = 600)
            vm.msg?.takeIf { !it.ok }?.let { Aviso(it.texto, false, vertical = 8.dp) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Botao("Salvar orçamento", { vm.salvar(StatusPedido.ORCAMENTO) }, Modifier.weight(1f).testTag("botao_orcamento"), estilo = EstiloBotao.CONTORNO)
                Botao("Confirmar pedido", { vm.salvar(StatusPedido.CONFIRMADO) }, Modifier.weight(1.3f).testTag("botao_confirmar"))
            }
        }
    }
}

/** So digitos, virgula e ponto: o resto nao vira dinheiro. */
private fun dinheiro(s: String) = s.filter { it.isDigit() || it == ',' || it == '.' }

@Composable
private fun OpcaoModelo(m: Modelo, selecionado: Boolean, diaria: Long, livres: Int, aoTocar: () -> Unit) {
    val forma = RoundedCornerShape(10.dp)
    Row(
        Modifier.fillMaxWidth().clip(forma)
            .background(if (selecionado) Cores.VerdeClaro else Cores.Branco)
            .border(1.dp, if (selecionado) Cores.Verde else Cores.Borda, forma)
            .clickable(role = Role.Button, onClick = aoTocar)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("modelo_${m.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Bolinha(Color(m.cor.argb), 18.dp)
        Column(Modifier.weight(1f)) {
            T(m.nome, 14.sp, 600, if (selecionado) Cores.VerdeEscuro else Cores.Texto)
            T(diaria.formatAsBrl() + "/dia", 12.sp, 400, Cores.Suave)
        }
        T("${maxOf(0, livres)} livres", 12.sp, 600, if (livres > 0) Cores.VerdeEscuro else Cores.Laranja)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CampoData(rotulo: String, data: LocalDate, aoEscolher: (LocalDate) -> Unit, modifier: Modifier) {
    var aberto by remember { mutableStateOf(false) }
    val forma = RoundedCornerShape(10.dp)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Rotulo(rotulo)
        Box(
            Modifier.fillMaxWidth().height(48.dp).clip(forma).background(Cores.Branco).border(1.dp, Cores.BordaCampo, forma)
                .clickable(role = Role.Button) { aberto = true }.padding(horizontal = 14.dp)
                .testTag("data_$rotulo"),
            contentAlignment = Alignment.CenterStart
        ) { T(Datas.formatar(data), 16.sp) }
    }
    if (aberto) {
        val state = rememberDatePickerState(initialSelectedDateMillis = Datas.millisUtcDeData(data))
        DatePickerDialog(
            onDismissRequest = { aberto = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { aoEscolher(Datas.dataDeMillisUtc(it)) }
                    aberto = false
                }) { T("OK", 14.sp, 600, Cores.Verde) }
            },
            dismissButton = { TextButton(onClick = { aberto = false }) { T("Cancelar", 14.sp, 600, Cores.Verde) } }
        ) { DatePicker(state = state) }
    }
}
