package br.com.mesasapp.domain

/**
 * O estoque tem **15 modelos fixos**: material x peca x cor. A pessoa so informa
 * quantas pecas tem de cada um. Ids iguais aos do prototipo
 * (`<p|m>-<mesa|cadeira|cadeiraBraco>-<pr|br|vm>`), para os dados baterem.
 */
enum class Material(val rotulo: String, val prefixo: String) {
    PLASTICO("Plástico", "p"),
    METAL("Metal", "m")
}

enum class Cor(val rotulo: String, val sufixo: String, val argb: Long) {
    PRETA("Preta", "pr", 0xFF1C1C1C),
    BRANCA("Branca", "br", 0xFFFFFFFF),
    VERMELHA("Vermelha", "vm", 0xFFB42318)
}

/** Mesa, cadeira ou cadeira com braco. Cadeira com braco so existe em plastico. */
enum class Peca(val chave: String, val ehMesa: Boolean, val braco: Boolean, val rotulo: String, val materiais: List<Material>) {
    MESA("mesa", true, false, "Mesas", listOf(Material.PLASTICO, Material.METAL)),
    CADEIRA("cadeira", false, false, "Cadeiras", listOf(Material.PLASTICO, Material.METAL)),
    CADEIRA_BRACO("cadeiraBraco", false, true, "Cadeiras com braço", listOf(Material.PLASTICO))
}

data class Modelo(val material: Material, val peca: Peca, val cor: Cor) {
    val id: String get() = "${material.prefixo}-${peca.chave}-${cor.sufixo}"

    /** Chave da tabela de precos: o preco vale para todas as cores. */
    val chavePreco: String get() = "${material.prefixo}-${peca.chave}"

    val ehMesa: Boolean get() = peca.ehMesa

    /** "Mesa de plástico branca", "Cadeira de metal preta", "Cadeira de plástico com braço vermelha". */
    val nome: String
        get() {
            val mat = material.rotulo.lowercase()
            val cor = cor.rotulo.lowercase()
            return when (peca) {
                Peca.MESA -> "Mesa de $mat $cor"
                Peca.CADEIRA -> "Cadeira de $mat $cor"
                Peca.CADEIRA_BRACO -> "Cadeira de $mat com braço $cor"
            }
        }
}

object Catalogo {
    val modelos: List<Modelo> = Material.entries.flatMap { mat ->
        Peca.entries.filter { mat in it.materiais }.flatMap { peca ->
            Cor.entries.map { cor -> Modelo(mat, peca, cor) }
        }
    }

    private val porId = modelos.associateBy { it.id }

    fun modelo(id: String): Modelo? = porId[id]

    /** Ao trocar o filtro de material, o modelo em edicao vira o equivalente do outro material. */
    fun equivalente(id: String, material: Material): String {
        val atual = modelo(id) ?: return id
        if (atual.material == material) return id
        val candidatos = modelos.filter { it.material == material && it.ehMesa == atual.ehMesa }
        val achado = candidatos.firstOrNull { it.cor == atual.cor && it.peca.braco == atual.peca.braco }
            ?: candidatos.firstOrNull { it.cor == atual.cor && !it.peca.braco }
            ?: candidatos.firstOrNull()
        return achado?.id ?: id
    }
}

/** Filtro do menu: Todos, Plastico ou Metal. So muda o que aparece. */
enum class FiltroMaterial(val rotulo: String, val material: Material?) {
    TODOS("Todos", null),
    PLASTICO("Plástico", Material.PLASTICO),
    METAL("Metal", Material.METAL);

    fun materiais(): List<Material> = material?.let { listOf(it) } ?: Material.entries
}
