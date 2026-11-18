package br.com.mesasapp

import br.com.mesasapp.domain.LinhaPedido
import br.com.mesasapp.domain.MULTIPLICAR_POR_DIAS
import br.com.mesasapp.domain.Pedido
import br.com.mesasapp.domain.Precos
import br.com.mesasapp.domain.StatusPedido
import br.com.mesasapp.domain.TipoLinha
import br.com.mesasapp.domain.PecaPedido
import br.com.mesasapp.domain.conta
import br.com.mesasapp.domain.diarias
import br.com.mesasapp.domain.fechamento
import br.com.mesasapp.domain.jogosCompletos
import br.com.mesasapp.domain.Material
import br.com.mesasapp.domain.Cor
import br.com.mesasapp.domain.reservado
import br.com.mesasapp.domain.Catalogo
import br.com.mesasapp.util.Datas
import br.com.mesasapp.util.Links
import br.com.mesasapp.util.Pix
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Paridade com o prototipo: os numeros esperados vem de
 * `handoff/reference/expected-from-prototype.json`, gerados pelo codigo do prototipo
 * com os dados de `sample-data.json`. O prototipo multiplica pelas diarias, entao a
 * paridade e conferida com `multiplicarPorDias = true`.
 */
class RegrasTest {

    private val precos = Precos()

    private fun jogo(mesa: String, cad: String, q: Int) = LinhaPedido.nova(TipoLinha.JOGO, mesa, cad, q, precos)
    private fun mesa(id: String, q: Int) = LinhaPedido.nova(TipoLinha.MESA, id, "", q, precos)
    private fun cadeira(id: String, q: Int) = LinhaPedido.nova(TipoLinha.CADEIRA, "", id, q, precos)

    private fun pedido(
        id: Long, entrega: String, retirada: String, status: StatusPedido, linhas: List<LinhaPedido>,
        frete: Long, desconto: Long = 0, sinal: Long = 0, cliente: String = "Cliente", tel: String = "", ref: String = "", end: String = ""
    ): Pedido {
        val e = LocalDate.parse(entrega)
        val r = LocalDate.parse(retirada)
        val u = br.com.mesasapp.domain.uso(linhas)
        return Pedido(
            id, cliente, tel, "", end, ref, Datas.entregaEpochSeg(e), Datas.retiradaEpochSeg(r), e, r,
            frete * 100, desconto * 100, sinal * 100, 0, "Pix", status,
            linhas = linhas, pecas = u.map { (m, n) -> PecaPedido(m, n, precos.peca(m).reposicaoCentavos) }
        )
    }

    private val p1044 = pedido(
        1044, "2026-11-28", "2026-11-30", StatusPedido.ORCAMENTO, listOf(jogo("p-mesa-br", "p-cadeira-br", 15)), 50,
        cliente = "Juliana Martins", tel = "(11) 91234-5678", ref = "Casa de esquina, muro verde", end = "Rua Primavera, 88 — Jardim Europa"
    )
    private val p1043 = pedido(1043, "2026-11-21", "2026-11-23", StatusPedido.CONFIRMADO, listOf(jogo("p-mesa-br", "p-cadeiraBraco-br", 8), cadeira("p-cadeira-br", 10)), 40, sinal = 150)
    private val p1042 = pedido(
        1042, "2026-11-17", "2026-11-18", StatusPedido.CONFIRMADO,
        listOf(jogo("m-mesa-pr", "m-cadeira-pr", 8), jogo("p-mesa-br", "p-cadeira-br", 4), mesa("p-mesa-br", 2)), 60, 20, 120
    )
    private val p1040 = pedido(1040, "2026-11-15", "2026-11-17", StatusPedido.ENTREGUE, listOf(jogo("p-mesa-br", "p-cadeira-br", 5), cadeira("p-cadeiraBraco-br", 6)), 40, sinal = 80)
    private val p1037 = pedido(1037, "2026-11-08", "2026-11-10", StatusPedido.DEVOLVIDO, listOf(jogo("p-mesa-br", "p-cadeira-br", 6)), 40, sinal = 60)
    private val todos = listOf(p1044, p1043, p1042, p1040, p1037)

    private fun c(p: Pedido) = conta(p.linhas, diarias(p.entrega, p.retirada), p.freteCentavos, p.descontoCentavos, p.sinalCentavos, multiplicarPorDias = true)

    @Test
    fun `totais batem com o prototipo`() {
        // id -> total, saldo, diarias, mesas, cadeiras (em reais, como no JSON)
        val esperado = mapOf(
            p1037 to listOf(256, 196, 2, 6, 24),
            p1040 to listOf(268, 188, 2, 5, 26),
            p1042 to listOf(368, 248, 1, 14, 48),
            p1043 to listOf(452, 302, 2, 8, 42),
            p1044 to listOf(590, 590, 2, 15, 60)
        )
        esperado.forEach { (p, e) ->
            val k = c(p)
            assertEquals("total ${p.id}", e[0] * 100L, k.totalCentavos)
            assertEquals("saldo ${p.id}", e[1] * 100L, k.saldoCentavos)
            assertEquals("dias ${p.id}", e[2], k.dias)
            assertEquals("mesas ${p.id}", e[3], k.mesas)
            assertEquals("cadeiras ${p.id}", e[4], k.cadeiras)
        }
        assertEquals(mapOf("p-mesa-br" to 8, "p-cadeiraBraco-br" to 32, "p-cadeira-br" to 10), c(p1043).uso)
    }

    @Test
    fun `sem multiplicar o aluguel e uma diaria so`() {
        val k = conta(p1043.linhas, 2, 4000, 0, 0, multiplicarPorDias = false)
        // 8 jogos x (8 + 4 x 4 - 2) = 176 + 10 cadeiras x 3 = 30 => 206 + frete 40
        assertEquals(24600, k.totalCentavos)
    }

    @Test
    fun `reserva so conta confirmado e entregue, e o dia da retirada ainda ocupa`() {
        val dia17 = LocalDate.parse("2026-11-17")
        val dia18 = LocalDate.parse("2026-11-18")
        val res = reservado(todos, Datas.entregaEpochSeg(dia17), Datas.retiradaEpochSeg(dia18))
        assertEquals(mapOf("m-mesa-pr" to 8, "m-cadeira-pr" to 32, "p-mesa-br" to 11, "p-cadeira-br" to 36, "p-cadeiraBraco-br" to 6), res)
    }

    @Test
    fun `vistoria soma reposicao e atraso`() {
        val f = fechamento(p1037, mapOf("p-mesa-br" to 1), mapOf("p-cadeira-br" to 2), 1)
        assertEquals(120_00L + 2 * 45_00L, f.avariasCentavos)
        assertEquals(p1037.conta.diariaCentavos, f.atrasoCentavos)
        assertEquals(p1037.conta.totalCentavos + f.avariasCentavos + f.atrasoCentavos, f.finalCentavos)
        assertEquals(f.finalCentavos - 60_00L, f.restoCentavos)
    }

    @Test
    fun `jogos completos usam so cadeira sem braco`() {
        val est = mapOf("p-mesa-br" to 40, "p-cadeira-br" to 160, "p-cadeiraBraco-br" to 40)
        assertEquals(40, jogosCompletos(est, precos, Material.PLASTICO, Cor.BRANCA))
        val cinco = precos.copy(cadeirasPorMesa = mapOf(Precos.chaveCadeiras(Material.PLASTICO, Cor.BRANCA) to 5))
        assertEquals(32, jogosCompletos(est, cinco, Material.PLASTICO, Cor.BRANCA))
    }

    @Test
    fun `sao 15 modelos com os ids do prototipo`() {
        assertEquals(15, Catalogo.modelos.size)
        assertEquals("Cadeira de plástico com braço branca", Catalogo.modelo("p-cadeiraBraco-br")?.nome)
        assertEquals("m-mesa-br", Catalogo.equivalente("p-mesa-br", Material.METAL))
        assertEquals("m-cadeira-vm", Catalogo.equivalente("p-cadeiraBraco-vm", Material.METAL))
    }

    @Test
    fun `pix igual ao do prototipo`() {
        assertEquals(
            "00020126470014br.gov.bcb.pix0125contato@suaempresa.com.br5204000053039865406120.505802BR5920SUA EMPRESA LOCACOES6009SAO PAULO62110507PED10446304D7B5",
            Pix.copiaECola("contato@suaempresa.com.br", "Sua Empresa Locacoes", "Sao Paulo", 12050, "PED1044")
        )
    }

    @Test
    fun `link do google agenda igual ao do prototipo`() {
        var esperado = "https://calendar.google.com/calendar/render?action=TEMPLATE&text=Entrega%20%E2%80%94%20Juliana%20Martins%20(%231044)&dates=20261128/20261129&details=%E2%80%A2%2015%20jogos%3A%20Mesa%20de%20pl%C3%A1stico%20branca%20%2B%204%20%C3%97%20cadeira%20de%20pl%C3%A1stico%20branca%0A%0ATotal%3A%2015%20mesas%20e%2060%20cadeiras%0ATelefone%3A%20(11)%2091234-5678%0ARefer%C3%AAncia%3A%20Casa%20de%20esquina%2C%20muro%20verde%0ASaldo%20a%20receber%3A%20R%24%C2%A0590%2C00&location=Rua%20Primavera%2C%2088%20%E2%80%94%20Jardim%20Europa"
        // O saldo do link segue a regra do app; sem multiplicar, 15 jogos x 18 + 50 = 320.
        if (!MULTIPLICAR_POR_DIAS) esperado = esperado.replace("590%2C00", "320%2C00")
        assertEquals(esperado, Links.googleAgenda(p1044, entrega = true))
    }
}
