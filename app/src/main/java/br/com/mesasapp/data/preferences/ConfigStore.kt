package br.com.mesasapp.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "config")

/**
 * Configuracao do negocio: dados da empresa, chave Pix e os padroes que a tela
 * de orcamento usa para nao perguntar o que quase nunca muda (hora de entrega,
 * hora de retirada, frete).
 */
data class Config(
    val nomeEmpresa: String = "",
    val chavePix: String = "",
    val nomeRecebedor: String = "",
    val cidadeRecebedor: String = "",
    val fretePadraoCentavos: Long = 0,
    val horaEntregaPadrao: Int = 8,
    val horaRetiradaPadrao: Int = 18
) {
    val pixConfigurado: Boolean
        get() = chavePix.isNotBlank() && nomeRecebedor.isNotBlank() && cidadeRecebedor.isNotBlank()
}

class ConfigStore(private val context: Context) {

    private object Chaves {
        val NOME_EMPRESA = stringPreferencesKey("nome_empresa")
        val CHAVE_PIX = stringPreferencesKey("chave_pix")
        val NOME_RECEBEDOR = stringPreferencesKey("nome_recebedor")
        val CIDADE_RECEBEDOR = stringPreferencesKey("cidade_recebedor")
        val FRETE_PADRAO = longPreferencesKey("frete_padrao")
        val HORA_ENTREGA = intPreferencesKey("hora_entrega")
        val HORA_RETIRADA = intPreferencesKey("hora_retirada")
    }

    val config: Flow<Config> = context.dataStore.data.map { prefs ->
        Config(
            nomeEmpresa = prefs[Chaves.NOME_EMPRESA].orEmpty(),
            chavePix = prefs[Chaves.CHAVE_PIX].orEmpty(),
            nomeRecebedor = prefs[Chaves.NOME_RECEBEDOR].orEmpty(),
            cidadeRecebedor = prefs[Chaves.CIDADE_RECEBEDOR].orEmpty(),
            fretePadraoCentavos = prefs[Chaves.FRETE_PADRAO] ?: 0L,
            horaEntregaPadrao = prefs[Chaves.HORA_ENTREGA] ?: 8,
            horaRetiradaPadrao = prefs[Chaves.HORA_RETIRADA] ?: 18
        )
    }

    suspend fun salvar(config: Config) {
        context.dataStore.edit { prefs ->
            prefs[Chaves.NOME_EMPRESA] = config.nomeEmpresa
            prefs[Chaves.CHAVE_PIX] = config.chavePix
            prefs[Chaves.NOME_RECEBEDOR] = config.nomeRecebedor
            prefs[Chaves.CIDADE_RECEBEDOR] = config.cidadeRecebedor
            prefs[Chaves.FRETE_PADRAO] = config.fretePadraoCentavos
            prefs[Chaves.HORA_ENTREGA] = config.horaEntregaPadrao
            prefs[Chaves.HORA_RETIRADA] = config.horaRetiradaPadrao
        }
    }
}
