package br.com.mesasapp.ui.novo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.mesasapp.di.AppContainer
import br.com.mesasapp.ui.theme.MesasappTheme
import br.com.mesasapp.util.Datas
import br.com.mesasapp.util.formatAsBrl
import java.time.LocalDate
import java.time.LocalTime

/**
 * Novo pedido em blocos, com o resumo do dinheiro sempre visivel no rodape.
 *
 * O resumo fica fixo porque e a pergunta que o cliente faz no telefone: a conta
 * tem que mudar junto com o que a pessoa toca, sem precisar rolar a tela.
 */
@Composable
fun NovoPedidoScreen(
    container: AppContainer,
    aoAbrirEstoque: () -> Unit,
    aoSalvar: () -> Unit
) {
    val viewModel: NovoPedidoViewModel = viewModel(
        factory = viewModelFactory {
            initializer { NovoPedidoViewModel(container.repository, container.configStore) }
        }
    )
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(estado.mensagem) {
        estado.mensagem?.let {
            snackbar.showSnackbar(it)
            viewModel.mensagemLida()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Text("Novo pedido", style = MaterialTheme.typography.headlineMedium) }

                item {
                    Bloco("Cliente") {
                        OutlinedTextField(
                            estado.clienteNome, viewModel::definirCliente,
                            label = { Text("Nome") }, singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            estado.telefone, viewModel::definirTelefone,
                            label = { Text("Telefone / WhatsApp") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            estado.documento, viewModel::definirDocumento,
                            label = { Text("CPF ou CNPJ (opcional)") }, singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    Bloco("Local da entrega") {
                        OutlinedTextField(
                            estado.endereco, viewModel::definirEndereco,
                            label = { Text("Endereco") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            estado.pontoReferencia, viewModel::definirPontoReferencia,
                            label = { Text("Ponto de referencia / observacoes") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    Bloco("Datas e horarios") {
                        LinhaPeriodo(
                            rotulo = "Entrega",
                            data = estado.dataEntrega,
                            hora = estado.horaEntrega,
                            aoMudarData = viewModel::definirDataEntrega,
                            aoMudarHora = viewModel::definirHoraEntrega
                        )
                        LinhaPeriodo(
                            rotulo = "Retirada",
                            data = estado.dataRetirada,
                            hora = estado.horaRetirada,
                            aoMudarData = viewModel::definirDataRetirada,
                            aoMudarHora = viewModel::definirHoraRetirada
                        )
                        Text(
                            "Calculado: ${estado.dias} ${if (estado.dias == 1) "diaria" else "diarias"}. " +
                                "O horario conta: retirada as 10h libera o item para uma entrega as 14h.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    Column {
                        Text("Itens", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "A quantidade livre ja desconta os outros pedidos do periodo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (estado.catalogoVazio) {
                    item {
                        Card {
                            Column(Modifier.padding(16.dp)) {
                                Text("Nada cadastrado ainda", style = MaterialTheme.typography.titleMedium)
                                Text("Informe no Estoque quantas mesas e cadeiras voce tem e o preco da diaria.")
                                Spacer(Modifier.width(8.dp))
                                Button(onClick = aoAbrirEstoque) { Text("Abrir estoque") }
                            }
                        }
                    }
                }

                items(estado.opcoes, key = { it.chave }) { opcao ->
                    LinhaItem(
                        opcao = opcao,
                        aoSomar = { viewModel.mudarQuantidade(opcao.chave, 1) },
                        aoSubtrair = { viewModel.mudarQuantidade(opcao.chave, -1) }
                    )
                }

                if (estado.faltas.isNotEmpty()) {
                    item {
                        Column {
                            estado.faltas.forEach {
                                Text(it, color = MesasappTheme.estado.erro, style = MaterialTheme.typography.bodyMedium)
                            }
                            Text(
                                "Da para salvar como orcamento, mas nao da para confirmar.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    Bloco("Valores e pagamento") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CampoDinheiro("Frete", estado.freteTexto, viewModel::definirFrete, Modifier.weight(1f))
                            CampoDinheiro("Desconto", estado.descontoTexto, viewModel::definirDesconto, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CampoDinheiro("Sinal pago", estado.sinalTexto, viewModel::definirSinal, Modifier.weight(1f))
                            CampoDinheiro("Caucao", estado.caucaoTexto, viewModel::definirCaucao, Modifier.weight(1f))
                        }
                        OutlinedTextField(
                            estado.observacoes, viewModel::definirObservacoes,
                            label = { Text("Observacoes do pedido") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            ResumoFixo(
                estado = estado,
                aoSalvarOrcamento = { viewModel.salvar(confirmar = false) { aoSalvar() } },
                aoConfirmar = { viewModel.salvar(confirmar = true) { aoSalvar() } }
            )
        }
    }
}

@Composable
private fun Bloco(titulo: String, conteudo: @Composable ColumnScopeAlias.() -> Unit) {
    Card {
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            conteudo()
        }
    }
}

private typealias ColumnScopeAlias = androidx.compose.foundation.layout.ColumnScope

@Composable
private fun CampoDinheiro(
    rotulo: String,
    valor: String,
    aoMudar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = valor,
        onValueChange = aoMudar,
        label = { Text(rotulo) },
        prefix = { Text("R$ ") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
    )
}

@Composable
private fun LinhaItem(opcao: Opcao, aoSomar: () -> Unit, aoSubtrair: () -> Unit) {
    Card(
        colors = if (opcao.quantidade > 0) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(opcao.nome, style = MaterialTheme.typography.titleMedium)
                if (opcao.detalhe.isNotBlank()) {
                    Text(
                        opcao.detalhe,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (opcao.precoDiariaCentavos > 0) {
                            "${opcao.precoDiariaCentavos.formatAsBrl()}/dia"
                        } else {
                            "sem preco"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "   ${opcao.disponivel} livres",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (opcao.disponivel > 0) MesasappTheme.estado.ok else MesasappTheme.estado.erro
                    )
                }
            }

            FilledTonalIconButton(onClick = aoSubtrair, enabled = opcao.quantidade > 0) {
                Icon(Icons.Default.Remove, contentDescription = "Menos um")
            }
            Text(
                opcao.quantidade.toString(),
                modifier = Modifier.width(40.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge
            )
            FilledTonalIconButton(onClick = aoSomar) {
                Icon(Icons.Default.Add, contentDescription = "Mais um")
            }
        }
    }
}

@Composable
private fun LinhaPeriodo(
    rotulo: String,
    data: LocalDate,
    hora: LocalTime,
    aoMudarData: (LocalDate) -> Unit,
    aoMudarHora: (LocalTime) -> Unit
) {
    var escolhendoData by remember { mutableStateOf(false) }
    var escolhendoHora by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(rotulo, style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { escolhendoData = true }) { Text(Datas.formatar(data)) }
            OutlinedButton(onClick = { escolhendoHora = true }) { Text(Datas.formatar(hora)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Manha (08h)" to 8, "Tarde (13h)" to 13, "Noite (18h)" to 18).forEach { (texto, h) ->
                FilterChip(
                    selected = hora.hour == h && hora.minute == 0,
                    onClick = { aoMudarHora(LocalTime.of(h, 0)) },
                    label = { Text(texto, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }

    if (escolhendoData) {
        SeletorData(data, { aoMudarData(it); escolhendoData = false }) { escolhendoData = false }
    }
    if (escolhendoHora) {
        SeletorHora(hora, { aoMudarHora(it); escolhendoHora = false }) { escolhendoHora = false }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeletorData(inicial: LocalDate, aoEscolher: (LocalDate) -> Unit, aoFechar: () -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = Datas.millisUtcDeData(inicial))
    DatePickerDialog(
        onDismissRequest = aoFechar,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { aoEscolher(Datas.dataDeMillisUtc(it)) } ?: aoFechar()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } }
    ) { DatePicker(state = state) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeletorHora(inicial: LocalTime, aoEscolher: (LocalTime) -> Unit, aoFechar: () -> Unit) {
    val state = rememberTimePickerState(initialHour = inicial.hour, initialMinute = inicial.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = aoFechar,
        confirmButton = { TextButton(onClick = { aoEscolher(LocalTime.of(state.hour, state.minute)) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } },
        text = { TimePicker(state = state) }
    )
}

@Composable
private fun ResumoFixo(
    estado: NovoPedidoUiState,
    aoSalvarOrcamento: () -> Unit,
    aoConfirmar: () -> Unit
) {
    Surface(tonalElevation = 3.dp) {
        Column(Modifier.fillMaxWidth().imePadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
            LinhaResumo("Aluguel", estado.aluguelCentavos.formatAsBrl())
            LinhaResumo("Frete", estado.freteCentavos.formatAsBrl())
            LinhaResumo("Desconto", "- ${estado.descontoCentavos.formatAsBrl()}")
            HorizontalDivider(Modifier.padding(vertical = 6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Total", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    estado.totalCentavos.formatAsBrl(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            if (estado.sinalCentavos > 0) {
                LinhaResumo("Saldo a cobrar (total - sinal)", estado.saldoCentavos.formatAsBrl())
            }
            if (estado.caucaoCentavos > 0) {
                Row {
                    AssistChip(
                        onClick = {},
                        label = { Text("Caucao ${estado.caucaoCentavos.formatAsBrl()}") }
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = aoSalvarOrcamento,
                    enabled = estado.temItens && !estado.salvando,
                    modifier = Modifier.weight(1f)
                ) { Text("Salvar orcamento") }
                Button(
                    onClick = aoConfirmar,
                    enabled = estado.temItens && estado.disponivel && !estado.salvando,
                    modifier = Modifier.weight(1f)
                ) { Text("Confirmar pedido") }
            }
        }
    }
}

@Composable
private fun LinhaResumo(rotulo: String, valor: String) {
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
