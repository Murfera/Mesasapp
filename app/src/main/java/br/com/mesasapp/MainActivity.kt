package br.com.mesasapp

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import br.com.mesasapp.data.preferences.TemaApp
import br.com.mesasapp.ui.MesasappApp
import br.com.mesasapp.ui.theme.MesasappTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val container = (application as MesasappApplication).container
        // Le o tema antes do primeiro frame (arquivo pequeno e local), para o app
        // nao piscar claro e depois escurecer.
        val temaInicial = runBlocking { container.configStore.tema.first() }

        setContent {
            val tema by container.configStore.tema.collectAsState(initial = temaInicial)
            val escuro = when (tema) {
                TemaApp.SISTEMA -> isSystemInDarkTheme()
                TemaApp.CLARO -> false
                TemaApp.ESCURO -> true
            }
            // Icones da barra de status e de navegacao acompanham o tema do app,
            // nao so o do Android.
            LaunchedEffect(escuro) {
                val estilo = if (escuro) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = estilo, navigationBarStyle = estilo)
            }
            MesasappTheme(escuro = escuro) {
                MesasappApp(container)
            }
        }
    }
}
