package br.com.mesasapp.ui.telas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.mesasapp.domain.CADEIRAS_POR_MESA_PADRAO
import br.com.mesasapp.domain.Cor
import br.com.mesasapp.domain.Precos
import br.com.mesasapp.domain.FiltroMaterial
import br.com.mesasapp.domain.Material
import br.com.mesasapp.domain.Modelo
import br.com.mesasapp.domain.Peca
import br.com.mesasapp.domain.jogosCompletos
import br.com.mesasapp.ui.MesasViewModel
import br.com.mesasapp.ui.comum.Bolinha
import br.com.mesasapp.ui.comum.CampoNumero
import br.com.mesasapp.ui.comum.Cartao
import br.com.mesasapp.ui.comum.Numero
import br.com.mesasapp.ui.comum.T
import br.com.mesasapp.ui.comum.Titulo
import br.com.mesasapp.ui.theme.Cores

@Composable
fun EstoqueTela(vm: MesasViewModel) {
    val estoque by vm.estoque.collectAsState()
    val precos by vm.precos.collectAsState()
    val filtro by vm.filtro.collectAsState()
    val mat = filtro.material ?: vm.estoqueMaterial

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("lista_estoque")
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Titulo("Estoque")

        if (filtro == FiltroMaterial.TODOS) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Cores.Abas).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Material.entries.forEach { m ->
                    val sel = m == mat
                    Box(
                        Modifier.weight(1f).height(44.dp)
                            .then(if (sel) Modifier.shadow(1.dp, RoundedCornerShape(9.dp)) else Modifier)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (sel) Cores.Superficie else Color.Transparent)
                            .clickable(role = Role.Tab) { vm.estoqueMaterial = m }
                            .testTag("aba_material_${m.name}"),
                        contentAlignment = Alignment.Center
                    ) { T(m.rotulo, 14.sp, if (sel) 600 else 500, if (sel) Cores.Texto else Cores.CinzaStatusTexto) }
                }
            }
        }

        // Um cartao de jogos completos por cor, com as cadeiras por mesa daquela cor.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Cor.entries.forEach { cor ->
                key(mat, cor) {
                    Cartao(Modifier.weight(1f), PaddingValues(horizontal = 8.dp, vertical = 12.dp), espaco = 10.dp) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Bolinha(Color(cor.argb))
                                T(cor.rotulo, 13.sp, 600)
                            }
                            Numero(jogosCompletos(estoque, precos, mat, cor).toString(), 28.sp, modifier = Modifier.padding(top = 4.dp).testTag("jogos_${mat.name}_${cor.name}"))
                            T("jogos completos", 10.sp, 400, Cores.Suave, umaLinha = true)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            T("Cadeiras por mesa", 10.sp, 400, Cores.Texto2, umaLinha = true)
                            // Vazio = padrao de 4 (aparece em cinza). Guardado como 0.
                            CampoNumero(
                                (precos.cadeirasPorMesa[Precos.chaveCadeiras(mat, cor)] ?: 0).toLong(),
                                { vm.definirCadeirasPorMesa(mat, cor, it) },
                                Modifier.fillMaxWidth().testTag("cpm_${mat.name}_${cor.name}"),
                                minimo = 1, placeholder = CADEIRAS_POR_MESA_PADRAO.toString(), altura = 44.dp, paddingH = 8.dp
                            )
                        }
                    }
                }
            }
        }

        Peca.entries.filter { mat in it.materiais }.forEach { peca ->
            val modelos = Cor.entries.map { Modelo(mat, peca, it) }
            Cartao(espaco = 12.dp) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    T(peca.rotulo, 16.sp, 600)
                    T("${modelos.sumOf { estoque[it.id] ?: 0 }} no total", 13.sp, 400, Cores.Suave, Modifier.testTag("total_${mat.name}_${peca.name}"))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    modelos.forEach { m ->
                        key(m.id) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Bolinha(Color(m.cor.argb))
                                    T(m.cor.rotulo, 13.sp, 600, Cores.Texto2)
                                }
                                CampoNumero(
                                    (estoque[m.id] ?: 0).toLong(),
                                    { vm.definirTotal(m.id, it) },
                                    Modifier.fillMaxWidth().testTag("qtd_${m.id}")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
