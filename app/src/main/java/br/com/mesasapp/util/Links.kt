package br.com.mesasapp.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import br.com.mesasapp.domain.Pedido
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.net.URLEncoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Links externos do pedido. Nenhum exige login nem permissao: so abrem outro app. */
object Links {

    private val YMD = DateTimeFormatter.BASIC_ISO_DATE

    /** encodeURIComponent do JavaScript (URLEncoder troca espaco por "+"). */
    fun codificar(texto: String): String =
        URLEncoder.encode(texto, "UTF-8")
            .replace("+", "%20")
            .replace("%21", "!").replace("%27", "'").replace("%28", "(").replace("%29", ")").replace("%7E", "~")

    fun soDigitos(tel: String) = tel.filter { it.isDigit() }

    /** Evento de dia inteiro no Google Agenda, ja preenchido (mesmo texto do prototipo). */
    fun googleAgenda(p: Pedido, entrega: Boolean): String {
        val dia: LocalDate = if (entrega) p.entrega else p.retirada
        val c = p.conta
        val linhas = p.linhas.joinToString("\n") { "• " + it.titulo + ": " + it.descricao }
        val titulo = (if (entrega) "Entrega" else "Retirada") + " — " + p.cliente + " (#" + p.id + ")"
        val det = linhas + "\n\nTotal: " + c.mesas + " mesas e " + c.cadeiras + " cadeiras\nTelefone: " + p.telefone +
            (if (p.referencia.isNotBlank()) "\nReferência: " + p.referencia else "") +
            "\nSaldo a receber: " + c.saldoCentavos.formatAsBrl()
        return "https://calendar.google.com/calendar/render?action=TEMPLATE" +
            "&text=" + codificar(titulo) +
            "&dates=" + dia.format(YMD) + "/" + dia.plusDays(1).format(YMD) +
            "&details=" + codificar(det) +
            "&location=" + codificar(p.endereco)
    }

    fun whatsApp(tel: String, texto: String) = "https://wa.me/55" + soDigitos(tel) + "?text=" + codificar(texto)

    fun abrir(context: Context, url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
        }
    }

    /** Abre o discador com o numero; nao liga sozinho, entao nao precisa de permissao. */
    fun ligar(context: Context, tel: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + soDigitos(tel))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
        }
    }

    /** QR Code do Pix, correcao de erro nivel M, como no prototipo. */
    fun qr(texto: String, tamanho: Int = 512): ImageBitmap? = runCatching {
        val matriz = QRCodeWriter().encode(
            texto, BarcodeFormat.QR_CODE, tamanho, tamanho,
            mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M, EncodeHintType.MARGIN to 2)
        )
        val pixels = IntArray(matriz.width * matriz.height) { i ->
            if (matriz.get(i % matriz.width, i / matriz.width)) 0xFF111111.toInt() else 0xFFFFFFFF.toInt()
        }
        Bitmap.createBitmap(pixels, matriz.width, matriz.height, Bitmap.Config.ARGB_8888).asImageBitmap()
    }.getOrNull()
}
