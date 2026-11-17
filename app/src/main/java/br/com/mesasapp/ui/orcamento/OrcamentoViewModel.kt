package br.com.mesasapp.ui.orcamento

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.mesasapp.data.preferences.ConfigStore
import br.com.mesasapp.data.repository.AluguelRepository
import br.com.mesasapp.domain.model.Item
import br.com.mesasapp.domain.model.Locacao
import br.com.mesasapp.domain.model.LocacaoLinha
import br.com.mesasapp.domain.model.LocacaoPeca
import br.com.mesasapp.domain.model.StatusLocacao
import br.com.mesasapp.domain.model.TipoLinha
import br.com.mesasapp.util.Datas
import br.com.mesasapp.util.formatAsPlain
import br.com.mesasapp.util.toCentsOrNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

/** Uma linha de "quanto voce quer" na tela principal: um jogo ou um item avulso. */
data class Opcao(
    val chave: String,
    val tipo: TipoLinha,
    val refId: Long,
    val nome: String,
    val detalhe: String,
    val precoDiariaCentavos: Long,
    val quantidade: Int,
    /** Quantas pecas de cada item uma unidade consome (o jogo consome mesa e cadeiras). */
    val consumo: Map<Long, Int>
)

data class OrcamentoUiState(
    val opcoes: List<Opcao> = emptyList(),
    val dataEntrega: LocalDate = LocalDate.now(),
    val horaEntrega: LocalTime = LocalTime.of(8, 0),
    val dataRetirada: LocalDate = LocalDate.now().plusDays(1),
    val horaRetirada: LocalTime = LocalTime.of(18, 0),
    val freteTexto: String = "",
    val faltas: List<String> = emptyList(),
    val subtotalCentavos: Long = 0,
    val freteCentavos: Long = 0,
    val totalCentavos: Long = 0,
    val dias: Int = 1,
    val catalogoVazio: Boolean = false,
    val semPrecoCadastrado: Boolean = false,
    val salvando: Boolean = false,
    val mensagem: String? = null
) {
    val temItensSelecionados: Boolean get() = opcoes.any { it.quantidade > 0 }
    val disponivel: Boolean get() = faltas.isEmpty()
}

/** O que a pessoa escolheu; o resto do estado e derivado disso e do banco. */
private data class Entrada(
    val quantidades: Map<String, Int> = emptyMap(),
    val dataEntrega: LocalDate = LocalDate.now(),
    val horaEntrega: LocalTime = LocalTime.of(8, 0),
    val dataRetirada: LocalDate = LocalDate.now().plusDays(1),
    val horaRetirada: LocalTime = LocalTime.of(18, 0),
    val freteTexto: String = "",
    val horasDoConfigAplicadas: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class OrcamentoViewModel(
    private val repository: AluguelRepository,
    private val configStore: ConfigStore
) : ViewModel() {

    private val entrada = MutableStateFlow(Entrada())
    private val mensagem = MutableStateFlow<String?>(null)
    private val salvando = MutableStateFlow(false)

    private val janela = entrada
        .map { inicioEpoch(it) to fimEpoch(it) }
        .distinctUntilChanged()

    private val reservas = janela.flatMapLatest { (inicio, fim) ->
        repository.reservas(inicio, fim)
    }

    val uiState: StateFlow<OrcamentoUiState> = combine(
        repository.itensAtivos(),
        repository.combosAtivos(),
        entrada,
        reservas,
        combine(mensagem, salvando) { m, s -> m to s }
    ) { itens, combos, atual, reservado, (msg, gravando) ->
        montarEstado(itens, combos, atual, reservado, msg, gravando)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OrcamentoUiState())

    init {
        // As horas padrao do negocio entram uma vez, sem sobrescrever o que a
        // pessoa ja ajustou na tela.
        viewModelScope.launch {
            val config = configStore.config.first()
            entrada.update { e ->
                if (e.horasDoConfigAplicadas) {
                    e
                } else {
                    e.copy(
                        horaEntrega = LocalTime.of(config.horaEntregaPadrao, 0),
                        horaRetirada = LocalTime.of(config.horaRetiradaPadrao, 0),
                        freteTexto = if (config.fretePadraoCentavos > 0) {
                            config.fretePadraoCentavos.formatAsPlain()
                        } else {
                            e.freteTexto
                        },
                        horasDoConfigAplicadas = true
                    )
                }
            }
        }
    }

    // --- acoes da tela ---

    fun mudarQuantidade(chave: String, delta: Int) = entrada.update { e ->
        val atual = e.quantidades[chave] ?: 0
        val novo = (atual + delta).coerceAtLeast(0)
        e.copy(quantidades = e.quantidades + (chave to novo))
    }

    fun definirQuantidade(chave: String, valor: Int) = entrada.update { e ->
        e.copy(quantidades = e.quantidades + (chave to valor.coerceAtLeast(0)))
    }

    fun definirDataEntrega(data: LocalDate) = entrada.update { e ->
        val retirada = if (e.dataRetirada.isBefore(data)) data else e.dataRetirada
        e.copy(dataEntrega = data, dataRetirada = retirada)
    }

    fun definirDataRetirada(data: LocalDate) = entrada.update { e ->
        e.copy(dataRetirada = if (data.isBefore(e.dataEntrega)) e.dataEntrega else data)
    }

    fun definirHoraEntrega(hora: LocalTime) = entrada.update { it.copy(horaEntrega = hora) }
    fun definirHoraRetirada(hora: LocalTime) = entrada.update { it.copy(horaRetirada = hora) }
    fun definirFrete(texto: String) = entrada.update { it.copy(freteTexto = texto) }

    fun limpar() = entrada.update { it.copy(quantidades = emptyMap()) }

    fun mensagemLida() { mensagem.value = null }

    /**
     * Grava o orcamento como locacao. [confirmar] decide se ja nasce CONFIRMADA
     * (ocupando estoque) ou se fica como ORCAMENTO, que nao reserva nada.
     */
    fun salvar(
        clienteNome: String,
        telefone: String,
        endereco: String,
        sinalTexto: String,
        descontoTexto: String,
        observacoes: String,
        confirmar: Boolean,
        aoTerminar: (Long) -> Unit = {}
    ) {
        val estado = uiState.value
        if (!estado.temItensSelecionados) {
            mensagem.value = "Escolha pelo menos um item."
            return
        }

        viewModelScope.launch {
            salvando.value = true
            try {
                val e = entrada.value
                val clienteId = repository.garantirCliente(clienteNome, telefone, endereco)

                val linhas = estado.opcoes.filter { it.quantidade > 0 }.map { opcao ->
                    LocacaoLinha(
                        tipo = opcao.tipo,
                        refId = opcao.refId,
                        descricao = opcao.nome,
                        quantidade = opcao.quantidade,
                        precoDiariaCentavos = opcao.precoDiariaCentavos
                    )
                }

                val porId = repository.itensAtivos().first().associateBy { it.id }

                val pecas = necessidadePorItem(estado.opcoes).map { (itemId, quantidade) ->
                    val item = porId[itemId]
                    LocacaoPeca(
                        itemId = itemId,
                        descricao = item?.nome ?: "Item $itemId",
                        quantidade = quantidade,
                        valorReposicaoCentavos = item?.valorReposicaoCentavos ?: 0
                    )
                }

                val locacao = Locacao(
                    clienteId = clienteId,
                    clienteNome = clienteNome.trim(),
                    telefone = telefone.trim(),
                    endereco = endereco.trim(),
                    inicioEpochSeg = inicioEpoch(e),
                    fimEpochSeg = fimEpoch(e),
                    freteCentavos = e.freteTexto.toCentsOrNull() ?: 0,
                    descontoCentavos = descontoTexto.toCentsOrNull() ?: 0,
                    sinalCentavos = sinalTexto.toCentsOrNull() ?: 0,
                    observacoes = observacoes.trim(),
                    status = if (confirmar) StatusLocacao.CONFIRMADA else StatusLocacao.ORCAMENTO,
                    criadoEmEpochSeg = Datas.agoraEpochSeg()
                )

                val id = repository.salvarLocacao(locacao, linhas, pecas)
                mensagem.value = if (confirmar) "Locacao confirmada." else "Orcamento salvo."
                entrada.update { it.copy(quantidades = emptyMap()) }
                aoTerminar(id)
            } finally {
                salvando.value = false
            }
        }
    }

    // --- calculo ---

    private fun inicioEpoch(e: Entrada) = Datas.paraEpochSeg(e.dataEntrega, e.horaEntrega)
    private fun fimEpoch(e: Entrada) = Datas.paraEpochSeg(e.dataRetirada, e.horaRetirada)

    private fun montarEstado(
        itens: List<Item>,
        combos: List<br.com.mesasapp.domain.model.Combo>,
        e: Entrada,
        reservado: Map<Long, Int>,
        msg: String?,
        gravando: Boolean
    ): OrcamentoUiState {
        val porId = itens.associateBy { it.id }

        val opcoesCombo = combos.mapNotNull { combo ->
            val mesa = porId[combo.itemMesaId] ?: return@mapNotNull null
            val cadeira = porId[combo.itemCadeiraId] ?: return@mapNotNull null
            val chave = "C:${combo.id}"
            Opcao(
                chave = chave,
                tipo = TipoLinha.COMBO,
                refId = combo.id,
                nome = combo.nome,
                detalhe = "1 ${mesa.nome} + ${combo.qtdCadeiras} ${cadeira.nome}",
                precoDiariaCentavos = combo.precoDiariaCentavos,
                quantidade = e.quantidades[chave] ?: 0,
                consumo = mapOf(mesa.id to 1, cadeira.id to combo.qtdCadeiras)
            )
        }

        val opcoesItem = itens.map { item ->
            val chave = "I:${item.id}"
            Opcao(
                chave = chave,
                tipo = TipoLinha.ITEM,
                refId = item.id,
                nome = item.nome,
                detalhe = descricaoItem(item),
                precoDiariaCentavos = item.precoDiariaCentavos,
                quantidade = e.quantidades[chave] ?: 0,
                consumo = mapOf(item.id to 1)
            )
        }

        val opcoes = opcoesCombo + opcoesItem
        val selecionadas = opcoes.filter { it.quantidade > 0 }

        val necessidade = necessidadePorItem(opcoes)
        val faltas = necessidade.mapNotNull { (itemId, precisa) ->
            val item = porId[itemId] ?: return@mapNotNull null
            val livre = item.quantidadeTotal - (reservado[itemId] ?: 0)
            if (precisa > livre) {
                val falta = precisa - livre
                "Faltam $falta de ${item.nome} (livres: ${livre.coerceAtLeast(0)})"
            } else {
                null
            }
        }

        val subtotal = selecionadas.sumOf { it.quantidade * it.precoDiariaCentavos }
        val frete = e.freteTexto.toCentsOrNull() ?: 0

        return OrcamentoUiState(
            opcoes = opcoes,
            dataEntrega = e.dataEntrega,
            horaEntrega = e.horaEntrega,
            dataRetirada = e.dataRetirada,
            horaRetirada = e.horaRetirada,
            freteTexto = e.freteTexto,
            faltas = faltas,
            subtotalCentavos = subtotal,
            freteCentavos = frete,
            totalCentavos = subtotal + frete,
            dias = Datas.dias(inicioEpoch(e), fimEpoch(e)),
            catalogoVazio = opcoes.isEmpty(),
            semPrecoCadastrado = selecionadas.isNotEmpty() && selecionadas.all { it.precoDiariaCentavos == 0L },
            salvando = gravando,
            mensagem = msg
        )
    }

    private fun descricaoItem(item: Item): String = buildList {
        add(item.categoria.name.lowercase().replaceFirstChar { it.uppercase() })
        add(item.material.name.lowercase())
        if (item.cor.isNotBlank()) add(item.cor.lowercase())
        when (item.comBraco) {
            true -> add("com braco")
            false -> add("sem braco")
            null -> Unit
        }
        add("${item.quantidadeTotal} no estoque")
    }.joinToString(" · ")

    /** Soma, por item fisico, tudo que as opcoes escolhidas consomem. */
    private fun necessidadePorItem(opcoes: List<Opcao>): Map<Long, Int> {
        val total = mutableMapOf<Long, Int>()
        opcoes.filter { it.quantidade > 0 }.forEach { opcao ->
            opcao.consumo.forEach { (itemId, porUnidade) ->
                total[itemId] = (total[itemId] ?: 0) + porUnidade * opcao.quantidade
            }
        }
        return total
    }
}
