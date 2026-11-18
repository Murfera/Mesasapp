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
import br.com.mesasapp.ui.comum.Voltar
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.outlined.ContactPage
import androidx.compose.material.icons.outlined.CheckCircle
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
import br.com.mesasapp.domain.FiltroMaterial
import br.com.mesasapp.domain.Precos
import br.com.mesasapp.domain.Peca
import br.com.mesasapp.domain.Material
import br.com.mesasapp.domain.Cor
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

    if (vm.dadosClienteAbertos) DadosClienteDialogo(vm)

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).testTag("lista_novo")
                .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Titulo("Novo pedido")

            Cartao {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    T("Cliente", 15.sp, 600, modifier = Modifier.weight(1f))
                    BotaoDadosCliente(f) { vm.dadosClienteAbertos = true }
                }
                Campo("Nome", f.cliente, { vm.mudarForm(f.copy(cliente = it)) }, placeholder = "Nome completo", tag = "campo_cliente")
            }

            Cartao {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    T("Datas", 15.sp, 600, modifier = Modifier.weight(1f))
                    T(diariasTexto(c.dias), 13.sp, 600, Cores.Suave)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CampoData("Entrega", f.entrega, { vm.mudarForm(f.copy(entrega = it)) }, Modifier.weight(1f))
                    CampoData("Retirada", f.retirada, { vm.mudarForm(f.copy(retirada = it)) }, Modifier.weight(1f))
                }
            }

            Cartao {
                T("Itens", 15.sp, 600)
                // O que vai no jogo (1 mesa + N cadeiras) aparece na linha da quantidade.
                val tipos = listOf(TipoLinha.JOGO, TipoLinha.MESA, TipoLinha.CADEIRA)
                Segmentos(listOf("Jogo", "Mesa", "Cadeira"), tipos.indexOf(item.tipo), "tipo", destaque = true) { i ->
                    vm.itemForm = item.copy(tipo = tipos[i])
                }
                SeletorModelo(vm, filtro, precos) { id -> livre(id) - (c.uso[id] ?: 0) }

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
                                    Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(Cores.Superficie)
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
            Modifier.fillMaxWidth().shadow(8.dp).background(Cores.Superficie).padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 16.dp),
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

/**
 * Escolha do modelo em poucos toques: material (aba), braco (so plastico, so
 * cadeira) e um cartao por cor com quanto tem livre nas datas do pedido. A cor
 * vale para a mesa e as cadeiras do jogo.
 */
@Composable
private fun SeletorModelo(vm: MesasViewModel, filtro: FiltroMaterial, precos: Precos, livres: (String) -> Int) {
    val item = vm.itemForm
    val atual = Catalogo.modelo(if (item.tipo == TipoLinha.CADEIRA) item.cadId else item.mesaId) ?: return
    val mat = filtro.material ?: atual.material
    val braco = Catalogo.modelo(item.cadId)?.peca?.braco == true && mat == Material.PLASTICO

    fun escolher(m: Material, cor: Cor, comBraco: Boolean) {
        val cadeira = if (comBraco && m == Material.PLASTICO) Peca.CADEIRA_BRACO else Peca.CADEIRA
        vm.itemForm = item.copy(mesaId = Modelo(m, Peca.MESA, cor).id, cadId = Modelo(m, cadeira, cor).id)
    }

    if (filtro == FiltroMaterial.TODOS) {
        Segmentos(Material.entries.map { it.rotulo }, Material.entries.indexOf(mat), "material") { i ->
            escolher(Material.entries[i], atual.cor, braco)
        }
    }
    if (item.tipo != TipoLinha.MESA && mat == Material.PLASTICO) {
        Segmentos(listOf("Cadeira sem braço", "Com braço"), if (braco) 1 else 0, "braco") { i ->
            escolher(mat, atual.cor, i == 1)
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Cor.entries.forEach { cor ->
            val mesa = Modelo(mat, Peca.MESA, cor).id
            val cad = Modelo(mat, if (braco) Peca.CADEIRA_BRACO else Peca.CADEIRA, cor).id
            val n = when (item.tipo) {
                TipoLinha.JOGO -> minOf(livres(mesa), livres(cad) / precos.cadeirasPorMesa(mat, cor))
                TipoLinha.MESA -> livres(mesa)
                TipoLinha.CADEIRA -> livres(cad)
            }.coerceAtLeast(0)
            val sel = cor == atual.cor && mat == atual.material
            val forma = RoundedCornerShape(14.dp)
            Column(
                Modifier.weight(1f).clip(forma)
                    .background(if (sel) Cores.VerdeClaro else Cores.Superficie)
                    .border(if (sel) 2.dp else 1.dp, if (sel) Cores.Destaque else Cores.Borda, forma)
                    .clickable(role = Role.Button) { escolher(mat, cor, braco) }
                    .padding(horizontal = 10.dp, vertical = 12.dp)
                    .testTag("cor_${cor.name}"),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Bolinha(Color(cor.argb))
                    T(cor.rotulo, 13.sp, 600, umaLinha = true)
                }
                Numero(n.toString(), 26.sp, if (n > 0) Cores.Texto else Cores.Laranja)
                T(if (item.tipo == TipoLinha.JOGO) "jogos livres" else "livres", 11.sp, 400, Cores.Suave, umaLinha = true)
            }
        }
    }
}

/** Escolha de duas opcoes lado a lado, no mesmo estilo das abas do Estoque. */
@Composable
private fun Segmentos(opcoes: List<String>, selecionada: Int, tag: String, destaque: Boolean = false, aoEscolher: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Cores.Abas).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        opcoes.forEachIndexed { i, nome ->
            val sel = i == selecionada
            Box(
                Modifier.weight(1f).height(40.dp)
                    .then(if (sel) Modifier.shadow(1.dp, RoundedCornerShape(9.dp)) else Modifier)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (sel) (if (destaque) Cores.Verde else Cores.Superficie) else Color.Transparent)
                    .clickable(role = Role.Tab) { aoEscolher(i) }
                    .testTag("${tag}_$i"),
                contentAlignment = Alignment.Center
            ) {
                val cor = when { sel && destaque -> Cores.SobreVerde; sel -> Cores.Texto; else -> Cores.Suave }
                T(nome, 13.sp, if (sel) 600 else 500, cor, umaLinha = true)
            }
        }
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
            Modifier.fillMaxWidth().height(48.dp).clip(forma).background(Cores.Superficie).border(1.dp, Cores.BordaCampo, forma)
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
                }) { T("OK", 14.sp, 600, Cores.Destaque) }
            },
            dismissButton = { TextButton(onClick = { aberto = false }) { T("Cancelar", 14.sp, 600, Cores.Destaque) } }
        ) { DatePicker(state = state) }
    }
}

/**
 * Botaozinho do cartao Cliente: abre WhatsApp, CPF/CNPJ, endereco e referencia.
 * Mostra quantos dos obrigatorios (WhatsApp e endereco) faltam, para nao esconder
 * o que o "Salvar" vai cobrar.
 */
@Composable
private fun BotaoDadosCliente(f: br.com.mesasapp.ui.FormPedido, aoTocar: () -> Unit) {
    val faltam = listOf(f.telefone, f.endereco).count { it.isBlank() }
    val forma = RoundedCornerShape(18.dp)
    Row(
        Modifier.height(36.dp).clip(forma)
            .background(if (faltam == 0) Cores.VerdeClaro else Cores.Superficie)
            .border(1.dp, if (faltam == 0) Cores.Destaque else Cores.BordaCampo, forma)
            .clickable(role = Role.Button, onClick = aoTocar)
            .padding(horizontal = 12.dp)
            .testTag("botao_dados_cliente"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            if (faltam == 0) Icons.Outlined.CheckCircle else Icons.Outlined.ContactPage, null,
            tint = if (faltam == 0) Cores.VerdeEscuro else Cores.Texto, modifier = Modifier.size(16.dp)
        )
        T(
            when (faltam) { 0 -> "Dados ok"; else -> "Contato e endereço" },
            13.sp, 600, if (faltam == 0) Cores.VerdeEscuro else Cores.Texto, umaLinha = true
        )
    }
}

/** Tela cheia por cima do Novo pedido (Dialog, nao AlertDialog: AlertDialog com rolagem trava). */
@Composable
private fun DadosClienteDialogo(vm: MesasViewModel) {
    val f = vm.form
    Dialog(
        onDismissRequest = { vm.dadosClienteAbertos = false },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(Modifier.fillMaxSize().background(Cores.Fundo).imePadding()) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Voltar("Novo pedido") { vm.dadosClienteAbertos = false }
                Titulo("Dados do cliente")
                if (f.cliente.isNotBlank()) T(f.cliente, 14.sp, 400, Cores.Suave)
                vm.msg?.takeIf { !it.ok }?.let { Aviso(it.texto, false) }
                Cartao {
                    Campo("Telefone / WhatsApp", f.telefone, { vm.mudarForm(f.copy(telefone = it)) }, placeholder = "(00) 00000-0000", teclado = KeyboardType.Phone, tag = "campo_telefone")
                    Campo("CPF ou CNPJ (para o contrato)", f.documento, { vm.mudarForm(f.copy(documento = it)) }, placeholder = "Opcional")
                }
                Cartao {
                    T("Local da entrega", 15.sp, 600)
                    Campo("Endereço", f.endereco, { vm.mudarForm(f.copy(endereco = it)) }, placeholder = "Rua, número, bairro", tag = "campo_endereco")
                    Campo("Ponto de referência / observações", f.referencia, { vm.mudarForm(f.copy(referencia = it)) }, placeholder = "Ex.: portão lateral, 2º andar sem elevador")
                }
            }
            Column(Modifier.fillMaxWidth().background(Cores.Superficie).padding(horizontal = 20.dp, vertical = 14.dp)) {
                Botao("Pronto", { vm.dadosClienteAbertos = false }, Modifier.fillMaxWidth().testTag("botao_dados_pronto"))
            }
        }
    }
}
