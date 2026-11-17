package br.com.mesasapp.ui.itens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.mesasapp.data.repository.AluguelRepository
import br.com.mesasapp.domain.model.Categoria
import br.com.mesasapp.domain.model.Combo
import br.com.mesasapp.domain.model.Item
import br.com.mesasapp.util.Datas
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Um modelo de item com os numeros do momento. */
data class ItemNoEstoque(
    val item: Item,
    val alugadas: Int,
    val quebradas: Int
) {
    val livres: Int get() = (item.quantidadeTotal - alugadas).coerceAtLeast(0)
}

data class EstoqueUiState(
    val mesas: List<ItemNoEstoque> = emptyList(),
    val cadeiras: List<ItemNoEstoque> = emptyList(),
    val combos: List<Combo> = emptyList(),
    val todosItens: List<Item> = emptyList(),
    val mensagem: String? = null
) {
    val totalMesas: Int get() = mesas.sumOf { it.item.quantidadeTotal }
    val totalCadeiras: Int get() = cadeiras.sumOf { it.item.quantidadeTotal }
}

class ItensViewModel(private val repository: AluguelRepository) : ViewModel() {

    private val mensagem = MutableStateFlow<String?>(null)
    private val agora = Datas.agoraEpochSeg()

    val uiState: StateFlow<EstoqueUiState> = combine(
        repository.itens(),
        repository.combos(),
        repository.reservas(agora, agora + 1),
        repository.quebrasPorItem(),
        mensagem
    ) { itens, combos, alugadas, quebradas, msg ->
        val comNumeros = itens.map { item ->
            ItemNoEstoque(
                item = item,
                alugadas = alugadas[item.id] ?: 0,
                quebradas = quebradas[item.id] ?: 0
            )
        }
        EstoqueUiState(
            mesas = comNumeros.filter { it.item.categoria == Categoria.MESA },
            cadeiras = comNumeros.filter { it.item.categoria == Categoria.CADEIRA },
            combos = combos,
            todosItens = itens,
            mensagem = msg
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstoqueUiState())

    fun salvarItem(item: Item) {
        viewModelScope.launch { repository.salvarItem(item) }
    }

    /**
     * Excluir so vale para modelo que nunca foi alugado: as pecas gravadas nos
     * pedidos apontam para ele e o historico ficaria sem descricao. Ja usado, o
     * modelo e desativado — some do pedido novo e o passado fica intacto.
     */
    fun removerItem(item: Item) {
        viewModelScope.launch {
            if (repository.itemPodeSerExcluido(item.id)) {
                repository.excluirItem(item)
                mensagem.value = "Modelo removido."
            } else {
                repository.salvarItem(item.copy(ativo = false))
                mensagem.value = "Modelo ja usado em pedido: foi desativado, nao removido."
            }
        }
    }

    fun reativarItem(item: Item) {
        viewModelScope.launch { repository.salvarItem(item.copy(ativo = true)) }
    }

    fun salvarCombo(combo: Combo) {
        viewModelScope.launch { repository.salvarCombo(combo) }
    }

    fun removerCombo(combo: Combo) {
        viewModelScope.launch {
            repository.excluirCombo(combo)
            mensagem.value = "Jogo removido."
        }
    }

    fun mensagemLida() { mensagem.value = null }
}
