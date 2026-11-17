package br.com.mesasapp.ui.pedido

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.mesasapp.data.preferences.Config
import br.com.mesasapp.data.preferences.ConfigStore
import br.com.mesasapp.data.repository.AluguelRepository
import br.com.mesasapp.domain.model.LocacaoCompleta
import br.com.mesasapp.domain.model.LocacaoPeca
import br.com.mesasapp.domain.model.StatusLocacao
import br.com.mesasapp.util.Datas
import br.com.mesasapp.util.Pix
import br.com.mesasapp.util.formatAsBrl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PedidoUiState(
    val pedido: LocacaoCompleta? = null,
    val config: Config = Config(),
    val mensagem: String? = null
) {
    val pixDisponivel: Boolean get() = config.pixConfigurado

    /** Texto do Pix copia e cola do saldo a receber deste pedido. */
    fun pixCopiaECola(): String? {
        val completa = pedido ?: return null
        if (!config.pixConfigurado) return null
        val valor = completa.aReceberCentavos.coerceAtLeast(0)
        return Pix.copiaECola(
            chave = config.chavePix,
            nomeRecebedor = config.nomeRecebedor,
            cidade = config.cidadeRecebedor,
            valorCentavos = valor
        )
    }

    /** Resumo para mandar no WhatsApp. */
    fun resumoParaCompartilhar(): String {
        val completa = pedido ?: return ""
        val locacao = completa.locacao
        return buildString {
            if (config.nomeEmpresa.isNotBlank()) appendLine(config.nomeEmpresa)
            appendLine("Pedido ${locacao.id}")
            if (locacao.clienteNome.isNotBlank()) appendLine("Cliente: ${locacao.clienteNome}")
            appendLine("Entrega: ${Datas.formatarDataHora(locacao.inicioEpochSeg)}")
            appendLine("Retirada: ${Datas.formatarDataHora(locacao.fimEpochSeg)}")
            if (locacao.endereco.isNotBlank()) appendLine("Endereco: ${locacao.endereco}")
            appendLine()
            completa.linhas.forEach { linha ->
                appendLine("${linha.quantidade}x ${linha.descricao} - ${linha.subtotalCentavos.formatAsBrl()}")
            }
            if (locacao.freteCentavos > 0) appendLine("Frete: ${locacao.freteCentavos.formatAsBrl()}")
            if (locacao.descontoCentavos > 0) appendLine("Desconto: -${locacao.descontoCentavos.formatAsBrl()}")
            if (completa.quebrasCentavos > 0) appendLine("Quebras: ${completa.quebrasCentavos.formatAsBrl()}")
            appendLine("Total: ${completa.totalCentavos.formatAsBrl()}")
            if (locacao.sinalCentavos > 0) {
                appendLine("Sinal pago: ${locacao.sinalCentavos.formatAsBrl()}")
                appendLine("A pagar: ${completa.aReceberCentavos.formatAsBrl()}")
            }
            pixCopiaECola()?.let {
                appendLine()
                appendLine("Pix copia e cola:")
                appendLine(it)
            }
        }.trim()
    }
}

class PedidoViewModel(
    private val repository: AluguelRepository,
    configStore: ConfigStore,
    private val pedidoId: Long
) : ViewModel() {

    private val mensagem = MutableStateFlow<String?>(null)

    val uiState: StateFlow<PedidoUiState> = combine(
        repository.locacaoCompleta(pedidoId),
        configStore.config,
        mensagem
    ) { pedido, config, msg ->
        PedidoUiState(pedido, config, msg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PedidoUiState())

    fun mudarStatus(status: StatusLocacao) {
        viewModelScope.launch {
            repository.mudarStatus(pedidoId, status)
            mensagem.value = "Pedido marcado como ${status.name.lowercase()}."
        }
    }

    /**
     * Conferencia da devolucao: quantas pecas voltaram e quantas voltaram
     * quebradas. A quebra entra no total pelo valor de reposicao congelado no
     * pedido, e nao pelo preco de hoje do catalogo.
     */
    fun registrarDevolucao(conferencia: Map<Long, Pair<Int, Int>>, encerrar: Boolean) {
        val pedido = uiState.value.pedido ?: return

        viewModelScope.launch {
            pedido.pecas.forEach { peca ->
                val (devolvidas, quebradas) = conferencia[peca.id] ?: return@forEach
                repository.atualizarPeca(
                    peca.copy(
                        quantidadeDevolvida = devolvidas.coerceIn(0, peca.quantidade),
                        quantidadeQuebrada = quebradas.coerceIn(0, peca.quantidade)
                    )
                )
            }
            if (encerrar) repository.mudarStatus(pedidoId, StatusLocacao.DEVOLVIDA)
            mensagem.value = "Devolucao registrada."
        }
    }

    fun mensagemLida() { mensagem.value = null }
}

/** Peca com o que falta voltar, para a tela de conferencia. */
val LocacaoPeca.faltamVoltar: Int
    get() = (quantidade - quantidadeDevolvida - quantidadeQuebrada).coerceAtLeast(0)
