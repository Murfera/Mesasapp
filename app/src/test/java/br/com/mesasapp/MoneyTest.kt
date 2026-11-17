package br.com.mesasapp

import br.com.mesasapp.util.formatAsBrl
import br.com.mesasapp.util.toCentsOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {

    @Test
    fun `valor inteiro em reais vira centavos`() {
        assertEquals(5000L, "50".toCentsOrNull())
        assertEquals(4700L, "47".toCentsOrNull())
    }

    @Test
    fun `virgula e o separador decimal e o ponto e milhar`() {
        assertEquals(123456L, "1.234,56".toCentsOrNull())
        assertEquals(4750L, "47,50".toCentsOrNull())
    }

    @Test
    fun `so ponto - uma ou duas casas e decimal, tres e milhar`() {
        assertEquals(123456L, "1234.56".toCentsOrNull())
        assertEquals(150L, "1.5".toCentsOrNull())
        assertEquals(123400L, "1.234".toCentsOrNull())
    }

    @Test
    fun `frete zero e valido`() {
        assertEquals(0L, "0".toCentsOrNull())
    }

    @Test
    fun `texto invalido devolve null`() {
        assertNull("".toCentsOrNull())
        assertNull("abc".toCentsOrNull())
        assertNull("-10".toCentsOrNull())
    }

    @Test
    fun `formata centavos como moeda`() {
        // O espaco entre o simbolo e o numero e um espaco nao separavel (NBSP).
        assertEquals("R$", 47000L.formatAsBrl().take(2))
        assertEquals("470,00", 47000L.formatAsBrl().takeLast(6))
    }
}
