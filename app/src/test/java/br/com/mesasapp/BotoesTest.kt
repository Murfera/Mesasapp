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
        diaria: String,
        reposicao: String? = null
    ) {
        irPara("Estoque")
        regra.onNodeWithTag("botao_cadastrar_modelo").performClick()
        regra.waitForIdle()

        // Pelos marcadores, e nao pelo texto: o mesmo "Metal" aparece nos cartoes
        // dos modelos ja cadastrados, atras do formulario.
        rolarAte("form_modelo", "chip_$categoria")
        regra.onNodeWithTag("chip_$categoria").performClick()
        rolarAte("form_modelo", "chip_$material")
        regra.onNodeWithTag("chip_$material").performClick()
        rolarAte("form_modelo", "chip_$cor")
        regra.onNodeWithTag("chip_$cor").performClick()

        rolarAte("form_modelo", "campo_quantidade")
        regra.onNodeWithTag("campo_quantidade").performTextReplacement(quantidade)

        // O formulario rola: o campo da diaria so existe depois de chegar nele.
        rolarAte("form_modelo", "campo_diaria")
        regra.onNodeWithTag("campo_diaria").performTextReplacement(diaria)

        if (reposicao != null) {
            rolarAte("form_modelo", "campo_reposicao")
            regra.onNodeWithTag("campo_reposicao").performTextReplacement(reposicao)
        }

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
        cadastrarModelo("Cadeira", "Metal", "Preta", quantidade = "9", diaria = "50")


        irPara("Novo")

        val nome = "Cadeira de metal sem braco preta"
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

    @Test
    fun configuracoes_salvam_a_chave_pix() {
        regra.onNodeWithTag("botao_config").performClick()
        regra.waitForIdle()

        rolarAte("lista_config", "campo_chave_pix")
        regra.onNodeWithTag("campo_chave_pix").performTextReplacement("11999998888")
        regra.onNodeWithTag("campo_recebedor").performTextReplacement("Murf Eventos")
        regra.onNodeWithTag("campo_cidade").performTextReplacement("Campinas")

        rolarAte("lista_config", "botao_salvar_config")
        regra.onNodeWithTag("botao_salvar_config").performClick()

        esperarTexto("Configuracoes salvas")
    }

    @Test
    fun pedido_confirmado_vira_cobranca_pix_e_devolucao_soma_a_quebra() {
        // Estoque: cadeira a 50,00 de diaria e 120,00 de reposicao.
        cadastrarModelo("Cadeira", "Metal", "Vermelha", quantidade = "9", diaria = "50", reposicao = "120")

        // Sem chave Pix cadastrada a cobranca nao tem o que gerar.
        regra.onNodeWithText("Inicio").performClick()
        regra.waitForIdle()
        regra.onNodeWithTag("botao_config").performClick()
        regra.waitForIdle()
        rolarAte("lista_config", "campo_chave_pix")
        regra.onNodeWithTag("campo_chave_pix").performTextReplacement("11999998888")
        regra.onNodeWithTag("campo_recebedor").performTextReplacement("Murf Eventos")
        regra.onNodeWithTag("campo_cidade").performTextReplacement("Campinas")
        rolarAte("lista_config", "botao_salvar_config")
        regra.onNodeWithTag("botao_salvar_config").performClick()
        esperarTexto("Configuracoes salvas")

        // Pedido com 2 cadeiras = 100,00.
        irPara("Novo")
        val nome = "Cadeira de metal sem braco vermelha"
        rolarAte("lista_novo", "mais_$nome")
        regra.onNodeWithTag("mais_$nome").performClick()
        regra.onNodeWithTag("mais_$nome").performClick()
        esperarTexto("100,00")

        regra.onNodeWithTag("botao_confirmar").performClick()
        regra.waitForIdle()

        // O pedido aparece na lista e abre no detalhe.
        irPara("Pedidos")
        rolarAte("lista_pedidos", "pedido_1")
        regra.onNodeWithTag("pedido_1").performClick()
        regra.waitForIdle()
        esperarTexto("100,00")

        // Pix: o copia e cola sai pronto, com o cabecalho do padrao do Banco Central.
        rolarAte("lista_pedido", "botao_pix")
        regra.onNodeWithTag("botao_pix").performClick()
        regra.waitForIdle()
        esperarTexto("000201")
        regra.onNodeWithText("Fechar").performClick()
        regra.waitForIdle()

        // Devolucao com uma cadeira quebrada: 100,00 + 120,00 = 220,00.
        rolarAte("lista_pedido", "botao_devolucao")
        regra.onNodeWithTag("botao_devolucao").performClick()
        regra.waitForIdle()
        regra.onNodeWithTag("quebradas_$nome").performTextReplacement("1")
        regra.onNodeWithTag("voltaram_$nome").performTextReplacement("1")
        regra.onNodeWithTag("confirmar_devolucao").performClick()
        regra.waitForIdle()

        esperarTexto("220,00")
    }
}
