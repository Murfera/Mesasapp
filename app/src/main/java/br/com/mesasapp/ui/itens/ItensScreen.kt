package br.com.mesasapp.ui.itens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.mesasapp.di.AppContainer
import br.com.mesasapp.domain.model.Categoria
import br.com.mesasapp.domain.model.Combo
import br.com.mesasapp.domain.model.Item
import br.com.mesasapp.domain.model.Material
import br.com.mesasapp.util.formatAsBrl
import br.com.mesasapp.util.formatAsPlain
import br.com.mesasapp.util.toCentsOrNull

/** Estoque: o que existe, quanto custa a diaria e quanto custa repor se quebrar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItensScreen(container: AppContainer, aoVoltar: () -> Unit) {
    val viewModel: ItensViewModel = viewModel(
        factory = viewModelFactory { initializer { ItensViewModel(container.repository) } }
    )
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    var editando by remember { mutableStateOf<Item?>(null) }
    var editandoCombo by remember { mutableStateOf<Combo?>(null) }

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
                title = { Text("Estoque") },
                navigationIcon = {
                    IconButton(onClick = aoVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editando = Item(nome = "", categoria = Categoria.CADEIRA) }) {
                Icon(Icons.Default.Add, contentDescription = "Novo item")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Text("Itens", style = MaterialTheme.typography.titleMedium) }

            items(estado.itens, key = { "i${it.id}" }) { item ->
                Card(onClick = { editando = item }) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (item.ativo) item.nome else "${item.nome} (inativo)",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                "${item.quantidadeTotal} em estoque · diaria ${item.precoDiariaCentavos.formatAsBrl()}" +
                                    " · reposicao ${item.valorReposicaoCentavos.formatAsBrl()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewModel.excluirItem(item) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Excluir")
                        }
                    }
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Jogos", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        val mesa = estado.itens.firstOrNull { it.categoria == Categoria.MESA }
                        val cadeira = estado.itens.firstOrNull { it.categoria == Categoria.CADEIRA }
                        if (mesa != null && cadeira != null) {
                            editandoCombo = Combo(itemMesaId = mesa.id, itemCadeiraId = cadeira.id)
                        }
                    }) { Text("Novo jogo") }
                }
            }

            items(estado.combos, key = { "c${it.id}" }) { combo ->
                val mesa = estado.itens.firstOrNull { it.id == combo.itemMesaId }
                val cadeira = estado.itens.firstOrNull { it.id == combo.itemCadeiraId }
                Card(onClick = { editandoCombo = combo }) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(combo.nome, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "1 ${mesa?.nome ?: "?"} + ${combo.qtdCadeiras} ${cadeira?.nome ?: "?"}" +
                                    " · diaria ${combo.precoDiariaCentavos.formatAsBrl()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewModel.excluirCombo(combo) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Excluir")
                        }
                    }
                }
            }

            item {
                Text(
                    "O jogo nao tem estoque proprio: alugar um jogo baixa 1 mesa e as cadeiras dele " +
                        "do estoque real, para a mesma cadeira nunca ser alugada duas vezes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    editando?.let { item ->
        DialogoItem(
            item = item,
            aoFechar = { editando = null },
            aoSalvar = { viewModel.salvarItem(it); editando = null }
        )
    }

    editandoCombo?.let { combo ->
        DialogoCombo(
            combo = combo,
            itens = estado.itens,
            aoFechar = { editandoCombo = null },
            aoSalvar = { viewModel.salvarCombo(it); editandoCombo = null }
        )
    }
}

@Composable
private fun DialogoItem(item: Item, aoFechar: () -> Unit, aoSalvar: (Item) -> Unit) {
    var nome by remember { mutableStateOf(item.nome) }
    var categoria by remember { mutableStateOf(item.categoria) }
    var material by remember { mutableStateOf(item.material) }
    var cor by remember { mutableStateOf(item.cor) }
    var comBraco by remember { mutableStateOf(item.comBraco ?: false) }
    var quantidade by remember { mutableStateOf(item.quantidadeTotal.toString()) }
    var diaria by remember { mutableStateOf(if (item.precoDiariaCentavos > 0) item.precoDiariaCentavos.formatAsPlain() else "") }
    var reposicao by remember { mutableStateOf(if (item.valorReposicaoCentavos > 0) item.valorReposicaoCentavos.formatAsPlain() else "") }

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(if (item.id == 0L) "Novo item" else "Editar item") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Categoria.entries.forEach { opcao ->
                        FilterChip(
                            selected = categoria == opcao,
                            onClick = { categoria = opcao },
                            label = { Text(if (opcao == Categoria.MESA) "Mesa" else "Cadeira") }
                        )
                    }
                }
                OutlinedTextField(nome, { nome = it }, label = { Text("Nome") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Material.entries.forEach { opcao ->
                        FilterChip(
                            selected = material == opcao,
                            onClick = { material = opcao },
                            label = { Text(opcao.name.lowercase().replaceFirstChar { c -> c.uppercase() }) }
                        )
                    }
                }
                OutlinedTextField(cor, { cor = it }, label = { Text("Cor") }, singleLine = true)
                if (categoria == Categoria.CADEIRA) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Com braco", modifier = Modifier.weight(1f))
                        Switch(checked = comBraco, onCheckedChange = { comBraco = it })
                    }
                }
                OutlinedTextField(
                    quantidade, { quantidade = it.filter { c -> c.isDigit() } },
                    label = { Text("Quantidade em estoque") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    diaria, { diaria = it }, label = { Text("Preco da diaria") }, prefix = { Text("R$ ") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    reposicao, { reposicao = it }, label = { Text("Valor de reposicao (quebra)") },
                    prefix = { Text("R$ ") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            Button(
                enabled = nome.isNotBlank(),
                onClick = {
                    aoSalvar(
                        item.copy(
                            nome = nome.trim(),
                            categoria = categoria,
                            material = material,
                            comBraco = if (categoria == Categoria.CADEIRA) comBraco else null,
                            cor = cor.trim(),
                            quantidadeTotal = quantidade.toIntOrNull() ?: 0,
                            precoDiariaCentavos = diaria.toCentsOrNull() ?: 0,
                            valorReposicaoCentavos = reposicao.toCentsOrNull() ?: 0,
                            ativo = true
                        )
                    )
                }
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } }
    )
}

@Composable
private fun DialogoCombo(combo: Combo, itens: List<Item>, aoFechar: () -> Unit, aoSalvar: (Combo) -> Unit) {
    var nome by remember { mutableStateOf(combo.nome) }
    var mesaId by remember { mutableStateOf(combo.itemMesaId) }
    var cadeiraId by remember { mutableStateOf(combo.itemCadeiraId) }
    var qtdCadeiras by remember { mutableStateOf(combo.qtdCadeiras.toString()) }
    var diaria by remember { mutableStateOf(if (combo.precoDiariaCentavos > 0) combo.precoDiariaCentavos.formatAsPlain() else "") }

    val mesas = itens.filter { it.categoria == Categoria.MESA }
    val cadeiras = itens.filter { it.categoria == Categoria.CADEIRA }

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(if (combo.id == 0L) "Novo jogo" else "Editar jogo") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(nome, { nome = it }, label = { Text("Nome") }, singleLine = true)

                Text("Mesa", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    mesas.forEach { mesa ->
                        FilterChip(
                            selected = mesaId == mesa.id,
                            onClick = { mesaId = mesa.id },
                            label = { Text(mesa.nome) }
                        )
                    }
                }

                Text("Cadeira", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    cadeiras.forEach { cadeira ->
                        FilterChip(
                            selected = cadeiraId == cadeira.id,
                            onClick = { cadeiraId = cadeira.id },
                            label = { Text(cadeira.nome) }
                        )
                    }
                }

                OutlinedTextField(
                    qtdCadeiras, { qtdCadeiras = it.filter { c -> c.isDigit() } },
                    label = { Text("Cadeiras por jogo") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    diaria, { diaria = it }, label = { Text("Preco da diaria do jogo") },
                    prefix = { Text("R$ ") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            Button(
                enabled = nome.isNotBlank() && mesas.any { it.id == mesaId } && cadeiras.any { it.id == cadeiraId },
                onClick = {
                    aoSalvar(
                        combo.copy(
                            nome = nome.trim(),
                            itemMesaId = mesaId,
                            itemCadeiraId = cadeiraId,
                            qtdCadeiras = qtdCadeiras.toIntOrNull() ?: 1,
                            precoDiariaCentavos = diaria.toCentsOrNull() ?: 0,
                            ativo = true
                        )
                    )
                }
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } }
    )
}
