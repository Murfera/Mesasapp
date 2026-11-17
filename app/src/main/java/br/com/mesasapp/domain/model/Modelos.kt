package br.com.mesasapp.domain.model

/** Mesa ou cadeira. O que existe fisicamente no estoque. */
enum class Categoria { MESA, CADEIRA }

enum class Material { PLASTICO, METAL, MADEIRA, OUTRO }

/**
 * Estagios de uma locacao.
 *
 * So CONFIRMADA e ENTREGUE ocupam estoque: orcamento nao reserva nada, senao
 * um orcamento esquecido bloquearia mesas que estao livres. DEVOLVIDA e
 * CANCELADA liberam o estoque.
 */
enum class StatusLocacao {
    ORCAMENTO,
    CONFIRMADA,
    ENTREGUE,
    DEVOLVIDA,
    CANCELADA;

    val ocupaEstoque: Boolean
        get() = this == CONFIRMADA || this == ENTREGUE
}

enum class FormaPagamento { PIX, DINHEIRO, CARTAO, OUTRO }

/**
 * Item do catalogo: uma mesa ou uma cadeira de um tipo especifico.
 * [quantidadeTotal] e quantas pecas existem; a disponibilidade de uma data sai
 * desse total menos o que ja esta reservado naquela janela.
 */
data class Item(
    val id: Long = 0,
    val nome: String,
    val categoria: Categoria,
    val material: Material = Material.PLASTICO,
    val comBraco: Boolean? = null,
    val cor: String = "",
    val quantidadeTotal: Int = 0,
    val precoDiariaCentavos: Long = 0,
    val valorReposicaoCentavos: Long = 0,
    val ativo: Boolean = true
)

/**
 * O "jogo": uma forma de vender, nao um item de estoque.
 *
 * 1 jogo = 1 mesa + [qtdCadeiras] cadeiras. O estoque continua sendo o de mesas
 * e o de cadeiras; alugar 10 jogos baixa 10 mesas e 40 cadeiras. Dar estoque
 * proprio ao jogo faria a mesma cadeira ser alugada duas vezes.
 */
data class Combo(
    val id: Long = 0,
    val nome: String = "Jogo",
    val itemMesaId: Long,
    val itemCadeiraId: Long,
    val qtdCadeiras: Int = 4,
    val precoDiariaCentavos: Long = 0,
    val ativo: Boolean = true
)

data class Cliente(
    val id: Long = 0,
    val nome: String,
    val telefone: String = "",
    val documento: String = "",
    val endereco: String = "",
    val observacoes: String = ""
)

/** Uma linha do que foi vendido: item avulso ou jogo. */
enum class TipoLinha { ITEM, COMBO }

/**
 * Linha do pedido com o preco **congelado** no momento da venda: mexer no preco
 * do catalogo nao pode mudar o valor de um pedido ja fechado.
 */
data class LocacaoLinha(
    val id: Long = 0,
    val locacaoId: Long = 0,
    val tipo: TipoLinha,
    val refId: Long,
    val descricao: String,
    val quantidade: Int,
    val precoDiariaCentavos: Long
) {
    val subtotalCentavos: Long get() = quantidade * precoDiariaCentavos
}

/**
 * Peca fisica reservada pelo pedido, ja com os jogos desmontados em mesas e
 * cadeiras. E esta tabela que responde "quantas cadeiras estao ocupadas no
 * dia 18" e que recebe a conferencia da devolucao.
 */
data class LocacaoPeca(
    val id: Long = 0,
    val locacaoId: Long = 0,
    val itemId: Long,
    val descricao: String,
    val quantidade: Int,
    val quantidadeDevolvida: Int = 0,
    val quantidadeQuebrada: Int = 0,
    val valorReposicaoCentavos: Long = 0
) {
    val totalQuebraCentavos: Long get() = quantidadeQuebrada * valorReposicaoCentavos
}

/**
 * Resumo de um pedido com os totais ja calculados, para listas e paineis.
 * [itens] e a descricao curta das linhas, para a lista nao precisar busca-las.
 */
data class LocacaoResumo(
    val id: Long,
    val clienteNome: String,
    val telefone: String,
    val endereco: String,
    val inicioEpochSeg: Long,
    val fimEpochSeg: Long,
    val status: StatusLocacao,
    val subtotalCentavos: Long,
    val freteCentavos: Long,
    val montagemCentavos: Long,
    val descontoCentavos: Long,
    val sinalCentavos: Long,
    val quebrasCentavos: Long,
    val itens: String
) {
    val totalCentavos: Long
        get() = subtotalCentavos + freteCentavos + montagemCentavos - descontoCentavos + quebrasCentavos

    val aReceberCentavos: Long get() = totalCentavos - sinalCentavos
}

data class Locacao(
    val id: Long = 0,
    val clienteId: Long? = null,
    val clienteNome: String = "",
    val telefone: String = "",
    val documento: String = "",
    val endereco: String = "",
    val pontoReferencia: String = "",
    val inicioEpochSeg: Long,
    val fimEpochSeg: Long,
    val freteCentavos: Long = 0,
    val montagemCentavos: Long = 0,
    val descontoCentavos: Long = 0,
    val sinalCentavos: Long = 0,
    val caucaoCentavos: Long = 0,
    val observacoes: String = "",
    val status: StatusLocacao = StatusLocacao.ORCAMENTO,
    val criadoEmEpochSeg: Long = 0
)

/** Locacao com as linhas e as pecas, que e como as telas precisam dela. */
data class LocacaoCompleta(
    val locacao: Locacao,
    val linhas: List<LocacaoLinha>,
    val pecas: List<LocacaoPeca>
) {
    val subtotalCentavos: Long get() = linhas.sumOf { it.subtotalCentavos }
    val quebrasCentavos: Long get() = pecas.sumOf { it.totalQuebraCentavos }

    val totalCentavos: Long
        get() = subtotalCentavos + locacao.freteCentavos + locacao.montagemCentavos -
            locacao.descontoCentavos + quebrasCentavos

    val aReceberCentavos: Long get() = totalCentavos - locacao.sinalCentavos
}
