package br.com.mesasapp.data.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import br.com.mesasapp.data.local.MesasDao
import br.com.mesasapp.data.preferences.ConfigStore

/** Resultado de uma restauracao bem sucedida, para informar o que entrou. */
data class ResumoRestauracao(
    val pedidos: Int,
    val modelos: Int,
    val config: Boolean
)

/**
 * Monta e aplica o backup: banco (estoque e pedidos) mais a configuracao do
 * DataStore (precos, Pix, campos do Novo pedido).
 *
 * Tambem grava na pasta escolhida pelo usuario, porque dois lugares precisam
 * disso com as mesmas regras de nome e rodizio: a tela de Backup e o worker
 * diario.
 */
class BackupRepository(
    private val context: Context,
    private val dao: MesasDao,
    private val config: ConfigStore
) {

    suspend fun exportarJson(): String = BackupSerializer.toJson(
        BackupData(
            estoque = dao.todoEstoque(),
            pedidos = dao.todosPedidos().map { p ->
                PedidoBackup(pedido = p.pedido, linhas = p.linhas, pecas = p.pecas)
            },
            config = ConfigBackup(
                precos = config.precosAgora(),
                pix = config.pixAgora(),
                pagamento = config.pagamentoAgora()
            )
        )
    )

    /**
     * Substitui todo o conteudo pelo do backup.
     *
     * O arquivo e validado inteiro **antes** de qualquer escrita: um arquivo
     * truncado ou de outro app nao chega a apagar nada.
     */
    suspend fun restaurarDeJson(texto: String): ResumoRestauracao {
        val data = BackupSerializer.fromJson(texto)

        dao.restaurar(
            estoque = data.estoque,
            pedidos = data.pedidos.map { it.pedido },
            linhas = data.pedidos.flatMap { it.linhas },
            pecas = data.pedidos.flatMap { it.pecas }
        )

        data.config?.let { config.restaurarConfig(it.precos, it.pix, it.pagamento) }

        return ResumoRestauracao(
            pedidos = data.pedidos.size,
            modelos = data.estoque.size,
            config = data.config != null
        )
    }

    /**
     * Grava um backup na pasta escolhida e faz o rodizio.
     *
     * Lanca em qualquer falha; quem chamou decide o que dizer ao usuario.
     */
    suspend fun gravarNaPasta() {
        val pastaUri = config.backupPastaUri() ?: throw IllegalStateException("pasta não escolhida")
        val json = exportarJson()

        val pasta = DocumentFile.fromTreeUri(context, Uri.parse(pastaUri))
            ?: throw IllegalStateException("pasta inacessível")
        if (!pasta.canWrite()) throw IllegalStateException("sem acesso de escrita à pasta")

        val arquivo = pasta.createFile("application/json", AutoBackupScheduler.nomeDoArquivo())
            ?: throw IllegalStateException("não foi possível criar o arquivo")

        context.contentResolver.openOutputStream(arquivo.uri)?.use { saida ->
            saida.write(json.toByteArray())
        } ?: throw IllegalStateException("sem stream de escrita")

        rodizio(pasta)
        config.definirBackupUltimoEm(System.currentTimeMillis())
    }

    /**
     * Mantem apenas os [AutoBackupScheduler.MANTER_ARQUIVOS] mais recentes.
     *
     * Rodizio em vez de sobrescrever um arquivo unico: se um backup sair
     * corrompido e for o unico, nao sobra para onde voltar.
     *
     * A ordem vem do nome, que comeca com a data no formato ano-mes-dia: ordenar
     * texto equivale a ordenar data.
     */
    private fun rodizio(pasta: DocumentFile) {
        pasta.listFiles()
            .filter { it.name?.startsWith(AutoBackupScheduler.PREFIXO) == true }
            .sortedByDescending { it.name }
            .drop(AutoBackupScheduler.MANTER_ARQUIVOS)
            .forEach { it.delete() }
    }
}
