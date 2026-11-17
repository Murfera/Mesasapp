package br.com.mesasapp

import android.app.Application
import br.com.mesasapp.di.AppContainer
import br.com.mesasapp.domain.model.Categoria
import br.com.mesasapp.domain.model.Combo
import br.com.mesasapp.domain.model.Item
import br.com.mesasapp.domain.model.Material
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MesasappApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        escopo.launch { semearCatalogo() }
    }

    /**
     * Primeira abertura: cria mesa, cadeira e o jogo padrao, com quantidade e
     * preco zerados para a pessoa ajustar no Estoque. Catalogo vazio deixaria a
     * tela principal sem nada para somar, que e o contrario da ideia do app.
     */
    private suspend fun semearCatalogo() {
        val repo = container.repository
        if (!repo.catalogoVazio()) return

        val mesaId = repo.salvarItem(
            Item(
                nome = "Mesa plastica",
                categoria = Categoria.MESA,
                material = Material.PLASTICO,
                cor = "Branca",
                quantidadeTotal = 0
            )
        )
        val cadeiraId = repo.salvarItem(
            Item(
                nome = "Cadeira plastica sem braco",
                categoria = Categoria.CADEIRA,
                material = Material.PLASTICO,
                comBraco = false,
                cor = "Branca",
                quantidadeTotal = 0
            )
        )
        repo.salvarCombo(
            Combo(
                nome = "Jogo (1 mesa + 4 cadeiras)",
                itemMesaId = mesaId,
                itemCadeiraId = cadeiraId,
                qtdCadeiras = 4
            )
        )
    }
}
