package br.com.mesasapp.ui.telas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.mesasapp.domain.Peca
import br.com.mesasapp.ui.MesasViewModel
import br.com.mesasapp.ui.comum.CampoNumero
import br.com.mesasapp.ui.comum.Cartao
import br.com.mesasapp.ui.comum.T
import br.com.mesasapp.ui.comum.Titulo
import br.com.mesasapp.ui.comum.Voltar
import br.com.mesasapp.ui.theme.Cores

@Composable
fun PrecosTela(vm: MesasViewModel) {
    val precos by vm.precos.collectAsState()
    val filtro by vm.filtro.collectAsState()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("lista_precos")
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Voltar("Voltar") { vm.voltarDoMenu() }
        Column {
            Titulo("Preços")
            T(
                "Diária por peça e valor de reposição se quebrar ou não voltar. O preço vale para todas as cores. Os valores são de exemplo.",
                13.sp, 400, Cores.Suave, Modifier.padding(top = 4.dp)
            )
        }
        filtro.materiais().forEach { mat ->
            Cartao(espaco = 18.dp) {
                T(mat.rotulo, 18.sp, 600)
                Peca.entries.filter { mat in it.materiais }.forEach { peca ->
                    val chave = "${mat.prefixo}-${peca.chave}"
                    val p = precos.tabela[chave] ?: return@forEach
                    key(chave) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            T(peca.rotulo, 15.sp, 600)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    T("Diária por peça (R$)", 12.sp, 400, Cores.Texto2)
                                    CampoNumero(p.diariaCentavos, { vm.definirDiaria(chave, it) }, Modifier.fillMaxWidth().testTag("diaria_$chave"), dinheiro = true, altura = 44.dp)
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    T("Reposição (R$)", 12.sp, 400, Cores.Texto2)
                                    CampoNumero(p.reposicaoCentavos, { vm.definirReposicao(chave, it) }, Modifier.fillMaxWidth().testTag("reposicao_$chave"), dinheiro = true, altura = 44.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
        Cartao {
            Column {
                T("Regras gerais", 15.sp, 600)
                T(
                    "Jogo = 1 mesa + cadeiras (quantidade por mesa definida no Estoque, por material e cor), menos o desconto abaixo.",
                    13.sp, 400, Cores.Suave, Modifier.padding(top = 4.dp)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                T("Desconto por jogo (R$/dia)", 14.sp, modifier = Modifier.weight(1f))
                CampoNumero(precos.descontoJogoCentavos, { vm.definirDescontoJogo(it) }, Modifier.width(110.dp).testTag("desconto_jogo"), dinheiro = true, alinhar = TextAlign.End, paddingH = 14.dp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                T("Frete padrão (R$)", 14.sp, modifier = Modifier.weight(1f))
                CampoNumero(precos.fretePadraoCentavos, { vm.definirFrete(it) }, Modifier.width(110.dp).testTag("frete_padrao"), dinheiro = true, alinhar = TextAlign.End, paddingH = 14.dp)
            }
        }
    }
}
