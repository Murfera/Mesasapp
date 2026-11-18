package br.com.mesasapp.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.mesasapp.data.preferences.PixConfig
import br.com.mesasapp.data.preferences.TemaApp
import br.com.mesasapp.di.AppContainer
import br.com.mesasapp.domain.Catalogo
import br.com.mesasapp.domain.Cor
import br.com.mesasapp.domain.FiltroMaterial
import br.com.mesasapp.domain.LinhaPedido
import br.com.mesasapp.domain.Material
import br.com.mesasapp.domain.PecaPedido
import br.com.mesasapp.domain.Pedido
import br.com.mesasapp.domain.Precos
import br.com.mesasapp.domain.StatusPedido
import br.com.mesasapp.domain.TipoLinha
import br.com.mesasapp.domain.conta
import br.com.mesasapp.domain.diarias
import br.com.mesasapp.domain.fechamento
import br.com.mesasapp.domain.reservado
import br.com.mesasapp.util.Datas
import br.com.mesasapp.util.toCentsOrNull
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

enum class Tela { INICIO, PEDIDOS, NOVO, AGENDA, ESTOQUE, DETALHE, PRECOS, PIX }

data class Mensagem(val ok: Boolean, val texto: String)

/** Item escolhido no Novo pedido, antes de entrar no pedido. */
data class ItemForm(val tipo: TipoLinha, val mesaId: String, val cadId: String)

/** Linha ainda sem preco congelado: o valor acompanha os Precos ate salvar. */
data class LinhaForm(val tipo: TipoLinha, val mesaId: String, val cadId: String, val qtd: Int) {
    fun comPrecos(p: Precos) = LinhaPedido.nova(tipo, mesaId, cadId, qtd, p)
}

data class FormPedido(
    val cliente: String = "",
    val telefone: String = "",
    val documento: String = "",
    val endereco: String = "",
    val referencia: String = "",
    val entrega: LocalDate = LocalDate.now(),
    val retirada: LocalDate = LocalDate.now().plusDays(1),
    val itens: List<LinhaForm> = emptyList(),
    val frete: String = "",
    val desconto: String = "",
    val sinal: String = "",
    val caucao: String = "",
    val pagamento: String = "Pix"
) {
    val datasOk: Boolean get() = !retirada.isBefore(entrega)
    val inicioEpochSeg: Long get() = Datas.entregaEpochSeg(entrega)
    val fimEpochSeg: Long get() = Datas.retiradaEpochSeg(retirada)

    fun linhas(p: Precos) = itens.map { it.comPrecos(p) }

    fun conta(p: Precos) = conta(
        linhas(p), diarias(entrega, retirada), centavos(frete), centavos(desconto), centavos(sinal)
    )
}

/** Campo de dinheiro vazio ou invalido conta como zero no calculo. */
fun centavos(texto: String): Long = texto.toCentsOrNull() ?: 0L

/** Valor para dentro do campo: 4000 -> "40", 4050 -> "40,50". */
fun centavosParaCampo(c: Long): String =
    if (c % 100 == 0L) (c / 100).toString() else "%d,%02d".format(c / 100, c % 100)

class MesasViewModel(private val container: AppContainer) : ViewModel() {

    private val repo = container.repositorio
    private val config = container.configStore

    val estoque: StateFlow<Map<String, Int>> = repo.estoque().stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())
    val precos: StateFlow<Precos> = config.precos.stateIn(viewModelScope, SharingStarted.Eagerly, Precos())
    val pix: StateFlow<PixConfig> = config.pix.stateIn(viewModelScope, SharingStarted.Eagerly, PixConfig())
    val tema: StateFlow<TemaApp> = config.tema.stateIn(viewModelScope, SharingStarted.Eagerly, TemaApp.SISTEMA)
    val filtro: StateFlow<FiltroMaterial> = config.filtro.stateIn(viewModelScope, SharingStarted.Eagerly, FiltroMaterial.TODOS)
    val pedidos: StateFlow<List<Pedido>> = repo.pedidos().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // --- navegacao ---
    var tela by mutableStateOf(Tela.INICIO); private set
    private var telaAntesDoMenu = Tela.INICIO
    var pedidoSelecionado by mutableStateOf(0L); private set
    var msg by mutableStateOf<Mensagem?>(null)
    var menuAberto by mutableStateOf(false)
    var menuMaterialAberto by mutableStateOf(false)
    var menuTemaAberto by mutableStateOf(false)

    fun escolherTema(t: TemaApp) {
        viewModelScope.launch { config.definirTema(t) }
        menuAberto = false
        menuTemaAberto = false
    }

    fun irPara(t: Tela) {
        tela = t
        msg = null
        menuAberto = false
    }

    fun abrirDoMenu(t: Tela) {
        if (tela != Tela.PRECOS && tela != Tela.PIX) telaAntesDoMenu = tela
        irPara(t)
    }

    fun voltarDoMenu() = irPara(telaAntesDoMenu)

    /** Botao voltar do Android: so as telas que abrem por cima voltam. */
    fun voltar(): Boolean = when (tela) {
        Tela.DETALHE -> { irPara(Tela.PEDIDOS); true }
        Tela.PRECOS, Tela.PIX -> { voltarDoMenu(); true }
        else -> false
    }

    fun escolherFiltro(f: FiltroMaterial) {
        viewModelScope.launch { config.definirFiltro(f) }
        f.material?.let { mat ->
            estoqueMaterial = mat
            itemForm = itemForm.copy(mesaId = Catalogo.equivalente(itemForm.mesaId, mat), cadId = Catalogo.equivalente(itemForm.cadId, mat))
        }
        menuAberto = false
        menuMaterialAberto = false
        msg = null
    }

    // --- pedidos ---
    var filtroStatus by mutableStateOf<StatusPedido?>(null)

    // --- novo pedido ---
    var form by mutableStateOf(FormPedido(frete = centavosParaCampo(precos.value.fretePadraoCentavos)))
    var itemForm by mutableStateOf(ItemForm(TipoLinha.JOGO, "p-mesa-br", "p-cadeira-br"))
    var qtdItem by mutableStateOf("1")

    private var freteAjustado = false

    init {
        // O frete padrao chega do DataStore depois do primeiro frame.
        viewModelScope.launch {
            precos.collect { p ->
                if (!freteAjustado && form.itens.isEmpty()) form = form.copy(frete = centavosParaCampo(p.fretePadraoCentavos))
            }
        }
    }

    fun mudarForm(f: FormPedido) {
        if (f.frete != form.frete) freteAjustado = true
        form = f
        msg = null
    }

    fun adicionarItem() {
        val q = qtdItem.toIntOrNull() ?: 1
        if (q <= 0) return
        val i = itemForm
        val nova = LinhaForm(i.tipo, i.mesaId, i.cadId, q)
        val iguais: (LinhaForm) -> Boolean = { l ->
            l.tipo == nova.tipo &&
                (l.tipo == TipoLinha.CADEIRA || l.mesaId == nova.mesaId) &&
                (l.tipo == TipoLinha.MESA || l.cadId == nova.cadId)
        }
        val itens = form.itens.toMutableList()
        val idx = itens.indexOfFirst(iguais)
        if (idx >= 0) itens[idx] = itens[idx].copy(qtd = itens[idx].qtd + q) else itens += nova
        form = form.copy(itens = itens)
        qtdItem = "1"
        msg = null
    }

    fun removerItem(indice: Int) {
        form = form.copy(itens = form.itens.filterIndexed { i, _ -> i != indice })
    }

    private fun faltaEstoque(linhas: List<LinhaPedido>, inicio: Long, fim: Long, excluirId: Long = -1): List<String> {
        val res = reservado(pedidos.value, inicio, fim, excluirId)
        val u = br.com.mesasapp.domain.uso(linhas)
        return u.filter { (id, n) -> n > (estoque.value[id] ?: 0) - (res[id] ?: 0) }.keys.toList()
    }

    private fun nomes(ids: List<String>) = ids.joinToString(", ") { Catalogo.modelo(it)?.nome?.lowercase() ?: it }

    fun salvar(status: StatusPedido) {
        val f = form
        val p = precos.value
        val linhas = f.linhas(p)
        val c = f.conta(p)
        fun erro(t: String) { msg = Mensagem(false, t) }
        when {
            f.cliente.isBlank() -> return erro("Informe o nome do cliente.")
            f.telefone.isBlank() -> return erro("Informe um telefone para contato.")
            f.endereco.isBlank() -> return erro("Informe o endereço de entrega.")
            !f.datasOk -> return erro("A retirada precisa ser no mesmo dia ou depois da entrega.")
            c.mesas + c.cadeiras == 0 -> return erro("Adicione pelo menos um item ao pedido.")
        }
        if (status == StatusPedido.CONFIRMADO) {
            val falta = faltaEstoque(linhas, f.inicioEpochSeg, f.fimEpochSeg)
            if (falta.isNotEmpty()) return erro("Falta estoque de: ${nomes(falta)}. Troque o modelo ou salve como orçamento.")
        }
        val pedido = Pedido(
            cliente = f.cliente.trim(), telefone = f.telefone.trim(), documento = f.documento.trim(),
            endereco = f.endereco.trim(), referencia = f.referencia.trim(),
            inicioEpochSeg = f.inicioEpochSeg, fimEpochSeg = f.fimEpochSeg, entrega = f.entrega, retirada = f.retirada,
            freteCentavos = centavos(f.frete), descontoCentavos = centavos(f.desconto),
            sinalCentavos = centavos(f.sinal), caucaoCentavos = centavos(f.caucao),
            pagamento = f.pagamento, status = status, linhas = linhas,
            pecas = c.uso.map { (id, n) -> PecaPedido(id, n, p.peca(id).reposicaoCentavos) }
        )
        viewModelScope.launch {
            val id = repo.salvarPedido(pedido)
            form = FormPedido(frete = centavosParaCampo(precos.value.fretePadraoCentavos))
            freteAjustado = false
            abrirPedido(id)
            msg = Mensagem(
                true,
                if (status == StatusPedido.ORCAMENTO) "Orçamento salvo. O estoque só é reservado ao confirmar."
                else "Pedido confirmado e estoque reservado para o período."
            )
        }
    }

    // --- detalhe ---
    var quebradas by mutableStateOf<Map<String, Int>>(emptyMap())
    var faltando by mutableStateOf<Map<String, Int>>(emptyMap())
    var atraso by mutableStateOf(0)
    var pixOpcao by mutableStateOf("saldo")
    var pixOutro by mutableStateOf("")
    var pixCopiado by mutableStateOf(false)

    fun abrirPedido(id: Long) {
        val p = pedidos.value.firstOrNull { it.id == id }
        pedidoSelecionado = id
        quebradas = p?.pecas?.filter { it.quebradas > 0 }?.associate { it.modeloId to it.quebradas }.orEmpty()
        faltando = p?.pecas?.filter { it.faltando > 0 }?.associate { it.modeloId to it.faltando }.orEmpty()
        atraso = p?.atrasoDias ?: 0
        pixOpcao = "saldo"
        pixOutro = ""
        pixCopiado = false
        irPara(Tela.DETALHE)
    }

    /** Quebradas + faltando nao passam da quantidade do pedido. */
    fun passoVistoria(quebrada: Boolean, id: String, delta: Int, max: Int) {
        val alvo = if (quebrada) quebradas else faltando
        val outro = if (quebrada) faltando else quebradas
        val novo = ((alvo[id] ?: 0) + delta).coerceIn(0, maxOf(0, max - (outro[id] ?: 0)))
        if (quebrada) quebradas = alvo + (id to novo) else faltando = alvo + (id to novo)
    }

    fun passoAtraso(delta: Int) { atraso = (atraso + delta).coerceIn(0, 30) }

    fun acaoDoStatus(p: Pedido) {
        when (p.status) {
            StatusPedido.ORCAMENTO -> {
                val falta = faltaEstoque(p.linhas, p.inicioEpochSeg, p.fimEpochSeg, p.id)
                if (falta.isNotEmpty()) { msg = Mensagem(false, "Falta estoque de: ${nomes(falta)}."); return }
                viewModelScope.launch { repo.mudarStatus(p.id, StatusPedido.CONFIRMADO) }
                msg = Mensagem(true, "Pedido confirmado e estoque reservado.")
            }
            StatusPedido.CONFIRMADO -> {
                viewModelScope.launch { repo.mudarStatus(p.id, StatusPedido.ENTREGUE) }
                msg = Mensagem(true, "Entrega registrada. Na retirada, faça a vistoria abaixo.")
            }
            StatusPedido.ENTREGUE -> {
                val f = fechamento(p, quebradas, faltando, atraso)
                val q = quebradas
                val fa = faltando
                val a = atraso
                viewModelScope.launch { repo.registrarDevolucao(p.id, q, fa, a, f.finalCentavos) }
                msg = Mensagem(true, "Devolução registrada. Quebradas e faltantes saíram do estoque; ajuste a quantidade quando repor.")
            }
            StatusPedido.DEVOLVIDO -> Unit
        }
    }

    // --- estoque e precos ---
    var estoqueMaterial by mutableStateOf(Material.PLASTICO)

    fun definirTotal(id: String, total: Long) = viewModelScope.launch { repo.definirTotal(id, total.toInt()) }
    fun definirCadeirasPorMesa(m: Material, c: Cor, n: Long) = viewModelScope.launch { config.definirCadeirasPorMesa(m, c, n.toInt()) }
    fun definirDiaria(chave: String, c: Long) = viewModelScope.launch { config.definirDiaria(chave, c) }
    fun definirReposicao(chave: String, c: Long) = viewModelScope.launch { config.definirReposicao(chave, c) }
    fun definirDescontoJogo(c: Long) = viewModelScope.launch { config.definirDescontoJogo(c) }
    fun definirFrete(c: Long) = viewModelScope.launch { config.definirFrete(c) }
    fun definirPix(p: PixConfig) = viewModelScope.launch { config.definirPix(p) }

    // --- agenda ---
    var mesCalendario by mutableStateOf(YearMonth.now())
    var diaCalendario by mutableStateOf(LocalDate.now())

    fun mudarMes(delta: Long) {
        mesCalendario = mesCalendario.plusMonths(delta)
        diaCalendario = mesCalendario.atDay(1)
    }

    class Fabrica(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MesasViewModel(container) as T
    }
}
