package br.com.mesasapp.ui.pedido

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.mesasapp.di.AppContainer
import br.com.mesasapp.domain.model.LocacaoCompleta
import br.com.mesasapp.domain.model.StatusLocacao
import br.com.mesasapp.ui.inicio.rotulo
import br.com.mesasapp.ui.theme.MesasappTheme
import br.com.mesasapp.util.Datas
import br.com.mesasapp.util.formatAsBrl
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

/** Detalhe do pedido: valores, conferencia da devolucao, Pix e compartilhamento. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PedidoScreen(container: AppContainer, pedidoId: Long, aoVoltar: () -> Unit) {
    val viewModel: PedidoViewModel = viewModel(
        key = "pedido-$pedidoId",
        factory = viewModelFactory {
            initializer { PedidoViewModel(container.repository, container.configStore, pedidoId) }
        }
    )
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val contexto = LocalContext.current

    var conferindo by remember { mutableStateOf(false) }
    var mostrandoPix by remember { mutableStateOf(false) }

    LaunchedEffect(estado.mensagem) {
        estado.mensagem?.let {
            snackbar.showSnackbar(it)
            viewModel.mensagemLida()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Pedido $pedidoId") },
                navigationIcon = {
                    IconButton(onClick = aoVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val envio = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, estado.resumoParaCompartilhar())
                            }
                            contexto.startActivity(Intent.createChooser(envio, "Compartilhar pedido"))
                        }
                    ) { Icon(Icons.Default.Share, contentDescription = "Compartilhar") }
                }
            )
        }
    ) { padding ->
        val completa = estado.pedido

        if (completa == null) {
            Text("Pedido nao encontrado.", modifier = Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.padding(padding).testTag("lista_pedido"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { BlocoCliente(completa) }
            item { BlocoValores(completa) }

            item {
                Card {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Pecas reservadas", style = MaterialTheme.typography.titleMedium)
                        completa.pecas.forEach { peca ->
                            Row {
                                Text("${peca.quantidade}x ${peca.descricao}", modifier = Modifier.weight(1f))
                                if (peca.quantidadeQuebrada > 0) {
                                    Text(
                                        "${peca.quantidadeQuebrada} quebrada(s)",
                                        color = MesasappTheme.estado.erro,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { conferindo = true },
                        modifier = Modifier.fillMaxWidth().testTag("botao_devolucao")
                    ) { Text("Registrar devolucao") }

                    OutlinedButton(
                        onClick = { mostrandoPix = true },
                        modifier = Modifier.fillMaxWidth().testTag("botao_pix")
                    ) { Text("Cobrar por Pix") }
                }
            }

            item {
                Card {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text("Situacao", style = MaterialTheme.typography.titleMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusLocacao.entries.forEach { status ->
                                AssistChip(
                                    onClick = { viewModel.mudarStatus(status) },
                                    label = { Text(status.rotulo()) },
                                    leadingIcon = {
                                        if (completa.locacao.status == status) Text("•")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (conferindo) {
        estado.pedido?.let { completa ->
            DialogoDevolucao(
                completa = completa,
                aoFechar = { conferindo = false },
                aoConfirmar = { conferencia, encerrar ->
                    viewModel.registrarDevolucao(conferencia, encerrar)
                    conferindo = false
                }
            )
        }
    }

    if (mostrandoPix) {
        DialogoPix(
            estado = estado,
            aoFechar = { mostrandoPix = false },
            aoCompartilhar = { texto ->
                val envio = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, texto)
                }
                contexto.startActivity(Intent.createChooser(envio, "Enviar Pix"))
            }
        )
    }
}

@Composable
private fun BlocoCliente(completa: LocacaoCompleta) {
    val locacao = completa.locacao
    Card {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                locacao.clienteNome.ifBlank { "Sem nome" },
                style = MaterialTheme.typography.titleMedium
            )
            AssistChip(onClick = {}, label = { Text(locacao.status.rotulo()) })
            if (locacao.telefone.isNotBlank()) Text(locacao.telefone)
            if (locacao.endereco.isNotBlank()) Text(locacao.endereco)
            if (locacao.pontoReferencia.isNotBlank()) {
                Text(
                    locacao.pontoReferencia,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text("Entrega: ${Datas.formatarDataHora(locacao.inicioEpochSeg)}")
            Text("Retirada: ${Datas.formatarDataHora(locacao.fimEpochSeg)}")
            if (locacao.observacoes.isNotBlank()) Text(locacao.observacoes)
        }
    }
}

@Composable
private fun BlocoValores(completa: LocacaoCompleta) {
    val locacao = completa.locacao
    Card {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Valores", style = MaterialTheme.typography.titleMedium)
            completa.linhas.forEach { linha ->
                Linha("${linha.quantidade}x ${linha.descricao}", linha.subtotalCentavos.formatAsBrl())
            }
            if (locacao.freteCentavos > 0) Linha("Frete", locacao.freteCentavos.formatAsBrl())
            if (locacao.montagemCentavos > 0) Linha("Montagem", locacao.montagemCentavos.formatAsBrl())
            if (locacao.descontoCentavos > 0) Linha("Desconto", "- ${locacao.descontoCentavos.formatAsBrl()}")
            if (completa.quebrasCentavos > 0) Linha("Quebras", completa.quebrasCentavos.formatAsBrl())

            HorizontalDivider(Modifier.padding(vertical = 6.dp))

            Row {
                Text("Total", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    completa.totalCentavos.formatAsBrl(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("total_pedido_detalhe")
                )
            }
            if (locacao.sinalCentavos > 0) {
                Linha("Sinal pago", locacao.sinalCentavos.formatAsBrl())
                Linha("A receber", completa.aReceberCentavos.formatAsBrl())
            }
            if (locacao.caucaoCentavos > 0) Linha("Caucao", locacao.caucaoCentavos.formatAsBrl())
        }
    }
}

@Composable
private fun Linha(rotulo: String, valor: String) {
    Row {
        Text(
            rotulo,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(valor, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * Conferencia da devolucao. A pessoa informa quantas voltaram boas e quantas
 * voltaram quebradas; o que faltar aparece como pendente, porque sumir com a
 * diferenca em silencio e o que faz a conta nao fechar no fim do mes.
 */
@Composable
private fun DialogoDevolucao(
    completa: LocacaoCompleta,
    aoFechar: () -> Unit,
    aoConfirmar: (Map<Long, Pair<Int, Int>>, Boolean) -> Unit
) {
    val devolvidas = remember {
        mutableStateMapOf<Long, String>().apply {
            completa.pecas.forEach { peca ->
                put(peca.id, (peca.quantidade - peca.quantidadeQuebrada).toString())
            }
        }
    }
    val quebradas = remember {
        mutableStateMapOf<Long, String>().apply {
            completa.pecas.forEach { peca -> put(peca.id, peca.quantidadeQuebrada.toString()) }
        }
    }

    // Dialogo em tela cheia, e nao AlertDialog: lista rolavel dentro de um
    // AlertDialog tem altura livre e o layout entra em laco infinito — no
    // aparelho isso trava a tela.
    androidx.compose.ui.window.Dialog(
        onDismissRequest = aoFechar,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        androidx.compose.material3.Surface(modifier = Modifier.fillMaxWidth()) {
            LazyColumn(
                modifier = Modifier.testTag("lista_devolucao"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Text("Devolucao", style = MaterialTheme.typography.headlineSmall) }

                item {
                    Text(
                        "Confira quantas voltaram e quantas voltaram quebradas. O que faltar " +
                            "fica pendente no pedido.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(completa.pecas.size) { indice ->
                    val peca = completa.pecas[indice]
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${peca.descricao} (saiu ${peca.quantidade})")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = devolvidas[peca.id].orEmpty(),
                                onValueChange = { devolvidas[peca.id] = it.filter(Char::isDigit) },
                                label = { Text("Voltaram") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f).testTag("voltaram_${peca.descricao}")
                            )
                            OutlinedTextField(
                                value = quebradas[peca.id].orEmpty(),
                                onValueChange = { quebradas[peca.id] = it.filter(Char::isDigit) },
                                label = { Text("Quebradas") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f).testTag("quebradas_${peca.descricao}")
                            )
                        }
                        if (peca.valorReposicaoCentavos > 0) {
                            Text(
                                "Reposicao ${peca.valorReposicaoCentavos.formatAsBrl()} cada",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                "Sem valor de reposicao cadastrado: a quebra nao entra na conta.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MesasappTheme.estado.alerta
                            )
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = aoFechar, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                        Button(
                            onClick = {
                                val conferencia = completa.pecas.associate { peca ->
                                    peca.id to Pair(
                                        devolvidas[peca.id]?.toIntOrNull() ?: 0,
                                        quebradas[peca.id]?.toIntOrNull() ?: 0
                                    )
                                }
                                aoConfirmar(conferencia, true)
                            },
                            modifier = Modifier.weight(1f).testTag("confirmar_devolucao")
                        ) { Text("Salvar e encerrar") }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogoPix(
    estado: PedidoUiState,
    aoFechar: () -> Unit,
    aoCompartilhar: (String) -> Unit
) {
    val payload = estado.pixCopiaECola()

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text("Cobrar por Pix") },
        text = {
            if (payload == null) {
                Text(
                    "Falta cadastrar a chave Pix, o nome do recebedor e a cidade em Configuracoes.",
                    modifier = Modifier.testTag("pix_sem_config")
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    qrCode(payload)?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "QR Code do Pix",
                            modifier = Modifier.size(220.dp)
                        )
                    }
                    Text(
                        "Valor: ${(estado.pedido?.aReceberCentavos ?: 0).formatAsBrl()}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        payload,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.testTag("pix_payload")
                    )
                    Text(
                        "O QR estatico nao avisa quando o cliente paga: a baixa e manual.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            if (payload != null) {
                Button(onClick = { aoCompartilhar(payload) }) { Text("Enviar") }
            }
        },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Fechar") } }
    )
}

/** Desenha o QR do Pix no proprio aparelho, sem rede. */
private fun qrCode(conteudo: String, lado: Int = 512): Bitmap? = runCatching {
    val matriz = QRCodeWriter().encode(conteudo, BarcodeFormat.QR_CODE, lado, lado)
    val bitmap = Bitmap.createBitmap(lado, lado, Bitmap.Config.ARGB_8888)
    for (x in 0 until lado) {
        for (y in 0 until lado) {
            bitmap.setPixel(x, y, if (matriz[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
        }
    }
    bitmap
}.getOrNull()
