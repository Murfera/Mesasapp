package br.com.mesasapp.ui.telas

import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.mesasapp.domain.Catalogo
import br.com.mesasapp.domain.MULTIPLICAR_POR_DIAS
import br.com.mesasapp.domain.StatusPedido
import br.com.mesasapp.domain.fechamento
import br.com.mesasapp.ui.MesasViewModel
import br.com.mesasapp.ui.Tela
import br.com.mesasapp.ui.centavos
import br.com.mesasapp.ui.comum.Aviso
import br.com.mesasapp.ui.comum.Bolinha
import br.com.mesasapp.ui.comum.Botao
import br.com.mesasapp.ui.comum.BotaoPasso
import br.com.mesasapp.ui.comum.Campo
import br.com.mesasapp.ui.comum.Cartao
import br.com.mesasapp.ui.comum.Chip
import br.com.mesasapp.ui.comum.Divisor
import br.com.mesasapp.ui.comum.EstiloBotao
import br.com.mesasapp.ui.comum.LinhaValor
import br.com.mesasapp.ui.comum.Numero
import br.com.mesasapp.ui.comum.PilulaStatus
import br.com.mesasapp.ui.comum.T
import br.com.mesasapp.ui.comum.Titulo
import br.com.mesasapp.ui.comum.Tracejado
import br.com.mesasapp.ui.comum.Voltar
import br.com.mesasapp.ui.theme.Cores
import br.com.mesasapp.util.Datas
import br.com.mesasapp.util.Links
import br.com.mesasapp.util.Pix
import br.com.mesasapp.util.formatAsBrl

/** Margem interna dos cartoes do detalhe: menor que o padrao, para caber mais na tela. */
private val PAD_CARTAO = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 12.dp)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetalheTela(vm: MesasViewModel) {
    val pedidos by vm.pedidos.collectAsState()
    val pixCfg by vm.pix.collectAsState()
    val exibir by vm.pagamento.collectAsState()
    val contexto = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val o = pedidos.firstOrNull { it.id == vm.pedidoSelecionado }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("lista_pedido")
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        // Cartoes mais juntos e mais baixos (pedido do usuario: "ta comendo muito espaco").
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (o == null) { Voltar("Pedidos") { vm.irPara(Tela.PEDIDOS) }; return@Column }

        val c = o.conta
        val editavel = o.status == StatusPedido.ENTREGUE
        val fec = fechamento(o, vm.quebradas, vm.faltando, vm.atraso)
        // Seguem o menu > Exibir. Valor ja gravado continua visivel mesmo com a chave
        // desligada: ele entra no saldo e, escondido, a conta nao fecharia na tela.
        val mostraSinal = exibir.mostrarSinal || o.sinalCentavos > 0
        val mostraCaucao = exibir.mostrarCaucao || o.caucaoCentavos > 0

        // Numero, emissao e situacao na linha do "‹ Pedidos" (pedido do usuario), no canto direito.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Voltar("Pedidos") { vm.irPara(Tela.PEDIDOS) }
            T(
                // Curto para caber numa linha: "#2 · emissão 27/11/2026  14:35" (pedido do usuario).
                "#${o.id}" + (if (o.criadoEmEpochSeg > 0) " · emissão ${Datas.formatarData(o.criadoEmEpochSeg)}  ${Datas.formatarHora(o.criadoEmEpochSeg)}" else ""),
                12.sp, 400, Cores.Suave,
                Modifier.weight(1f).testTag("emitido")
            )
            PilulaStatus(o.status, Modifier.testTag("status_pedido"))
        }
        Titulo(o.cliente)

        // No topo (pedido do usuario). Editar so antes da entrega; excluir sempre, com confirmacao.
        var confirmarExclusao by remember { mutableStateOf(false) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (vm.podeEditar(o)) {
                Botao("Editar pedido", { vm.editarPedido(o) }, Modifier.weight(1f).testTag("botao_editar"), estilo = EstiloBotao.CONTORNO) {
                    Icon(Icons.Outlined.Edit, null, tint = Cores.Destaque, modifier = Modifier.size(18.dp))
                }
            }
            Botao("Excluir pedido", { confirmarExclusao = true }, Modifier.weight(1f).testTag("botao_excluir"), estilo = EstiloBotao.NEUTRO) {
                Icon(Icons.Outlined.Delete, null, tint = Cores.Erro, modifier = Modifier.size(18.dp))
            }
        }
        if (!vm.podeEditar(o)) {
            T("Pedido já entregue não pode ser editado, só excluído.", 12.sp, 400, Cores.Suave)
        }
        // Avisos de sucesso (pedido alterado, confirmado, entregue...) viram um pop-up rapido,
        // sem ocupar uma linha da tela (pedido do usuario). Erro continua fixo na tela.
        LaunchedEffect(vm.msg) {
            vm.msg?.takeIf { it.ok }?.let {
                Toast.makeText(contexto, it.texto, Toast.LENGTH_LONG).show()
                vm.msg = null
            }
        }
        vm.msg?.takeIf { !it.ok }?.let { Aviso(it.texto, false) }

        // Arranjo do usuario: em cima as datas (com o ano) e o botao WhatsApp; embaixo o
        // endereco e, a direita dele, o telefone (tocar abre o discador).
        Cartao(padding = PAD_CARTAO, espaco = 8.dp) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CalendarMonth, null, tint = Cores.Suave, modifier = Modifier.size(18.dp))
                // Numa linha so (pedido do usuario); medido com a fonte Inter: em 11 sp cabe
                // ao lado do botao compacto.
                T(
                    "Entrega ${Datas.formatar(o.entrega)} · Retirada ${Datas.formatar(o.retirada)}",
                    11.sp, 500, Cores.Texto2, Modifier.weight(1f), umaLinha = true
                )
                // Abre a conversa no WhatsApp (nao existe link que ja inicie a chamada por la).
                if (o.telefone.any { it.isDigit() }) {
                    Box(
                        Modifier.height(30.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, Cores.Destaque, RoundedCornerShape(8.dp))
                            .clickable(role = Role.Button) { Links.abrir(contexto, Links.whatsApp(o.telefone, "")) }
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) { T("WhatsApp", 12.sp, 600, Cores.Destaque, umaLinha = true) }
                }
            }
            val temEndereco = o.endereco.isNotBlank() || o.referencia.isNotBlank()
            if (temEndereco || o.telefone.isNotBlank()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (temEndereco) {
                        Icon(Icons.Outlined.Place, null, tint = Cores.Suave, modifier = Modifier.size(18.dp).padding(top = 2.dp))
                        Column(Modifier.weight(1f)) {
                            if (o.endereco.isNotBlank()) T(o.endereco, 14.sp)
                            if (o.referencia.isNotBlank()) T(o.referencia, 13.sp, 400, Cores.Suave, Modifier.padding(top = 2.dp))
                        }
                    } else {
                        Box(Modifier.weight(1f))
                    }
                    if (o.telefone.isNotBlank()) {
                        Row(
                            Modifier.clickable(role = Role.Button) { Links.ligar(contexto, o.telefone) },
                            horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Call, null, tint = Cores.Destaque, modifier = Modifier.size(16.dp))
                            T(o.telefone, 14.sp, 600, Cores.Destaque, umaLinha = true)
                        }
                    }
                }
            }
        }

        Cartao(padding = PAD_CARTAO, espaco = 6.dp) {
            T("Itens e valores", 15.sp, 600, modifier = Modifier.padding(bottom = 2.dp))
            o.linhas.forEach { l ->
                Column {
                    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f)) {
                            T(l.titulo, 14.sp, 600)
                            T(l.descricao, 12.sp, 400, Cores.Suave, Modifier.padding(top = 2.dp))
                        }
                        T(l.diariaCentavos.formatAsBrl() + "/dia", 13.sp, 400, Cores.Texto2)
                    }
                    Divisor()
                }
            }
            LinhaValor(if (MULTIPLICAR_POR_DIAS) "Aluguel (${diariasTexto(c.dias)})" else "Aluguel (valor de 1 diária)", c.aluguelCentavos.formatAsBrl())
            LinhaValor("Frete", o.freteCentavos.formatAsBrl())
            LinhaValor("Desconto", "− " + o.descontoCentavos.formatAsBrl())
            Column {
                Tracejado()
                LinhaValor("Total do aluguel", c.totalCentavos.formatAsBrl(), Modifier.padding(top = 6.dp), 14.sp, 600, Cores.Texto)
            }
            // A forma de pagamento aparece sempre, com ou sem sinal e caucao.
            LinhaValor("Forma de pagamento", o.pagamento)
            if (mostraSinal) LinhaValor("Sinal pago", o.sinalCentavos.formatAsBrl())
            if (mostraCaucao) LinhaValor("Caução recebida", o.caucaoCentavos.formatAsBrl())
        }

        if (o.status == StatusPedido.ENTREGUE || o.status == StatusPedido.DEVOLVIDO) {
            Cartao(padding = androidx.compose.foundation.layout.PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 2.dp), espaco = 0.dp) {
                T("Vistoria na retirada", 15.sp, 600)
                T(
                    "Conte por modelo o que voltou. Quebradas e faltando saem do estoque; ajuste a quantidade quando repor.",
                    13.sp, 400, Cores.Suave, Modifier.padding(top = 4.dp, bottom = 6.dp)
                )
                o.pecas.forEach { peca ->
                    val m = Catalogo.modelo(peca.modeloId)
                    Column(Modifier.padding(top = 4.dp)) {
                        Divisor()
                        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Bolinha(Color(m?.cor?.argb ?: 0xFFE5E7E6), 14.dp)
                            T(m?.nome ?: peca.modeloId, 14.sp, 600)
                        }
                        T("${peca.quantidade} no pedido · reposição ${peca.reposicaoCentavos.formatAsBrl()} cada", 12.sp, 400, Cores.Suave, Modifier.padding(start = 22.dp, top = 2.dp))
                        val plural = if (m?.ehMesa == true) "mesas" else "cadeiras"
                        LinhaPasso("Quebradas", vm.quebradas[peca.modeloId] ?: 0, editavel, "$plural quebradas", "quebradas_${peca.modeloId}") { d ->
                            vm.passoVistoria(true, peca.modeloId, d, peca.quantidade)
                        }
                        LinhaPasso("Faltando", vm.faltando[peca.modeloId] ?: 0, editavel, "$plural faltando", "faltando_${peca.modeloId}") { d ->
                            vm.passoVistoria(false, peca.modeloId, d, peca.quantidade)
                        }
                    }
                }
                Divisor(Modifier.padding(top = 4.dp))
                Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        T("Dias de atraso", 14.sp, 600)
                        T(c.diariaCentavos.formatAsBrl() + " por dia a mais", 12.sp, 400, Cores.Suave, Modifier.padding(top = 2.dp))
                    }
                    if (editavel) BotaoPasso("−", "Diminuir dias de atraso", { vm.passoAtraso(-1) })
                    T(vm.atraso.toString(), 17.sp, 600, modifier = Modifier.width(36.dp), alinhar = TextAlign.Center)
                    if (editavel) BotaoPasso("+", "Aumentar dias de atraso", { vm.passoAtraso(1) })
                }
            }

            Cartao(padding = PAD_CARTAO, espaco = 6.dp, borda = BorderStroke(2.dp, Cores.Destaque)) {
                T("Total refeito", 15.sp, 600, modifier = Modifier.padding(bottom = 2.dp))
                LinhaValor("Total do aluguel", c.totalCentavos.formatAsBrl())
                LinhaValor("+ Avarias e faltas", fec.avariasCentavos.formatAsBrl())
                LinhaValor("+ Atraso na devolução", fec.atrasoCentavos.formatAsBrl())
                LinhaValor("Novo total", fec.finalCentavos.formatAsBrl(), tamanho = 14.sp, peso = 600, cor = Cores.Texto)
                if (mostraSinal || mostraCaucao) {
                    val rotulo = when {
                        mostraSinal && mostraCaucao -> "− Sinal e caução já recebidos"
                        mostraSinal -> "− Sinal já recebido"
                        else -> "− Caução já recebida"
                    }
                    LinhaValor(rotulo, "− " + fec.abatidoCentavos.formatAsBrl())
                }
                Column {
                    Tracejado()
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        T(if (fec.restoCentavos >= 0) "A receber do cliente" else "Devolver ao cliente", 14.sp, 600)
                        Numero(
                            kotlin.math.abs(fec.restoCentavos).formatAsBrl(), 24.sp,
                            if (fec.restoCentavos >= 0) Cores.VerdeEscuro else Cores.Laranja, Modifier.testTag("resto")
                        )
                    }
                }
            }
        }

        // --- Pix ---
        Cartao(padding = PAD_CARTAO, espaco = 10.dp) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.QrCode2, null, tint = Cores.Destaque, modifier = Modifier.size(22.dp))
                Column {
                    T("Cobrar com Pix", 15.sp, 600)
                    T(if (exibir.mostrarQrCode) "QR Code e copia e cola para enviar ao cliente." else "Copia e cola para enviar ao cliente.", 13.sp, 400, Cores.Suave, Modifier.padding(top = 2.dp))
                }
            }
            val temChave = pixCfg.chave.isNotBlank()
            if (!temChave) {
                Aviso("Cadastre sua chave Pix no menu ⋮ → Config. Pix para gerar a cobrança.", false)
            } else {
                // Um valor so, calculado do pedido (pedido do usuario): o cliente paga na
                // retirada, entao depois da entrega ja entram quebras, faltas e atraso.
                // Sinal e caucao, se ligados em Exibir, ja vem descontados.
                val aposVistoria = o.status == StatusPedido.ENTREGUE || o.status == StatusPedido.DEVOLVIDO
                val rotulo = if (aposVistoria) "Valor após vistoria" else "Valor do pedido"
                val valor = maxOf(0L, if (aposVistoria) fec.restoCentavos else c.saldoCentavos)
                if (valor > 0) {
                    val codigo = remember(pixCfg, valor, o.id) {
                        Pix.copiaECola(pixCfg.chave, pixCfg.nome, pixCfg.cidade, valor, "PED${o.id}")
                    }
                    val qr = if (exibir.mostrarQrCode) remember(codigo) { Links.qr(codigo) } else null
                    val nome = Pix.limpar(pixCfg.nome, 25).ifBlank { "RECEBEDOR" }
                    val cidade = Pix.limpar(pixCfg.cidade, 15).ifBlank { "CIDADE" }
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Cores.QrFundo)
                            .border(1.dp, Cores.Borda, RoundedCornerShape(12.dp)).padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        T(rotulo + " · pedido #${o.id}", 13.sp, 400, Cores.Suave)
                        Numero(valor.formatAsBrl(), 28.sp, modifier = Modifier.testTag("pix_valor"))
                        // Menu > Exibir > QR Code: desligado, fica so o copia e cola.
                        if (!exibir.mostrarQrCode) Unit
                        else if (qr != null) {
                            Image(
                                qr, "QR Code do Pix",
                                Modifier.size(208.dp).clip(RoundedCornerShape(8.dp)).background(Color.White),
                                filterQuality = FilterQuality.None
                            )
                        } else {
                            T("QR Code indisponível aqui; use o copia e cola.", 13.sp, 400, Cores.Suave)
                        }
                        T("$nome · $cidade · chave ${pixCfg.chave}", 12.sp, 400, Cores.Suave, alinhar = TextAlign.Center)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        T("Pix copia e cola", 13.sp, 500, Cores.Texto2)
                        T(
                            codigo, 11.sp, 400, Cores.Texto2,
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Cores.Cinza)
                                .padding(horizontal = 12.dp, vertical = 10.dp).testTag("pix_codigo"),
                            fonte = FontFamily.Monospace
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Botao(
                            if (vm.pixCopiado) "Copiado!" else "Copiar código",
                            { clipboard.setText(AnnotatedString(codigo)); vm.pixCopiado = true },
                            Modifier.weight(1f), estilo = EstiloBotao.CONTORNO
                        )
                        // Mensagem enxuta (pedido do usuario): o essencial, com o codigo no fim para copiar.
                        val texto = "*Aluguel de mesas e cadeiras*\n" +
                            "Pedido #${o.id}" + emitido(o.criadoEmEpochSeg)?.let { " · $it" }.orEmpty() + "\n" +
                            "Entrega: ${Datas.dm(o.entrega)} · Retirada: ${Datas.dm(o.retirada)}\n" +
                            "Valor: ${valor.formatAsBrl()}\n" +
                            "Recebedor: ${pixCfg.nome.trim().ifBlank { nome }}\n\n" +
                            "Pix copia e cola:\n" + codigo
                        Botao("Enviar no WhatsApp", { Links.abrir(contexto, Links.whatsApp(o.telefone, texto)) }, Modifier.weight(1.3f)) {
                            Icon(Icons.Outlined.Chat, null, tint = Cores.SobreVerde, modifier = Modifier.size(18.dp))
                        }
                    }
                } else {
                    T("Nada a cobrar neste pedido.", 13.sp, 400, Cores.Suave)
                }
            }
        }

        val acao = when (o.status) {
            StatusPedido.ORCAMENTO -> "Confirmar pedido"
            StatusPedido.CONFIRMADO -> "Marcar como entregue"
            StatusPedido.ENTREGUE -> "Confirmar devolução"
            StatusPedido.DEVOLVIDO -> null
        }
        if (acao != null) {
            Botao(acao, { vm.acaoDoStatus(o) }, Modifier.fillMaxWidth().testTag("botao_acao"), altura = 52.dp, tamanho = 15.sp)
        }

        if (confirmarExclusao) {
            val aviso = when (o.status) {
                StatusPedido.CONFIRMADO, StatusPedido.ENTREGUE -> "As peças reservadas voltam a ficar livres nessas datas."
                StatusPedido.DEVOLVIDO -> "As peças quebradas ou faltando que já saíram do estoque não voltam; ajuste no Estoque se precisar."
                StatusPedido.ORCAMENTO -> "O orçamento some da lista."
            }
            AlertDialog(
                onDismissRequest = { confirmarExclusao = false },
                containerColor = Cores.Superficie,
                title = { T("Excluir o pedido #${o.id}?", 17.sp, 600) },
                text = { T("${o.cliente}. $aviso Não dá para desfazer.", 14.sp, 400, Cores.Texto2) },
                confirmButton = {
                    TextButton({ confirmarExclusao = false; vm.excluirPedido(o) }, Modifier.testTag("confirmar_exclusao")) {
                        T("Excluir", 14.sp, 600, Cores.Erro)
                    }
                },
                dismissButton = {
                    TextButton({ confirmarExclusao = false }) { T("Cancelar", 14.sp, 600, Cores.Destaque) }
                }
            )
        }
    }
}

/** "emitido em 26/11/2026 às 14:35" (pedido antigo sem a hora gravada: null). */
private fun emitido(epochSeg: Long): String? =
    if (epochSeg <= 0) null else "emitido em ${Datas.formatarData(epochSeg)} às ${Datas.formatarHora(epochSeg)}"

@Composable
private fun LinhaPasso(rotulo: String, valor: Int, editavel: Boolean, descricao: String, tag: String, passo: (Int) -> Unit) {
    Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        T(rotulo, 14.sp, 400, Cores.Texto2, Modifier.weight(1f))
        if (editavel) BotaoPasso("−", "Diminuir $descricao", { passo(-1) })
        T(valor.toString(), 17.sp, 600, modifier = Modifier.width(36.dp).testTag(tag), alinhar = TextAlign.Center)
        if (editavel) BotaoPasso("+", "Aumentar $descricao", { passo(1) }, Modifier.testTag("mais_$tag"))
    }
}
