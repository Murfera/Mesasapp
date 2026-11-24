package br.com.mesasapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import br.com.mesasapp.data.preferences.ModoPrivacidade
import br.com.mesasapp.data.seguranca.Trava
import br.com.mesasapp.ui.comum.Botao
import br.com.mesasapp.ui.comum.EstiloBotao
import br.com.mesasapp.ui.comum.Nota
import br.com.mesasapp.ui.comum.T
import br.com.mesasapp.ui.comum.Titulo
import br.com.mesasapp.ui.theme.Cores

/**
 * Fica entre o app e a pessoa: com a trava ligada (menu > Privacidade), nada do
 * conteudo e montado antes do Android confirmar quem esta ali.
 *
 * O conteudo nao e apenas coberto, e sim nao composto: um pedido nao chega a
 * virar pixel sem o desbloqueio, nem por um frame.
 */
@Composable
fun PortaoPrivacidade(modo: ModoPrivacidade, conteudo: @Composable () -> Unit) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    // Se a trava esta ligada mas o aparelho perdeu a senha depois disso, nao ha
    // como autenticar: insistir trancaria o dono fora dos proprios pedidos para
    // sempre. Nesse caso o app abre, e a tela de Privacidade explica o porque.
    val possivel = activity != null &&
        modo != ModoPrivacidade.NENHUM &&
        Trava.telaDeBloqueioConfigurada(context)

    var liberado by remember(modo) { mutableStateOf(!possivel) }
    var erro by remember(modo) { mutableStateOf<String?>(null) }

    // Sair do app tranca de novo: deixar destravado para sempre depois do
    // primeiro desbloqueio do dia nao travaria nada.
    val dono = LocalLifecycleOwner.current
    DisposableEffect(dono, possivel) {
        val observador = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_STOP && possivel) {
                liberado = false
                erro = null
            }
        }
        dono.lifecycle.addObserver(observador)
        onDispose { dono.lifecycle.removeObserver(observador) }
    }

    fun pedirDesbloqueio() {
        val act = activity ?: return
        erro = null
        Trava.pedir(
            activity = act,
            modo = modo,
            titulo = "Mesasapp",
            subtitulo = "Desbloqueie para ver os pedidos",
            aoLiberar = { liberado = true },
            aoFalhar = { erro = it }
        )
    }

    // Pede sozinho ao abrir: obrigar um toque a mais antes do que o Android ja
    // vai perguntar so acrescenta atrito. O botao existe para a segunda vez.
    LaunchedEffect(liberado, possivel) {
        if (!liberado && possivel) pedirDesbloqueio()
    }

    if (liberado) conteudo() else TelaTravada(erro) { pedirDesbloqueio() }
}

@Composable
private fun TelaTravada(erro: String?, aoDesbloquear: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Cores.Fundo).padding(32.dp).testTag("tela_travada"),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            Modifier.size(72.dp).clip(CircleShape).background(Cores.VerdeClaro),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Outlined.Lock, null, tint = Cores.VerdeEscuro, modifier = Modifier.size(32.dp))
        }

        Titulo("Mesasapp bloqueado")
        T(
            "Confirme que é você para ver os pedidos.",
            14.sp, 400, Cores.Suave, alinhar = TextAlign.Center
        )

        if (erro != null) Nota(erro)

        Botao(
            "Desbloquear",
            aoDesbloquear,
            Modifier.fillMaxWidth().testTag("botao_desbloquear"),
            estilo = EstiloBotao.PRIMARIO
        )
    }
}
