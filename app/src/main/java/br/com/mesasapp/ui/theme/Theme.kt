package br.com.mesasapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import br.com.mesasapp.R

/** Cores do prototipo (handoff/prototype/Main.dc.html). */
object Cores {
    val Fundo = Color(0xFFF6F7F5)
    val Branco = Color(0xFFFFFFFF)
    val Texto = Color(0xFF16201E)
    val Texto2 = Color(0xFF3D4744)
    val Suave = Color(0xFF5B6763)
    val Borda = Color(0xFFE2E6E3)
    val BordaCampo = Color(0xFFC5CDC9)
    val Linha = Color(0xFFEEF1EF)
    val Tracejado = Color(0xFFD5DBD8)
    val Bolinha = Color(0xFFA9B3AE)
    val Verde = Color(0xFF0F6B5C)
    val VerdeEscuro = Color(0xFF0B5A4D)
    val VerdeClaro = Color(0xFFE3F1EE)
    val VerdeTexto = Color(0xFFCFE7E1)
    val VerdeBorda = Color(0xFF5FA597)
    val Cinza = Color(0xFFF1F4F2)
    val Abas = Color(0xFFE6EAE8)
    val Laranja = Color(0xFF9A3412)
    val LaranjaClaro = Color(0xFFFDEDE3)
    val Azul = Color(0xFF1E40AF)
    val AzulClaro = Color(0xFFE7EEFD)
    val PontoEntrega = Color(0xFF1D4ED8)
    val PontoRetirada = Color(0xFFC2410C)
    val Roxo = Color(0xFF5B3C99)
    val RoxoClaro = Color(0xFFF3EEFB)
    val CinzaStatus = Color(0xFFEEF0EF)
    val CinzaStatusTexto = Color(0xFF47514E)
    val QrFundo = Color(0xFFFBFCFB)
}

private fun variavel(res: Int, peso: Int) =
    Font(res, FontWeight(peso), variationSettings = FontVariation.Settings(FontVariation.weight(peso)))

val Inter = FontFamily(
    variavel(R.font.inter, 400), variavel(R.font.inter, 500), variavel(R.font.inter, 600), variavel(R.font.inter, 700)
)

val SpaceGrotesk = FontFamily(
    variavel(R.font.space_grotesk, 500), variavel(R.font.space_grotesk, 600), variavel(R.font.space_grotesk, 700)
)

private val Esquema = lightColorScheme(
    primary = Cores.Verde,
    onPrimary = Cores.Branco,
    background = Cores.Fundo,
    onBackground = Cores.Texto,
    surface = Cores.Branco,
    onSurface = Cores.Texto,
    surfaceContainerHigh = Cores.Branco,
    surfaceContainerHighest = Cores.Branco,
    outline = Cores.BordaCampo
)

private val base = TextStyle(fontFamily = Inter, color = Cores.Texto)

private val Tipografia = Typography(
    bodyLarge = base, bodyMedium = base, bodySmall = base,
    labelLarge = base, labelMedium = base, labelSmall = base,
    titleLarge = base, titleMedium = base, titleSmall = base,
    headlineLarge = base, headlineMedium = base, headlineSmall = base
)

@Composable
fun MesasappTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Esquema, typography = Tipografia, content = content)
}
