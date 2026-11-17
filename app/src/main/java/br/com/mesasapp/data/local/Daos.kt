package br.com.mesasapp.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("SELECT * FROM item ORDER BY categoria, nome COLLATE NOCASE")
    fun observarTodos(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM item WHERE ativo = 1 ORDER BY categoria, nome COLLATE NOCASE")
    fun observarAtivos(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM item WHERE id = :id")
    suspend fun buscar(id: Long): ItemEntity?

    @Query("SELECT COUNT(*) FROM item")
    suspend fun contar(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(item: ItemEntity): Long

    @Update
    suspend fun atualizar(item: ItemEntity)

    @Delete
    suspend fun excluir(item: ItemEntity)

    /** Quantas pecas deste item ja foram usadas em alguma locacao. */
    @Query("SELECT COUNT(*) FROM locacao_peca WHERE itemId = :itemId")
    suspend fun usosEmLocacoes(itemId: Long): Int
}

@Dao
interface ComboDao {
    @Query("SELECT * FROM combo ORDER BY nome COLLATE NOCASE")
    fun observarTodos(): Flow<List<ComboEntity>>

    @Query("SELECT * FROM combo WHERE ativo = 1 ORDER BY nome COLLATE NOCASE")
    fun observarAtivos(): Flow<List<ComboEntity>>

    @Query("SELECT * FROM combo WHERE id = :id")
    suspend fun buscar(id: Long): ComboEntity?

    @Query("SELECT COUNT(*) FROM combo")
    suspend fun contar(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(combo: ComboEntity): Long

    @Update
    suspend fun atualizar(combo: ComboEntity)

    @Delete
    suspend fun excluir(combo: ComboEntity)
}

@Dao
interface ClienteDao {
    @Query("SELECT * FROM cliente ORDER BY nome COLLATE NOCASE")
    fun observarTodos(): Flow<List<ClienteEntity>>

    @Query("SELECT * FROM cliente WHERE nome = :nome COLLATE NOCASE LIMIT 1")
    suspend fun buscarPorNome(nome: String): ClienteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(cliente: ClienteEntity): Long

    @Update
    suspend fun atualizar(cliente: ClienteEntity)

    @Delete
    suspend fun excluir(cliente: ClienteEntity)
}

@Dao
interface LocacaoDao {
    @Query("SELECT * FROM locacao ORDER BY inicioEpochSeg DESC")
    fun observarTodas(): Flow<List<LocacaoEntity>>

    /**
     * Todos os pedidos com os totais somados no proprio SQL, inclusive a
     * descricao curta das linhas. Uma consulta so alimenta a lista de pedidos,
     * o painel de abertura e a agenda, sem buscar as linhas pedido a pedido.
     */
    @Query(
        """
        SELECT
            l.id AS id,
            l.clienteNome AS clienteNome,
            l.telefone AS telefone,
            l.endereco AS endereco,
            l.inicioEpochSeg AS inicioEpochSeg,
            l.fimEpochSeg AS fimEpochSeg,
            l.status AS status,
            l.freteCentavos AS freteCentavos,
            l.montagemCentavos AS montagemCentavos,
            l.descontoCentavos AS descontoCentavos,
            l.sinalCentavos AS sinalCentavos,
            IFNULL((
                SELECT SUM(li.quantidade * li.precoDiariaCentavos)
                FROM locacao_linha li WHERE li.locacaoId = l.id
            ), 0) AS subtotalCentavos,
            IFNULL((
                SELECT SUM(p.quantidadeQuebrada * p.valorReposicaoCentavos)
                FROM locacao_peca p WHERE p.locacaoId = l.id
            ), 0) AS quebrasCentavos,
            (
                SELECT GROUP_CONCAT(li.quantidade || 'x ' || li.descricao, ', ')
                FROM locacao_linha li WHERE li.locacaoId = l.id
            ) AS itens
        FROM locacao l
        ORDER BY l.inicioEpochSeg DESC
        """
    )
    fun observarResumos(): Flow<List<LocacaoResumoEntity>>

    /** Quantas pecas de cada item ja foram dadas como quebradas. */
    @Query(
        """
        SELECT itemId AS itemId, SUM(quantidadeQuebrada) AS quantidade
        FROM locacao_peca
        WHERE quantidadeQuebrada > 0
        GROUP BY itemId
        """
    )
    fun observarQuebras(): Flow<List<QuebraPorItem>>

    @Query("SELECT * FROM locacao WHERE status IN ('CONFIRMADA', 'ENTREGUE') ORDER BY inicioEpochSeg")
    fun observarAgenda(): Flow<List<LocacaoEntity>>

    @Query("SELECT * FROM locacao WHERE id = :id")
    fun observar(id: Long): Flow<LocacaoEntity?>

    @Query("SELECT * FROM locacao WHERE id = :id")
    suspend fun buscar(id: Long): LocacaoEntity?

    @Query("SELECT * FROM locacao_linha WHERE locacaoId = :locacaoId ORDER BY id")
    fun observarLinhas(locacaoId: Long): Flow<List<LocacaoLinhaEntity>>

    @Query("SELECT * FROM locacao_peca WHERE locacaoId = :locacaoId ORDER BY id")
    fun observarPecas(locacaoId: Long): Flow<List<LocacaoPecaEntity>>

    /**
     * Quanto de cada item esta reservado na janela pedida.
     *
     * Duas janelas se sobrepoem quando `inicioA < fimB && fimA > inicioB`. O `<` e
     * o `>` sao estritos de proposito: retirada as 10h e nova entrega as 10h do
     * mesmo dia nao conflitam. So contam os status que ocupam estoque; [excluirId]
     * permite editar uma locacao sem ela brigar consigo mesma.
     */
    @Query(
        """
        SELECT p.itemId AS itemId, SUM(p.quantidade) AS quantidade
        FROM locacao_peca p
        JOIN locacao l ON l.id = p.locacaoId
        WHERE l.status IN ('CONFIRMADA', 'ENTREGUE')
          AND l.id != :excluirId
          AND l.inicioEpochSeg < :fimEpochSeg
          AND l.fimEpochSeg > :inicioEpochSeg
        GROUP BY p.itemId
        """
    )
    fun observarReservas(
        inicioEpochSeg: Long,
        fimEpochSeg: Long,
        excluirId: Long = -1
    ): Flow<List<ReservaPorItem>>

    @Insert
    suspend fun inserirLocacao(locacao: LocacaoEntity): Long

    @Update
    suspend fun atualizarLocacao(locacao: LocacaoEntity)

    @Delete
    suspend fun excluirLocacao(locacao: LocacaoEntity)

    @Insert
    suspend fun inserirLinhas(linhas: List<LocacaoLinhaEntity>)

    @Insert
    suspend fun inserirPecas(pecas: List<LocacaoPecaEntity>)

    @Update
    suspend fun atualizarPeca(peca: LocacaoPecaEntity)

    @Query("DELETE FROM locacao_linha WHERE locacaoId = :locacaoId")
    suspend fun apagarLinhas(locacaoId: Long)

    @Query("DELETE FROM locacao_peca WHERE locacaoId = :locacaoId")
    suspend fun apagarPecas(locacaoId: Long)

    @Query("UPDATE locacao SET status = :status WHERE id = :id")
    suspend fun mudarStatus(id: Long, status: StatusLocacaoSql)

    /** Grava a locacao inteira: cabecalho, linhas vendidas e pecas reservadas. */
    @Transaction
    suspend fun salvarCompleta(
        locacao: LocacaoEntity,
        linhas: List<LocacaoLinhaEntity>,
        pecas: List<LocacaoPecaEntity>
    ): Long {
        val id = if (locacao.id == 0L) {
            inserirLocacao(locacao)
        } else {
            atualizarLocacao(locacao)
            apagarLinhas(locacao.id)
            apagarPecas(locacao.id)
            locacao.id
        }
        inserirLinhas(linhas.map { it.copy(id = 0, locacaoId = id) })
        inserirPecas(pecas.map { it.copy(id = 0, locacaoId = id) })
        return id
    }
}

/** Alias so para deixar explicito que o Room grava o enum pelo nome. */
typealias StatusLocacaoSql = br.com.mesasapp.domain.model.StatusLocacao
