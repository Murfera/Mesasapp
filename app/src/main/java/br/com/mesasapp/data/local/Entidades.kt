package br.com.mesasapp.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.mesasapp.domain.model.Categoria
import br.com.mesasapp.domain.model.Cliente
import br.com.mesasapp.domain.model.Combo
import br.com.mesasapp.domain.model.Item
import br.com.mesasapp.domain.model.Locacao
import br.com.mesasapp.domain.model.LocacaoLinha
import br.com.mesasapp.domain.model.LocacaoPeca
import br.com.mesasapp.domain.model.Material
import br.com.mesasapp.domain.model.StatusLocacao
import br.com.mesasapp.domain.model.TipoLinha

@Entity(tableName = "item")
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val categoria: Categoria,
    val material: Material,
    val comBraco: Boolean?,
    val cor: String,
    val quantidadeTotal: Int,
    val precoDiariaCentavos: Long,
    val valorReposicaoCentavos: Long,
    val ativo: Boolean
)

@Entity(tableName = "combo")
data class ComboEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val itemMesaId: Long,
    val itemCadeiraId: Long,
    val qtdCadeiras: Int,
    val precoDiariaCentavos: Long,
    val ativo: Boolean
)

@Entity(tableName = "cliente", indices = [Index("nome")])
data class ClienteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val telefone: String,
    val documento: String,
    val endereco: String,
    val observacoes: String
)

@Entity(
    tableName = "locacao",
    indices = [Index("inicioEpochSeg"), Index("status")]
)
data class LocacaoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clienteId: Long?,
    val clienteNome: String,
    val telefone: String,
    val documento: String,
    val endereco: String,
    val pontoReferencia: String,
    val inicioEpochSeg: Long,
    val fimEpochSeg: Long,
    val freteCentavos: Long,
    val montagemCentavos: Long,
    val descontoCentavos: Long,
    val sinalCentavos: Long,
    val caucaoCentavos: Long,
    val observacoes: String,
    val status: StatusLocacao,
    val criadoEmEpochSeg: Long
)

@Entity(
    tableName = "locacao_linha",
    foreignKeys = [
        ForeignKey(
            entity = LocacaoEntity::class,
            parentColumns = ["id"],
            childColumns = ["locacaoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("locacaoId")]
)
data class LocacaoLinhaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val locacaoId: Long,
    val tipo: TipoLinha,
    val refId: Long,
    val descricao: String,
    val quantidade: Int,
    @ColumnInfo(name = "precoDiariaCentavos") val precoDiariaCentavos: Long
)

@Entity(
    tableName = "locacao_peca",
    foreignKeys = [
        ForeignKey(
            entity = LocacaoEntity::class,
            parentColumns = ["id"],
            childColumns = ["locacaoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("locacaoId"), Index("itemId")]
)
data class LocacaoPecaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val locacaoId: Long,
    val itemId: Long,
    val descricao: String,
    val quantidade: Int,
    val quantidadeDevolvida: Int,
    val quantidadeQuebrada: Int,
    val valorReposicaoCentavos: Long
)

/** Quanto de cada item esta reservado numa janela de datas. */
data class ReservaPorItem(val itemId: Long, val quantidade: Int)

/** Quantas pecas de cada item ja foram dadas como quebradas, somando o historico. */
data class QuebraPorItem(val itemId: Long, val quantidade: Int)

/**
 * Cabecalho do pedido com os totais ja somados no SQL.
 *
 * As telas de lista, o painel e a agenda precisam do total de dezenas de pedidos
 * ao mesmo tempo; carregar as linhas de cada um so para somar seria um N+1.
 */
data class LocacaoResumoEntity(
    val id: Long,
    val clienteNome: String,
    val telefone: String,
    val endereco: String,
    val inicioEpochSeg: Long,
    val fimEpochSeg: Long,
    val status: br.com.mesasapp.domain.model.StatusLocacao,
    val freteCentavos: Long,
    val montagemCentavos: Long,
    val descontoCentavos: Long,
    val sinalCentavos: Long,
    val subtotalCentavos: Long,
    val quebrasCentavos: Long,
    val itens: String?
)

// --- conversoes entre entidade e modelo de dominio ---

fun ItemEntity.toModel() = Item(
    id, nome, categoria, material, comBraco, cor,
    quantidadeTotal, precoDiariaCentavos, valorReposicaoCentavos, ativo
)

fun Item.toEntity() = ItemEntity(
    id, nome, categoria, material, comBraco, cor,
    quantidadeTotal, precoDiariaCentavos, valorReposicaoCentavos, ativo
)

fun ComboEntity.toModel() = Combo(id, nome, itemMesaId, itemCadeiraId, qtdCadeiras, precoDiariaCentavos, ativo)
fun Combo.toEntity() = ComboEntity(id, nome, itemMesaId, itemCadeiraId, qtdCadeiras, precoDiariaCentavos, ativo)

fun ClienteEntity.toModel() = Cliente(id, nome, telefone, documento, endereco, observacoes)
fun Cliente.toEntity() = ClienteEntity(id, nome, telefone, documento, endereco, observacoes)

fun LocacaoEntity.toModel() = Locacao(
    id, clienteId, clienteNome, telefone, documento, endereco, pontoReferencia, inicioEpochSeg, fimEpochSeg,
    freteCentavos, montagemCentavos, descontoCentavos, sinalCentavos, caucaoCentavos,
    observacoes, status, criadoEmEpochSeg
)

fun Locacao.toEntity() = LocacaoEntity(
    id, clienteId, clienteNome, telefone, documento, endereco, pontoReferencia, inicioEpochSeg, fimEpochSeg,
    freteCentavos, montagemCentavos, descontoCentavos, sinalCentavos, caucaoCentavos,
    observacoes, status, criadoEmEpochSeg
)

fun LocacaoLinhaEntity.toModel() = LocacaoLinha(id, locacaoId, tipo, refId, descricao, quantidade, precoDiariaCentavos)
fun LocacaoLinha.toEntity() = LocacaoLinhaEntity(id, locacaoId, tipo, refId, descricao, quantidade, precoDiariaCentavos)

fun LocacaoPecaEntity.toModel() = LocacaoPeca(
    id, locacaoId, itemId, descricao, quantidade, quantidadeDevolvida, quantidadeQuebrada, valorReposicaoCentavos
)

fun LocacaoPeca.toEntity() = LocacaoPecaEntity(
    id, locacaoId, itemId, descricao, quantidade, quantidadeDevolvida, quantidadeQuebrada, valorReposicaoCentavos
)
