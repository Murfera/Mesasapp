package br.com.mesasapp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
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
 * controles funcionarem: o cadastro de modelo estava impossivel de preencher e
 * so um toque mostrava isso.
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

    private fun irPara(aba: String) {
        regra.onNodeWithText(aba).performClick()
        regra.waitForIdle()
    }

    /**
     * Rola a lista ate o controle aparecer. Em lista preguicosa, o que esta fora
     * da tela nao existe na arvore: sem isso, o teste procura um botao que o
     * Compose ainda nem criou.
     */
    private fun rolarAte(lista: String, tag: String) {
        repeat(20) { indice ->
            if (regra.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()) return
            runCatching { regra.onNodeWithTag(lista).performScrollToIndex(indice) }
            regra.waitForIdle()
        }
    }

    private fun esperarTexto(texto: String) {
        regra.waitUntil(10_000) {
            regra.onAllNodesWithText(texto, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun cadastrarModelo(
        categoria: String,
        material: String,
        cor: String,
        quantidade: String,
        diaria: String
    ) {
        irPara("Estoque")
        regra.onNodeWithTag("botao_cadastrar_modelo").performClick()
        regra.waitForIdle()

        regra.onNodeWithText(categoria).performClick()
        regra.onNodeWithText(material).performClick()
        regra.onNodeWithText(cor).performClick()

        rolarAte("form_modelo", "campo_quantidade")
        regra.onNodeWithTag("campo_quantidade").performTextReplacement(quantidade)

        // O formulario rola: o campo da diaria so existe depois de chegar nele.
        rolarAte("form_modelo", "campo_diaria")
        regra.onNodeWithTag("campo_diaria").performTextReplacement(diaria)

        rolarAte("form_modelo", "botao_salvar_modelo")
        regra.onNodeWithTag("botao_salvar_modelo").performClick()
        regra.waitForIdle()
    }

    @Test
    fun cadastra_modelo_com_quantidade_e_diaria() {
        // Mesa de metal vermelha: as tres escolhas ficavam cortadas fora da tela.
        cadastrarModelo("Mesa", "Metal", "Vermelha", quantidade = "7", diaria = "1234")

        // O modelo salvo aparece na lista, com o nome montado e a diaria digitada
        // inteira: 1234 -> R$ 1.234,00. Antes, digitar na diaria era impossivel.
        esperarTexto("Mesa de metal vermelha")
        regra.onNodeWithText("Mesa de metal vermelha", substring = true).assertIsDisplayed()
        esperarTexto("1.234,00")
    }

    @Test
    fun somar_item_no_pedido_muda_o_total() {
        cadastrarModelo("Cadeira", "Madeira", "Preta", quantidade = "9", diaria = "50")


        irPara("Novo")

        val nome = "Cadeira de madeira sem braco preta"
        rolarAte("lista_novo", "mais_$nome")

        regra.onNodeWithTag("mais_$nome").performClick()
        regra.onNodeWithTag("mais_$nome").performClick()
        regra.waitForIdle()

        // Duas unidades a 50,00 = 100,00 no rodape, sem contar frete.
        // Sem "R$" na comparacao: o formato usa espaco nao separavel depois do simbolo.
        esperarTexto("100,00")
    }

    @Test
    fun salvar_pedido_vazio_explica_o_que_falta() {
        irPara("Novo")

        // Antes, os dois botoes ficavam desabilitados em silencio e pareciam quebrados.
        regra.onNodeWithTag("botao_orcamento").assertIsDisplayed().performClick()
        regra.waitForIdle()
        esperarTexto("Escolha pelo menos um item")
    }
}
