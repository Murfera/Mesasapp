package br.com.mesasapp.di

import android.content.Context
import br.com.mesasapp.data.local.MesasappDatabase
import br.com.mesasapp.data.preferences.ConfigStore
import br.com.mesasapp.data.repository.AluguelRepository

/**
 * Injecao de dependencia manual: um container criado na Application e lido pelas
 * telas. Sem Hilt, para nao carregar mais uma camada de build (mesma decisao do Finapp).
 */
class AppContainer(context: Context) {

    private val database = MesasappDatabase.obter(context)

    val repository = AluguelRepository(
        itemDao = database.itemDao(),
        comboDao = database.comboDao(),
        clienteDao = database.clienteDao(),
        locacaoDao = database.locacaoDao()
    )

    val configStore = ConfigStore(context.applicationContext)
}
