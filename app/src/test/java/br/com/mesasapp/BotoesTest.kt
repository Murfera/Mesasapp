package br.com.mesasapp

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Testes que tocam nos botoes de verdade, com o banco de verdade.
 *
 * Existem porque conferir que o app compila e abre nao diz nada sobre os
 * controles funcionarem: o cadastro de modelo ja foi entregue impossivel de
 * preencher e so um toque mostrava isso.
 *
 * Rodam na JVM (Robolectric) de proposito: assim entram no `gradlew test` de
 * cada build e nao dependem de o celular estar ligado, desbloqueado e livre.
 */
@RunWith(AndroidJUnit4::class)
// Tela de celular de verdade: no tamanho minusculo padrao do Robolectric, as
// linhas ficam fora da area visivel e os toques se perdem.
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BotoesTest {

    @get:Rule
    val regra = createAndroidComposeRule<MainActivity>()

    /** Rola ate o controle (se estiver numa lista que rola) e toca. */
    private fun toque(tag: String) {
        esperarTag(tag)
        runCatching { regra.onNodeWithTag(tag).performScrollTo() }
        regra.onNodeWithTag(tag).performClick()
        regra.waitForIdle()
    }

    private fun digitar(tag: String, texto: String) {
        esperarTag(tag)
        runCatching { regra.onNodeWithTag(tag).performScrollTo() }
        regra.onNodeWithTag(tag).performTextReplacement(texto)
        regra.waitForIdle()
    }

    private fun esperarTag(tag: String) {
        regra.waitUntil(10_000) { regra.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun esperarTexto(texto: String) {
        regra.waitUntil(10_000) {
            regra.onAllNodesWithText(texto, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * Espera o texto aparecer no no marcado ou dentro dele (o selo de status
     * guarda o texto num filho). O valor salvo volta do banco de forma assincrona.
     */
    private fun esperarNoCom(tag: String, texto: String) {
        esperarTag(tag)
        val alvo = hasText(texto, substring = true) and (hasTestTag(tag) or hasAnyAncestor(hasTestTag(tag)))
        regra.waitUntil(10_000) {
            regra.onAllNodes(alvo, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() ||
                runCatching { regra.onNodeWithTag(tag).assertTextContains(texto, substring = true) }.isSuccess
        }
    }

    private fun esperarSumir(tag: String) {
        regra.waitUntil(10_000) { regra.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isEmpty() }
    }

    private fun aba(nome: String) = toque("aba_$nome")

    private fun estoqueBranco(mesas: String, cadeiras: String) {
        aba("Estoque")
        toque("aba_material_PLASTICO")
        digitar("qtd_p-mesa-br", mesas)
        digitar("qtd_p-cadeira-br", cadeiras)
    }

    @Test
    fun estoque_conta_jogos_completos_e_aceita_campo_vazio() {
        estoqueBranco("10", "40")
        digitar("cpm_PLASTICO_BRANCA", "4")
        esperarNoCom("total_PLASTICO_MESA", "10 no total")
        esperarNoCom("jogos_PLASTICO_BRANCA", "10")

        // 5 cadeiras por mesa: 40 / 5 = 8 jogos.
        digitar("cpm_PLASTICO_BRANCA", "5")
        esperarNoCom("jogos_PLASTICO_BRANCA", "8")

        // Apagar o campo nao trava: conta como zero.
        digitar("qtd_p-mesa-br", "")
        esperarNoCom("total_PLASTICO_MESA", "0 no total")
        digitar("cpm_PLASTICO_BRANCA", "4")
    }

    @Test
    fun novo_pedido_soma_jogos_e_explica_o_que_falta() {
        aba("Novo")
        // Jogo padrao: mesa de plastico branca + 4 cadeiras = 8 + 4 x 3 - 2 = 18,00.
        toque("modelo_p-mesa-br")
        toque("modelo_p-cadeira-br")
        digitar("campo_qtd", "1")
        toque("botao_adicionar")
        esperarTexto("18,00/dia")
        // Total = 18,00 + frete padrao de 40,00.
        esperarNoCom("total_novo", "58,00")

        toque("mais_qtd")
        toque("mais_qtd")
        toque("botao_adicionar")
        // 4 jogos x 18,00 = 72,00 (+ frete padrao, que pode ter sido mudado por outro teste).
        esperarTexto("4 jogos")
        esperarTexto("72,00/dia")

        toque("botao_orcamento")
        esperarTexto("Informe o nome do cliente.")
    }

    @Test
    fun pedido_confirmado_entregue_e_devolvido_com_quebra() {
        estoqueBranco("10", "40")
        digitar("cpm_PLASTICO_BRANCA", "4")

        aba("Novo")
        digitar("campo_cliente", "Mariana Souza")
        digitar("campo_telefone", "11987654321")
        digitar("campo_endereco", "Rua das Palmeiras, 120")
        toque("modelo_p-mesa-br")
        toque("modelo_p-cadeira-br")
        digitar("campo_qtd", "1")
        toque("botao_adicionar")
        toque("botao_confirmar")
        esperarTexto("Pedido confirmado e estoque reservado")
        esperarNoCom("status_pedido", "Confirmado")

        // Pix: configura pelo menu e volta para o pedido.
        toque("botao_menu")
        toque("menu_pix")
        digitar("campo_chave_pix", "11999998888")
        digitar("campo_recebedor", "Murf Eventos")
        digitar("campo_cidade", "Campinas")
        regra.onAllNodesWithText("Voltar")[0].performClick()
        regra.waitForIdle()
        esperarNoCom("pix_codigo", "000201")
        esperarNoCom("pix_codigo", "MURF EVENTOS")

        toque("botao_acao") // Marcar como entregue
        esperarNoCom("status_pedido", "Entregue")

        // Uma cadeira quebrada: reposicao de 45,00 entra no total refeito.
        toque("mais_quebradas_p-cadeira-br")
        esperarNoCom("quebradas_p-cadeira-br", "1")
        esperarTexto("45,00")

        toque("botao_acao") // Confirmar devolucao
        esperarNoCom("status_pedido", "Devolvido")

        // A quebrada saiu do estoque.
        aba("Estoque")
        toque("aba_material_PLASTICO")
        esperarNoCom("total_PLASTICO_CADEIRA", "39 no total")
    }

    @Test
    fun filtro_de_material_vale_para_o_app() {
        toque("botao_menu")
        toque("menu_material")
        toque("filtro_METAL")
        try {
            aba("Estoque")
            esperarTag("qtd_m-mesa-br")
            // Com o filtro, as abas Plastico | Metal somem e so aparece metal.
            esperarSumir("aba_material_PLASTICO")
            esperarSumir("qtd_p-mesa-br")
            aba("Novo")
            esperarTag("modelo_m-mesa-br")
            esperarSumir("modelo_p-mesa-br")
        } finally {
            // Volta para Todos, para nao afetar os outros testes (o filtro fica salvo).
            toque("botao_menu")
            toque("menu_material")
            toque("filtro_TODOS")
            aba("Estoque")
            esperarTag("aba_material_PLASTICO")
        }
    }
}
