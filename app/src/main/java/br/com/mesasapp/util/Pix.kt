package br.com.mesasapp.util

import java.math.BigDecimal
import java.text.Normalizer

/**
 * Monta o "copia e cola" do Pix (BR Code estatico, padrao EMV do Banco Central)
 * sem rede e sem conta de API: tudo acontece aqui dentro.
 *
 * Limite a saber: QR estatico **nao avisa quando o cliente paga**. A baixa e
 * manual. Confirmacao automatica exigiria conta PJ com API Pix de banco.
 */
object Pix {

    private const val GUI = "br.gov.bcb.pix"

    /**
     * Campo no formato EMV: id + tamanho com dois digitos + valor.
     * O tamanho e em caracteres, por isso nada de acento ou emoji no conteudo.
     */
    private fun campo(id: String, valor: String): String =
        id + valor.length.toString().padStart(2, '0') + valor

    /**
     * Texto do Pix copia e cola.
     *
     * [chave] e a chave Pix (CPF, CNPJ, telefone, e-mail ou aleatoria), como ela
     * foi cadastrada no banco. [valorCentavos] zero gera um QR sem valor, que
     * deixa o cliente digitar quanto vai pagar.
     */
    fun copiaECola(
        chave: String,
        nomeRecebedor: String,
        cidade: String,
        valorCentavos: Long = 0,
        identificador: String = "***"
    ): String {
        require(chave.isNotBlank()) { "Chave Pix vazia" }

        val conta = campo("00", GUI) + campo("01", chave.trim())

        val corpo = buildString {
            append(campo("00", "01"))
            append(campo("26", conta))
            append(campo("52", "0000"))
            append(campo("53", "986"))
            if (valorCentavos > 0) {
                append(campo("54", BigDecimal(valorCentavos).movePointLeft(2).toPlainString()))
            }
            append(campo("58", "BR"))
            append(campo("59", limpar(nomeRecebedor, 25)))
            append(campo("60", limpar(cidade, 15)))
            append(campo("62", campo("05", limpar(identificador, 25).ifBlank { "***" })))
        }

        val semCrc = corpo + "6304"
        return semCrc + crc16(semCrc)
    }

    /**
     * Tira acento, deixa em maiusculas e corta no limite do campo. O padrao so
     * aceita ASCII; "Jose Antonio" com acento quebraria a leitura no banco.
     */
    internal fun limpar(texto: String, limite: Int): String =
        Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")
            .filter { it.code in 32..126 }
            .uppercase()
            .take(limite)
            .trim()

    /**
     * CRC-16/CCITT-FALSE, exigido pelo padrao: polinomio 0x1021, inicio 0xFFFF,
     * resultado em quatro digitos hexadecimais maiusculos.
     */
    internal fun crc16(texto: String): String {
        var crc = 0xFFFF
        texto.toByteArray(Charsets.UTF_8).forEach { byte ->
            crc = crc xor ((byte.toInt() and 0xFF) shl 8)
            repeat(8) {
                crc = if (crc and 0x8000 != 0) (crc shl 1) xor 0x1021 else crc shl 1
                crc = crc and 0xFFFF
            }
        }
        return crc.toString(16).uppercase().padStart(4, '0')
    }
}
