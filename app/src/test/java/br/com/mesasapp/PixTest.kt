package br.com.mesasapp

import br.com.mesasapp.util.Pix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PixTest {

    @Test
    fun `crc16 bate com o vetor conhecido do padrao`() {
        // CRC-16/CCITT-FALSE de "123456789" e 0x29B1: e o teste classico do algoritmo.
        assertEquals("29B1", Pix.crc16("123456789"))
    }

    @Test
    fun `o payload termina com o crc calculado sobre o resto`() {
        val payload = Pix.copiaECola(
            chave = "11999998888",
            nomeRecebedor = "Mesas e Cadeiras LTDA",
            cidade = "Sao Paulo",
            valorCentavos = 47000
        )

        val semCrc = payload.dropLast(4)
        assertEquals(Pix.crc16(semCrc), payload.takeLast(4))
        assertTrue(semCrc.endsWith("6304"))
    }

    @Test
    fun `o valor entra com duas casas e ponto decimal`() {
        val payload = Pix.copiaECola("chave", "Fulano", "Campinas", valorCentavos = 47000)
        // 54 = valor, 06 = seis caracteres, 470.00
        assertTrue(payload.contains("5406470.00"))
    }

    @Test
    fun `sem valor o campo 54 nao aparece`() {
        val payload = Pix.copiaECola("chave", "Fulano", "Campinas", valorCentavos = 0)
        // Sem valor, a moeda (5303986) emenda direto no pais (5802BR).
        assertTrue(payload.contains("53039865802BR"))
    }

    @Test
    fun `nome e cidade perdem acento e viram maiusculas`() {
        assertEquals("JOSE ANTONIO", Pix.limpar("José Antônio", 25))
        assertEquals("SAO PAULO", Pix.limpar("São Paulo", 15))
    }

    @Test
    fun `nome longo e cortado no limite do campo`() {
        // O padrao permite 25 caracteres no nome do recebedor.
        assertEquals(25, Pix.limpar("Locadora de Mesas e Cadeiras do Centro", 25).length)
    }

    @Test
    fun `o cabecalho e o identificador padrao estao no payload`() {
        val payload = Pix.copiaECola("minhachave", "Fulano", "Campinas")
        assertTrue(payload.startsWith("000201"))
        assertTrue(payload.contains("br.gov.bcb.pix"))
        assertTrue(payload.contains("62070503***"))
    }
}
