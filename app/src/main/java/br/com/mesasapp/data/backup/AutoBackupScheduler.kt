package br.com.mesasapp.data.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import br.com.mesasapp.MesasappApplication
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * Backup automatico diario numa pasta escolhida pelo usuario.
 *
 * Nao existe horario exato: o Android so concede alarme exato a apps onde o
 * horario e a propria funcao (despertador, agenda), e o Play recusa o resto.
 * Entao o pedido e "uma vez por dia, com o aparelho carregando" — na pratica de
 * madrugada, sem gastar bateria do uso normal.
 */
object AutoBackupScheduler {

    private const val NOME_TAREFA = "backup-automatico"
    /** Quatro dias para tras bastam para escapar de um backup estragado (decisao dele, 2026-11-24). */
    const val MANTER_ARQUIVOS = 4

    /** Subpasta criada dentro da pasta escolhida, para nao misturar com outros arquivos (ex.: os do Finapp). */
    const val SUBPASTA = "Backup Mesasapp"
    const val PREFIXO = "mesasapp-backup-"

    private val SELO = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm")

    fun nomeDoArquivo(momento: LocalDateTime = LocalDateTime.now()): String =
        PREFIXO + momento.format(SELO) + ".json"

    fun agendar(context: Context) {
        val restricoes = Constraints.Builder()
            .setRequiresCharging(true)
            .setRequiresBatteryNotLow(true)
            .build()

        val pedido = PeriodicWorkRequestBuilder<AutoBackupWorker>(1, TimeUnit.DAYS)
            .setConstraints(restricoes)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            NOME_TAREFA,
            ExistingPeriodicWorkPolicy.UPDATE,
            pedido
        )
    }

    fun cancelar(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(NOME_TAREFA)
    }
}

class AutoBackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as MesasappApplication).container
        // Sem pasta nao ha onde gravar. Nao e erro: o usuario desligou a opcao.
        container.configStore.backupPastaUri() ?: return Result.success()

        return try {
            container.backupRepository.gravarNaPasta()
            Result.success()
        } catch (e: SecurityException) {
            // A pasta foi revogada ou apagada: insistir nao resolve, o usuario
            // precisa escolher outra. Falhar sem retentar evita loop inutil.
            Result.failure()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
