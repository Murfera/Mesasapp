package br.com.mesasapp.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import br.com.mesasapp.domain.Catalogo
import br.com.mesasapp.domain.LinhaPedido
import br.com.mesasapp.domain.TipoLinha
import java.io.File

/** "Plástico Preta", "Plástico Branca com braço": o que vai depois de "1 jogo · ". */
fun detalheLinha(l: LinhaPedido): String? {
    val modelo = (if (l.tipo == TipoLinha.CADEIRA) l.cadId else l.mesaId)?.let { Catalogo.modelo(it) } ?: return null
    val braco = (if (l.tipo == TipoLinha.MESA) null else l.cadId)?.let { Catalogo.modelo(it) }?.peca?.braco == true
    return "${modelo.material.rotulo} ${modelo.cor.rotulo}" + if (braco) " com braço" else ""
}

object Compartilhar {

    /**
     * Grava o PNG no cache do app e abre o WhatsApp com a imagem (e a [legenda], se houver).
     * Tenta ir direto para a conversa do [telefone] (extra "jid", nao oficial); se o WhatsApp
     * ignorar, ele pede para escolher o contato. Sem permissao: o arquivo sai por FileProvider.
     */
    fun imagemNoWhatsApp(ctx: Context, imagem: Bitmap, telefone: String, legenda: String = "", nomeArquivo: String = "pedido.png") {
        val pasta = File(ctx.cacheDir, "compartilhar").apply { mkdirs() }
        pasta.listFiles()?.forEach { it.delete() }
        val arquivo = File(pasta, nomeArquivo)
        arquivo.outputStream().use { imagem.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".arquivos", arquivo)
        val digitos = Links.soDigitos(telefone)

        fun intent(pacote: String?) = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            if (legenda.isNotBlank()) putExtra(Intent.EXTRA_TEXT, legenda)
            if (digitos.isNotEmpty()) putExtra("jid", "55$digitos@s.whatsapp.net")
            if (pacote != null) setPackage(pacote)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        for (pacote in listOf("com.whatsapp", "com.whatsapp.w4b")) {
            try {
                ctx.startActivity(intent(pacote)); return
            } catch (_: ActivityNotFoundException) {
            }
        }
        try {
            ctx.startActivity(Intent.createChooser(intent(null), "Enviar").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
        }
    }

    /** Poe a imagem sobre um fundo liso com margem (o cartao tem cantos arredondados). */
    fun comMargem(img: Bitmap, fundoArgb: Int, margem: Int = 32): Bitmap {
        val soft = if (img.config == Bitmap.Config.HARDWARE) img.copy(Bitmap.Config.ARGB_8888, false) else img
        val out = Bitmap.createBitmap(soft.width + 2 * margem, soft.height + 2 * margem, Bitmap.Config.ARGB_8888)
        val cv = android.graphics.Canvas(out)
        cv.drawColor(fundoArgb)
        cv.drawBitmap(soft, margem.toFloat(), margem.toFloat(), null)
        return out
    }
}
