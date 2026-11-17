package br.com.mesasapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.com.mesasapp.ui.MesasappApp
import br.com.mesasapp.ui.theme.MesasappTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val container = (application as MesasappApplication).container

        setContent {
            MesasappTheme {
                MesasappApp(container)
            }
        }
    }
}
