package br.com.mesasapp.ui.itens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
private const val OUTRA_COR = "Outra"

/** Estoque: o que existe, o que esta na rua, o que quebrou e quanto custa. */
@Composable
fun ItensScreen(container: AppContainer) {
    val viewModel: ItensViewModel = viewModel(
        factory = viewModelFactory { initializer { ItensViewModel(container.repository) } }
    )
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    // null = ninguem editando; Item(id = 0) = cadastro novo.
    var emEdicao by remember { mutableStateOf<Item?>(null) }
    var editandoCombo by remember { mutableStateOf<Combo?>(null) }

    LaunchedEffect(estado.mensagem) {
        estado.mensagem?.let {
            snackbar.showSnackbar(it)
            viewModel.mensagemLida()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).testTag("lista_estoque"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text("Estoque", style = MaterialTheme.typography.headlineMedium) }

            // O cadastro fica no topo: enfiado depois da lista, era preciso rolar
            // o estoque inteiro para chegar nele.
            item {
                Button(
                    onClick = { emEdicao = Item(nome = "", categoria = Categoria.CADEIRA) },
                    modifier = Modifier.fillMaxWidth().testTag("botao_cadastrar_modelo")
                ) { Text("Cadastrar modelo") }
            }

            item { CabecalhoGrupo("Mesas", estado.totalMesas, estado.mesas.size) }
            items(estado.mesas, key = { "m${it.item.id}" }) { linha ->
                CartaoItem(
                    linha = linha,
                    aoEditar = { emEdicao = linha.item },
                    aoRemover = { viewModel.removerItem(linha.item) },
                    aoReativar = { viewModel.reativarItem(linha.item) }
                )
            }

            item { CabecalhoGrupo("Cadeiras", estado.totalCadeiras, estado.cadeiras.size) }
            items(estado.cadeiras, key = { "c${it.item.id}" }) { linha ->
                CartaoItem(
                    linha = linha,
                    aoEditar = { emEdicao = linha.item },
                    aoRemover = { viewModel.removerItem(linha.item) },
                    aoReativar = { viewModel.reativarItem(linha.item) }
                )
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
                CartaoCombo(
                    combo = combo,
                    itens = estado.todosItens,
                    aoEditar = { editandoCombo = combo },
                    aoRemover = { viewModel.removerCombo(combo) }
                )
            }

            item {
                Text(
                    "O jogo nao tem estoque proprio: alugar um jogo baixa 1 mesa e as cadeiras dele do " +
                        "estoque real, para a mesma cadeira nunca sair em dois pedidos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    emEdicao?.let { item ->
        FormularioModelo(
            inicial = item,
            aoFechar = { emEdicao = null },
            aoSalvar = {
                viewModel.salvarItem(it)
                emEdicao = null
            }
        )
    }

    editandoCombo?.let { combo ->
        FormularioJogo(
            combo = combo,
            itens = estado.todosItens,
            aoFechar = { editandoCombo = null },
            aoSalvar = {
                viewModel.salvarCombo(it)
                editandoCombo = null
            }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CartaoItem(
    linha: ItemNoEstoque,
    aoEditar: () -> Unit,
    aoRemover: () -> Unit,
    aoReativar: () -> Unit
) {
    val item = linha.item

    Card(modifier = Modifier.semantics { contentDescription = "Modelo ${item.nome}" }) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (item.ativo) item.nome else "${item.nome} (inativo)",
                style = MaterialTheme.typography.titleMedium
            )

            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AssistChip(
                    onClick = aoEditar,
                    label = { Text(item.material.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
                when (item.comBraco) {
                    true -> AssistChip(onClick = aoEditar, label = { Text("Com braco") })
                    false -> AssistChip(onClick = aoEditar, label = { Text("Sem braco") })
                    null -> Unit
                }
                if (item.cor.isNotBlank()) AssistChip(onClick = aoEditar, label = { Text(item.cor) })
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

            Row {
                Valor("Total", item.quantidadeTotal.toString(), Modifier.weight(1f))
                Valor(
                    "Diaria",
                    if (item.precoDiariaCentavos > 0) "R$ ${item.precoDiariaCentavos.formatAsPlain()}" else "—",
                    Modifier.weight(1f)
                )
                Valor(
                    "Reposicao",
                    if (item.valorReposicaoCentavos > 0) "R$ ${item.valorReposicaoCentavos.formatAsPlain()}" else "—",
                    Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = aoEditar) { Text("Editar") }
                if (!item.ativo) {
                    TextButton(onClick = aoReativar) { Text("Reativar") }
                }
                TextButton(onClick = aoRemover) { Text("Remover") }
            }
        }
    }
}

@Composable
private fun CartaoCombo(combo: Combo, itens: List<Item>, aoEditar: () -> Unit, aoRemover: () -> Unit) {
    val mesa = itens.firstOrNull { it.id == combo.itemMesaId }
    val cadeira = itens.firstOrNull { it.id == combo.itemCadeiraId }

    Card {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(combo.nome, style = MaterialTheme.typography.titleSmall)
                Text(
                    "1 ${mesa?.nome ?: "?"} + ${combo.qtdCadeiras} ${cadeira?.nome ?: "?"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    if (combo.precoDiariaCentavos > 0) {
                        "R$ ${combo.precoDiariaCentavos.formatAsPlain()} por jogo por dia"
                    } else {
                        "sem preco"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Button(onClick = aoEditar) { Text("Editar") }
            TextButton(onClick = aoRemover) { Text("Remover") }
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

@Composable
private fun Valor(rotulo: String, valor: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(rotulo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

/**
 * Cadastro e edicao de um modelo, em tela cheia.
 *
 * O texto digitado vive **so aqui** ate a pessoa tocar em Salvar. A versao
 * anterior gravava a cada tecla e o valor voltava formatado do banco no meio da
 * digitacao: digitar "13" na diaria era impossivel.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormularioModelo(inicial: Item, aoFechar: () -> Unit, aoSalvar: (Item) -> Unit) {
    var categoria by remember { mutableStateOf(inicial.categoria) }
    var material by remember { mutableStateOf(inicial.material) }
    var comBraco by remember { mutableStateOf(inicial.comBraco ?: false) }
    var cor by remember { mutableStateOf(if (inicial.cor.isBlank() || inicial.cor in CORES) inicial.cor.ifBlank { CORES.first() } else OUTRA_COR) }
    var outraCor by remember { mutableStateOf(if (inicial.cor.isNotBlank() && inicial.cor !in CORES) inicial.cor else "") }
    var quantidade by remember { mutableStateOf(if (inicial.quantidadeTotal > 0) inicial.quantidadeTotal.toString() else "") }
    var diaria by remember { mutableStateOf(if (inicial.precoDiariaCentavos > 0) inicial.precoDiariaCentavos.formatAsPlain() else "") }
    var reposicao by remember { mutableStateOf(if (inicial.valorReposicaoCentavos > 0) inicial.valorReposicaoCentavos.formatAsPlain() else "") }
    var nomeManual by remember { mutableStateOf(inicial.id != 0L) }
    var nome by remember { mutableStateOf(inicial.nome) }

    val corFinal = if (cor == OUTRA_COR) outraCor.trim() else cor
    val nomeSugerido = buildString {
        append(if (categoria == Categoria.MESA) "Mesa" else "Cadeira")
        append(" de ${material.name.lowercase()}")
        if (categoria == Categoria.CADEIRA) append(if (comBraco) " com braco" else " sem braco")
        if (corFinal.isNotBlank()) append(" ${corFinal.lowercase()}")
    }.replaceFirstChar { it.uppercase() }

    val nomeFinal = if (nomeManual && nome.isNotBlank()) nome.trim() else nomeSugerido

    androidx.compose.ui.window.Dialog(
        onDismissRequest = aoFechar,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        androidx.compose.material3.Surface(modifier = Modifier.fillMaxWidth()) {
            LazyColumn(
                modifier = Modifier.testTag("form_modelo"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        if (inicial.id == 0L) "Cadastrar modelo" else "Editar modelo",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Categoria.entries.forEach { opcao ->
                            FilterChip(
                                selected = categoria == opcao,
                                onClick = { categoria = opcao },
                                label = { Text(if (opcao == Categoria.MESA) "Mesa" else "Cadeira") }
                            )
                        }
                    }
                }

                item {
                    Column {
                        Text("Material", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Material.entries.forEach { opcao ->
                                FilterChip(
                                    selected = material == opcao,
                                    onClick = { material = opcao },
                                    label = { Text(rotuloMaterial(opcao)) }
                                )
                            }
                        }
                    }
                }

                if (categoria == Categoria.CADEIRA) {
                    item {
                        Column {
                            Text("Braco", style = MaterialTheme.typography.labelLarge)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = !comBraco,
                                    onClick = { comBraco = false },
                                    label = { Text("Sem braco") }
                                )
                                FilterChip(
                                    selected = comBraco,
                                    onClick = { comBraco = true },
                                    label = { Text("Com braco") }
                                )
                            }
                        }
                    }
                }

                item {
                    Column {
                        Text("Cor", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            (CORES + OUTRA_COR).forEach { opcao ->
                                FilterChip(
                                    selected = cor == opcao,
                                    onClick = { cor = opcao },
                                    label = { Text(opcao) }
                                )
                            }
                        }
                    }
                }

                if (cor == OUTRA_COR) {
                    item {
                        OutlinedTextField(
                            outraCor, { outraCor = it },
                            label = { Text("Qual cor") }, singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = quantidade,
                        onValueChange = { quantidade = it.filter(Char::isDigit) },
                        label = { Text("Quantidade em estoque") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("campo_quantidade")
                    )
                }

                item {
                    OutlinedTextField(
                        value = diaria,
                        onValueChange = { diaria = it.filter { c -> c.isDigit() || c == ',' || c == '.' } },
                        label = { Text("Preco da diaria") },
                        prefix = { Text("R$ ") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().testTag("campo_diaria")
                    )
                }

                item {
                    OutlinedTextField(
                        value = reposicao,
                        onValueChange = { reposicao = it.filter { c -> c.isDigit() || c == ',' || c == '.' } },
                        label = { Text("Valor de reposicao (se quebrar)") },
                        prefix = { Text("R$ ") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Column {
                        Text(
                            "Vai aparecer como: $nomeFinal",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = { nomeManual = !nomeManual; if (!nomeManual) nome = "" }) {
                            Text(if (nomeManual) "Usar o nome automatico" else "Escrever outro nome")
                        }
                        if (nomeManual) {
                            OutlinedTextField(
                                value = nome,
                                onValueChange = { nome = it },
                                label = { Text("Nome do modelo") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = aoFechar, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                        Button(
                            enabled = corFinal.isNotBlank(),
                            onClick = {
                                aoSalvar(
                                    inicial.copy(
                                        nome = nomeFinal,
                                        categoria = categoria,
                                        material = material,
                                        comBraco = if (categoria == Categoria.CADEIRA) comBraco else null,
                                        cor = corFinal,
                                        quantidadeTotal = quantidade.toIntOrNull() ?: 0,
                                        precoDiariaCentavos = diaria.toCentsOrNull() ?: 0,
                                        valorReposicaoCentavos = reposicao.toCentsOrNull() ?: 0,
                                        ativo = true
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f).testTag("botao_salvar_modelo")
                        ) { Text("Salvar") }
                    }
                }
            }
        }
    }
}

private fun rotuloMaterial(material: Material): String = when (material) {
    Material.PLASTICO -> "Plastico"
    Material.METAL -> "Metal"
    Material.MADEIRA -> "Madeira"
    Material.OUTRO -> "Outro"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormularioJogo(
    combo: Combo,
    itens: List<Item>,
    aoFechar: () -> Unit,
    aoSalvar: (Combo) -> Unit
) {
    var nome by remember { mutableStateOf(combo.nome) }
    var mesaId by remember { mutableStateOf(combo.itemMesaId) }
    var cadeiraId by remember { mutableStateOf(combo.itemCadeiraId) }
    var qtdCadeiras by remember { mutableStateOf(combo.qtdCadeiras.toString()) }
    var diaria by remember {
        mutableStateOf(if (combo.precoDiariaCentavos > 0) combo.precoDiariaCentavos.formatAsPlain() else "")
    }

    val mesas = itens.filter { it.categoria == Categoria.MESA && it.ativo }
    val cadeiras = itens.filter { it.categoria == Categoria.CADEIRA && it.ativo }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = aoFechar,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        androidx.compose.material3.Surface(modifier = Modifier.fillMaxWidth()) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        if (combo.id == 0L) "Novo jogo" else "Editar jogo",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                item {
                    OutlinedTextField(
                        nome, { nome = it }, label = { Text("Nome") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Column {
                        Text("Modelo da mesa", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            mesas.forEach { mesa ->
                                FilterChip(
                                    selected = mesaId == mesa.id,
                                    onClick = { mesaId = mesa.id },
                                    label = { Text(mesa.nome) }
                                )
                            }
                        }
                    }
                }

                item {
                    Column {
                        Text("Modelo da cadeira", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            cadeiras.forEach { cadeira ->
                                FilterChip(
                                    selected = cadeiraId == cadeira.id,
                                    onClick = { cadeiraId = cadeira.id },
                                    label = { Text(cadeira.nome) }
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = qtdCadeiras,
                        onValueChange = { qtdCadeiras = it.filter(Char::isDigit) },
                        label = { Text("Cadeiras por jogo") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = diaria,
                        onValueChange = { diaria = it.filter { c -> c.isDigit() || c == ',' || c == '.' } },
                        label = { Text("Preco do jogo por dia") },
                        prefix = { Text("R$ ") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = aoFechar, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                        Button(
                            enabled = nome.isNotBlank() &&
                                mesas.any { it.id == mesaId } &&
                                cadeiras.any { it.id == cadeiraId },
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
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Salvar") }
                    }
                }
            }
        }
    }
}
