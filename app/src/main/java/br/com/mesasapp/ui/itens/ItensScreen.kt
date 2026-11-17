package br.com.mesasapp.ui.itens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import br.com.mesasapp.domain.model.Categoria
import br.com.mesasapp.domain.model.Combo
import br.com.mesasapp.domain.model.Item
import br.com.mesasapp.domain.model.Material
import br.com.mesasapp.ui.theme.MesasappTheme
import br.com.mesasapp.util.formatAsPlain
import br.com.mesasapp.util.toCentsOrNull

/**
 * Cores oferecidas no cadastro. Poucas de proposito: o que a locadora tem de
 * verdade e preto, branco e vermelho. "Outra" existe para o caso raro, sem
 * encher a tela de opcoes que ninguem usa.
 */
private val CORES = listOf("Branca", "Preta", "Vermelha")

/** Estoque: o que existe, o que esta na rua, o que quebrou e quanto custa. */
@Composable
fun ItensScreen(container: AppContainer) {
    val viewModel: ItensViewModel = viewModel(
        factory = viewModelFactory { initializer { ItensViewModel(container.repository) } }
    )
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    var cadastrando by remember { mutableStateOf(false) }
    var editandoCombo by remember { mutableStateOf<Combo?>(null) }

    LaunchedEffect(estado.mensagem) {
        estado.mensagem?.let {
            snackbar.showSnackbar(it)
            viewModel.mensagemLida()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text("Estoque", style = MaterialTheme.typography.headlineMedium) }

            item {
                CabecalhoGrupo("Mesas", estado.totalMesas, estado.mesas.size)
            }
            items(estado.mesas, key = { "m${it.item.id}" }) { linha ->
                CartaoItem(linha, viewModel)
            }

            item {
                CabecalhoGrupo("Cadeiras", estado.totalCadeiras, estado.cadeiras.size)
            }
            items(estado.cadeiras, key = { "c${it.item.id}" }) { linha ->
                CartaoItem(linha, viewModel)
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Jogos", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = {
                            val mesa = estado.todosItens.firstOrNull { it.categoria == Categoria.MESA }
                            val cadeira = estado.todosItens.firstOrNull { it.categoria == Categoria.CADEIRA }
                            if (mesa != null && cadeira != null) {
                                editandoCombo = Combo(itemMesaId = mesa.id, itemCadeiraId = cadeira.id)
                            }
                        }
                    ) { Text("Novo jogo") }
                }
            }

            items(estado.combos, key = { "j${it.id}" }) { combo ->
                val mesa = estado.todosItens.firstOrNull { it.id == combo.itemMesaId }
                val cadeira = estado.todosItens.firstOrNull { it.id == combo.itemCadeiraId }
                Card(onClick = { editandoCombo = combo }) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(combo.nome, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "1 ${mesa?.nome ?: "?"} + ${combo.qtdCadeiras} ${cadeira?.nome ?: "?"} · " +
                                    "${combo.precoDiariaCentavos.formatAsPlain()}/dia",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { viewModel.removerCombo(combo) }) { Text("Remover") }
                    }
                }
            }

            item {
                Text(
                    "O jogo nao tem estoque proprio: alugar um jogo baixa 1 mesa e as cadeiras dele do " +
                        "estoque real, para a mesma cadeira nunca sair em dois pedidos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                if (cadastrando) {
                    FormularioItem(
                        aoCancelar = { cadastrando = false },
                        aoSalvar = { viewModel.salvarItem(it); cadastrando = false }
                    )
                } else {
                    Button(onClick = { cadastrando = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Cadastrar modelo")
                    }
                }
            }
        }
    }

    editandoCombo?.let { combo ->
        DialogoCombo(
            combo = combo,
            itens = estado.todosItens,
            aoFechar = { editandoCombo = null },
            aoSalvar = { viewModel.salvarCombo(it); editandoCombo = null }
        )
    }
}

@Composable
private fun CabecalhoGrupo(titulo: String, total: Int, modelos: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Text(
            "$total no total · $modelos ${if (modelos == 1) "modelo" else "modelos"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CartaoItem(linha: ItemNoEstoque, viewModel: ItensViewModel) {
    val item = linha.item

    Card {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (item.ativo) item.nome else "${item.nome} (inativo)",
                style = MaterialTheme.typography.titleMedium
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AssistChip(onClick = {}, label = { Text(item.material.name.lowercase().replaceFirstChar { it.uppercase() }) })
                when (item.comBraco) {
                    true -> AssistChip(onClick = {}, label = { Text("Com braco") })
                    false -> AssistChip(onClick = {}, label = { Text("Sem braco") })
                    null -> Unit
                }
                if (item.cor.isNotBlank()) AssistChip(onClick = {}, label = { Text(item.cor) })
            }

            if (item.quantidadeTotal > 0) {
                LinearProgressIndicator(
                    progress = { linha.livres.toFloat() / item.quantidadeTotal },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row {
                Numero("Livres agora", linha.livres.toString(), MesasappTheme.estado.ok, Modifier.weight(1f))
                Numero("Alugadas", linha.alugadas.toString(), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
                Numero("Quebradas", linha.quebradas.toString(), MesasappTheme.estado.erro, Modifier.weight(1f))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CampoNumero(
                    rotulo = "Total",
                    valor = item.quantidadeTotal.toString(),
                    modifier = Modifier.weight(1f)
                ) { texto -> viewModel.salvarItem(item.copy(quantidadeTotal = texto.toIntOrNull() ?: 0)) }

                CampoNumero(
                    rotulo = "Diaria R$",
                    valor = if (item.precoDiariaCentavos > 0) item.precoDiariaCentavos.formatAsPlain() else "",
                    decimal = true,
                    modifier = Modifier.weight(1f)
                ) { texto -> viewModel.salvarItem(item.copy(precoDiariaCentavos = texto.toCentsOrNull() ?: 0)) }

                CampoNumero(
                    rotulo = "Reposicao R$",
                    valor = if (item.valorReposicaoCentavos > 0) item.valorReposicaoCentavos.formatAsPlain() else "",
                    decimal = true,
                    modifier = Modifier.weight(1f)
                ) { texto -> viewModel.salvarItem(item.copy(valorReposicaoCentavos = texto.toCentsOrNull() ?: 0)) }
            }

            Row {
                if (!item.ativo) {
                    TextButton(onClick = { viewModel.reativarItem(item) }) { Text("Reativar") }
                }
                TextButton(onClick = { viewModel.removerItem(item) }) { Text("Remover modelo") }
            }
        }
    }
}

@Composable
private fun Numero(
    rotulo: String,
    valor: String,
    cor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(rotulo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.titleLarge, color = cor, fontWeight = FontWeight.Bold)
    }
}

/**
 * Campo que guarda o texto enquanto a pessoa digita e so avisa quem salva.
 * Sem o estado local, apagar o campo salvaria zero e o cursor pularia.
 */
@Composable
private fun CampoNumero(
    rotulo: String,
    valor: String,
    decimal: Boolean = false,
    modifier: Modifier = Modifier,
    aoMudar: (String) -> Unit
) {
    var texto by remember(valor) { mutableStateOf(valor) }

    OutlinedTextField(
        value = texto,
        onValueChange = { novo ->
            texto = if (decimal) novo.filter { it.isDigit() || it == ',' || it == '.' } else novo.filter { it.isDigit() }
            aoMudar(texto)
        },
        label = { Text(rotulo, style = MaterialTheme.typography.labelSmall) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number
        ),
        modifier = modifier
    )
}

@Composable
private fun FormularioItem(aoCancelar: () -> Unit, aoSalvar: (Item) -> Unit) {
    var categoria by remember { mutableStateOf(Categoria.CADEIRA) }
    var material by remember { mutableStateOf(Material.PLASTICO) }
    var comBraco by remember { mutableStateOf(false) }
    var cor by remember { mutableStateOf(CORES.first()) }
    var outraCor by remember { mutableStateOf("") }
    var quantidade by remember { mutableStateOf("") }
    var diaria by remember { mutableStateOf("") }
    var reposicao by remember { mutableStateOf("") }

    val corFinal = if (cor == "Outra") outraCor.trim() else cor
    val nome = buildString {
        append(if (categoria == Categoria.MESA) "Mesa" else "Cadeira")
        append(" de ${material.name.lowercase()}")
        if (categoria == Categoria.CADEIRA) append(if (comBraco) " com braco" else " sem braco")
        if (corFinal.isNotBlank()) append(" ${corFinal.lowercase()}")
    }

    Card {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Cadastrar modelo", style = MaterialTheme.typography.titleMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Categoria.entries.forEach { opcao ->
                    FilterChip(
                        selected = categoria == opcao,
                        onClick = { categoria = opcao },
                        label = { Text(if (opcao == Categoria.MESA) "Mesa" else "Cadeira") }
                    )
                }
            }

            Text("Material", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Material.entries.forEach { opcao ->
                    FilterChip(
                        selected = material == opcao,
                        onClick = { material = opcao },
                        label = { Text(opcao.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            if (categoria == Categoria.CADEIRA) {
                Text("Braco", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !comBraco, onClick = { comBraco = false }, label = { Text("Sem braco") })
                    FilterChip(selected = comBraco, onClick = { comBraco = true }, label = { Text("Com braco") })
                }
            }

            Text("Cor", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (CORES + "Outra").forEach { opcao ->
                    FilterChip(
                        selected = cor == opcao,
                        onClick = { cor = opcao },
                        label = { Text(opcao) }
                    )
                }
            }
            if (cor == "Outra") {
                OutlinedTextField(
                    outraCor, { outraCor = it },
                    label = { Text("Qual cor") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CampoNumero("Quantidade", quantidade, modifier = Modifier.weight(1f)) { quantidade = it }
                CampoNumero("Diaria R$", diaria, decimal = true, modifier = Modifier.weight(1f)) { diaria = it }
                CampoNumero("Reposicao R$", reposicao, decimal = true, modifier = Modifier.weight(1f)) { reposicao = it }
            }

            Text(
                "Vai aparecer como: $nome",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = aoCancelar) { Text("Cancelar") }
                Button(
                    enabled = corFinal.isNotBlank(),
                    onClick = {
                        aoSalvar(
                            Item(
                                nome = nome.replaceFirstChar { it.uppercase() },
                                categoria = categoria,
                                material = material,
                                comBraco = if (categoria == Categoria.CADEIRA) comBraco else null,
                                cor = corFinal,
                                quantidadeTotal = quantidade.toIntOrNull() ?: 0,
                                precoDiariaCentavos = diaria.toCentsOrNull() ?: 0,
                                valorReposicaoCentavos = reposicao.toCentsOrNull() ?: 0
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Adicionar ao estoque") }
            }
        }
    }
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

    androidx.compose.material3.AlertDialog(
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

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CampoNumero("Cadeiras por jogo", qtdCadeiras, modifier = Modifier.weight(1f)) { qtdCadeiras = it }
                    CampoNumero("Diaria R$", diaria, decimal = true, modifier = Modifier.weight(1f)) { diaria = it }
                }
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
