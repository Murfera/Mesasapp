package br.com.mesasapp.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import br.com.mesasapp.domain.LinhaPedido
import br.com.mesasapp.domain.PecaPedido
import br.com.mesasapp.domain.Pedido
import br.com.mesasapp.domain.StatusPedido
import br.com.mesasapp.domain.TipoLinha
import br.com.mesasapp.util.Datas

/** Quantidade de cada um dos 15 modelos fixos. Modelo sem linha vale 0. */
@Entity(tableName = "estoque")
data class EstoqueEntity(
    @PrimaryKey val modeloId: String,
    val total: Int
)

@Entity(tableName = "pedido", indices = [Index("inicioEpochSeg"), Index("status")])
data class PedidoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cliente: String,
    val telefone: String,
    val documento: String,
    val endereco: String,
    val referencia: String,
    val inicioEpochSeg: Long,
    val fimEpochSeg: Long,
    val freteCentavos: Long,
    val descontoCentavos: Long,
    val sinalCentavos: Long,
    val caucaoCentavos: Long,
    val pagamento: String,
    val status: StatusPedido,
    val atrasoDias: Int,
    val finalCentavos: Long?,
    val criadoEmEpochSeg: Long
)

@Entity(
    tableName = "pedido_linha",
    foreignKeys = [ForeignKey(entity = PedidoEntity::class, parentColumns = ["id"], childColumns = ["pedidoId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("pedidoId")]
)
data class LinhaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pedidoId: Long,
    val tipo: TipoLinha,
    val mesaId: String?,
    val cadId: String?,
    val qtd: Int,
    val cadeirasPorMesa: Int,
    val precoUnitCentavos: Long
)

@Entity(
    tableName = "pedido_peca",
    foreignKeys = [ForeignKey(entity = PedidoEntity::class, parentColumns = ["id"], childColumns = ["pedidoId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("pedidoId"), Index("modeloId")]
)
data class PecaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pedidoId: Long,
    val modeloId: String,
    val quantidade: Int,
    val reposicaoCentavos: Long,
    val quebradas: Int,
    val faltando: Int
)

data class PedidoComItens(
    @Embedded val pedido: PedidoEntity,
    @Relation(parentColumn = "id", entityColumn = "pedidoId") val linhas: List<LinhaEntity>,
    @Relation(parentColumn = "id", entityColumn = "pedidoId") val pecas: List<PecaEntity>
)

fun PedidoComItens.toModel(): Pedido = with(pedido) {
    Pedido(
        id = id, cliente = cliente, telefone = telefone, documento = documento, endereco = endereco,
        referencia = referencia, inicioEpochSeg = inicioEpochSeg, fimEpochSeg = fimEpochSeg,
        entrega = Datas.paraLocal(inicioEpochSeg).toLocalDate(),
        retirada = Datas.paraLocal(fimEpochSeg).toLocalDate(),
        freteCentavos = freteCentavos, descontoCentavos = descontoCentavos, sinalCentavos = sinalCentavos,
        caucaoCentavos = caucaoCentavos, pagamento = pagamento, status = status, atrasoDias = atrasoDias,
        finalCentavos = finalCentavos,
        linhas = linhas.sortedBy { it.id }.map { LinhaPedido(it.tipo, it.mesaId, it.cadId, it.qtd, it.cadeirasPorMesa, it.precoUnitCentavos) },
        pecas = pecas.sortedBy { it.id }.map { PecaPedido(it.modeloId, it.quantidade, it.reposicaoCentavos, it.quebradas, it.faltando) }
    )
}

fun Pedido.toEntity(criadoEm: Long) = PedidoEntity(
    id, cliente, telefone, documento, endereco, referencia, inicioEpochSeg, fimEpochSeg,
    freteCentavos, descontoCentavos, sinalCentavos, caucaoCentavos, pagamento, status, atrasoDias, finalCentavos, criadoEm
)
