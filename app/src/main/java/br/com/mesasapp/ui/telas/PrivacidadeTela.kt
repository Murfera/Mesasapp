package br.com.mesasapp.ui.telas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.com.mesasapp.data.preferences.ModoPrivacidade
import br.com.mesasapp.data.seguranca.Trava
import br.com.mesasapp.ui.MesasViewModel
import br.com.mesasapp.ui.comum.TemaDoFinapp

/**
 * Menu > Privacidade: a mesma janelinha do Finapp (textos, opcoes e
 * funcionamento), aberta por cima da tela onde o menu estava.
 */
@Composable
fun PrivacidadeDialogo(vm: MesasViewModel) {
    val atual by vm.privacidade.collectAsState()
    var mensagem by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    // Trocar ou desligar o bloqueio pede o bloqueio atual: quem pegou o celular
    // desbloqueado nao desfaz a protecao. Quem confere e sempre o Android.
    fun trocar(modo: ModoPrivacidade) {
        if (modo == atual) return
        val activity = Trava.acharActivity(context) ?: return
        val falta = when (modo) {
            ModoPrivacidade.BIOMETRIA -> !Trava.temDigital(context)
            ModoPrivacidade.DISPOSITIVO -> !Trava.temSenhaDoCelular(context)
            ModoPrivacidade.NENHUM -> false
        }
        if (falta) {
            mensagem = if (modo == ModoPrivacidade.BIOMETRIA) {
                "Este celular não tem nenhuma digital cadastrada. " +
                    "Cadastre uma nas configurações do Android e tente de novo."
            } else {
                "Este celular não tem senha, PIN ou padrão de desbloqueio. " +
                    "Configure um nas configurações do Android e tente de novo."
            }
            return
        }
        Trava.autenticar(activity, atual, titulo = "Confirme sua identidade") {
            // O novo jeito tambem e testado antes de valer, para ninguem se trancar fora.
            Trava.autenticar(activity, modo, titulo = "Confirme para ativar") {
                vm.definirPrivacidade(modo)
                vm.privacidadeAberta = false
            }
        }
    }

    TemaDoFinapp(barras = false) {
        AlertDialog(
            onDismissRequest = { vm.privacidadeAberta = false },
            title = { Text("Privacidade") },
            text = {
                Column {
                    Text(
                        text = "Pedir identificação ao abrir o app. Só uma forma vale por vez.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Opcao(
                        rotulo = "Sem senha",
                        selecionada = atual == ModoPrivacidade.NENHUM,
                        aoEscolher = { trocar(ModoPrivacidade.NENHUM) }
                    )
                    Opcao(
                        rotulo = "Senha do celular",
                        selecionada = atual == ModoPrivacidade.DISPOSITIVO,
                        aoEscolher = { trocar(ModoPrivacidade.DISPOSITIVO) }
                    )
                    Opcao(
                        rotulo = "Digital",
                        selecionada = atual == ModoPrivacidade.BIOMETRIA,
                        aoEscolher = { trocar(ModoPrivacidade.BIOMETRIA) }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.privacidadeAberta = false }) { Text("Fechar") }
            }
        )

        mensagem?.let { texto ->
            AlertDialog(
                onDismissRequest = { mensagem = null },
                title = { Text("Não disponível") },
                text = { Text(texto) },
                confirmButton = {
                    TextButton(onClick = { mensagem = null }) { Text("Entendi") }
                }
            )
        }
    }
}

@Composable
private fun Opcao(rotulo: String, selecionada: Boolean, aoEscolher: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = aoEscolher),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selecionada, onClick = aoEscolher)
        Text(text = rotulo, style = MaterialTheme.typography.bodyLarge)
    }
}
