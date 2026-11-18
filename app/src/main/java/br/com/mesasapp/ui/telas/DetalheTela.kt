package br.com.mesasapp.ui.telas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.input.KeyboardType
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetalheTela(vm: MesasViewModel) {
    val pedidos by vm.pedidos.collectAsState()
    val pixCfg by vm.pix.collectAsState()
    val contexto = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val o = pedidos.firstOrNull { it.id == vm.pedidoSelecionado }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("lista_pedido")
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Voltar("Pedidos") { vm.irPara(Tela.PEDIDOS) }
        if (o == null) return@Column

        val c = o.conta
        val editavel = o.status == StatusPedido.ENTREGUE
        val fec = fechamento(o, vm.quebradas, vm.faltando, vm.atraso)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                T("Pedido #${o.id}", 13.sp, 400, Cores.Suave)
                Titulo(o.cliente, modifier = Modifier.padding(top = 2.dp))
            }
            PilulaStatus(o.status, Modifier.testTag("status_pedido"))
        }

        vm.msg?.let { Aviso(it.texto, it.ok) }

        Cartao(espaco = 10.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.Place, null, tint = Cores.Suave, modifier = Modifier.size(18.dp).padding(top = 2.dp))
                Column {
                    T(o.endereco, 14.sp)
                    T(o.referencia.ifBlank { "Sem ponto de referência" }, 13.sp, 400, Cores.Suave, Modifier.padding(top = 2.dp))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CalendarMonth, null, tint = Cores.Suave, modifier = Modifier.size(18.dp))
                T("Entrega ${Datas.dm(o.entrega)} · Retirada ${Datas.dm(o.retirada)}", 14.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Call, null, tint = Cores.Suave, modifier = Modifier.size(18.dp))
                T(o.telefone, 14.sp, 600, Cores.Destaque, Modifier.clickable(role = Role.Button) { Links.ligar(contexto, o.telefone) })
            }
        }

        Cartao(espaco = 8.dp) {
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
            T("Sai do estoque: ${c.mesas} mesas e ${c.cadeiras} cadeiras", 13.sp, 400, Cores.Suave, Modifier.padding(bottom = 6.dp))
            LinhaValor(if (MULTIPLICAR_POR_DIAS) "Aluguel (${diariasTexto(c.dias)})" else "Aluguel (valor de 1 diária)", c.aluguelCentavos.formatAsBrl())
            LinhaValor("Frete", o.freteCentavos.formatAsBrl())
            LinhaValor("Desconto", "− " + o.descontoCentavos.formatAsBrl())
            Column {
                Tracejado()
                LinhaValor("Total do aluguel", c.totalCentavos.formatAsBrl(), Modifier.padding(top = 6.dp), 14.sp, 600, Cores.Texto)
            }
            LinhaValor("Sinal pago (${o.pagamento})", o.sinalCentavos.formatAsBrl())
            LinhaValor("Caução recebida", o.caucaoCentavos.formatAsBrl())
        }

        if (o.status == StatusPedido.ENTREGUE || o.status == StatusPedido.DEVOLVIDO) {
            Cartao(padding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp), espaco = 0.dp) {
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

            Cartao(espaco = 8.dp, borda = BorderStroke(2.dp, Cores.Destaque)) {
                T("Total refeito", 15.sp, 600, modifier = Modifier.padding(bottom = 2.dp))
                LinhaValor("Total do aluguel", c.totalCentavos.formatAsBrl())
                LinhaValor("+ Avarias e faltas", fec.avariasCentavos.formatAsBrl())
                LinhaValor("+ Atraso na devolução", fec.atrasoCentavos.formatAsBrl())
                LinhaValor("Novo total", fec.finalCentavos.formatAsBrl(), tamanho = 14.sp, peso = 600, cor = Cores.Texto)
                LinhaValor("− Sinal e caução já recebidos", "− " + fec.abatidoCentavos.formatAsBrl())
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

        Cartao(espaco = 10.dp) {
            Column {
                T("Google Agenda", 15.sp, 600)
                T("Cria o evento já preenchido com cliente, endereço, itens e telefone.", 13.sp, 400, Cores.Suave, Modifier.padding(top = 2.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Botao("Entrega ${Datas.dm(o.entrega)}", { Links.abrir(contexto, Links.googleAgenda(o, true)) }, Modifier.weight(1f), estilo = EstiloBotao.NEUTRO, tamanho = 13.sp) {
                    Icon(Icons.Outlined.EditCalendar, null, tint = Cores.Texto, modifier = Modifier.size(16.dp))
                }
                Botao("Retirada ${Datas.dm(o.retirada)}", { Links.abrir(contexto, Links.googleAgenda(o, false)) }, Modifier.weight(1f), estilo = EstiloBotao.NEUTRO, tamanho = 13.sp) {
                    Icon(Icons.Outlined.EditCalendar, null, tint = Cores.Texto, modifier = Modifier.size(16.dp))
                }
            }
        }

        // --- Pix ---
        Cartao(espaco = 12.dp) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.QrCode2, null, tint = Cores.Destaque, modifier = Modifier.size(22.dp))
                Column {
                    T("Cobrar com Pix", 15.sp, 600)
                    T("QR Code e copia e cola para enviar ao cliente.", 13.sp, 400, Cores.Suave, Modifier.padding(top = 2.dp))
                }
            }
            val temChave = pixCfg.chave.isNotBlank()
            if (!temChave) {
                Aviso("Cadastre sua chave Pix no menu ⋮ → Config. Pix para gerar a cobrança.", false)
            } else {
                val opcoes = buildList {
                    add(Triple("sinal", "Sinal (50%)", (c.totalCentavos + 1) / 2))
                    add(Triple("saldo", "Saldo (total − sinal)", c.saldoCentavos))
                    if ((o.status == StatusPedido.ENTREGUE || o.status == StatusPedido.DEVOLVIDO) && fec.restoCentavos > 0) {
                        add(Triple("vistoria", "Valor após vistoria", fec.restoCentavos))
                    }
                    add(Triple("outro", "Outro valor", centavos(vm.pixOutro)))
                }
                val atual = opcoes.firstOrNull { it.first == vm.pixOpcao } ?: opcoes[1]
                val valor = maxOf(0L, atual.third)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    opcoes.forEach { op ->
                        Chip(op.second, atual.first == op.first, { vm.pixOpcao = op.first; vm.pixCopiado = false }, Modifier.testTag("pix_${op.first}"))
                    }
                }
                if (atual.first == "outro") {
                    Campo("Valor a cobrar (R$)", vm.pixOutro, { vm.pixOutro = it.filter { ch -> ch.isDigit() || ch == ',' || ch == '.' }; vm.pixCopiado = false }, teclado = KeyboardType.Decimal)
                }
                if (valor > 0) {
                    val codigo = remember(pixCfg, valor, o.id) {
                        Pix.copiaECola(pixCfg.chave, pixCfg.nome, pixCfg.cidade, valor, "PED${o.id}")
                    }
                    val qr = remember(codigo) { Links.qr(codigo) }
                    val nome = Pix.limpar(pixCfg.nome, 25).ifBlank { "RECEBEDOR" }
                    val cidade = Pix.limpar(pixCfg.cidade, 15).ifBlank { "CIDADE" }
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Cores.QrFundo)
                            .border(1.dp, Cores.Borda, RoundedCornerShape(12.dp)).padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        T(atual.second + " · pedido #${o.id}", 13.sp, 400, Cores.Suave)
                        Numero(valor.formatAsBrl(), 28.sp, modifier = Modifier.testTag("pix_valor"))
                        if (qr != null) {
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
                        val primeiro = o.cliente.trim().substringBefore(' ')
                        val texto = "Olá, $primeiro! Segue o Pix do pedido #${o.id} (entrega ${Datas.dm(o.entrega)}, retirada ${Datas.dm(o.retirada)}).\n" +
                            atual.second.replace(Regex(" \\(.*\\)"), "") + ": " + valor.formatAsBrl() +
                            "\n\nPix copia e cola:\n" + codigo + "\n\nRecebedor: " + nome
                        Botao("Enviar no WhatsApp", { Links.abrir(contexto, Links.whatsApp(o.telefone, texto)) }, Modifier.weight(1.3f)) {
                            Icon(Icons.Outlined.Chat, null, tint = Cores.SobreVerde, modifier = Modifier.size(18.dp))
                        }
                    }
                } else {
                    T("Não há valor a cobrar nessa opção.", 13.sp, 400, Cores.Suave)
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
    }
}

@Composable
private fun LinhaPasso(rotulo: String, valor: Int, editavel: Boolean, descricao: String, tag: String, passo: (Int) -> Unit) {
    Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        T(rotulo, 14.sp, 400, Cores.Texto2, Modifier.weight(1f))
        if (editavel) BotaoPasso("−", "Diminuir $descricao", { passo(-1) })
        T(valor.toString(), 17.sp, 600, modifier = Modifier.width(36.dp).testTag(tag), alinhar = TextAlign.Center)
        if (editavel) BotaoPasso("+", "Aumentar $descricao", { passo(1) }, Modifier.testTag("mais_$tag"))
    }
}
