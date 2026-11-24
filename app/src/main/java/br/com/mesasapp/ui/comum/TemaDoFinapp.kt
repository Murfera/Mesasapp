package br.com.mesasapp.ui.comum

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Cores e letras iguais as do Finapp, para as telas copiadas de la (Backup,
 * Privacidade, tela de bloqueio): claro ou escuro **segue o celular** (como o
 * Finapp, e nao o Tema do menu), cores do sistema tiradas do papel de parede
 * (Android 12+) e a tipografia padrao do Material.
 *
 * [barras] ajusta os icones da barra de status para o fundo desta tela; uma
 * janelinha (dialogo) por cima de outra tela passa false, para nao mexer neles.
 */
@Composable
fun TemaDoFinapp(barras: Boolean = true, conteudo: @Composable () -> Unit) {
    val escuro = isSystemInDarkTheme()
    val context = LocalContext.current
    val cores = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (escuro) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        escuro -> darkColorScheme()
        else -> lightColorScheme()
    }

    // Icones da barra de status legiveis sobre o fundo desta tela; ao sair,
    // volta como o resto do app deixou.
    if (barras) {
        val view = LocalView.current
        DisposableEffect(escuro) {
            val janela = (view.context as? Activity)?.window
            val controle = janela?.let { WindowCompat.getInsetsController(it, view) }
            val antesStatus = controle?.isAppearanceLightStatusBars
            val antesNavegacao = controle?.isAppearanceLightNavigationBars
            controle?.isAppearanceLightStatusBars = !escuro
            controle?.isAppearanceLightNavigationBars = !escuro
            onDispose {
                if (antesStatus != null) controle.isAppearanceLightStatusBars = antesStatus
                if (antesNavegacao != null) controle.isAppearanceLightNavigationBars = antesNavegacao
            }
        }
    }

    val tipografia = Typography()
    MaterialTheme(colorScheme = cores, typography = tipografia) {
        // O tema do Mesasapp deixa uma cor de texto fixa (a do Claro/Escuro
        // escolhido no menu) no estilo padrao. Sem zerar aqui, titulos e botoes
        // saiam escuros sobre o fundo escuro.
        CompositionLocalProvider(LocalTextStyle provides tipografia.bodyLarge, content = conteudo)
    }
}
