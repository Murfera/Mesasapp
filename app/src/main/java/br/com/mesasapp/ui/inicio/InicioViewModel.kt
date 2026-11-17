package br.com.mesasapp.ui.inicio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.mesasapp.data.preferences.ConfigStore
import br.com.mesasapp.data.repository.AluguelRepository
import br.com.mesasapp.domain.model.Categoria
import br.com.mesasapp.domain.model.LocacaoResumo
import br.com.mesasapp.domain.model.StatusLocacao
import br.com.mesasapp.util.Datas
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.LocalTime

data class InicioUiState(
    val nomeNegocio: String = "Mesasapp",
    val hoje: LocalDate = LocalDate.now(),
    val mesasLivres: Int = 0,
    val mesasTotal: Int = 0,
    val cadeirasLivres: Int = 0,
    val cadeirasTotal: Int = 0,
    val entregasHoje: List<LocacaoResumo> = emptyList(),
    val retiradasHoje: List<LocacaoResumo> = emptyList(),
    val aReceberCentavos: Long = 0,
    val pedidosEmAberto: Int = 0,
    val proximos: List<LocacaoResumo> = emptyList()
)

class InicioViewModel(
    repository: AluguelRepository,
    configStore: ConfigStore
) : ViewModel() {

    private val agora = Datas.agoraEpochSeg()

    val uiState: StateFlow<InicioUiState> = combine(
        repository.itensAtivos(),
        repository.resumos(),
        repository.reservas(agora, agora + 1),
        configStore.config
    ) { itens, resumos, reservadoAgora, config ->
        val hoje = LocalDate.now()
        val inicioDoDia = Datas.paraEpochSeg(hoje, LocalTime.MIN)
        val fimDoDia = Datas.paraEpochSeg(hoje, LocalTime.MAX)

        fun soma(categoria: Categoria, livres: Boolean): Int =
            itens.filter { it.categoria == categoria }.sumOf { item ->
                if (livres) {
                    (item.quantidadeTotal - (reservadoAgora[item.id] ?: 0)).coerceAtLeast(0)
                } else {
                    item.quantidadeTotal
                }
            }

        // Pedido cancelado nao conta em lugar nenhum; orcamento so vira numero
        // quando e confirmado, por isso "em aberto" olha os dois status que ocupam
        // estoque.
        val emAberto = resumos.filter { it.status.ocupaEstoque }

        InicioUiState(
            nomeNegocio = config.nomeEmpresa.ifBlank { "Mesasapp" },
            hoje = hoje,
            mesasLivres = soma(Categoria.MESA, livres = true),
            mesasTotal = soma(Categoria.MESA, livres = false),
            cadeirasLivres = soma(Categoria.CADEIRA, livres = true),
            cadeirasTotal = soma(Categoria.CADEIRA, livres = false),
            entregasHoje = emAberto.filter { it.inicioEpochSeg in inicioDoDia..fimDoDia },
            retiradasHoje = emAberto.filter { it.fimEpochSeg in inicioDoDia..fimDoDia },
            aReceberCentavos = emAberto.sumOf { it.aReceberCentavos },
            pedidosEmAberto = emAberto.size,
            proximos = emAberto
                .filter { it.inicioEpochSeg > fimDoDia }
                .sortedBy { it.inicioEpochSeg }
                .take(5)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InicioUiState())
}

/** Rotulo curto do status, usado nas listas. */
fun StatusLocacao.rotulo(): String = when (this) {
    StatusLocacao.ORCAMENTO -> "Orcamento"
    StatusLocacao.CONFIRMADA -> "Confirmado"
    StatusLocacao.ENTREGUE -> "Entregue"
    StatusLocacao.DEVOLVIDA -> "Devolvido"
    StatusLocacao.CANCELADA -> "Cancelado"
}
