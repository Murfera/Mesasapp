package br.com.mesasapp

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Com o Android no modo escuro, todas as abas e o menu abrem sem quebrar. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-night-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TemaEscuroTest {

    @get:Rule
    val regra = createAndroidComposeRule<MainActivity>()

    @Test
    fun abas_abrem_no_modo_escuro() {
        listOf("Início", "Pedidos", "Novo", "Agenda", "Estoque").forEach { aba ->
            regra.waitUntil(10_000) { regra.onAllNodes(hasTestTag("aba_$aba")).fetchSemanticsNodes().isNotEmpty() }
            regra.onNodeWithTag("aba_$aba").performClick()
            regra.waitForIdle()
        }
        regra.onNodeWithTag("botao_menu").performClick()
        regra.waitForIdle()
        regra.onNodeWithTag("menu_precos").performClick()
        regra.waitForIdle()
        regra.waitUntil(10_000) { regra.onAllNodes(hasTestTag("lista_precos")).fetchSemanticsNodes().isNotEmpty() }
    }
}
