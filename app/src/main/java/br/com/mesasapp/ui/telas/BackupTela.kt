package br.com.mesasapp.ui.telas

import android.app.Activity
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.mesasapp.BuildConfig
import br.com.mesasapp.data.backup.AutoBackupScheduler
import br.com.mesasapp.ui.MesasViewModel
import br.com.mesasapp.ui.comum.TemaDoFinapp
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

private val PT_BR = Locale.forLanguageTag("pt-BR")
private val MOMENTO = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH'h'mm", PT_BR)

/**
 * Menu > Backup. Copia da tela do Finapp (mesmo layout, textos e botoes): em
 * cima o backup manual, embaixo o automatico. O botao "Salvar na pasta agora"
 * grava direto na pasta escolhida; o manual abre o seletor do sistema.
 */
@Composable
fun BackupTela(vm: MesasViewModel) {
    TemaDoFinapp { ConteudoBackup(vm) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConteudoBackup(vm: MesasViewModel) {
    val automatico by vm.backupAutomatico.collectAsState()
    val pasta by vm.backupPasta.collectAsState()
    val ultimo by vm.backupUltimoEm.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val escopo = rememberCoroutineScope()
    val context = LocalContext.current
    var confirmarRestauracao by remember { mutableStateOf<String?>(null) }

    // Escolher a pasta uma vez e guardar a permissao: o backup automatico nao
    // pode parar para perguntar onde gravar no meio da madrugada.
    val escolherPasta = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        // Sem pasta nao ha onde gravar: a opcao continua desligada.
        if (uri == null) return@rememberLauncherForActivityResult
        escopo.launch { snackbar.showSnackbar(vm.pastaEscolhida(uri)) }
    }

    // ACTION_CREATE_DOCUMENT: quem escolhe o destino e o usuario, no seletor do
    // sistema — Google Drive, Downloads, pendrive. O app nao precisa de nenhuma
    // permissao de armazenamento para isso.
    val salvarBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        escopo.launch {
            val mensagem = try {
                val json = vm.montarBackup()
                context.contentResolver.openOutputStream(uri)?.use { saida ->
                    saida.write(json.toByteArray())
                } ?: error("sem stream")
                "Backup salvo"
            } catch (e: Exception) {
                "Não foi possível salvar o backup."
            }
            snackbar.showSnackbar(mensagem)
        }
    }

    val abrirBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        escopo.launch {
            confirmarRestauracao = try {
                context.contentResolver.openInputStream(uri)?.use { entrada ->
                    entrada.readBytes().decodeToString()
                }
            } catch (e: Exception) {
                null
            }
            if (confirmarRestauracao == null) {
                snackbar.showSnackbar("Não foi possível abrir o arquivo.")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Backup") },
                navigationIcon = {
                    IconButton(onClick = { vm.voltarDoMenu() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "Backup manual",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Você escolhe onde salvar ou de onde ler: Google Drive, " +
                    "Downloads ou um pendrive.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { salvarBackup.launch(vm.nomeSugeridoDoBackup()) },
                    modifier = Modifier.weight(1f)
                ) { Text("Fazer backup") }
                Spacer(Modifier.width(12.dp))
                OutlinedButton(
                    onClick = { abrirBackup.launch(arrayOf("application/json")) },
                    modifier = Modifier.weight(1f)
                ) { Text("Restaurar backup") }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            Text(
                text = "Backup automático local",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Salvar uma cópia por dia",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "O arquivo fica na pasta que você escolher, no " +
                            "próprio aparelho. Roda quando o celular estiver " +
                            "carregando e mantém os ${AutoBackupScheduler.MANTER_ARQUIVOS} mais recentes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = automatico,
                    onCheckedChange = { ligar ->
                        if (ligar) escolherPasta.launch(null) else vm.desligarBackupAutomatico()
                    }
                )
            }

            if (automatico) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                Text(text = "Pasta", style = MaterialTheme.typography.labelLarge)
                Text(
                    text = pasta?.let { nomeLegivel(it) } ?: "—",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { escolherPasta.launch(null) }) {
                    Text("Trocar pasta")
                }

                Spacer(Modifier.height(16.dp))

                Text(text = "Último backup", style = MaterialTheme.typography.labelLarge)
                Text(
                    text = ultimo?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(MOMENTO)
                    } ?: "ainda não houve",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Sem este aviso o usuario confiaria num backup que parou de
                // rodar sem avisar — pior do que nao ter backup nenhum.
                if (atrasado(ultimo)) {
                    Spacer(Modifier.height(16.dp))
                    Card {
                        Row(modifier = Modifier.padding(16.dp)) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "O último backup tem mais de uma semana. " +
                                    "Isso costuma ser a economia de bateria do " +
                                    "aparelho segurando tarefas em segundo plano. " +
                                    "Em Ajustes → Bateria, deixe o Mesasapp sem " +
                                    "restrição.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                TextButton(
                    onClick = { escopo.launch { snackbar.showSnackbar(vm.backupAgora()) } }
                ) {
                    // Diferente do backup manual: grava direto na pasta escolhida.
                    Text("Salvar na pasta agora")
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Text(
                // Lido do build: digitar a versao aqui desencontraria do Play
                // na primeira atualizacao.
                text = "Mesasapp ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }

    confirmarRestauracao?.let { json ->
        AlertDialog(
            onDismissRequest = { confirmarRestauracao = null },
            title = { Text("Restaurar backup?") },
            text = {
                Text(
                    "Todos os pedidos, o estoque e os preços atuais serão " +
                        "substituídos pelo conteúdo do arquivo. Esta ação não " +
                        "pode ser desfeita."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmarRestauracao = null
                        escopo.launch { snackbar.showSnackbar(vm.restaurarBackup(json)) }
                    }
                ) { Text("Restaurar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarRestauracao = null }) { Text("Cancelar") }
            }
        )
    }
}

/** Uma semana sem backup indica que o agendamento parou de rodar. */
private fun atrasado(ultimo: Long?): Boolean {
    val quando = ultimo ?: return false
    return System.currentTimeMillis() - quando > TimeUnit.DAYS.toMillis(7)
}

/** O caminho tecnico do seletor nao diz nada: mostra so a ultima parte. */
private fun nomeLegivel(uri: String): String =
    Uri.decode(uri).substringAfterLast(':').ifBlank { "pasta escolhida" }
