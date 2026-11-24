package br.com.mesasapp.ui.telas

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import br.com.mesasapp.data.backup.AutoBackupScheduler
import br.com.mesasapp.ui.MesasViewModel
import br.com.mesasapp.ui.comum.Aviso
import br.com.mesasapp.ui.comum.Botao
import br.com.mesasapp.ui.comum.Cartao
import br.com.mesasapp.ui.comum.Divisor
import br.com.mesasapp.ui.comum.EstiloBotao
import br.com.mesasapp.ui.comum.LinhaValor
import br.com.mesasapp.ui.comum.Nota
import br.com.mesasapp.ui.comum.T
import br.com.mesasapp.ui.comum.Titulo
import br.com.mesasapp.ui.comum.Voltar
import br.com.mesasapp.ui.theme.Cores
import br.com.mesasapp.ui.theme.SpaceGrotesk
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

private val PT_BR = Locale.forLanguageTag("pt-BR")
private val MOMENTO = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH'h'mm", PT_BR)

/** Uma semana sem backup indica que o agendamento parou de rodar. */
private fun atrasado(ultimo: Long?): Boolean {
    val quando = ultimo ?: return false
    return System.currentTimeMillis() - quando > TimeUnit.DAYS.toMillis(7)
}

/**
 * Menu > Backup: salvar e restaurar o arquivo na hora, e o backup diario
 * automatico numa pasta do aparelho.
 *
 * O backup leva pedidos, estoque, precos e os dados do Pix — tudo o que o app
 * guarda. Nada sai do aparelho: quem escolhe o destino e o usuario, no seletor
 * do sistema.
 */
@Composable
fun BackupTela(vm: MesasViewModel) {
    val automatico by vm.backupAutomatico.collectAsState()
    val pasta by vm.backupPasta.collectAsState()
    val ultimo by vm.backupUltimoEm.collectAsState()
    val context = LocalContext.current
    val escopo = rememberCoroutineScope()

    // ACTION_CREATE_DOCUMENT: quem escolhe o destino e o usuario, no seletor do
    // sistema — Google Drive, Downloads, pendrive. O app nao precisa de nenhuma
    // permissao de armazenamento para isso.
    val salvarArquivo = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        escopo.launch {
            val ok = try {
                val json = vm.montarBackup()
                context.contentResolver.openOutputStream(uri)?.use { saida ->
                    saida.write(json.toByteArray())
                } != null
            } catch (e: Exception) {
                false
            }
            vm.backupSalvo(ok)
        }
    }

    val abrirArquivo = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        escopo.launch {
            vm.prepararRestauracao(
                try {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                } catch (e: Exception) {
                    null
                }
            )
        }
    }

    // Escolher a pasta uma vez e guardar a permissao: o backup automatico nao
    // pode parar para perguntar onde gravar no meio da madrugada.
    val escolherPasta = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        // Sem pasta nao ha onde gravar: a opcao continua desligada.
        if (uri != null) vm.pastaEscolhida(uri)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Voltar("Voltar") { vm.voltarDoMenu() }
        Titulo("Backup")

        vm.msg?.let { Aviso(it.texto, it.ok) }

        Cartao {
            T("Backup agora", 15.sp, 600)
            T(
                "O arquivo leva pedidos, estoque, preços e os dados do Pix. " +
                    "Você escolhe onde salvar: o aparelho, um pendrive ou a nuvem.",
                13.sp, 400, Cores.Suave
            )
            Botao(
                "Salvar arquivo de backup",
                { salvarArquivo.launch(vm.nomeSugeridoDoBackup()) },
                Modifier.fillMaxWidth().testTag("botao_salvar_backup")
            )
            Divisor()
            T("Restaurar", 15.sp, 600)
            T(
                "Substitui os pedidos, o estoque e os preços atuais pelo conteúdo " +
                    "do arquivo. O app confere o arquivo antes de trocar qualquer coisa.",
                13.sp, 400, Cores.Suave
            )
            Botao(
                "Restaurar de um arquivo",
                { abrirArquivo.launch(arrayOf("application/json")) },
                Modifier.fillMaxWidth().testTag("botao_restaurar_backup"),
                estilo = EstiloBotao.NEUTRO
            )
        }

        Cartao(espaco = 0.dp) {
            Chave(
                "Backup automático local",
                "Uma cópia por dia na pasta que você escolher, no próprio aparelho. " +
                    "Roda quando o celular estiver carregando e mantém os " +
                    "${AutoBackupScheduler.MANTER_ARQUIVOS} arquivos mais recentes.",
                automatico,
                "chave_backup_auto"
            ) { ligar ->
                if (ligar) escolherPasta.launch(null) else vm.desligarBackupAutomatico()
            }

            if (automatico) {
                Divisor()
                Column(
                    Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LinhaValor("Pasta", nomeLegivel(pasta), pesoValor = 600)
                    LinhaValor(
                        "Último backup",
                        ultimo?.let {
                            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(MOMENTO)
                        } ?: "ainda não houve",
                        pesoValor = 600
                    )

                    // Sem este aviso o usuario confiaria num backup que parou de
                    // rodar sem avisar — pior do que nao ter backup nenhum.
                    if (atrasado(ultimo)) {
                        Nota(
                            "O último backup tem mais de uma semana. Isso costuma ser a " +
                                "economia de bateria do aparelho segurando tarefas em " +
                                "segundo plano. Em Ajustes → Bateria, deixe o Mesasapp " +
                                "sem restrição."
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Botao(
                            "Fazer backup agora",
                            { vm.backupAgoraNaPasta() },
                            Modifier.weight(1f).testTag("botao_backup_pasta"),
                            estilo = EstiloBotao.SUAVE
                        )
                        Botao(
                            "Trocar pasta",
                            { escolherPasta.launch(null) },
                            Modifier.weight(1f),
                            estilo = EstiloBotao.NEUTRO
                        )
                    }
                }
            }
        }
    }

    vm.restauracaoPendente?.let {
        ConfirmarRestauracao(
            aoConfirmar = { vm.confirmarRestauracao() },
            aoCancelar = { vm.cancelarRestauracao() }
        )
    }
}

/**
 * Confirmacao antes de substituir tudo.
 *
 * Restaurar apaga os pedidos atuais e nao da para desfazer, entao um toque
 * errado no menu nao pode bastar.
 */
@Composable
private fun ConfirmarRestauracao(aoConfirmar: () -> Unit, aoCancelar: () -> Unit) {
    Dialog(onDismissRequest = aoCancelar) {
        Cartao(padding = PaddingValues(20.dp)) {
            T("Restaurar backup?", 17.sp, 600, fonte = SpaceGrotesk)
            T(
                "Os pedidos, o estoque e os preços atuais serão substituídos pelo " +
                    "conteúdo do arquivo. Isso não pode ser desfeito.",
                13.sp, 400, Cores.Texto2
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Botao(
                    "Cancelar", aoCancelar, Modifier.weight(1f),
                    estilo = EstiloBotao.NEUTRO
                )
                Botao(
                    "Restaurar", aoConfirmar,
                    Modifier.weight(1f).testTag("botao_confirmar_restauracao")
                )
            }
        }
    }
}

/** O caminho tecnico do seletor nao diz nada: mostra so a ultima parte. */
private fun nomeLegivel(uri: String?): String =
    uri?.let { Uri.decode(it).substringAfterLast(':').ifBlank { "pasta escolhida" } } ?: "—"

@Composable
private fun Chave(
    titulo: String,
    descricao: String,
    ligado: Boolean,
    tag: String,
    aoMudar: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Switch) { aoMudar(!ligado) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            T(titulo, 15.sp, 600)
            T(descricao, 12.sp, 400, Cores.Suave, Modifier.padding(top = 2.dp))
        }
        Switch(
            checked = ligado, onCheckedChange = aoMudar, modifier = Modifier.testTag(tag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Cores.SobreVerde, checkedTrackColor = Cores.Verde,
                uncheckedThumbColor = Cores.Suave, uncheckedTrackColor = Cores.Abas,
                uncheckedBorderColor = Cores.BordaCampo
            )
        )
    }
}
