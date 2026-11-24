package br.com.mesasapp

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import br.com.mesasapp.data.preferences.TemaApp
import androidx.fragment.app.FragmentActivity
import br.com.mesasapp.ui.MesasappApp
import br.com.mesasapp.ui.PortaoPrivacidade
import br.com.mesasapp.ui.theme.MesasappTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * FragmentActivity, e nao ComponentActivity, porque o BiometricPrompt da trava
 * (menu > Privacidade) exige: ele se apoia em fragmentos para sobreviver a
 * rotacao enquanto o dialogo do sistema esta aberto.
 */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val container = (application as MesasappApplication).container
        // Le o tema antes do primeiro frame (arquivo pequeno e local), para o app
        // nao piscar claro e depois escurecer.
        // A trava vem na mesma leitura: decidir depois do primeiro frame
        // mostraria os pedidos por um instante antes de pedir o desbloqueio.
        val (temaInicial, modoInicial) = runBlocking {
            container.configStore.tema.first() to container.configStore.privacidade.first()
        }

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
                val modo by container.configStore.privacidade.collectAsState(initial = modoInicial)
                PortaoPrivacidade(modo) { MesasappApp(container) }
            }
        }
    }
}
