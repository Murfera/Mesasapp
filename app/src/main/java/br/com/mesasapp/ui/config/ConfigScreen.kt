package br.com.mesasapp.ui.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.mesasapp.di.AppContainer
import br.com.mesasapp.util.formatAsPlain
import br.com.mesasapp.util.toCentsOrNull

/** Dados do negocio, chave Pix e os padroes que o pedido novo ja vem preenchido. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(container: AppContainer, aoVoltar: () -> Unit) {
    val viewModel: ConfigViewModel = viewModel(
        factory = viewModelFactory { initializer { ConfigViewModel(container.configStore) } }
    )
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(estado.mensagem) {
        estado.mensagem?.let {
            snackbar.showSnackbar(it)
            viewModel.mensagemLida()
        }
    }

    // O formulario parte do que esta salvo e so grava no Salvar.
    var nomeEmpresa by remember(estado.carregado) { mutableStateOf(estado.config.nomeEmpresa) }
    var chavePix by remember(estado.carregado) { mutableStateOf(estado.config.chavePix) }
    var nomeRecebedor by remember(estado.carregado) { mutableStateOf(estado.config.nomeRecebedor) }
    var cidade by remember(estado.carregado) { mutableStateOf(estado.config.cidadeRecebedor) }
    var frete by remember(estado.carregado) {
        mutableStateOf(
            if (estado.config.fretePadraoCentavos > 0) estado.config.fretePadraoCentavos.formatAsPlain() else ""
        )
    }
    var horaEntrega by remember(estado.carregado) { mutableStateOf(estado.config.horaEntregaPadrao.toString()) }
    var horaRetirada by remember(estado.carregado) { mutableStateOf(estado.config.horaRetiradaPadrao.toString()) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Configuracoes") },
                navigationIcon = {
                    IconButton(onClick = aoVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).testTag("lista_config"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Negocio", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            nomeEmpresa, { nomeEmpresa = it },
                            label = { Text("Nome que aparece na abertura") }, singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("campo_nome_empresa")
                        )
                    }
                }
            }

            item {
                Card {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Pix", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Usado para gerar o copia e cola e o QR da cobranca. Fica so no aparelho.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            chavePix, { chavePix = it },
                            label = { Text("Chave Pix") }, singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("campo_chave_pix")
                        )
                        OutlinedTextField(
                            nomeRecebedor, { nomeRecebedor = it },
                            label = { Text("Nome do recebedor") }, singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("campo_recebedor")
                        )
                        OutlinedTextField(
                            cidade, { cidade = it },
                            label = { Text("Cidade do recebedor") }, singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("campo_cidade")
                        )
                    }
                }
            }

            item {
                Card {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Padroes do pedido", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            frete, { frete = it.filter { c -> c.isDigit() || c == ',' || c == '.' } },
                            label = { Text("Frete padrao") }, prefix = { Text("R$ ") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth().testTag("campo_frete_padrao")
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                horaEntrega, { horaEntrega = it.filter(Char::isDigit).take(2) },
                                label = { Text("Hora da entrega") }, singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                horaRetirada, { horaRetirada = it.filter(Char::isDigit).take(2) },
                                label = { Text("Hora da retirada") }, singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        viewModel.salvar(
                            estado.config.copy(
                                nomeEmpresa = nomeEmpresa.trim(),
                                chavePix = chavePix.trim(),
                                nomeRecebedor = nomeRecebedor.trim(),
                                cidadeRecebedor = cidade.trim(),
                                fretePadraoCentavos = frete.toCentsOrNull() ?: 0,
                                horaEntregaPadrao = horaEntrega.toIntOrNull()?.coerceIn(0, 23) ?: 8,
                                horaRetiradaPadrao = horaRetirada.toIntOrNull()?.coerceIn(0, 23) ?: 18
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth().testTag("botao_salvar_config")
                ) { Text("Salvar") }
            }

            item {
                Text(
                    "Nada daqui sai do aparelho: o app nao manda dado para servidor nenhum.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}
