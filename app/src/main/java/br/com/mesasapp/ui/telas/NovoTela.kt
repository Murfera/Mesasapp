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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import kotlinx.coroutines.launch

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
    val pagamento by vm.pagamento.collectAsState()
    val f = vm.form
    val item = vm.itemForm
    val c = f.conta(precos)
    val res = reservado(pedidos, f.inicioEpochSeg, f.fimEpochSeg)
    val visiveis = Catalogo.modelos.filter { it.material in filtro.materiais() }
    fun livre(id: String) = (estoque[id] ?: 0) - (res[id] ?: 0)
    val cpm = precos.cadeirasPorMesa(item.mesaId)
    val qtd = vm.qtdItem.toIntOrNull()?.coerceAtLeast(1) ?: 1
    val rolagem = rememberScrollState()
    val focoCliente = remember { FocusRequester() }
    val teclado = LocalSoftwareKeyboardController.current
    val escopo = rememberCoroutineScope()
    var nomeFaltando by remember { mutableStateOf(false) }
    val verItens = remember { BringIntoViewRequester() }
    val focos = LocalFocusManager.current
    val semItens = c.mesas + c.cadeiras == 0
    var itensFaltando by remember { mutableStateOf(false) }
    // Some sozinho assim que entra o primeiro item.
    val erroItens = itensFaltando && semItens

    if (vm.dadosClienteAbertos) DadosClienteDialogo(vm)

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rolagem).testTag("lista_novo")
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Titulo("Novo pedido")

            Cartao(espaco = 10.dp) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    T("Cliente", 15.sp, 600, modifier = Modifier.weight(1f))
                    BotaoDadosCliente(f) { vm.dadosClienteAbertos = true }
                }
                // Sem o rotulo "Nome": o texto de exemplo dentro do campo ja diz o que vai ali.
                CampoBase(
                    f.cliente,
                    {
                        if (it.isNotBlank()) nomeFaltando = false
                        vm.mudarForm(f.copy(cliente = it))
                    },
                    Modifier.fillMaxWidth().focusRequester(focoCliente).testTag("campo_cliente"),
                    placeholder = "Nome do cliente", erro = nomeFaltando
                )
                if (nomeFaltando) T("Informe o nome do cliente", 12.sp, 600, Cores.Erro)
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
                SeletorModelo(vm, filtro, precos, estoque) { id -> livre(id) - (c.uso[id] ?: 0) }

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
                Column(Modifier.bringIntoViewRequester(verItens), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Botao(
                        "Adicionar ao pedido", { vm.adicionarItem() }, Modifier.fillMaxWidth().testTag("botao_adicionar"),
                        estilo = EstiloBotao.SUAVE, erro = erroItens
                    )
                    if (erroItens) T("Adicione pelo menos um item ao pedido", 12.sp, 600, Cores.Erro)
                }

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
                    // Sinal e caucao so aparecem se ligados em menu > Exibir.
                    if (pagamento.mostrarSinal || pagamento.mostrarCaucao) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (pagamento.mostrarSinal) Campo("Sinal pago (R$)", f.sinal, { vm.mudarForm(f.copy(sinal = dinheiro(it))) }, Modifier.weight(1f), teclado = KeyboardType.Decimal)
                            if (pagamento.mostrarCaucao) Campo("Caução (R$)", f.caucao, { vm.mudarForm(f.copy(caucao = dinheiro(it))) }, Modifier.weight(1f), teclado = KeyboardType.Decimal)
                            if (!(pagamento.mostrarSinal && pagamento.mostrarCaucao)) Box(Modifier.weight(1f))
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Rotulo("Forma de pagamento")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FORMAS_PAGAMENTO.forEach { nome -> Chip(nome, f.pagamento == nome, { vm.mudarForm(f.copy(pagamento = nome)) }) }
                    }
                }
            }

            // Sem rodape fixo: o botao fica no fim do formulario e abre o resumo.
            Botao(
                "Confirmar pedido · " + c.totalCentavos.formatAsBrl(),
                {
                    vm.msg = null
                    // Faltando nome ou item, o resumo nem abre: marca em vermelho o que falta
                    // e leva ate o primeiro (nome antes, depois o botao de adicionar).
                    if (f.cliente.isBlank()) nomeFaltando = true
                    if (semItens) itensFaltando = true
                    if (f.cliente.isBlank()) {
                        escopo.launch {
                            rolagem.animateScrollTo(0)
                            focoCliente.requestFocus()
                            teclado?.show()
                        }
                    } else if (semItens) {
                        focos.clearFocus()
                        escopo.launch { verItens.bringIntoView() }
                    } else {
                        vm.resumoAberto = true
                    }
                },
                Modifier.fillMaxWidth().testTag("botao_resumo"), altura = 52.dp, tamanho = 15.sp
            )
        }
    }

    if (vm.resumoAberto) ResumoPedido(vm, c, f)
}

/** Painel que sobe de baixo: valores do pedido e as duas formas de salvar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResumoPedido(vm: MesasViewModel, c: br.com.mesasapp.domain.Conta, f: br.com.mesasapp.ui.FormPedido) {
    ModalBottomSheet(
        onDismissRequest = { vm.resumoAberto = false },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Cores.Superficie
    ) {
        Column(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            T("Resumo do pedido", 17.sp, 600, modifier = Modifier.padding(bottom = 4.dp))
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
            // Sem o campo de sinal, o saldo e igual ao total: a linha so repetiria o numero.
            if (vm.pagamento.collectAsState().value.mostrarSinal) {
                LinhaValor("Saldo a cobrar na entrega (total − sinal)", c.saldoCentavos.formatAsBrl(), pesoValor = 600)
            }
            vm.msg?.takeIf { !it.ok }?.let { Aviso(it.texto, false, vertical = 8.dp) }
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Botao("Salvar orçamento", { vm.salvar(StatusPedido.ORCAMENTO) }, Modifier.weight(1f).testTag("botao_orcamento"), estilo = EstiloBotao.CONTORNO)
                Botao("Emitir pedido", { vm.salvar(StatusPedido.CONFIRMADO) }, Modifier.weight(1.3f).testTag("botao_confirmar"))
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
private fun SeletorModelo(vm: MesasViewModel, filtro: FiltroMaterial, precos: Precos, estoque: Map<String, Int>, livres: (String) -> Int) {
    val item = vm.itemForm
    val atual = Catalogo.modelo(if (item.tipo == TipoLinha.CADEIRA) item.cadId else item.mesaId) ?: return
    val mat = filtro.material ?: atual.material
    val braco = Catalogo.modelo(item.cadId)?.peca?.braco == true && mat == Material.PLASTICO

    fun escolher(m: Material, cor: Cor, comBraco: Boolean) {
        val cadeira = if (comBraco && m == Material.PLASTICO) Peca.CADEIRA_BRACO else Peca.CADEIRA
        vm.itemForm = item.copy(mesaId = Modelo(m, Peca.MESA, cor).id, cadId = Modelo(m, cadeira, cor).id)
    }

    // So aparece o que existe no estoque: cores, materiais e cadeira com/sem braco.
    // Cor com estoque mas sem nada livre nas datas continua aparecendo (0 em laranja).
    fun cadeiraDe(m: Material, comBraco: Boolean, cor: Cor) =
        Modelo(m, if (comBraco && m == Material.PLASTICO) Peca.CADEIRA_BRACO else Peca.CADEIRA, cor).id
    fun coresDe(m: Material, comBraco: Boolean): List<Cor> = Cor.entries.filter { cor ->
        val mesas = estoque[Modelo(m, Peca.MESA, cor).id] ?: 0
        val cadeiras = estoque[cadeiraDe(m, comBraco, cor)] ?: 0
        when (item.tipo) {
            TipoLinha.JOGO -> mesas > 0 && cadeiras >= precos.cadeirasPorMesa(m, cor)
            TipoLinha.MESA -> mesas > 0
            TipoLinha.CADEIRA -> cadeiras > 0
        }
    }
    fun bracosDe(m: Material): List<Boolean> =
        if (item.tipo == TipoLinha.MESA || m != Material.PLASTICO) listOf(false)
        else listOf(false, true).filter { coresDe(m, it).isNotEmpty() }
    val materiais = (filtro.material?.let { listOf(it) } ?: Material.entries).filter { m -> bracosDe(m).any { coresDe(m, it).isNotEmpty() } }
    val bracos = bracosDe(mat)

    // Se o que esta marcado nao existe no estoque, marca o que existe (material, braco).
    LaunchedEffect(materiais, bracos, mat, braco) {
        if (materiais.isNotEmpty() && mat !in materiais) {
            val m = materiais.first()
            escolher(m, atual.cor, bracosDe(m).firstOrNull() ?: false)
        } else if (bracos.isNotEmpty() && braco !in bracos) {
            escolher(mat, atual.cor, bracos.first())
        }
    }

    if (materiais.size > 1) {
        Segmentos(materiais.map { it.rotulo }, materiais.indexOf(mat).coerceAtLeast(0), "material") { i ->
            val m = materiais[i]
            escolher(m, atual.cor, if (braco && true in bracosDe(m)) true else bracosDe(m).firstOrNull() ?: false)
        }
    }
    // Ordem pedida pelo usuario: material primeiro, depois Jogo | Mesa | Cadeira.
    // O que vai no jogo (1 mesa + N cadeiras) aparece na linha da quantidade.
    val tipos = listOf(TipoLinha.JOGO, TipoLinha.MESA, TipoLinha.CADEIRA)
    Segmentos(listOf("Jogo", "Mesa", "Cadeira"), tipos.indexOf(item.tipo), "tipo") { i ->
        vm.itemForm = item.copy(tipo = tipos[i])
    }
    if (bracos.size > 1) {
        Segmentos(listOf("Cadeira sem braço", "Com braço"), if (braco) 1 else 0, "braco") { i ->
            escolher(mat, atual.cor, i == 1)
        }
    }
    val cores = coresDe(mat, braco)
    // Se a cor marcada sumiu (sem estoque), marca a primeira que aparece.
    LaunchedEffect(cores, atual.cor) {
        if (cores.isNotEmpty() && atual.cor !in cores) escolher(mat, cores.first(), braco)
    }
    if (cores.isEmpty()) {
        T("Nenhuma cor com estoque aqui. Cadastre as quantidades na aba Estoque.", 13.sp, 400, Cores.Suave)
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        cores.forEach { cor ->
            val mesa = Modelo(mat, Peca.MESA, cor).id
            val cad = cadeiraDe(mat, braco, cor)
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
        repeat(3 - cores.size) { Box(Modifier.weight(1f)) }
    }
}

/** Escolha de duas opcoes lado a lado, no mesmo estilo das abas do Estoque. */
@Composable
private fun Segmentos(opcoes: List<String>, selecionada: Int, tag: String, aoEscolher: (Int) -> Unit) {
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
                    .then(if (sel) Modifier.background(Cores.GradienteVerde) else Modifier)
                    .clickable(role = Role.Tab) { aoEscolher(i) }
                    .testTag("${tag}_$i"),
                contentAlignment = Alignment.Center
            ) {
                val cor = if (sel) Cores.SobreVerde else Cores.Suave
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
 * Fica verde, com um visto, quando ha algum dado preenchido (todos sao opcionais).
 */
@Composable
private fun BotaoDadosCliente(f: br.com.mesasapp.ui.FormPedido, aoTocar: () -> Unit) {
    // Tudo aqui e opcional: fica verde quando ha algum dado preenchido.
    val faltam = if (listOf(f.telefone, f.documento, f.endereco, f.referencia).any { it.isNotBlank() }) 0 else 1
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
            "Contato e endereço",
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
