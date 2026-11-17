package br.com.mesasapp.ui.novo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.mesasapp.data.preferences.ConfigStore
import br.com.mesasapp.data.repository.AluguelRepository
import br.com.mesasapp.domain.model.Combo
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

/** Uma linha escolhivel do pedido: o jogo ou um item avulso. */
data class Opcao(
    val chave: String,
    val tipo: TipoLinha,
    val refId: Long,
    val nome: String,
    val detalhe: String,
    val precoDiariaCentavos: Long,
    val quantidade: Int,
    /** Quantas pecas de cada item uma unidade consome (o jogo consome mesa e cadeiras). */
    val consumo: Map<Long, Int>,
    /** Quantas unidades ainda cabem no periodo escolhido. */
    val disponivel: Int
)

data class NovoPedidoUiState(
    val clienteNome: String = "",
    val telefone: String = "",
    val documento: String = "",
    val endereco: String = "",
    val pontoReferencia: String = "",
    val observacoes: String = "",
    val dataEntrega: LocalDate = LocalDate.now(),
    val horaEntrega: LocalTime = LocalTime.of(8, 0),
    val dataRetirada: LocalDate = LocalDate.now().plusDays(1),
    val horaRetirada: LocalTime = LocalTime.of(18, 0),
    val dias: Int = 1,
    val opcoes: List<Opcao> = emptyList(),
    val freteTexto: String = "",
    val descontoTexto: String = "",
    val sinalTexto: String = "",
    val caucaoTexto: String = "",
    val faltas: List<String> = emptyList(),
    val aluguelCentavos: Long = 0,
    val freteCentavos: Long = 0,
    val descontoCentavos: Long = 0,
    val sinalCentavos: Long = 0,
    val caucaoCentavos: Long = 0,
    val catalogoVazio: Boolean = false,
    val salvando: Boolean = false,
    val mensagem: String? = null
) {
    val totalCentavos: Long get() = aluguelCentavos + freteCentavos - descontoCentavos
    val saldoCentavos: Long get() = totalCentavos - sinalCentavos
    val temItens: Boolean get() = opcoes.any { it.quantidade > 0 }
    val disponivel: Boolean get() = faltas.isEmpty()
}

private data class Formulario(
    val clienteNome: String = "",
    val telefone: String = "",
    val documento: String = "",
    val endereco: String = "",
    val pontoReferencia: String = "",
    val observacoes: String = "",
    val quantidades: Map<String, Int> = emptyMap(),
    val dataEntrega: LocalDate = LocalDate.now(),
    val horaEntrega: LocalTime = LocalTime.of(8, 0),
    val dataRetirada: LocalDate = LocalDate.now().plusDays(1),
    val horaRetirada: LocalTime = LocalTime.of(18, 0),
    val freteTexto: String = "",
    val descontoTexto: String = "",
    val sinalTexto: String = "",
    val caucaoTexto: String = "",
    val padroesAplicados: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class NovoPedidoViewModel(
    private val repository: AluguelRepository,
    private val configStore: ConfigStore
) : ViewModel() {

    private val formulario = MutableStateFlow(Formulario())
    private val mensagem = MutableStateFlow<String?>(null)
    private val salvando = MutableStateFlow(false)

    private val reservas = formulario
        .map { inicioEpoch(it) to fimEpoch(it) }
        .distinctUntilChanged()
        .flatMapLatest { (inicio, fim) -> repository.reservas(inicio, fim) }

    val uiState: StateFlow<NovoPedidoUiState> = combine(
        repository.itensAtivos(),
        repository.combosAtivos(),
        formulario,
        reservas,
        combine(mensagem, salvando) { m, s -> m to s }
    ) { itens, combos, form, reservado, (msg, gravando) ->
        montar(itens, combos, form, reservado, msg, gravando)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NovoPedidoUiState())

    init {
        viewModelScope.launch {
            val config = configStore.config.first()
            formulario.update { f ->
                if (f.padroesAplicados) {
                    f
                } else {
                    f.copy(
                        horaEntrega = LocalTime.of(config.horaEntregaPadrao, 0),
                        horaRetirada = LocalTime.of(config.horaRetiradaPadrao, 0),
                        freteTexto = if (config.fretePadraoCentavos > 0) {
                            config.fretePadraoCentavos.formatAsPlain()
                        } else {
                            f.freteTexto
                        },
                        padroesAplicados = true
                    )
                }
            }
        }
    }

    // --- edicao do formulario ---

    fun definirCliente(valor: String) = formulario.update { it.copy(clienteNome = valor) }
    fun definirTelefone(valor: String) = formulario.update { it.copy(telefone = valor) }
    fun definirDocumento(valor: String) = formulario.update { it.copy(documento = valor) }
    fun definirEndereco(valor: String) = formulario.update { it.copy(endereco = valor) }
    fun definirPontoReferencia(valor: String) = formulario.update { it.copy(pontoReferencia = valor) }
    fun definirObservacoes(valor: String) = formulario.update { it.copy(observacoes = valor) }
    fun definirFrete(valor: String) = formulario.update { it.copy(freteTexto = valor) }
    fun definirDesconto(valor: String) = formulario.update { it.copy(descontoTexto = valor) }
    fun definirSinal(valor: String) = formulario.update { it.copy(sinalTexto = valor) }
    fun definirCaucao(valor: String) = formulario.update { it.copy(caucaoTexto = valor) }

    fun mudarQuantidade(chave: String, delta: Int) = formulario.update { f ->
        val atual = f.quantidades[chave] ?: 0
        f.copy(quantidades = f.quantidades + (chave to (atual + delta).coerceAtLeast(0)))
    }

    fun definirDataEntrega(data: LocalDate) = formulario.update { f ->
        f.copy(
            dataEntrega = data,
            dataRetirada = if (f.dataRetirada.isBefore(data)) data else f.dataRetirada
        )
    }

    fun definirDataRetirada(data: LocalDate) = formulario.update { f ->
        f.copy(dataRetirada = if (data.isBefore(f.dataEntrega)) f.dataEntrega else data)
    }

    fun definirHoraEntrega(hora: LocalTime) = formulario.update { it.copy(horaEntrega = hora) }
    fun definirHoraRetirada(hora: LocalTime) = formulario.update { it.copy(horaRetirada = hora) }

    fun limpar() = formulario.update {
        Formulario(
            horaEntrega = it.horaEntrega,
            horaRetirada = it.horaRetirada,
            freteTexto = it.freteTexto,
            padroesAplicados = true
        )
    }

    fun mensagemLida() { mensagem.value = null }

    /**
     * Grava o pedido. [confirmar] decide entre ORCAMENTO, que nao reserva nada, e
     * CONFIRMADA, que passa a ocupar estoque na janela escolhida.
     */
    fun salvar(confirmar: Boolean, aoTerminar: () -> Unit = {}) {
        val estado = uiState.value
        if (!estado.temItens) {
            mensagem.value = "Escolha pelo menos um item."
            return
        }
        if (confirmar && !estado.disponivel) {
            mensagem.value = "Nao da para confirmar: falta item no periodo."
            return
        }

        viewModelScope.launch {
            salvando.value = true
            try {
                val f = formulario.value
                val clienteId = repository.garantirCliente(
                    f.clienteNome, f.telefone, f.documento, f.endereco
                )

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
                val pecas = necessidade(estado.opcoes).map { (itemId, quantidade) ->
                    val item = porId[itemId]
                    LocacaoPeca(
                        itemId = itemId,
                        descricao = item?.nome ?: "Item $itemId",
                        quantidade = quantidade,
                        valorReposicaoCentavos = item?.valorReposicaoCentavos ?: 0
                    )
                }

                repository.salvarLocacao(
                    Locacao(
                        clienteId = clienteId,
                        clienteNome = f.clienteNome.trim(),
                        telefone = f.telefone.trim(),
                        documento = f.documento.trim(),
                        endereco = f.endereco.trim(),
                        pontoReferencia = f.pontoReferencia.trim(),
                        inicioEpochSeg = inicioEpoch(f),
                        fimEpochSeg = fimEpoch(f),
                        freteCentavos = estado.freteCentavos,
                        descontoCentavos = estado.descontoCentavos,
                        sinalCentavos = estado.sinalCentavos,
                        caucaoCentavos = estado.caucaoCentavos,
                        observacoes = f.observacoes.trim(),
                        status = if (confirmar) StatusLocacao.CONFIRMADA else StatusLocacao.ORCAMENTO,
                        criadoEmEpochSeg = Datas.agoraEpochSeg()
                    ),
                    linhas,
                    pecas
                )

                mensagem.value = if (confirmar) "Pedido confirmado." else "Orcamento salvo."
                limpar()
                aoTerminar()
            } finally {
                salvando.value = false
            }
        }
    }

    // --- calculo ---

    private fun inicioEpoch(f: Formulario) = Datas.paraEpochSeg(f.dataEntrega, f.horaEntrega)
    private fun fimEpoch(f: Formulario) = Datas.paraEpochSeg(f.dataRetirada, f.horaRetirada)

    private fun montar(
        itens: List<Item>,
        combos: List<Combo>,
        f: Formulario,
        reservado: Map<Long, Int>,
        msg: String?,
        gravando: Boolean
    ): NovoPedidoUiState {
        val porId = itens.associateBy { it.id }
        fun livre(itemId: Long): Int =
            ((porId[itemId]?.quantidadeTotal ?: 0) - (reservado[itemId] ?: 0)).coerceAtLeast(0)

        /** Quantas unidades da opcao cabem: manda o item que acaba primeiro. */
        fun cabem(consumo: Map<Long, Int>): Int =
            consumo.minOfOrNull { (itemId, porUnidade) -> livre(itemId) / porUnidade } ?: 0

        val opcoesCombo = combos.mapNotNull { combo ->
            val mesa = porId[combo.itemMesaId] ?: return@mapNotNull null
            val cadeira = porId[combo.itemCadeiraId] ?: return@mapNotNull null
            val consumo = mapOf(mesa.id to 1, cadeira.id to combo.qtdCadeiras)
            val chave = "C:${combo.id}"
            Opcao(
                chave = chave,
                tipo = TipoLinha.COMBO,
                refId = combo.id,
                nome = combo.nome,
                detalhe = "1 ${mesa.nome} + ${combo.qtdCadeiras} ${cadeira.nome}",
                precoDiariaCentavos = combo.precoDiariaCentavos,
                quantidade = f.quantidades[chave] ?: 0,
                consumo = consumo,
                disponivel = cabem(consumo)
            )
        }

        val opcoesItem = itens.map { item ->
            val chave = "I:${item.id}"
            Opcao(
                chave = chave,
                tipo = TipoLinha.ITEM,
                refId = item.id,
                nome = item.nome,
                detalhe = detalhe(item),
                precoDiariaCentavos = item.precoDiariaCentavos,
                quantidade = f.quantidades[chave] ?: 0,
                consumo = mapOf(item.id to 1),
                disponivel = livre(item.id)
            )
        }

        val opcoes = opcoesCombo + opcoesItem
        val escolhidas = opcoes.filter { it.quantidade > 0 }

        val faltas = necessidade(opcoes).mapNotNull { (itemId, precisa) ->
            val item = porId[itemId] ?: return@mapNotNull null
            val livres = livre(itemId)
            if (precisa > livres) "Faltam ${precisa - livres} de ${item.nome} (livres: $livres)" else null
        }

        return NovoPedidoUiState(
            clienteNome = f.clienteNome,
            telefone = f.telefone,
            documento = f.documento,
            endereco = f.endereco,
            pontoReferencia = f.pontoReferencia,
            observacoes = f.observacoes,
            dataEntrega = f.dataEntrega,
            horaEntrega = f.horaEntrega,
            dataRetirada = f.dataRetirada,
            horaRetirada = f.horaRetirada,
            dias = Datas.dias(inicioEpoch(f), fimEpoch(f)),
            opcoes = opcoes,
            freteTexto = f.freteTexto,
            descontoTexto = f.descontoTexto,
            sinalTexto = f.sinalTexto,
            caucaoTexto = f.caucaoTexto,
            faltas = faltas,
            aluguelCentavos = escolhidas.sumOf { it.quantidade * it.precoDiariaCentavos },
            freteCentavos = f.freteTexto.toCentsOrNull() ?: 0,
            descontoCentavos = f.descontoTexto.toCentsOrNull() ?: 0,
            sinalCentavos = f.sinalTexto.toCentsOrNull() ?: 0,
            caucaoCentavos = f.caucaoTexto.toCentsOrNull() ?: 0,
            catalogoVazio = opcoes.isEmpty(),
            salvando = gravando,
            mensagem = msg
        )
    }

    private fun detalhe(item: Item): String = buildList {
        add(item.material.name.lowercase())
        if (item.cor.isNotBlank()) add(item.cor.lowercase())
        when (item.comBraco) {
            true -> add("com braco")
            false -> add("sem braco")
            null -> Unit
        }
    }.joinToString(" · ")

    private fun necessidade(opcoes: List<Opcao>): Map<Long, Int> {
        val total = mutableMapOf<Long, Int>()
        opcoes.filter { it.quantidade > 0 }.forEach { opcao ->
            opcao.consumo.forEach { (itemId, porUnidade) ->
                total[itemId] = (total[itemId] ?: 0) + porUnidade * opcao.quantidade
            }
        }
        return total
    }
}
