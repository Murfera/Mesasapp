package br.com.mesasapp.data.backup

import br.com.mesasapp.data.local.EstoqueEntity
import br.com.mesasapp.data.local.LinhaEntity
import br.com.mesasapp.data.local.PecaEntity
import br.com.mesasapp.data.local.PedidoEntity
import br.com.mesasapp.data.preferences.OpcoesPagamento
import br.com.mesasapp.data.preferences.PixConfig
import br.com.mesasapp.domain.Catalogo
import br.com.mesasapp.domain.PrecoPeca
import br.com.mesasapp.domain.Precos
import br.com.mesasapp.domain.StatusPedido
import br.com.mesasapp.domain.TipoLinha
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Um pedido com as linhas e as pecas dele, como saem e entram no arquivo. */
data class PedidoBackup(
    val pedido: PedidoEntity,
    val linhas: List<LinhaEntity>,
    val pecas: List<PecaEntity>
)

/** Configuracao do app que fica fora do banco (DataStore). */
data class ConfigBackup(
    val precos: Precos,
    val pix: PixConfig,
    val pagamento: OpcoesPagamento
)

/** Conteudo de um backup, ja validado. */
data class BackupData(
    val estoque: List<EstoqueEntity>,
    val pedidos: List<PedidoBackup>,
    val config: ConfigBackup?
)

/**
 * Le e escreve o backup em JSON.
 *
 * JSON em vez do arquivo .db por dois motivos: o usuario consegue abrir e
 * conferir o que esta guardado, e o formato sobrevive a mudancas internas do
 * Room que quebrariam um .db copiado.
 *
 * Vai junto a configuracao (precos, Pix, campos do Novo pedido) porque ela mora
 * no DataStore, nao no banco: um backup que devolvesse os pedidos e deixasse a
 * tabela de precos no padrao nao serviria para voltar ao normal.
 *
 * O campo [VERSION] e do **formato do backup**, nao do banco. Se o formato
 * mudar, um backup antigo ainda precisa ser lido.
 */
object BackupSerializer {

    const val VERSION = 1
    private val TIMESTAMP: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun toJson(data: BackupData): String {
        val estoque = JSONArray()
        data.estoque.forEach { item ->
            estoque.put(
                JSONObject().apply {
                    put("modeloId", item.modeloId)
                    put("total", item.total)
                }
            )
        }

        val pedidos = JSONArray()
        data.pedidos.forEach { item ->
            val linhas = JSONArray()
            item.linhas.forEach { l ->
                linhas.put(
                    JSONObject().apply {
                        put("tipo", l.tipo.name)
                        put("mesaId", l.mesaId ?: JSONObject.NULL)
                        put("cadId", l.cadId ?: JSONObject.NULL)
                        put("qtd", l.qtd)
                        put("cadeirasPorMesa", l.cadeirasPorMesa)
                        put("precoUnitCentavos", l.precoUnitCentavos)
                    }
                )
            }

            val pecas = JSONArray()
            item.pecas.forEach { p ->
                pecas.put(
                    JSONObject().apply {
                        put("modeloId", p.modeloId)
                        put("quantidade", p.quantidade)
                        put("reposicaoCentavos", p.reposicaoCentavos)
                        put("quebradas", p.quebradas)
                        put("faltando", p.faltando)
                    }
                )
            }

            pedidos.put(
                JSONObject().apply {
                    with(item.pedido) {
                        put("id", id)
                        put("cliente", cliente)
                        put("telefone", telefone)
                        put("documento", documento)
                        put("endereco", endereco)
                        put("referencia", referencia)
                        put("inicioEpochSeg", inicioEpochSeg)
                        put("fimEpochSeg", fimEpochSeg)
                        put("freteCentavos", freteCentavos)
                        put("descontoCentavos", descontoCentavos)
                        put("sinalCentavos", sinalCentavos)
                        put("caucaoCentavos", caucaoCentavos)
                        put("pagamento", pagamento)
                        put("status", status.name)
                        put("atrasoDias", atrasoDias)
                        put("finalCentavos", finalCentavos ?: JSONObject.NULL)
                        put("criadoEmEpochSeg", criadoEmEpochSeg)
                    }
                    put("linhas", linhas)
                    put("pecas", pecas)
                }
            )
        }

        return JSONObject().apply {
            put("formato", VERSION)
            put("app", "Mesasapp")
            put("exportadoEm", LocalDateTime.now().format(TIMESTAMP))
            put("estoque", estoque)
            put("pedidos", pedidos)
            data.config?.let { put("configuracao", configParaJson(it)) }
        }.toString(2)
    }

    private fun configParaJson(c: ConfigBackup): JSONObject {
        val tabela = JSONObject()
        c.precos.tabela.forEach { (chave, preco) ->
            tabela.put(
                chave,
                JSONObject().apply {
                    put("diariaCentavos", preco.diariaCentavos)
                    put("reposicaoCentavos", preco.reposicaoCentavos)
                }
            )
        }

        val cadeiras = JSONObject()
        c.precos.cadeirasPorMesa.forEach { (chave, n) -> cadeiras.put(chave, n) }

        return JSONObject().apply {
            put(
                "precos",
                JSONObject().apply {
                    put("tabela", tabela)
                    put("descontoJogoCentavos", c.precos.descontoJogoCentavos)
                    put("fretePadraoCentavos", c.precos.fretePadraoCentavos)
                    put("cadeirasPorMesa", cadeiras)
                }
            )
            put(
                "pix",
                JSONObject().apply {
                    put("chave", c.pix.chave)
                    put("nome", c.pix.nome)
                    put("cidade", c.pix.cidade)
                }
            )
            put(
                "exibir",
                JSONObject().apply {
                    put("mostrarSinal", c.pagamento.mostrarSinal)
                    put("mostrarCaucao", c.pagamento.mostrarCaucao)
                    put("mostrarPeriodoPedidos", c.pagamento.mostrarPeriodoPedidos)
                    put("pedidosPorSemana", c.pagamento.pedidosPorSemana)
                    put("mostrarQrCode", c.pagamento.mostrarQrCode)
                }
            )
        }
    }

    /**
     * Converte o texto em [BackupData].
     *
     * Lanca [BackupFormatException] em qualquer coisa fora do esperado: e melhor
     * recusar um arquivo duvidoso do que substituir os pedidos do usuario por
     * dado corrompido.
     */
    fun fromJson(text: String): BackupData {
        val root = try {
            JSONObject(text)
        } catch (e: Exception) {
            throw BackupFormatException("O arquivo não é um backup do Mesasapp.")
        }

        val formato = root.optInt("formato", -1)
        if (formato !in 1..VERSION) {
            throw BackupFormatException(
                "Este backup foi feito por uma versão mais nova do app. Atualize o Mesasapp."
            )
        }

        val estoqueJson = root.optJSONArray("estoque")
            ?: throw BackupFormatException("O backup não tem estoque.")
        val pedidosJson = root.optJSONArray("pedidos")
            ?: throw BackupFormatException("O backup não tem pedidos.")

        val estoque = (0 until estoqueJson.length()).map { i ->
            val item = estoqueJson.getJSONObject(i)
            val modeloId = item.texto("modeloId")
            // Um modelo fora do catalogo nao tem onde aparecer: o numero ficaria
            // guardado e invisivel. Melhor recusar o arquivo.
            if (Catalogo.modelo(modeloId) == null) {
                throw BackupFormatException("O backup tem um modelo desconhecido: $modeloId")
            }
            EstoqueEntity(modeloId = modeloId, total = item.inteiro("total").coerceAtLeast(0))
        }

        if (estoque.distinctBy { it.modeloId }.size != estoque.size) {
            throw BackupFormatException("O backup tem o mesmo modelo repetido no estoque.")
        }

        val pedidos = (0 until pedidosJson.length()).map { i ->
            lerPedido(pedidosJson.getJSONObject(i))
        }

        if (pedidos.distinctBy { it.pedido.id }.size != pedidos.size) {
            throw BackupFormatException("O backup tem pedidos com o mesmo número.")
        }

        return BackupData(
            estoque = estoque,
            pedidos = pedidos,
            config = root.optJSONObject("configuracao")?.let { lerConfig(it) }
        )
    }

    private fun lerPedido(item: JSONObject): PedidoBackup {
        val id = item.numero("id")
        if (id <= 0) throw BackupFormatException("O backup tem um pedido sem número.")

        val pedido = PedidoEntity(
            id = id,
            cliente = item.texto("cliente"),
            telefone = item.textoOpcional("telefone"),
            documento = item.textoOpcional("documento"),
            endereco = item.textoOpcional("endereco"),
            referencia = item.textoOpcional("referencia"),
            inicioEpochSeg = item.numero("inicioEpochSeg"),
            fimEpochSeg = item.numero("fimEpochSeg"),
            freteCentavos = item.numero("freteCentavos"),
            descontoCentavos = item.numero("descontoCentavos"),
            sinalCentavos = item.numero("sinalCentavos"),
            caucaoCentavos = item.numero("caucaoCentavos"),
            pagamento = item.textoOpcional("pagamento"),
            status = enumOuFalha<StatusPedido>(item.texto("status")),
            atrasoDias = item.inteiro("atrasoDias"),
            finalCentavos = if (item.isNull("finalCentavos")) null else item.numero("finalCentavos"),
            criadoEmEpochSeg = item.numero("criadoEmEpochSeg")
        )

        val linhasJson = item.optJSONArray("linhas") ?: JSONArray()
        val linhas = (0 until linhasJson.length()).map { i ->
            val l = linhasJson.getJSONObject(i)
            // id 0: o Room gera de novo. Esses ids sao internos e nada fora da
            // propria linha aponta para eles.
            LinhaEntity(
                id = 0,
                pedidoId = id,
                tipo = enumOuFalha<TipoLinha>(l.texto("tipo")),
                mesaId = l.modeloOuNulo("mesaId"),
                cadId = l.modeloOuNulo("cadId"),
                qtd = l.inteiro("qtd"),
                cadeirasPorMesa = l.inteiro("cadeirasPorMesa"),
                precoUnitCentavos = l.numero("precoUnitCentavos")
            )
        }

        val pecasJson = item.optJSONArray("pecas") ?: JSONArray()
        val pecas = (0 until pecasJson.length()).map { i ->
            val p = pecasJson.getJSONObject(i)
            val modeloId = p.texto("modeloId")
            if (Catalogo.modelo(modeloId) == null) {
                throw BackupFormatException("O backup tem um modelo desconhecido: $modeloId")
            }
            PecaEntity(
                id = 0,
                pedidoId = id,
                modeloId = modeloId,
                quantidade = p.inteiro("quantidade"),
                reposicaoCentavos = p.numero("reposicaoCentavos"),
                quebradas = p.inteiro("quebradas"),
                faltando = p.inteiro("faltando")
            )
        }

        return PedidoBackup(pedido, linhas, pecas)
    }

    private fun lerConfig(item: JSONObject): ConfigBackup {
        val precosJson = item.optJSONObject("precos")
        val padrao = Precos()

        // Chave fora do catalogo e ignorada: ela nao tem peca para precificar, e
        // recusar o backup inteiro por causa disso seria exagero.
        val tabela = precosJson?.optJSONObject("tabela")?.let { t ->
            Precos.TABELA_PADRAO.mapValues { (chave, base) ->
                t.optJSONObject(chave)?.let { p ->
                    PrecoPeca(
                        diariaCentavos = p.numero("diariaCentavos"),
                        reposicaoCentavos = p.numero("reposicaoCentavos")
                    )
                } ?: base
            }
        } ?: Precos.TABELA_PADRAO

        val cadeiras = mutableMapOf<String, Int>()
        precosJson?.optJSONObject("cadeirasPorMesa")?.let { c ->
            c.keys().forEach { chave -> cadeiras[chave] = c.optInt(chave, 0) }
        }

        val pixJson = item.optJSONObject("pix")
        val exibirJson = item.optJSONObject("exibir")

        return ConfigBackup(
            precos = Precos(
                tabela = tabela,
                descontoJogoCentavos = precosJson
                    ?.numeroOpcional("descontoJogoCentavos", padrao.descontoJogoCentavos)
                    ?: padrao.descontoJogoCentavos,
                fretePadraoCentavos = precosJson
                    ?.numeroOpcional("fretePadraoCentavos", padrao.fretePadraoCentavos)
                    ?: padrao.fretePadraoCentavos,
                cadeirasPorMesa = cadeiras.filterValues { it > 0 }
            ),
            pix = PixConfig(
                chave = pixJson?.optString("chave").orEmpty(),
                nome = pixJson?.optString("nome").orEmpty(),
                cidade = pixJson?.optString("cidade").orEmpty()
            ),
            pagamento = OpcoesPagamento(
                mostrarSinal = exibirJson?.optBoolean("mostrarSinal", false) ?: false,
                mostrarCaucao = exibirJson?.optBoolean("mostrarCaucao", false) ?: false,
                mostrarPeriodoPedidos = exibirJson?.optBoolean("mostrarPeriodoPedidos", true) ?: true,
                pedidosPorSemana = exibirJson?.optBoolean("pedidosPorSemana", false) ?: false,
                mostrarQrCode = exibirJson?.optBoolean("mostrarQrCode", false) ?: false
            )
        )
    }

    // --- leitura com mensagem em portugues no lugar da JSONException crua ---

    private fun JSONObject.texto(campo: String): String =
        if (isNull(campo)) throw faltando(campo) else optString(campo)

    private fun JSONObject.textoOpcional(campo: String): String =
        if (isNull(campo)) "" else optString(campo)

    private fun JSONObject.numero(campo: String): Long =
        if (has(campo) && !isNull(campo)) optLong(campo) else throw faltando(campo)

    private fun JSONObject.numeroOpcional(campo: String, padrao: Long): Long =
        if (has(campo) && !isNull(campo)) optLong(campo) else padrao

    private fun JSONObject.inteiro(campo: String): Int =
        if (has(campo) && !isNull(campo)) optInt(campo) else throw faltando(campo)

    /** Modelo nulo e normal (linha de mesa nao tem cadeira), modelo invalido nao. */
    private fun JSONObject.modeloOuNulo(campo: String): String? {
        if (isNull(campo)) return null
        val id = optString(campo)
        if (Catalogo.modelo(id) == null) {
            throw BackupFormatException("O backup tem um modelo desconhecido: $id")
        }
        return id
    }

    private fun faltando(campo: String) =
        BackupFormatException("O backup está incompleto: falta \"$campo\".")

    private inline fun <reified T : Enum<T>> enumOuFalha(value: String): T =
        enumValues<T>().firstOrNull { it.name == value }
            ?: throw BackupFormatException("O backup tem um valor desconhecido: $value")
}

class BackupFormatException(message: String) : Exception(message)
