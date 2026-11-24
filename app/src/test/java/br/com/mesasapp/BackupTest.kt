package br.com.mesasapp

import br.com.mesasapp.data.backup.AutoBackupScheduler
import br.com.mesasapp.data.backup.BackupData
import br.com.mesasapp.data.backup.BackupFormatException
import br.com.mesasapp.data.backup.BackupSerializer
import br.com.mesasapp.data.backup.ConfigBackup
import br.com.mesasapp.data.backup.PedidoBackup
import br.com.mesasapp.data.local.EstoqueEntity
import br.com.mesasapp.data.local.LinhaEntity
import br.com.mesasapp.data.local.PecaEntity
import br.com.mesasapp.data.local.PedidoEntity
import br.com.mesasapp.data.preferences.OpcoesPagamento
import br.com.mesasapp.data.preferences.PixConfig
import br.com.mesasapp.domain.Cor
import br.com.mesasapp.domain.Material
import br.com.mesasapp.domain.PrecoPeca
import br.com.mesasapp.domain.Precos
import br.com.mesasapp.domain.StatusPedido
import br.com.mesasapp.domain.TipoLinha
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime

/**
 * O backup e a unica saida depois de perder o aparelho: um arquivo que nao volta
 * exatamente como saiu nao serve. Estes testes passam os dados pelo JSON e
 * conferem o que voltou, inclusive os casos em que o arquivo tem que ser
 * recusado.
 *
 * Roda com Robolectric porque `org.json` e do Android, nao da JVM.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupTest {

    private val estoque = listOf(
        EstoqueEntity("p-mesa-br", 40),
        EstoqueEntity("p-cadeira-br", 160)
    )

    private val pedido = PedidoEntity(
        id = 7,
        cliente = "Dona Maria",
        telefone = "(11) 99999-0000",
        documento = "123.456.789-00",
        endereco = "Rua das Flores, 10",
        referencia = "portão lateral",
        inicioEpochSeg = 1_700_000_000,
        fimEpochSeg = 1_700_200_000,
        freteCentavos = 4000,
        descontoCentavos = 500,
        sinalCentavos = 10_000,
        caucaoCentavos = 5000,
        pagamento = "Pix",
        status = StatusPedido.ENTREGUE,
        atrasoDias = 2,
        finalCentavos = 98_700,
        criadoEmEpochSeg = 1_699_000_000
    )

    private val linhas = listOf(
        LinhaEntity(0, 7, TipoLinha.JOGO, "p-mesa-br", "p-cadeira-br", 3, 4, 2000),
        LinhaEntity(0, 7, TipoLinha.MESA, "p-mesa-br", null, 2, 4, 800)
    )

    private val pecas = listOf(
        PecaEntity(0, 7, "p-mesa-br", 5, 12_000, 1, 0),
        PecaEntity(0, 7, "p-cadeira-br", 12, 4500, 0, 2)
    )

    private val config = ConfigBackup(
        precos = Precos(
            tabela = Precos.TABELA_PADRAO + ("p-mesa" to PrecoPeca(999, 15_000)),
            descontoJogoCentavos = 300,
            fretePadraoCentavos = 5500,
            cadeirasPorMesa = mapOf(Precos.chaveCadeiras(Material.PLASTICO, Cor.entries.first()) to 6)
        ),
        pix = PixConfig("chave@teste.com", "Locadora Teste", "SAO PAULO"),
        pagamento = OpcoesPagamento(mostrarSinal = true, mostrarCaucao = false)
    )

    private val completo = BackupData(
        estoque = estoque,
        pedidos = listOf(PedidoBackup(pedido, linhas, pecas)),
        config = config
    )

    private fun idaEVolta(data: BackupData = completo) =
        BackupSerializer.fromJson(BackupSerializer.toJson(data))

    @Test
    fun `o pedido volta com todos os campos`() {
        val volta = idaEVolta().pedidos.single()
        assertEquals(pedido, volta.pedido)
    }

    @Test
    fun `linhas e pecas voltam na mesma ordem, apontando para o pedido`() {
        val volta = idaEVolta().pedidos.single()
        assertEquals(linhas, volta.linhas)
        assertEquals(pecas, volta.pecas)
        assertTrue(volta.linhas.all { it.pedidoId == 7L })
        assertTrue(volta.pecas.all { it.pedidoId == 7L })
    }

    /** Linha de mesa nao tem cadeira: o nulo nao pode virar a string "null". */
    @Test
    fun `modelo nulo na linha continua nulo`() {
        assertNull(idaEVolta().pedidos.single().linhas[1].cadId)
    }

    @Test
    fun `estoque volta inteiro`() {
        assertEquals(estoque, idaEVolta().estoque)
    }

    @Test
    fun `precos, Pix e Exibir voltam inteiros`() {
        val volta = idaEVolta().config
        assertNotNull(volta)
        assertEquals(config.precos.tabela, volta!!.precos.tabela)
        assertEquals(300L, volta.precos.descontoJogoCentavos)
        assertEquals(5500L, volta.precos.fretePadraoCentavos)
        assertEquals(config.precos.cadeirasPorMesa, volta.precos.cadeirasPorMesa)
        assertEquals(config.pix, volta.pix)
        assertEquals(config.pagamento, volta.pagamento)
    }

    /** Pedido fechado sem total final: o nulo precisa sobreviver ao arquivo. */
    @Test
    fun `total final nulo continua nulo`() {
        val semFinal = completo.copy(
            pedidos = listOf(PedidoBackup(pedido.copy(finalCentavos = null), linhas, pecas))
        )
        assertNull(idaEVolta(semFinal).pedidos.single().pedido.finalCentavos)
    }

    @Test
    fun `backup sem nenhum pedido e valido`() {
        val vazio = BackupData(emptyList(), emptyList(), config)
        val volta = idaEVolta(vazio)
        assertTrue(volta.pedidos.isEmpty())
        assertTrue(volta.estoque.isEmpty())
    }

    /** Backup de uma versao sem configuracao continua legivel. */
    @Test
    fun `arquivo sem a secao de configuracao e aceito`() {
        val json = BackupSerializer.toJson(completo.copy(config = null))
        val volta = BackupSerializer.fromJson(json)
        assertNull(volta.config)
        assertEquals(1, volta.pedidos.size)
    }

    private fun recusa(texto: String, json: String) {
        try {
            BackupSerializer.fromJson(json)
            fail("devia recusar: $texto")
        } catch (e: BackupFormatException) {
            assertTrue("mensagem vazia", !e.message.isNullOrBlank())
        }
    }

    @Test
    fun `arquivo que nao e JSON e recusado`() {
        recusa("texto solto", "isto nao e um backup")
    }

    @Test
    fun `formato mais novo e recusado`() {
        val json = BackupSerializer.toJson(completo)
            .replace("\"formato\": 1", "\"formato\": 99")
        recusa("formato futuro", json)
    }

    @Test
    fun `modelo fora do catalogo e recusado`() {
        val json = BackupSerializer.toJson(completo).replace("p-mesa-br", "x-mesa-xx")
        recusa("modelo inventado", json)
    }

    @Test
    fun `status desconhecido e recusado`() {
        val json = BackupSerializer.toJson(completo).replace("\"ENTREGUE\"", "\"SUMIU\"")
        recusa("status inventado", json)
    }

    @Test
    fun `pedido sem campo obrigatorio e recusado`() {
        val json = BackupSerializer.toJson(completo).replace("\"freteCentavos\"", "\"freteOutro\"")
        recusa("campo faltando", json)
    }

    @Test
    fun `dois pedidos com o mesmo numero sao recusados`() {
        val repetido = completo.copy(
            pedidos = listOf(
                PedidoBackup(pedido, linhas, pecas),
                PedidoBackup(pedido, linhas, pecas)
            )
        )
        recusa("id repetido", BackupSerializer.toJson(repetido))
    }

    @Test
    fun `o nome do arquivo tem a data para varios backups conviverem`() {
        val nome = AutoBackupScheduler.nomeDoArquivo(LocalDateTime.of(2026, 3, 9, 4, 5))
        assertEquals("mesasapp-backup-2026-03-09-0405.json", nome)
    }
}
