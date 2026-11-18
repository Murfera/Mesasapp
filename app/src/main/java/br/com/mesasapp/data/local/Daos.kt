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
