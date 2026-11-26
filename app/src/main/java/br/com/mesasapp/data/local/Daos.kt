package br.com.mesasapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import br.com.mesasapp.domain.StatusPedido
import kotlinx.coroutines.flow.Flow

@Dao
interface MesasDao {

    @Query("SELECT * FROM estoque")
    fun observarEstoque(): Flow<List<EstoqueEntity>>

    @Query("SELECT * FROM estoque WHERE modeloId = :id")
    suspend fun estoque(id: String): EstoqueEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun gravarEstoque(e: EstoqueEntity)

    /**
     * Todos os pedidos, com linhas e pecas. O Room faz tres consultas no total
     * (nao uma por pedido), e as telas somam em memoria, como no prototipo.
     */
    @Transaction
    @Query("SELECT * FROM pedido ORDER BY inicioEpochSeg DESC")
    fun observarPedidos(): Flow<List<PedidoComItens>>

    @Insert
    suspend fun inserirPedido(p: PedidoEntity): Long

    @Insert
    suspend fun inserirLinhas(l: List<LinhaEntity>)

    @Insert
    suspend fun inserirPecas(p: List<PecaEntity>)

    @Query("UPDATE pedido SET status = :status WHERE id = :id")
    suspend fun mudarStatus(id: Long, status: StatusPedido)

    @Query("UPDATE pedido SET status = :status, atrasoDias = :atraso, finalCentavos = :finalCentavos WHERE id = :id")
    suspend fun fecharPedido(id: Long, status: StatusPedido, atraso: Int, finalCentavos: Long)

    @Query("UPDATE pedido_peca SET quebradas = :quebradas, faltando = :faltando WHERE pedidoId = :pedidoId AND modeloId = :modeloId")
    suspend fun vistoriarPeca(pedidoId: Long, modeloId: String, quebradas: Int, faltando: Int)

    @Transaction
    suspend fun salvarPedido(p: PedidoEntity, linhas: List<LinhaEntity>, pecas: List<PecaEntity>): Long {
        val id = inserirPedido(p)
        inserirLinhas(linhas.map { it.copy(id = 0, pedidoId = id) })
        inserirPecas(pecas.map { it.copy(id = 0, pedidoId = id) })
        return id
    }

    @androidx.room.Update
    suspend fun atualizarPedidoEntity(p: PedidoEntity)

    @Query("DELETE FROM pedido_linha WHERE pedidoId = :id")
    suspend fun apagarLinhas(id: Long)

    @Query("DELETE FROM pedido_peca WHERE pedidoId = :id")
    suspend fun apagarPecas(id: Long)

    @Query("DELETE FROM pedido WHERE id = :id")
    suspend fun apagarPedidoEntity(id: Long)

    /** Edicao: mesmo numero de pedido, linhas e pecas trocadas pelas novas. */
    @Transaction
    suspend fun atualizarPedido(p: PedidoEntity, linhas: List<LinhaEntity>, pecas: List<PecaEntity>) {
        atualizarPedidoEntity(p)
        apagarLinhas(p.id)
        apagarPecas(p.id)
        inserirLinhas(linhas.map { it.copy(id = 0, pedidoId = p.id) })
        inserirPecas(pecas.map { it.copy(id = 0, pedidoId = p.id) })
    }

    /** Linhas e pecas apagadas explicitamente, sem depender do CASCADE. */
    @Transaction
    suspend fun excluirPedido(id: Long) {
        apagarLinhas(id)
        apagarPecas(id)
        apagarPedidoEntity(id)
    }

    // --- backup ---

    @Query("SELECT * FROM estoque ORDER BY modeloId")
    suspend fun todoEstoque(): List<EstoqueEntity>

    @Transaction
    @Query("SELECT * FROM pedido ORDER BY id")
    suspend fun todosPedidos(): List<PedidoComItens>

    @Query("DELETE FROM estoque")
    suspend fun apagarEstoque()

    /** As linhas e as pecas saem junto, pela chave estrangeira em CASCADE. */
    @Query("DELETE FROM pedido")
    suspend fun apagarPedidos()

    @Insert
    suspend fun inserirPedidos(p: List<PedidoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun gravarEstoques(e: List<EstoqueEntity>)

    /**
     * Restauracao: substitui estoque e pedidos pelo conteudo do backup.
     *
     * E substituicao, nao mesclagem: os numeros de pedido vem do arquivo e
     * mesclar criaria duplicatas ou colisoes silenciosas. Tudo numa transacao —
     * se qualquer passo falhar, o banco volta ao estado anterior em vez de ficar
     * meio importado.
     */
    @Transaction
    suspend fun restaurar(
        estoque: List<EstoqueEntity>,
        pedidos: List<PedidoEntity>,
        linhas: List<LinhaEntity>,
        pecas: List<PecaEntity>
    ) {
        apagarPedidos()
        apagarEstoque()
        gravarEstoques(estoque)
        // Pedidos antes das linhas e pecas: elas apontam para ele.
        inserirPedidos(pedidos)
        inserirLinhas(linhas)
        inserirPecas(pecas)
    }

    /**
     * Devolucao: grava a vistoria, fecha o pedido com o total final e tira do
     * estoque o que quebrou ou nao voltou. Tudo junto ou nada.
     */
    @Transaction
    suspend fun registrarDevolucao(
        pedidoId: Long,
        quebradas: Map<String, Int>,
        faltando: Map<String, Int>,
        atraso: Int,
        finalCentavos: Long
    ) {
        (quebradas.keys + faltando.keys).forEach { id ->
            val q = quebradas[id] ?: 0
            val f = faltando[id] ?: 0
            vistoriarPeca(pedidoId, id, q, f)
            if (q + f > 0) {
                val atual = estoque(id)?.total ?: 0
                gravarEstoque(EstoqueEntity(id, maxOf(0, atual - q - f)))
            }
        }
        fecharPedido(pedidoId, StatusPedido.DEVOLVIDO, atraso, finalCentavos)
    }
}
