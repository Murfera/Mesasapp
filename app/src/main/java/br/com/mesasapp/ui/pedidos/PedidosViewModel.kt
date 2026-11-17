package br.com.mesasapp.ui.pedidos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.mesasapp.data.repository.AluguelRepository
import br.com.mesasapp.domain.model.LocacaoResumo
import br.com.mesasapp.domain.model.StatusLocacao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PedidosUiState(
    val busca: String = "",
    val filtro: StatusLocacao? = null,
    val pedidos: List<LocacaoResumo> = emptyList(),
    val mensagem: String? = null
)

class PedidosViewModel(private val repository: AluguelRepository) : ViewModel() {

    private val busca = MutableStateFlow("")
    private val filtro = MutableStateFlow<StatusLocacao?>(null)
    private val mensagem = MutableStateFlow<String?>(null)

    val uiState: StateFlow<PedidosUiState> = combine(
        repository.resumos(),
        busca,
        filtro,
        mensagem
    ) { pedidos, texto, status, msg ->
        val termo = texto.trim().lowercase()
        PedidosUiState(
            busca = texto,
            filtro = status,
            pedidos = pedidos
                .filter { status == null || it.status == status }
                .filter {
                    termo.isEmpty() ||
                        it.clienteNome.lowercase().contains(termo) ||
                        it.telefone.contains(termo) ||
                        it.id.toString() == termo
                },
            mensagem = msg
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PedidosUiState())

    fun buscar(texto: String) { busca.value = texto }
    fun filtrar(status: StatusLocacao?) { filtro.value = status }
    fun mensagemLida() { mensagem.value = null }

    fun mudarStatus(id: Long, status: StatusLocacao) {
        viewModelScope.launch {
            repository.mudarStatus(id, status)
            mensagem.value = "Pedido $id: ${status.name.lowercase()}."
        }
    }
}
