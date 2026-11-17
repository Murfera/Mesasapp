package br.com.mesasapp.ui.orcamento

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.material3.TopAppBar
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
 * Tela principal: a calculadora de orcamento.
 *
 * A regra desta tela e nao pedir nada que nao seja preciso para dizer o preco.
 * Cliente, endereco e telefone so aparecem quando a pessoa decide salvar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrcamentoScreen(
    container: AppContainer,
    aoAbrirEstoque: () -> Unit
) {
    val viewModel: OrcamentoViewModel = viewModel(
        factory = viewModelFactory {
            initializer { OrcamentoViewModel(container.repository, container.configStore) }
        }
    )
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    var salvando by remember { mutableStateOf(false) }

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
                title = { Text("Mesasapp") },
                actions = {
                    IconButton(onClick = aoAbrirEstoque) {
                        Icon(Icons.Default.Inventory2, contentDescription = "Estoque")
                    }
                }
            )
        },
        bottomBar = {
            BarraTotal(
                totalCentavos = estado.totalCentavos,
                habilitado = estado.temItensSelecionados && !estado.salvando,
                aoLimpar = viewModel::limpar,
                aoSalvar = { salvando = true }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (estado.catalogoVazio) {
                item {
                    Card {
                        Column(Modifier.padding(16.dp)) {
                            Text("Nada cadastrado ainda", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            Text("Abra o Estoque e informe quantas mesas e cadeiras voce tem, com o preco da diaria.")
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = aoAbrirEstoque) { Text("Abrir estoque") }
                        }
                    }
                }
            }

            items(estado.opcoes, key = { it.chave }) { opcao ->
                LinhaQuantidade(
                    opcao = opcao,
                    aoSomar = { viewModel.mudarQuantidade(opcao.chave, 1) },
                    aoSubtrair = { viewModel.mudarQuantidade(opcao.chave, -1) }
                )
            }

            item { HorizontalDivider() }

            item {
                PeriodoEFrete(
                    estado = estado,
                    aoMudarDataEntrega = viewModel::definirDataEntrega,
                    aoMudarHoraEntrega = viewModel::definirHoraEntrega,
                    aoMudarDataRetirada = viewModel::definirDataRetirada,
                    aoMudarHoraRetirada = viewModel::definirHoraRetirada,
                    aoMudarFrete = viewModel::definirFrete
                )
            }

            item { LinhaDisponibilidade(estado) }
        }
    }

    if (salvando) {
        DialogoSalvar(
            estado = estado,
            aoFechar = { salvando = false },
            aoConfirmar = { nome, telefone, endereco, sinal, desconto, obs, confirmar ->
                viewModel.salvar(nome, telefone, endereco, sinal, desconto, obs, confirmar)
                salvando = false
            }
        )
    }
}

@Composable
private fun LinhaQuantidade(opcao: Opcao, aoSomar: () -> Unit, aoSubtrair: () -> Unit) {
    Card(
        colors = if (opcao.quantidade > 0) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(opcao.nome, style = MaterialTheme.typography.titleMedium)
                Text(
                    opcao.detalhe,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    if (opcao.precoDiariaCentavos > 0) opcao.precoDiariaCentavos.formatAsBrl()
                    else "sem preco",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            FilledTonalIconButton(onClick = aoSubtrair, enabled = opcao.quantidade > 0) {
                Icon(Icons.Default.Remove, contentDescription = "Menos um")
            }
            Text(
                opcao.quantidade.toString(),
                modifier = Modifier.width(44.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = MaterialTheme.typography.titleLarge
            )
            FilledTonalIconButton(onClick = aoSomar) {
                Icon(Icons.Default.Add, contentDescription = "Mais um")
            }
        }
    }
}

@Composable
private fun PeriodoEFrete(
    estado: OrcamentoUiState,
    aoMudarDataEntrega: (LocalDate) -> Unit,
    aoMudarHoraEntrega: (LocalTime) -> Unit,
    aoMudarDataRetirada: (LocalDate) -> Unit,
    aoMudarHoraRetirada: (LocalTime) -> Unit,
    aoMudarFrete: (String) -> Unit
) {
    var escolhendo by remember { mutableStateOf<Escolha?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CampoBotao(
                rotulo = "Entrega",
                valor = "${Datas.formatar(estado.dataEntrega)} ${Datas.formatar(estado.horaEntrega)}",
                modifier = Modifier.weight(1f),
                aoClicarData = { escolhendo = Escolha.DATA_ENTREGA },
                aoClicarHora = { escolhendo = Escolha.HORA_ENTREGA }
            )
            CampoBotao(
                rotulo = "Retirada",
                valor = "${Datas.formatar(estado.dataRetirada)} ${Datas.formatar(estado.horaRetirada)}",
                modifier = Modifier.weight(1f),
                aoClicarData = { escolhendo = Escolha.DATA_RETIRADA },
                aoClicarHora = { escolhendo = Escolha.HORA_RETIRADA }
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = estado.freteTexto,
                onValueChange = aoMudarFrete,
                label = { Text("Frete") },
                prefix = { Text("R$ ") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            Text(
                "${estado.dias} ${if (estado.dias == 1) "dia" else "dias"}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
        }
    }

    when (escolhendo) {
        Escolha.DATA_ENTREGA -> SeletorData(estado.dataEntrega, { aoMudarDataEntrega(it); escolhendo = null }) { escolhendo = null }
        Escolha.DATA_RETIRADA -> SeletorData(estado.dataRetirada, { aoMudarDataRetirada(it); escolhendo = null }) { escolhendo = null }
        Escolha.HORA_ENTREGA -> SeletorHora(estado.horaEntrega, { aoMudarHoraEntrega(it); escolhendo = null }) { escolhendo = null }
        Escolha.HORA_RETIRADA -> SeletorHora(estado.horaRetirada, { aoMudarHoraRetirada(it); escolhendo = null }) { escolhendo = null }
        null -> Unit
    }
}

private enum class Escolha { DATA_ENTREGA, DATA_RETIRADA, HORA_ENTREGA, HORA_RETIRADA }

@Composable
private fun CampoBotao(
    rotulo: String,
    valor: String,
    modifier: Modifier = Modifier,
    aoClicarData: () -> Unit,
    aoClicarHora: () -> Unit
) {
    Column(modifier) {
        Text(rotulo, style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedButton(onClick = aoClicarData, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)) {
                Text(valor.substringBefore(' '))
            }
            OutlinedButton(onClick = aoClicarHora, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)) {
                Text(valor.substringAfter(' '))
            }
        }
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
    ) {
        DatePicker(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeletorHora(inicial: LocalTime, aoEscolher: (LocalTime) -> Unit, aoFechar: () -> Unit) {
    val state = rememberTimePickerState(initialHour = inicial.hour, initialMinute = inicial.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = aoFechar,
        confirmButton = {
            TextButton(onClick = { aoEscolher(LocalTime.of(state.hour, state.minute)) }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } },
        text = { TimePicker(state = state) }
    )
}

@Composable
private fun LinhaDisponibilidade(estado: OrcamentoUiState) {
    if (!estado.temItensSelecionados) return

    val cores = MesasappTheme.estado
    if (estado.disponivel) {
        Text(
            "Disponivel para a data",
            color = cores.ok,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    } else {
        Column {
            estado.faltas.forEach { falta ->
                Text(falta, color = cores.erro, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                "Da para salvar como orcamento; confirmar a locacao vai estourar o estoque.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BarraTotal(
    totalCentavos: Long,
    habilitado: Boolean,
    aoLimpar: () -> Unit,
    aoSalvar: () -> Unit
) {
    // O app desenha de ponta a ponta: sem estes paddings a barra do total fica
    // atras da barra de navegacao do Android e do teclado.
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("TOTAL", style = MaterialTheme.typography.labelMedium)
                Text(
                    totalCentavos.formatAsBrl(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            TextButton(onClick = aoLimpar, enabled = habilitado) { Text("Limpar") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = aoSalvar, enabled = habilitado) { Text("Salvar") }
        }
    }
}

@Composable
private fun DialogoSalvar(
    estado: OrcamentoUiState,
    aoFechar: () -> Unit,
    aoConfirmar: (String, String, String, String, String, String, Boolean) -> Unit
) {
    var nome by remember { mutableStateOf("") }
    var telefone by remember { mutableStateOf("") }
    var endereco by remember { mutableStateOf("") }
    var sinal by remember { mutableStateOf("") }
    var desconto by remember { mutableStateOf("") }
    var observacoes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text("Salvar ${estado.totalCentavos.formatAsBrl()}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(nome, { nome = it }, label = { Text("Cliente") }, singleLine = true)
                OutlinedTextField(
                    telefone, { telefone = it }, label = { Text("Telefone") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                OutlinedTextField(endereco, { endereco = it }, label = { Text("Endereco da entrega") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        sinal, { sinal = it }, label = { Text("Sinal") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        desconto, { desconto = it }, label = { Text("Desconto") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(observacoes, { observacoes = it }, label = { Text("Observacoes") })
            }
        },
        confirmButton = {
            Button(onClick = {
                aoConfirmar(nome, telefone, endereco, sinal, desconto, observacoes, true)
            }) { Text("Confirmar locacao") }
        },
        dismissButton = {
            Box {
                TextButton(onClick = {
                    aoConfirmar(nome, telefone, endereco, sinal, desconto, observacoes, false)
                }) { Text("So orcamento") }
            }
        }
    )
}
