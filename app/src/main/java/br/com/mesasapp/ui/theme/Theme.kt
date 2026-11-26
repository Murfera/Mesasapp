package br.com.mesasapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import br.com.mesasapp.R

/**
 * Paleta do app. A clara e a do prototipo (handoff/prototype/Main.dc.html); a
 * escura e derivada dela e entra sozinha quando o Android esta no modo escuro.
 *
 * [verde] e o fundo dos botoes cheios; [destaque] e o verde usado em texto,
 * icone e borda. No claro sao a mesma cor; no escuro o texto precisa de um verde
 * mais claro para ter contraste com o fundo escuro.
 */
data class Paleta(
    val fundo: Color,
    val superficie: Color,
    val sobreVerde: Color,
    val texto: Color,
    val texto2: Color,
    val suave: Color,
    val borda: Color,
    val bordaCampo: Color,
    val linha: Color,
    val tracejado: Color,
    val bolinha: Color,
    val placeholder: Color,
    val verde: Color,
    val destaque: Color,
    val verdeEscuro: Color,
    val verdeClaro: Color,
    val verdeTexto: Color,
    val verdeBorda: Color,
    val cinza: Color,
    val abas: Color,
    val laranja: Color,
    val laranjaClaro: Color,
    /** Borda e texto de campo obrigatorio nao preenchido. */
    val erro: Color,
    val azul: Color,
    val azulClaro: Color,
    val pontoEntrega: Color,
    val pontoRetirada: Color,
    /** Orcamento: texto/etiqueta [amarelo], fundo da etiqueta [amareloClaro], base do cartao [amareloVivo]. */
    val amarelo: Color,
    val amareloClaro: Color,
    val amareloVivo: Color,
    val cinzaStatus: Color,
    val cinzaStatusTexto: Color,
    val qrFundo: Color,
    /** Fundo do Estoque conforme a aba: cinza claro no plastico, azul claro no metal (o usuario pediu invertido). */
    val fundoPlastico: Color,
    val fundoMetal: Color,
    /** Tom mais forte do alto da tela (o degrade desce ate fundoPlastico/fundoMetal). */
    val fundoPlasticoTopo: Color,
    val fundoMetalTopo: Color,
    /** Cartoes do Estoque: "gelo", quase branco (acinzentado no plastico, azulado no metal). */
    val geloPlastico: Color,
    val geloMetal: Color,
    /** Ponta clara do degrade verde (a outra ponta e [verde]). */
    val verdeGradiente: Color
)

val PaletaClara = Paleta(
    fundo = Color(0xFFF6F7F5),
    superficie = Color(0xFFFFFFFF),
    sobreVerde = Color(0xFFFFFFFF),
    texto = Color(0xFF16201E),
    texto2 = Color(0xFF3D4744),
    suave = Color(0xFF5B6763),
    borda = Color(0xFFE2E6E3),
    bordaCampo = Color(0xFFC5CDC9),
    linha = Color(0xFFEEF1EF),
    tracejado = Color(0xFFD5DBD8),
    bolinha = Color(0xFFA9B3AE),
    placeholder = Color(0xFFA9B3AE),
    verde = Color(0xFF0F6B5C),
    destaque = Color(0xFF0F6B5C),
    verdeEscuro = Color(0xFF0B5A4D),
    verdeClaro = Color(0xFFE3F1EE),
    verdeTexto = Color(0xFFCFE7E1),
    verdeBorda = Color(0xFF5FA597),
    cinza = Color(0xFFF1F4F2),
    abas = Color(0xFFE6EAE8),
    laranja = Color(0xFF9A3412),
    laranjaClaro = Color(0xFFFDEDE3),
    erro = Color(0xFFD32F2F),
    azul = Color(0xFF1E40AF),
    azulClaro = Color(0xFFE7EEFD),
    pontoEntrega = Color(0xFF1D4ED8),
    pontoRetirada = Color(0xFFC2410C),
    amarelo = Color(0xFF854D0E),
    amareloClaro = Color(0xFFFEF3C7),
    amareloVivo = Color(0xFFEAB308),
    cinzaStatus = Color(0xFFEEF0EF),
    cinzaStatusTexto = Color(0xFF47514E),
    qrFundo = Color(0xFFFBFCFB),
    fundoPlastico = Color(0xFFE6E8EA),
    fundoMetal = Color(0xFFE3EEFA),
    fundoPlasticoTopo = Color(0xFFD2D6DA),
    fundoMetalTopo = Color(0xFFC9DDF5),
    geloPlastico = Color(0xFFF5F6F7),
    geloMetal = Color(0xFFF4F8FD),
    verdeGradiente = Color(0xFF1C8F77)
)

val PaletaEscura = Paleta(
    fundo = Color(0xFF0F1513),
    superficie = Color(0xFF1A2220),
    sobreVerde = Color(0xFFFFFFFF),
    texto = Color(0xFFE6ECE9),
    texto2 = Color(0xFFC3CCC8),
    suave = Color(0xFF9AA6A1),
    borda = Color(0xFF2C3633),
    bordaCampo = Color(0xFF4A5652),
    linha = Color(0xFF26302D),
    tracejado = Color(0xFF3A4541),
    // Borda mais clara: a bolinha da cor Preta precisa aparecer no fundo escuro.
    bolinha = Color(0xFF9AA6A1),
    placeholder = Color(0xFF6B7773),
    verde = Color(0xFF1F7D6C),
    destaque = Color(0xFF5CC2AE),
    verdeEscuro = Color(0xFF8ED8C6),
    verdeClaro = Color(0xFF183A33),
    verdeTexto = Color(0xFFCFE7E1),
    verdeBorda = Color(0xFF6FB8A9),
    cinza = Color(0xFF222A28),
    abas = Color(0xFF222A28),
    laranja = Color(0xFFF2A57E),
    laranjaClaro = Color(0xFF3A2218),
    erro = Color(0xFFFF8A80),
    azul = Color(0xFF9DB6F5),
    azulClaro = Color(0xFF1C2A4D),
    pontoEntrega = Color(0xFF6B8EF0),
    pontoRetirada = Color(0xFFF07A45),
    amarelo = Color(0xFFF2CF6B),
    amareloClaro = Color(0xFF3A3013),
    amareloVivo = Color(0xFFF2CF6B),
    cinzaStatus = Color(0xFF2A302E),
    cinzaStatusTexto = Color(0xFFB9C2BE),
    qrFundo = Color(0xFF1F2826),
    fundoPlastico = Color(0xFF1A1D20),
    fundoMetal = Color(0xFF121D2A),
    fundoPlasticoTopo = Color(0xFF272B2F),
    fundoMetalTopo = Color(0xFF1A2D45),
    geloPlastico = Color(0xFF202427),
    geloMetal = Color(0xFF1A2430),
    verdeGradiente = Color(0xFF2EA88F)
)

private val LocalPaleta = staticCompositionLocalOf { PaletaClara }

/** Atalho para a paleta do tema atual: `Cores.Texto`, `Cores.Superficie`... */
object Cores {
    private val p: Paleta @Composable @ReadOnlyComposable get() = LocalPaleta.current

    val Fundo @Composable @ReadOnlyComposable get() = p.fundo
    val Superficie @Composable @ReadOnlyComposable get() = p.superficie
    val SobreVerde @Composable @ReadOnlyComposable get() = p.sobreVerde
    val Texto @Composable @ReadOnlyComposable get() = p.texto
    val Texto2 @Composable @ReadOnlyComposable get() = p.texto2
    val Suave @Composable @ReadOnlyComposable get() = p.suave
    val Borda @Composable @ReadOnlyComposable get() = p.borda
    val BordaCampo @Composable @ReadOnlyComposable get() = p.bordaCampo
    val Linha @Composable @ReadOnlyComposable get() = p.linha
    val Tracejado @Composable @ReadOnlyComposable get() = p.tracejado
    val Bolinha @Composable @ReadOnlyComposable get() = p.bolinha
    val Placeholder @Composable @ReadOnlyComposable get() = p.placeholder
    val Verde @Composable @ReadOnlyComposable get() = p.verde
    val Destaque @Composable @ReadOnlyComposable get() = p.destaque
    val VerdeEscuro @Composable @ReadOnlyComposable get() = p.verdeEscuro
    val VerdeClaro @Composable @ReadOnlyComposable get() = p.verdeClaro
    val VerdeTexto @Composable @ReadOnlyComposable get() = p.verdeTexto
    val VerdeBorda @Composable @ReadOnlyComposable get() = p.verdeBorda
    val Cinza @Composable @ReadOnlyComposable get() = p.cinza
    val Abas @Composable @ReadOnlyComposable get() = p.abas
    val Laranja @Composable @ReadOnlyComposable get() = p.laranja
    val LaranjaClaro @Composable @ReadOnlyComposable get() = p.laranjaClaro
    val Erro @Composable @ReadOnlyComposable get() = p.erro
    val Azul @Composable @ReadOnlyComposable get() = p.azul
    val AzulClaro @Composable @ReadOnlyComposable get() = p.azulClaro
    val PontoEntrega @Composable @ReadOnlyComposable get() = p.pontoEntrega
    val PontoRetirada @Composable @ReadOnlyComposable get() = p.pontoRetirada
    val Amarelo @Composable @ReadOnlyComposable get() = p.amarelo
    val AmareloClaro @Composable @ReadOnlyComposable get() = p.amareloClaro
    val AmareloVivo @Composable @ReadOnlyComposable get() = p.amareloVivo
    val CinzaStatus @Composable @ReadOnlyComposable get() = p.cinzaStatus
    val CinzaStatusTexto @Composable @ReadOnlyComposable get() = p.cinzaStatusTexto
    val QrFundo @Composable @ReadOnlyComposable get() = p.qrFundo
    val FundoPlastico @Composable @ReadOnlyComposable get() = p.fundoPlastico
    val FundoMetal @Composable @ReadOnlyComposable get() = p.fundoMetal
    val FundoPlasticoTopo @Composable @ReadOnlyComposable get() = p.fundoPlasticoTopo
    val FundoMetalTopo @Composable @ReadOnlyComposable get() = p.fundoMetalTopo
    val GeloPlastico @Composable @ReadOnlyComposable get() = p.geloPlastico
    val GeloMetal @Composable @ReadOnlyComposable get() = p.geloMetal

    /** Degrade do verde, da esquerda (escuro) para a direita (claro): botoes e opcoes marcadas. */
    val GradienteVerde: Brush @Composable @ReadOnlyComposable get() = Brush.horizontalGradient(listOf(p.verde, p.verdeGradiente))

    /** Fundo de tela do alto (mais forte) ate embaixo (mais claro), na mesma cor. */
    fun gradienteFundo(topo: Color, base: Color): Brush = Brush.verticalGradient(listOf(topo, base))
}

private fun variavel(res: Int, peso: Int) =
    Font(res, FontWeight(peso), variationSettings = FontVariation.Settings(FontVariation.weight(peso)))

val Inter = FontFamily(
    variavel(R.font.inter, 400), variavel(R.font.inter, 500), variavel(R.font.inter, 600), variavel(R.font.inter, 700)
)

val SpaceGrotesk = FontFamily(
    variavel(R.font.space_grotesk, 500), variavel(R.font.space_grotesk, 600), variavel(R.font.space_grotesk, 700)
)

private fun esquema(p: Paleta, escuro: Boolean) = if (escuro) {
    darkColorScheme(
        primary = p.destaque, onPrimary = Color(0xFF00382F), background = p.fundo, onBackground = p.texto,
        surface = p.superficie, onSurface = p.texto, onSurfaceVariant = p.texto2,
        surfaceContainerHigh = p.superficie, surfaceContainerHighest = p.superficie, outline = p.bordaCampo
    )
} else {
    lightColorScheme(
        primary = p.verde, onPrimary = p.sobreVerde, background = p.fundo, onBackground = p.texto,
        surface = p.superficie, onSurface = p.texto, onSurfaceVariant = p.texto2,
        surfaceContainerHigh = p.superficie, surfaceContainerHighest = p.superficie, outline = p.bordaCampo
    )
}

private fun tipografia(cor: Color): Typography {
    val base = TextStyle(fontFamily = Inter, color = cor)
    return Typography(
        bodyLarge = base, bodyMedium = base, bodySmall = base,
        labelLarge = base, labelMedium = base, labelSmall = base,
        titleLarge = base, titleMedium = base, titleSmall = base,
        headlineLarge = base, headlineMedium = base, headlineSmall = base
    )
}

/** Segue o modo claro/escuro do Android (Configuracoes > Tela > Tema escuro). */
@Composable
fun MesasappTheme(escuro: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val paleta = if (escuro) PaletaEscura else PaletaClara
    CompositionLocalProvider(LocalPaleta provides paleta) {
        MaterialTheme(colorScheme = esquema(paleta, escuro), typography = tipografia(paleta.texto), content = content)
    }
}
