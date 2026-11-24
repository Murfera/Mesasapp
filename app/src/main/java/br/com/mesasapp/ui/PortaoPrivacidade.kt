package br.com.mesasapp.ui

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import br.com.mesasapp.data.preferences.ModoPrivacidade
import br.com.mesasapp.data.seguranca.Trava
import br.com.mesasapp.ui.comum.TemaDoFinapp

/** Tempo fora do app que ainda nao trava de novo (o mesmo do Finapp). */
private const val TRAVAR_DEPOIS_MS = 30_000L

/**
 * Fica entre o app e a pessoa, com o mesmo funcionamento do Finapp: com a trava
 * ligada (menu > Privacidade), pede a identificacao ao abrir o app e ao voltar
 * depois de 30 s fora. Ate passar, mostra a tela "Bloqueado"; os pedidos nem
 * chegam a ser montados.
 *
 * [aoDesligar] grava "Sem senha" quando o jeito escolhido sumiu do aparelho.
 */
@Composable
fun PortaoPrivacidade(
    modo: ModoPrivacidade,
    aoDesligar: () -> Unit,
    conteudo: @Composable () -> Unit
) {
    val context = LocalContext.current
    val modoAtual by rememberUpdatedState(modo)

    // So o abrir do app (e a volta depois de 30 s) tranca. Ligar a trava pelo
    // menu nao tranca na hora: quem ligou acabou de se identificar duas vezes.
    var travado by remember { mutableStateOf(modo != ModoPrivacidade.NENHUM) }
    if (modo == ModoPrivacidade.NENHUM && travado) travado = false

    fun desbloquear() {
        val activity = Trava.acharActivity(context) ?: run { travado = false; return }
        // O jeito escolhido pode ter desaparecido depois (digital apagada, senha do
        // celular removida). Sem nenhuma forma de provar quem e, os dados ficariam
        // inacessiveis para sempre: ai o bloqueio se desliga.
        val usavel = when (modoAtual) {
            ModoPrivacidade.BIOMETRIA -> when {
                Trava.temDigital(context) -> ModoPrivacidade.BIOMETRIA
                Trava.temSenhaDoCelular(context) -> ModoPrivacidade.DISPOSITIVO
                else -> ModoPrivacidade.NENHUM
            }
            ModoPrivacidade.DISPOSITIVO ->
                if (Trava.temSenhaDoCelular(context)) ModoPrivacidade.DISPOSITIVO else ModoPrivacidade.NENHUM
            ModoPrivacidade.NENHUM -> ModoPrivacidade.NENHUM
        }
        if (usavel == ModoPrivacidade.NENHUM) {
            aoDesligar()
            travado = false
            return
        }
        Trava.autenticar(activity, usavel, titulo = "Desbloquear o Mesasapp") { travado = false }
    }

    // Pequena tolerancia: escolher a pasta do backup sai do app e nao deve travar.
    val dono = LocalLifecycleOwner.current
    var saiuEm by remember { mutableStateOf(0L) }
    DisposableEffect(dono) {
        val observador = LifecycleEventObserver { _, evento ->
            when (evento) {
                Lifecycle.Event.ON_STOP -> saiuEm = SystemClock.elapsedRealtime()
                Lifecycle.Event.ON_START -> {
                    val fora = SystemClock.elapsedRealtime() - saiuEm
                    if (saiuEm != 0L && modoAtual != ModoPrivacidade.NENHUM && !travado &&
                        fora > TRAVAR_DEPOIS_MS
                    ) {
                        travado = true
                        desbloquear()
                    }
                }
                else -> Unit
            }
        }
        dono.lifecycle.addObserver(observador)
        onDispose { dono.lifecycle.removeObserver(observador) }
    }

    // Ao abrir o app ja pede, como o Finapp.
    LaunchedEffect(Unit) { if (travado) desbloquear() }

    if (travado) TelaBloqueada { desbloquear() } else conteudo()
}

/** A mesma tela do Finapp: nome do app, "Bloqueado" e o botao. */
@Composable
private fun TelaBloqueada(aoDesbloquear: () -> Unit) {
    TemaDoFinapp {
        Surface(modifier = Modifier.fillMaxSize().testTag("tela_travada")) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Mesasapp", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "Bloqueado",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                )
                Button(onClick = aoDesbloquear, modifier = Modifier.testTag("botao_desbloquear")) {
                    Text("Desbloquear")
                }
            }
        }
    }
}
