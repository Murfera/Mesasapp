package br.com.mesasapp.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

private val BRL: NumberFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))

private val PLAIN: NumberFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR")).apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}

/** Formata centavos como moeda: 123456 -> "R$ 1.234,56". */
fun Long.formatAsBrl(): String = BRL.format(BigDecimal(this).movePointLeft(2))

/** Formata centavos sem o simbolo: 123456 -> "1.234,56". */
fun Long.formatAsPlain(): String = PLAIN.format(BigDecimal(this).movePointLeft(2))

/**
 * Converte o texto digitado em centavos.
 *
 * Regras de separador, nesta ordem:
 *  - Virgula presente: a virgula e o decimal e os pontos sao milhar ("1.234,56" -> 123456).
 *  - Somente pontos: o ultimo ponto e decimal se tiver 1 ou 2 digitos depois
 *    ("1234.56" -> 123456, "1.5" -> 150); com 3 digitos e milhar ("1.234" -> 123400).
 *  - Sem separador: valor inteiro em reais ("50" -> 5000).
 *
 * Aceita zero (frete pode ser zero). Retorna null para texto vazio, invalido ou negativo.
 */
fun String.toCentsOrNull(): Long? {
    val raw = trim()
        .replace("R$", "")
        .replace(" ", "")
        .replace(" ", "")

    if (raw.isEmpty()) return null
    if (raw.any { !it.isDigit() && it != ',' && it != '.' }) return null

    val normalized = when {
        raw.contains(',') -> raw.replace(".", "").replace(',', '.')

        raw.contains('.') -> {
            val digitsAfterLastDot = raw.length - raw.lastIndexOf('.') - 1
            if (raw.count { it == '.' } == 1 && digitsAfterLastDot in 1..2) raw else raw.replace(".", "")
        }

        else -> raw
    }

    val value = normalized.toBigDecimalOrNull() ?: return null
    if (value.signum() < 0) return null

    return value.movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong()
}

/** Maior valor que cabe num campo de dinheiro: R$ 9.999.999,99. */
private const val DIGITOS_DINHEIRO = 9

/**
 * Mascara de moeda para campo digitado, como nos apps de banco: so digitos, que
 * enchem da direita para a esquerda ("1" -> "0,01", "1500" -> "15,00",
 * "150000" -> "1.500,00"). Sempre virgula e 2 casas; zero vira vazio.
 */
fun mascaraDinheiro(texto: String): String {
    val digitos = texto.filter { it.isDigit() }.trimStart('0').take(DIGITOS_DINHEIRO)
    val centavos = digitos.toLongOrNull() ?: 0L
    return if (centavos == 0L) "" else centavos.formatAsPlain()
}
