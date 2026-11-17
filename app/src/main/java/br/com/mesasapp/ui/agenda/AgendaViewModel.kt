package br.com.mesasapp.ui.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.mesasapp.data.repository.AluguelRepository
import br.com.mesasapp.domain.model.Categoria
import br.com.mesasapp.domain.model.LocacaoResumo
import br.com.mesasapp.util.Datas
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

data class EventoAgenda(val pedido: LocacaoResumo, val entrega: Boolean)

data class AgendaUiState(
    val mes: LocalDate = LocalDate.now().withDayOfMonth(1),
    val diaSelecionado: LocalDate = LocalDate.now(),
    val semanas: List<List<DiaAgenda?>> = emptyList(),
    val eventosDoDia: List<EventoAgenda> = emptyList(),
    val mesasNaRua: Int = 0,
    val cadeirasNaRua: Int = 0
)

private data class Navegacao(
    val mes: LocalDate = LocalDate.now().withDayOfMonth(1),
    val dia: LocalDate = LocalDate.now()
)

@OptIn(ExperimentalCoroutinesApi::class)
class AgendaViewModel(private val repository: AluguelRepository) : ViewModel() {

    private val navegacao = MutableStateFlow(Navegacao())

    /** Quanto de cada item esta na rua no dia escolhido, ao meio-dia. */
    private val ocupacaoDoDia = navegacao
        .map { it.dia }
        .distinctUntilChanged()
        .flatMapLatest { dia ->
            val meioDia = Datas.paraEpochSeg(dia, LocalTime.NOON)
            repository.reservas(meioDia, meioDia + 1)
        }

    val uiState: StateFlow<AgendaUiState> = combine(
        repository.resumos(),
        repository.itensAtivos(),
        navegacao,
        ocupacaoDoDia
    ) { pedidos, itens, nav, ocupacao ->
        // Orcamento nao ocupa estoque, mas aparecer na agenda ajudaria a confundir
        // o que esta fechado com o que e so proposta; por isso a agenda mostra
        // apenas os pedidos que de fato vao sair.
        val agendaveis = pedidos.filter { it.status.ocupaEstoque }

        val entregasPorDia = agendaveis.groupingBy { Datas.paraLocal(it.inicioEpochSeg).toLocalDate() }.eachCount()
        val retiradasPorDia = agendaveis.groupingBy { Datas.paraLocal(it.fimEpochSeg).toLocalDate() }.eachCount()

        val hoje = LocalDate.now()
        val primeiro = nav.mes.withDayOfMonth(1)
        val diasNoMes = YearMonth.from(primeiro).lengthOfMonth()
        // getValue() do domingo e 7; a grade comeca no domingo, como no calendario comum.
        val vaziosAntes = primeiro.dayOfWeek.value % 7

        val celulas = buildList<DiaAgenda?> {
            repeat(vaziosAntes) { add(null) }
            (1..diasNoMes).forEach { numero ->
                val data = primeiro.withDayOfMonth(numero)
                add(
                    DiaAgenda(
                        data = data,
                        entregas = entregasPorDia[data] ?: 0,
                        retiradas = retiradasPorDia[data] ?: 0,
                        hoje = data == hoje
                    )
                )
            }
            while (size % 7 != 0) add(null)
        }

        val porItem = itens.associateBy { it.id }
        fun naRua(categoria: Categoria) = ocupacao.entries.sumOf { (itemId, quantidade) ->
            if (porItem[itemId]?.categoria == categoria) quantidade else 0
        }

        val eventos = (
            agendaveis.filter { Datas.paraLocal(it.inicioEpochSeg).toLocalDate() == nav.dia }
                .map { EventoAgenda(it, entrega = true) } +
                agendaveis.filter { Datas.paraLocal(it.fimEpochSeg).toLocalDate() == nav.dia }
                    .map { EventoAgenda(it, entrega = false) }
            ).sortedBy { if (it.entrega) it.pedido.inicioEpochSeg else it.pedido.fimEpochSeg }

        AgendaUiState(
            mes = primeiro,
            diaSelecionado = nav.dia,
            semanas = celulas.chunked(7),
            eventosDoDia = eventos,
            mesasNaRua = naRua(Categoria.MESA),
            cadeirasNaRua = naRua(Categoria.CADEIRA)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AgendaUiState())

    fun mesAnterior() = navegacao.update { it.copy(mes = it.mes.minusMonths(1)) }
    fun proximoMes() = navegacao.update { it.copy(mes = it.mes.plusMonths(1)) }
    fun selecionarDia(dia: LocalDate) = navegacao.update { it.copy(dia = dia, mes = dia.withDayOfMonth(1)) }
}
