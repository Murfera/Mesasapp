package br.com.mesasapp.ui.itens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.mesasapp.data.repository.AluguelRepository
import br.com.mesasapp.domain.model.Combo
import br.com.mesasapp.domain.model.Item
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EstoqueUiState(
    val itens: List<Item> = emptyList(),
    val combos: List<Combo> = emptyList(),
    val mensagem: String? = null
)

class ItensViewModel(private val repository: AluguelRepository) : ViewModel() {

    private val mensagem = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    val uiState: StateFlow<EstoqueUiState> = combine(
        repository.itens(),
        repository.combos(),
        mensagem
    ) { itens, combos, msg ->
        EstoqueUiState(itens, combos, msg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstoqueUiState())

    fun salvarItem(item: Item) {
        viewModelScope.launch { repository.salvarItem(item) }
    }

    /**
     * Excluir so vale para item que nunca foi alugado: as pecas gravadas nas
     * locacoes apontam para ele e o historico ficaria sem descricao. Quando ja
     * foi usado, o item e desativado — some do orcamento e o passado fica intacto.
     */
    fun excluirItem(item: Item) {
        viewModelScope.launch {
            if (repository.itemPodeSerExcluido(item.id)) {
                repository.excluirItem(item)
                mensagem.value = "Item excluido."
            } else {
                repository.salvarItem(item.copy(ativo = false))
                mensagem.value = "Item ja usado em locacao: foi desativado, nao excluido."
            }
        }
    }

    fun salvarCombo(combo: Combo) {
        viewModelScope.launch { repository.salvarCombo(combo) }
    }

    fun excluirCombo(combo: Combo) {
        viewModelScope.launch {
            repository.excluirCombo(combo)
            mensagem.value = "Jogo excluido."
        }
    }

    fun mensagemLida() { mensagem.value = null }
}
