package br.com.mesasapp.di

import android.content.Context
import br.com.mesasapp.data.backup.BackupRepository
import br.com.mesasapp.data.local.EstoqueEntity
import br.com.mesasapp.data.local.LinhaEntity
import br.com.mesasapp.data.local.MesasappDatabase
import br.com.mesasapp.data.local.PecaEntity
import br.com.mesasapp.data.local.toEntity
import br.com.mesasapp.data.local.toModel
import br.com.mesasapp.data.preferences.ConfigStore
import br.com.mesasapp.domain.Pedido
import br.com.mesasapp.domain.StatusPedido
import br.com.mesasapp.util.Datas
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Acesso ao banco: estoque e pedidos. */
class Repositorio(context: Context) {
    private val dao = MesasappDatabase.obter(context).dao()

    fun estoque(): Flow<Map<String, Int>> = dao.observarEstoque().map { l -> l.associate { it.modeloId to it.total } }

    suspend fun definirTotal(modeloId: String, total: Int) = dao.gravarEstoque(EstoqueEntity(modeloId, total))

    fun pedidos(): Flow<List<Pedido>> = dao.observarPedidos().map { l -> l.map { it.toModel() } }

    suspend fun salvarPedido(p: Pedido): Long = dao.salvarPedido(
        p.toEntity(Datas.agoraEpochSeg()),
        p.linhas.map { LinhaEntity(0, 0, it.tipo, it.mesaId, it.cadId, it.qtd, it.cadeirasPorMesa, it.precoUnitCentavos) },
        p.pecas.map { PecaEntity(0, 0, it.modeloId, it.quantidade, it.reposicaoCentavos, it.quebradas, it.faltando) }
    )

    suspend fun mudarStatus(id: Long, status: StatusPedido) = dao.mudarStatus(id, status)

    suspend fun registrarDevolucao(id: Long, quebradas: Map<String, Int>, faltando: Map<String, Int>, atraso: Int, finalCentavos: Long) =
        dao.registrarDevolucao(id, quebradas, faltando, atraso, finalCentavos)
}

/**
 * Injecao de dependencia manual: um container criado na Application e lido pelas
 * telas. Sem Hilt, para nao carregar mais uma camada de build (mesma decisao do Finapp).
 */
class AppContainer(context: Context) {
    /** Context da Application: usado pelo backup (seletor de pasta, WorkManager). */
    val appContext: Context = context.applicationContext

    val repositorio = Repositorio(appContext)
    val configStore = ConfigStore(appContext)

    /** O banco e singleton: obter de novo aqui devolve a mesma instancia. */
    val backupRepository = BackupRepository(
        context = appContext,
        dao = MesasappDatabase.obter(appContext).dao(),
        config = configStore
    )
}
