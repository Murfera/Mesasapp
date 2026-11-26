package br.com.mesasapp.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Regras de negocio, portadas de `handoff/reference/core.mjs`. Tudo em centavos
 * (Long), nunca Double.
 *
 * **Regra em aberto**: o prototipo multiplica o aluguel pelo numero de diarias.
 * O usuario ainda nao decidiu se o app faz isso; enquanto nao decidir, fica
 * desligado e as diarias aparecem so como informacao. Ligar e trocar esta linha.
 */
const val MULTIPLICAR_POR_DIAS = false

const val CADEIRAS_POR_MESA_PADRAO = 4

data class PrecoPeca(val diariaCentavos: Long, val reposicaoCentavos: Long)

data class Precos(
    val tabela: Map<String, PrecoPeca> = TABELA_PADRAO,
    val descontoJogoCentavos: Long = 200,
    val fretePadraoCentavos: Long = 4000,
    /** Cadeiras por mesa, por "Material|Cor" da MESA do jogo. */
    val cadeirasPorMesa: Map<String, Int> = emptyMap()
) {
    fun peca(id: String): PrecoPeca =
        Catalogo.modelo(id)?.let { tabela[it.chavePreco] } ?: PrecoPeca(0, 0)

    fun cadeirasPorMesa(mesaId: String): Int {
        val m = Catalogo.modelo(mesaId) ?: return CADEIRAS_POR_MESA_PADRAO
        return cadeirasPorMesa(m.material, m.cor)
    }

    fun cadeirasPorMesa(material: Material, cor: Cor): Int =
        cadeirasPorMesa[chaveCadeiras(material, cor)]?.takeIf { it > 0 } ?: CADEIRAS_POR_MESA_PADRAO

    /** Diaria de um jogo = mesa + cadeiras por mesa x cadeira - desconto por jogo (minimo 0). */
    fun precoJogo(mesaId: String, cadId: String): Long =
        maxOf(0, peca(mesaId).diariaCentavos + cadeirasPorMesa(mesaId) * peca(cadId).diariaCentavos - descontoJogoCentavos)

    companion object {
        fun chaveCadeiras(material: Material, cor: Cor) = "${material.rotulo}|${cor.rotulo}"

        /** Valores de exemplo do prototipo, para a calculadora ja funcionar na primeira abertura. */
        val TABELA_PADRAO = mapOf(
            "p-mesa" to PrecoPeca(800, 12000),
            "p-cadeira" to PrecoPeca(300, 4500),
            "p-cadeiraBraco" to PrecoPeca(400, 6000),
            "m-mesa" to PrecoPeca(1200, 18000),
            "m-cadeira" to PrecoPeca(500, 8000)
        )
    }
}

enum class TipoLinha { JOGO, MESA, CADEIRA }

/**
 * Linha do pedido. O preco da unidade (ou do jogo) e as cadeiras por mesa ficam
 * **congelados** ao salvar: mexer nos Precos nao muda pedido ja feito.
 */
data class LinhaPedido(
    val tipo: TipoLinha,
    val mesaId: String?,
    val cadId: String?,
    val qtd: Int,
    val cadeirasPorMesa: Int,
    val precoUnitCentavos: Long
) {
    val diariaCentavos: Long get() = qtd * precoUnitCentavos

    val titulo: String
        get() = when (tipo) {
            TipoLinha.JOGO -> "$qtd " + if (qtd == 1) "jogo" else "jogos"
            TipoLinha.MESA -> "$qtd " + if (qtd == 1) "mesa avulsa" else "mesas avulsas"
            TipoLinha.CADEIRA -> "$qtd " + if (qtd == 1) "cadeira avulsa" else "cadeiras avulsas"
        }

    val descricao: String
        get() = when (tipo) {
            TipoLinha.JOGO -> nome(mesaId) + " + " + cadeirasPorMesa + " × " + nome(cadId).lowercase()
            TipoLinha.MESA -> nome(mesaId)
            TipoLinha.CADEIRA -> nome(cadId)
        }

    private fun nome(id: String?) = id?.let { Catalogo.modelo(it)?.nome } ?: "?"

    fun ids(): List<String> = listOfNotNull(mesaId.takeIf { tipo != TipoLinha.CADEIRA }, cadId.takeIf { tipo != TipoLinha.MESA })

    companion object {
        /** Monta a linha com os precos de agora. */
        fun nova(tipo: TipoLinha, mesaId: String, cadId: String, qtd: Int, precos: Precos): LinhaPedido = when (tipo) {
            TipoLinha.JOGO -> LinhaPedido(tipo, mesaId, cadId, qtd, precos.cadeirasPorMesa(mesaId), precos.precoJogo(mesaId, cadId))
            TipoLinha.MESA -> LinhaPedido(tipo, mesaId, null, qtd, 0, precos.peca(mesaId).diariaCentavos)
            TipoLinha.CADEIRA -> LinhaPedido(tipo, null, cadId, qtd, 0, precos.peca(cadId).diariaCentavos)
        }
    }
}

/** Quanto de cada modelo o pedido usa. Jogo usa qtd mesas e qtd x cadeiras por mesa cadeiras. */
fun uso(linhas: List<LinhaPedido>): Map<String, Int> {
    val u = linkedMapOf<String, Int>()
    fun soma(id: String?, n: Int) { if (id != null) u[id] = (u[id] ?: 0) + n }
    linhas.forEach { l ->
        when (l.tipo) {
            TipoLinha.JOGO -> { soma(l.mesaId, l.qtd); soma(l.cadId, l.qtd * l.cadeirasPorMesa) }
            TipoLinha.MESA -> soma(l.mesaId, l.qtd)
            TipoLinha.CADEIRA -> soma(l.cadId, l.qtd)
        }
    }
    return u
}

/** Diarias = dias entre entrega e retirada, minimo 1. */
fun diarias(entrega: LocalDate, retirada: LocalDate): Int =
    ChronoUnit.DAYS.between(entrega, retirada).toInt().coerceAtLeast(1)

data class Conta(
    val dias: Int,
    val diariaCentavos: Long,
    val aluguelCentavos: Long,
    val totalCentavos: Long,
    val saldoCentavos: Long,
    val mesas: Int,
    val cadeiras: Int,
    val uso: Map<String, Int>
)

fun conta(
    linhas: List<LinhaPedido>,
    dias: Int,
    freteCentavos: Long,
    descontoCentavos: Long,
    sinalCentavos: Long,
    multiplicarPorDias: Boolean = MULTIPLICAR_POR_DIAS
): Conta {
    val diaria = linhas.sumOf { it.diariaCentavos }
    val aluguel = if (multiplicarPorDias) diaria * dias else diaria
    val total = maxOf(0, aluguel + freteCentavos - descontoCentavos)
    val u = uso(linhas)
    var mesas = 0
    var cadeiras = 0
    u.forEach { (id, n) -> if (Catalogo.modelo(id)?.ehMesa == true) mesas += n else cadeiras += n }
    return Conta(dias, diaria, aluguel, total, total - sinalCentavos, mesas, cadeiras, u)
}

enum class StatusPedido(val rotulo: String) {
    ORCAMENTO("Orçamento"),
    CONFIRMADO("Confirmado"),
    ENTREGUE("Entregue"),
    DEVOLVIDO("Devolvido");

    /** So Confirmado e Entregue reservam estoque: orcamento esquecido nao pode bloquear mesa livre. */
    val ocupaEstoque: Boolean get() = this == CONFIRMADO || this == ENTREGUE
}

val FORMAS_PAGAMENTO = listOf("Pix", "Dinheiro", "Cartão", "Transferência")

/** Peca fisica do pedido, com o valor de reposicao congelado e o resultado da vistoria. */
data class PecaPedido(
    val modeloId: String,
    val quantidade: Int,
    val reposicaoCentavos: Long,
    val quebradas: Int = 0,
    val faltando: Int = 0
)

data class Pedido(
    val id: Long = 0,
    val cliente: String,
    val telefone: String,
    val documento: String,
    val endereco: String,
    val referencia: String,
    /** Epoch em segundos: a sobreposicao de janelas vira comparacao numerica. */
    val inicioEpochSeg: Long,
    val fimEpochSeg: Long,
    val entrega: LocalDate,
    val retirada: LocalDate,
    val freteCentavos: Long,
    val descontoCentavos: Long,
    val sinalCentavos: Long,
    val caucaoCentavos: Long,
    val pagamento: String,
    val status: StatusPedido,
    val atrasoDias: Int = 0,
    val finalCentavos: Long? = null,
    val linhas: List<LinhaPedido>,
    val pecas: List<PecaPedido>,
    /** Quando o pedido foi emitido (ou alterado pela ultima vez): vai no recibo. */
    val criadoEmEpochSeg: Long = 0
) {
    val conta: Conta by lazy {
        conta(linhas, diarias(entrega, retirada), freteCentavos, descontoCentavos, sinalCentavos)
    }

    /** Pedido aparece no filtro se tiver pelo menos um item do material. */
    fun temMaterial(filtro: FiltroMaterial): Boolean {
        val mat = filtro.material ?: return true
        return linhas.any { l -> l.ids().any { Catalogo.modelo(it)?.material == mat } }
    }
}

/**
 * Quanto de cada modelo esta reservado na janela. Duas janelas se sobrepoem
 * quando `inicioA < fimB && fimA > inicioB`. Como a entrega e gravada as 08h e a
 * retirada as 18h, o dia da retirada ainda conta como ocupado (regra do prototipo).
 */
fun reservado(pedidos: List<Pedido>, inicioEpochSeg: Long, fimEpochSeg: Long, excluirId: Long = -1): Map<String, Int> {
    val res = hashMapOf<String, Int>()
    pedidos.forEach { p ->
        if (p.id == excluirId || !p.status.ocupaEstoque) return@forEach
        if (p.inicioEpochSeg < fimEpochSeg && p.fimEpochSeg > inicioEpochSeg) {
            p.pecas.forEach { res[it.modeloId] = (res[it.modeloId] ?: 0) + it.quantidade }
        }
    }
    return res
}

/** Jogos completos de um material + cor: min(mesas, cadeiras sem braco / cadeiras por mesa). */
fun jogosCompletos(estoque: Map<String, Int>, precos: Precos, material: Material, cor: Cor): Int {
    val mesas = estoque[Modelo(material, Peca.MESA, cor).id] ?: 0
    val cadeiras = estoque[Modelo(material, Peca.CADEIRA, cor).id] ?: 0
    return minOf(mesas, cadeiras / precos.cadeirasPorMesa(material, cor))
}

data class Fechamento(
    val avariasCentavos: Long,
    val atrasoCentavos: Long,
    val finalCentavos: Long,
    val abatidoCentavos: Long,
    val restoCentavos: Long
)

/**
 * Total refeito na vistoria: total + (quebradas + faltando) x reposicao + atraso x diaria.
 * Resto = final - (sinal + caucao). Positivo: cobrar. Negativo: devolver ao cliente.
 */
fun fechamento(pedido: Pedido, quebradas: Map<String, Int>, faltando: Map<String, Int>, atrasoDias: Int): Fechamento {
    val c = pedido.conta
    val avarias = pedido.pecas.sumOf { ((quebradas[it.modeloId] ?: 0) + (faltando[it.modeloId] ?: 0)) * it.reposicaoCentavos }
    val atraso = atrasoDias * c.diariaCentavos
    val final = c.totalCentavos + avarias + atraso
    val abatido = pedido.sinalCentavos + pedido.caucaoCentavos
    return Fechamento(avarias, atraso, final, abatido, final - abatido)
}
