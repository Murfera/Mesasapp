package br.com.mesasapp.ui.telas

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import br.com.mesasapp.data.preferences.ModoPrivacidade
import br.com.mesasapp.data.seguranca.Trava
import br.com.mesasapp.ui.MesasViewModel
import br.com.mesasapp.ui.comum.Aviso
import br.com.mesasapp.ui.comum.Cartao
import br.com.mesasapp.ui.comum.Divisor
import br.com.mesasapp.ui.comum.Nota
import br.com.mesasapp.ui.comum.T
import br.com.mesasapp.ui.comum.Titulo
import br.com.mesasapp.ui.comum.Voltar
import br.com.mesasapp.ui.theme.Cores

private fun descricao(modo: ModoPrivacidade): String = when (modo) {
    ModoPrivacidade.NENHUM -> "O app abre direto, sem perguntar nada."
    ModoPrivacidade.DISPOSITIVO -> "Pede a senha, o PIN ou o padrão que destrava o celular."
    ModoPrivacidade.BIOMETRIA -> "Pede a digital. A senha do celular continua servindo, " +
        "para o caso de o leitor não reconhecer o dedo."
}

/**
 * Menu > Privacidade: se o app pede desbloqueio ao abrir, e de que jeito.
 *
 * Quem confere a senha e a digital e o Android — o app so recebe sim ou nao e
 * nunca ve credencial nenhuma.
 */
@Composable
fun PrivacidadeTela(vm: MesasViewModel) {
    val atual by vm.privacidade.collectAsState()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Voltar("Voltar") { vm.voltarDoMenu() }
        Titulo("Privacidade")

        vm.msg?.let { Aviso(it.texto, it.ok) }

        Cartao(espaco = 0.dp) {
            T("Bloqueio ao abrir o app", 15.sp, 600)
            T(
                "Com o bloqueio ligado, os pedidos, os valores e os dados dos " +
                    "clientes só aparecem depois que o Android confirmar que é você.",
                13.sp, 400, Cores.Suave, Modifier.padding(top = 4.dp, bottom = 4.dp)
            )

            ModoPrivacidade.entries.forEachIndexed { indice, modo ->
                if (indice > 0) Divisor()
                val motivo = Trava.indisponivelPorque(context, modo)
                Opcao(
                    rotulo = modo.rotulo,
                    descricao = motivo ?: descricao(modo),
                    selecionada = modo == atual,
                    disponivel = motivo == null,
                    tag = "privacidade_${modo.name}"
                ) {
                    escolher(vm, activity, modo, motivo)
                }
            }
        }

        // Prometer mais do que a trava entrega seria pior do que nao ter trava:
        // o usuario deixaria de tomar outros cuidados confiando nela.
        Cartao {
            T("O que esse bloqueio protege", 15.sp, 600)
            T(
                "Protege a tela: quem pegar o celular destravado não abre o app e " +
                    "não vê seus pedidos. Não é criptografia do arquivo — os dados " +
                    "ficam na área privada do app, que o Android já isola, mas " +
                    "continuam legíveis para quem tenha o aparelho e ferramentas " +
                    "de desenvolvedor. Serve para o celular esquecido na mesa.",
                13.sp, 400, Cores.Suave
            )
            T(
                "O arquivo de backup também não é protegido por aqui. Ele sai em " +
                    "texto, então escolha com cuidado onde salvar.",
                13.sp, 400, Cores.Suave
            )

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                Nota(
                    "Neste Android (versão 10 ou anterior), o sistema não permite " +
                        "pedir apenas a senha do celular: as duas opções acabam " +
                        "aceitando digital ou senha."
                )
            }
        }
    }
}

/**
 * Ligar a trava exige passar por ela na hora.
 *
 * Gravar o modo primeiro e descobrir depois que o leitor nao funciona deixaria o
 * dono fora dos proprios pedidos no proximo abrir.
 */
private fun escolher(
    vm: MesasViewModel,
    activity: FragmentActivity?,
    modo: ModoPrivacidade,
    motivo: String?
) {
    when {
        motivo != null -> vm.avisar(false, motivo)

        modo == ModoPrivacidade.NENHUM -> vm.definirPrivacidade(modo)

        activity == null -> vm.avisar(false, "Não foi possível abrir o desbloqueio do sistema.")

        else -> Trava.pedir(
            activity = activity,
            modo = modo,
            titulo = "Confirme para ligar o bloqueio",
            subtitulo = "Assim o app garante que você consegue entrar depois",
            aoLiberar = { vm.definirPrivacidade(modo) },
            aoFalhar = { vm.avisar(false, "Bloqueio não alterado: $it") }
        )
    }
}

@Composable
private fun Opcao(
    rotulo: String,
    descricao: String,
    selecionada: Boolean,
    disponivel: Boolean,
    tag: String,
    aoTocar: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.RadioButton, onClick = aoTocar)
            .padding(vertical = 12.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            Modifier.size(22.dp).clip(CircleShape)
                .background(if (selecionada) Cores.Verde else Cores.Abas),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Filled.Check, null,
                tint = if (selecionada) Cores.SobreVerde else Color.Transparent,
                modifier = Modifier.size(14.dp)
            )
        }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            T(rotulo, 15.sp, if (selecionada) 600 else 500, if (disponivel) Cores.Texto else Cores.Suave)
            T(descricao, 12.sp, 400, Cores.Suave)
        }
    }
}
