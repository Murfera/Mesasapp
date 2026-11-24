package br.com.mesasapp.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import br.com.mesasapp.domain.Cor
import br.com.mesasapp.domain.FiltroMaterial
import br.com.mesasapp.domain.Material
import br.com.mesasapp.domain.PrecoPeca
import br.com.mesasapp.domain.Precos
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "config")

/** Tema do app: segue o Android, ou fica fixo no claro ou no escuro (menu > Tema). */
enum class TemaApp(val rotulo: String) {
    SISTEMA("Automático"),
    CLARO("Claro"),
    ESCURO("Escuro")
}

/**
 * Menu > Privacidade: se o app pede desbloqueio ao abrir, e de que jeito.
 *
 * De proposito fora do backup (ver restaurarConfig): restaurar num aparelho
 * sem digital cadastrada ligaria uma trava impossivel de abrir ali.
 */
enum class ModoPrivacidade(val rotulo: String) {
    NENHUM("Sem senha"),
    DISPOSITIVO("Senha do celular"),
    BIOMETRIA("Digital")
}

/** Menu > Config. pagamento: campos opcionais do Novo pedido. Desligados por padrao. */
data class OpcoesPagamento(val mostrarSinal: Boolean = false, val mostrarCaucao: Boolean = false)

data class PixConfig(val chave: String = "", val nome: String = "", val cidade: String = "")

/**
 * Configuracao que nao e pedido nem estoque: precos (menu > Precos), dados do
 * Pix (menu > Config. Pix) e o filtro de material do menu.
 */
class ConfigStore(private val context: Context) {

    private fun diaria(chave: String) = longPreferencesKey("diaria_$chave")
    private fun reposicao(chave: String) = longPreferencesKey("reposicao_$chave")
    private fun cadeiras(chave: String) = intPreferencesKey("cpm_$chave")
    private val DESC_JOGO = longPreferencesKey("desconto_jogo")
    private val FRETE = longPreferencesKey("frete_padrao_v2")
    private val PIX_CHAVE = stringPreferencesKey("chave_pix")
    private val PIX_NOME = stringPreferencesKey("nome_recebedor")
    private val PIX_CIDADE = stringPreferencesKey("cidade_recebedor")
    private val FILTRO = stringPreferencesKey("filtro_material")
    private val TEMA = stringPreferencesKey("tema")
    private val MOSTRAR_SINAL = booleanPreferencesKey("mostrar_sinal")
    private val MOSTRAR_CAUCAO = booleanPreferencesKey("mostrar_caucao")
    private val BACKUP_AUTO = booleanPreferencesKey("backup_automatico")
    private val BACKUP_PASTA = stringPreferencesKey("backup_pasta_uri")
    private val BACKUP_ULTIMO = longPreferencesKey("backup_ultimo_em")
    private val PRIVACIDADE = stringPreferencesKey("privacidade")

    val precos: Flow<Precos> = context.dataStore.data.map { p ->
        val padrao = Precos()
        Precos(
            tabela = Precos.TABELA_PADRAO.mapValues { (chave, base) ->
                PrecoPeca(p[diaria(chave)] ?: base.diariaCentavos, p[reposicao(chave)] ?: base.reposicaoCentavos)
            },
            descontoJogoCentavos = p[DESC_JOGO] ?: padrao.descontoJogoCentavos,
            fretePadraoCentavos = p[FRETE] ?: padrao.fretePadraoCentavos,
            cadeirasPorMesa = Material.entries.flatMap { m -> Cor.entries.map { c -> Precos.chaveCadeiras(m, c) } }
                .mapNotNull { k -> p[cadeiras(k)]?.let { k to it } }
                .toMap()
        )
    }

    val pix: Flow<PixConfig> = context.dataStore.data.map { p ->
        PixConfig(p[PIX_CHAVE].orEmpty(), p[PIX_NOME].orEmpty(), p[PIX_CIDADE].orEmpty())
    }

    val filtro: Flow<FiltroMaterial> = context.dataStore.data.map { p ->
        p[FILTRO]?.let { nome -> FiltroMaterial.entries.firstOrNull { it.name == nome } } ?: FiltroMaterial.TODOS
    }

    suspend fun definirDiaria(chave: String, centavos: Long) = context.dataStore.edit { it[diaria(chave)] = centavos }
    suspend fun definirReposicao(chave: String, centavos: Long) = context.dataStore.edit { it[reposicao(chave)] = centavos }
    suspend fun definirDescontoJogo(centavos: Long) = context.dataStore.edit { it[DESC_JOGO] = centavos }
    suspend fun definirFrete(centavos: Long) = context.dataStore.edit { it[FRETE] = centavos }
    suspend fun definirCadeirasPorMesa(material: Material, cor: Cor, n: Int) =
        context.dataStore.edit { it[cadeiras(Precos.chaveCadeiras(material, cor))] = n }

    suspend fun definirPix(pix: PixConfig) = context.dataStore.edit {
        it[PIX_CHAVE] = pix.chave
        it[PIX_NOME] = pix.nome
        it[PIX_CIDADE] = pix.cidade
    }

    val tema: Flow<TemaApp> = context.dataStore.data.map { p ->
        p[TEMA]?.let { nome -> TemaApp.entries.firstOrNull { it.name == nome } } ?: TemaApp.SISTEMA
    }

    suspend fun definirTema(t: TemaApp) = context.dataStore.edit { it[TEMA] = t.name }

    // --- privacidade (menu > Privacidade) ---

    val privacidade: Flow<ModoPrivacidade> = context.dataStore.data.map { p ->
        p[PRIVACIDADE]?.let { nome -> ModoPrivacidade.entries.firstOrNull { it.name == nome } }
            ?: ModoPrivacidade.NENHUM
    }

    suspend fun definirPrivacidade(m: ModoPrivacidade) =
        context.dataStore.edit { it[PRIVACIDADE] = m.name }

    val pagamento: Flow<OpcoesPagamento> = context.dataStore.data.map { p ->
        OpcoesPagamento(p[MOSTRAR_SINAL] ?: false, p[MOSTRAR_CAUCAO] ?: false)
    }

    suspend fun definirPagamento(o: OpcoesPagamento) = context.dataStore.edit {
        it[MOSTRAR_SINAL] = o.mostrarSinal
        it[MOSTRAR_CAUCAO] = o.mostrarCaucao
    }

    suspend fun definirFiltro(f: FiltroMaterial) = context.dataStore.edit { it[FILTRO] = f.name }

    // --- backup (menu > Backup) ---

    val backupAutomatico: Flow<Boolean> = context.dataStore.data.map { it[BACKUP_AUTO] ?: false }

    val backupPasta: Flow<String?> = context.dataStore.data.map { it[BACKUP_PASTA] }

    val backupUltimoEm: Flow<Long?> = context.dataStore.data.map { it[BACKUP_ULTIMO] }

    /** Leitura pontual, para quem roda fora da tela (o worker do backup diario). */
    suspend fun backupPastaUri(): String? = backupPasta.first()

    suspend fun definirBackupAutomatico(ligado: Boolean) =
        context.dataStore.edit { it[BACKUP_AUTO] = ligado }

    suspend fun definirBackupPasta(uri: String) =
        context.dataStore.edit { it[BACKUP_PASTA] = uri }

    suspend fun definirBackupUltimoEm(millis: Long) =
        context.dataStore.edit { it[BACKUP_ULTIMO] = millis }

    /** Leitura pontual dos precos, do Pix e do Exibir, para montar o backup. */
    suspend fun precosAgora(): Precos = precos.first()
    suspend fun pixAgora(): PixConfig = pix.first()
    suspend fun pagamentoAgora(): OpcoesPagamento = pagamento.first()

    /**
     * Grava de uma vez a configuracao que veio de um backup.
     *
     * Uma unica edicao, e nao uma por campo: assim as telas nao recalculam a
     * cada preco restaurado, e nao existe instante com metade da tabela nova e
     * metade da antiga.
     *
     * As chaves ausentes no arquivo sao apagadas, nao mantidas: o que vale e o
     * backup, senao um preco antigo sobreviveria a restauracao.
     */
    suspend fun restaurarConfig(p: Precos, pixConfig: PixConfig, op: OpcoesPagamento) {
        context.dataStore.edit { prefs ->
            p.tabela.forEach { (chave, preco) ->
                prefs[diaria(chave)] = preco.diariaCentavos
                prefs[reposicao(chave)] = preco.reposicaoCentavos
            }
            prefs[DESC_JOGO] = p.descontoJogoCentavos
            prefs[FRETE] = p.fretePadraoCentavos

            Material.entries.flatMap { m -> Cor.entries.map { c -> Precos.chaveCadeiras(m, c) } }
                .forEach { chave ->
                    val n = p.cadeirasPorMesa[chave]
                    if (n != null && n > 0) prefs[cadeiras(chave)] = n else prefs.remove(cadeiras(chave))
                }

            prefs[PIX_CHAVE] = pixConfig.chave
            prefs[PIX_NOME] = pixConfig.nome
            prefs[PIX_CIDADE] = pixConfig.cidade
            prefs[MOSTRAR_SINAL] = op.mostrarSinal
            prefs[MOSTRAR_CAUCAO] = op.mostrarCaucao
        }
    }
}
