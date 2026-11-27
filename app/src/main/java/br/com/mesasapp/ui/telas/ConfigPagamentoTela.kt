package br.com.mesasapp.ui.telas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.mesasapp.ui.MesasViewModel
import br.com.mesasapp.ui.comum.Cartao
import br.com.mesasapp.ui.comum.Divisor
import br.com.mesasapp.ui.comum.T
import br.com.mesasapp.ui.comum.Titulo
import br.com.mesasapp.ui.comum.Voltar
import br.com.mesasapp.ui.theme.Cores

/** Menu > Exibir: campos opcionais do Novo pedido e o filtro Semana | Mês da lista de Pedidos. */
@Composable
fun ConfigPagamentoTela(vm: MesasViewModel) {
    val op by vm.pagamento.collectAsState()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Voltar("Voltar") { vm.voltarDoMenu() }
        Titulo("Exibir")
        Cartao(espaco = 0.dp) {
            T("Campos que aparecem no Novo pedido.", 13.sp, 400, Cores.Suave, Modifier.padding(bottom = 6.dp))
            Chave("Sinal pago", "Valor que o cliente já adiantou.", op.mostrarSinal, "chave_sinal") {
                vm.definirPagamento(op.copy(mostrarSinal = it))
            }
            Divisor()
            Chave("Caução", "Valor de garantia, devolvido se tudo voltar inteiro.", op.mostrarCaucao, "chave_caucao") {
                vm.definirPagamento(op.copy(mostrarCaucao = it))
            }
        }
        Cartao(espaco = 0.dp) {
            T("Tela de Pedidos.", 13.sp, 400, Cores.Suave, Modifier.padding(bottom = 6.dp))
            Chave(
                "Filtro Semana | Mês", "Mostra a escolha entre semana e mês acima da lista.",
                op.mostrarPeriodoPedidos, "chave_periodo"
            ) { vm.definirPagamento(op.copy(mostrarPeriodoPedidos = it)) }
            Divisor()
            Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                T("Abrir mostrando", 15.sp, 600)
                T("Período da lista ao abrir os Pedidos.", 12.sp, 400, Cores.Suave)
                Segmentos(listOf("Semana", "Mês"), if (op.pedidosPorSemana) 0 else 1, "padrao_periodo", altura = 36.dp) {
                    vm.definirPagamento(op.copy(pedidosPorSemana = it == 0))
                }
            }
        }
        Cartao(espaco = 0.dp) {
            T("Cobrar com Pix, no pedido.", 13.sp, 400, Cores.Suave, Modifier.padding(bottom = 6.dp))
            Chave(
                "QR Code", "Desligado, mostra só o código copia e cola e o botão Copiar.",
                op.mostrarQrCode, "chave_qr"
            ) { vm.definirPagamento(op.copy(mostrarQrCode = it)) }
        }
    }
}

@Composable
private fun Chave(titulo: String, descricao: String, ligado: Boolean, tag: String, aoMudar: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Switch) { aoMudar(!ligado) }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            T(titulo, 15.sp, 600)
            T(descricao, 12.sp, 400, Cores.Suave, Modifier.padding(top = 2.dp))
        }
        Switch(
            checked = ligado, onCheckedChange = aoMudar, modifier = Modifier.testTag(tag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Cores.SobreVerde, checkedTrackColor = Cores.Verde,
                uncheckedThumbColor = Cores.Suave, uncheckedTrackColor = Cores.Abas, uncheckedBorderColor = Cores.BordaCampo
            )
        )
    }
}
