package br.com.mesasapp.data.repository

import br.com.mesasapp.data.local.ClienteDao
import br.com.mesasapp.data.local.ComboDao
import br.com.mesasapp.data.local.ItemDao
import br.com.mesasapp.data.local.LocacaoDao
import br.com.mesasapp.data.local.toEntity
import br.com.mesasapp.data.local.toModel
import br.com.mesasapp.domain.model.Cliente
import br.com.mesasapp.domain.model.Combo
import br.com.mesasapp.domain.model.Item
import br.com.mesasapp.domain.model.Locacao
import br.com.mesasapp.domain.model.LocacaoCompleta
import br.com.mesasapp.domain.model.LocacaoLinha
import br.com.mesasapp.domain.model.LocacaoPeca
import br.com.mesasapp.domain.model.LocacaoResumo
import br.com.mesasapp.domain.model.StatusLocacao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class AluguelRepository(
    private val itemDao: ItemDao,
    private val comboDao: ComboDao,
    private val clienteDao: ClienteDao,
    private val locacaoDao: LocacaoDao
) {

    // --- catalogo ---

    fun itens(): Flow<List<Item>> = itemDao.observarTodos().map { lista -> lista.map { it.toModel() } }

    fun itensAtivos(): Flow<List<Item>> = itemDao.observarAtivos().map { lista -> lista.map { it.toModel() } }

    suspend fun item(id: Long): Item? = itemDao.buscar(id)?.toModel()

    /** Primeira abertura: ainda nao ha nada cadastrado. */
    suspend fun catalogoVazio(): Boolean = itemDao.contar() == 0 && comboDao.contar() == 0

    suspend fun salvarItem(item: Item): Long =
        if (item.id == 0L) itemDao.inserir(item.toEntity())
        else { itemDao.atualizar(item.toEntity()); item.id }

    /**
     * Item usado em alguma locacao nao pode ser excluido: as pecas gravadas
     * apontam para ele e o historico ficaria sem descricao. Nesse caso a tela
     * oferece desativar, que tira do orcamento sem apagar o passado.
     */
    suspend fun itemPodeSerExcluido(id: Long): Boolean = itemDao.usosEmLocacoes(id) == 0

    suspend fun excluirItem(item: Item) = itemDao.excluir(item.toEntity())

    fun combos(): Flow<List<Combo>> = comboDao.observarTodos().map { lista -> lista.map { it.toModel() } }

    fun combosAtivos(): Flow<List<Combo>> = comboDao.observarAtivos().map { lista -> lista.map { it.toModel() } }

    suspend fun combo(id: Long): Combo? = comboDao.buscar(id)?.toModel()

    suspend fun salvarCombo(combo: Combo): Long =
        if (combo.id == 0L) comboDao.inserir(combo.toEntity())
        else { comboDao.atualizar(combo.toEntity()); combo.id }

    suspend fun excluirCombo(combo: Combo) = comboDao.excluir(combo.toEntity())

    // --- clientes ---

    fun clientes(): Flow<List<Cliente>> = clienteDao.observarTodos().map { lista -> lista.map { it.toModel() } }

    suspend fun salvarCliente(cliente: Cliente): Long =
        if (cliente.id == 0L) clienteDao.inserir(cliente.toEntity())
        else { clienteDao.atualizar(cliente.toEntity()); cliente.id }

    /** Reaproveita o cliente de mesmo nome em vez de duplicar o cadastro. */
    suspend fun garantirCliente(
        nome: String,
        telefone: String,
        documento: String,
        endereco: String
    ): Long? {
        val limpo = nome.trim()
        if (limpo.isEmpty()) return null
        val existente = clienteDao.buscarPorNome(limpo)
        return if (existente == null) {
            clienteDao.inserir(
                Cliente(
                    nome = limpo,
                    telefone = telefone,
                    documento = documento,
                    endereco = endereco
                ).toEntity()
            )
        } else {
            clienteDao.atualizar(
                existente.copy(telefone = telefone, documento = documento, endereco = endereco)
            )
            existente.id
        }
    }

    // --- locacoes ---

    fun locacoes(): Flow<List<Locacao>> = locacaoDao.observarTodas().map { lista -> lista.map { it.toModel() } }

    /** Todos os pedidos com os totais ja somados: lista, painel e agenda bebem daqui. */
    fun resumos(): Flow<List<LocacaoResumo>> = locacaoDao.observarResumos().map { lista ->
        lista.map { r ->
            LocacaoResumo(
                id = r.id,
                clienteNome = r.clienteNome,
                telefone = r.telefone,
                endereco = r.endereco,
                inicioEpochSeg = r.inicioEpochSeg,
                fimEpochSeg = r.fimEpochSeg,
                status = r.status,
                subtotalCentavos = r.subtotalCentavos,
                freteCentavos = r.freteCentavos,
                montagemCentavos = r.montagemCentavos,
                descontoCentavos = r.descontoCentavos,
                sinalCentavos = r.sinalCentavos,
                quebrasCentavos = r.quebrasCentavos,
                itens = r.itens.orEmpty()
            )
        }
    }

    /** Pecas dadas como quebradas, por item, somando todo o historico. */
    fun quebrasPorItem(): Flow<Map<Long, Int>> = locacaoDao.observarQuebras().map { lista ->
        lista.associate { it.itemId to it.quantidade }
    }

    fun agenda(): Flow<List<Locacao>> = locacaoDao.observarAgenda().map { lista -> lista.map { it.toModel() } }

    fun locacaoCompleta(id: Long): Flow<LocacaoCompleta?> = combine(
        locacaoDao.observar(id),
        locacaoDao.observarLinhas(id),
        locacaoDao.observarPecas(id)
    ) { locacao, linhas, pecas ->
        locacao?.let {
            LocacaoCompleta(
                it.toModel(),
                linhas.map { linha -> linha.toModel() },
                pecas.map { peca -> peca.toModel() }
            )
        }
    }

    suspend fun salvarLocacao(
        locacao: Locacao,
        linhas: List<LocacaoLinha>,
        pecas: List<LocacaoPeca>
    ): Long = locacaoDao.salvarCompleta(
        locacao.toEntity(),
        linhas.map { it.toEntity() },
        pecas.map { it.toEntity() }
    )

    suspend fun mudarStatus(id: Long, status: StatusLocacao) = locacaoDao.mudarStatus(id, status)

    suspend fun atualizarPeca(peca: LocacaoPeca) = locacaoDao.atualizarPeca(peca.toEntity())

    suspend fun excluirLocacao(locacao: Locacao) = locacaoDao.excluirLocacao(locacao.toEntity())

    /**
     * Quanto de cada item ja esta comprometido na janela pedida.
     * A tela de orcamento subtrai isso do estoque total para dizer se da ou nao.
     */
    fun reservas(
        inicioEpochSeg: Long,
        fimEpochSeg: Long,
        excluirLocacaoId: Long = -1
    ): Flow<Map<Long, Int>> =
        locacaoDao.observarReservas(inicioEpochSeg, fimEpochSeg, excluirLocacaoId)
            .map { lista -> lista.associate { it.itemId to it.quantidade } }
}
