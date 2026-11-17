package br.com.mesasapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val Azul40 = Color(0xFF1D4E89)
val Azul80 = Color(0xFFA9C7F5)
val Ambar40 = Color(0xFF8A5A00)
val Ambar80 = Color(0xFFF2C14E)
val Verde40 = Color(0xFF2E6B3E)
val Verde80 = Color(0xFF9BD5A8)

private val CoresClaras = lightColorScheme(primary = Azul40, secondary = Verde40, tertiary = Ambar40)
private val CoresEscuras = darkColorScheme(primary = Azul80, secondary = Verde80, tertiary = Ambar80)

/**
 * Cores de estado (disponivel / indisponivel) expostas pelo tema, para nao
 * ficarem fixas em cada tela. Os tons claros e escuros sao diferentes porque o
 * mesmo verde nao tem contraste nos dois fundos.
 */
data class CoresEstado(val ok: Color, val alerta: Color, val erro: Color)

private val LocalCoresEstado = staticCompositionLocalOf {
    CoresEstado(Color(0xFF1B7A3D), Color(0xFF9A6400), Color(0xFFB3261E))
}

object MesasappTheme {
    val estado: CoresEstado
        @Composable @ReadOnlyComposable
        get() = LocalCoresEstado.current
}

@Composable
fun MesasappTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> CoresEscuras
        else -> CoresClaras
    }

    val estado = if (darkTheme) {
        CoresEstado(ok = Color(0xFF7ED39A), alerta = Color(0xFFF2C14E), erro = Color(0xFFF2B8B5))
    } else {
        CoresEstado(ok = Color(0xFF1B7A3D), alerta = Color(0xFF9A6400), erro = Color(0xFFB3261E))
    }

    CompositionLocalProvider(LocalCoresEstado provides estado) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
