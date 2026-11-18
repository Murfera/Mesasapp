package br.com.mesasapp.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "config")

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

    suspend fun definirFiltro(f: FiltroMaterial) = context.dataStore.edit { it[FILTRO] = f.name }
}
