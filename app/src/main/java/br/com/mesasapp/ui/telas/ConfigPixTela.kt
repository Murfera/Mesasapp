package br.com.mesasapp.ui.telas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.mesasapp.ui.MesasViewModel
import br.com.mesasapp.ui.comum.Campo
import br.com.mesasapp.ui.comum.Cartao
import br.com.mesasapp.ui.comum.Nota
import br.com.mesasapp.ui.comum.T
import br.com.mesasapp.ui.comum.Titulo
import br.com.mesasapp.ui.comum.Voltar
import br.com.mesasapp.ui.theme.Cores

/** Os campos salvam a cada letra, como no prototipo: nao ha botao Salvar. */
@Composable
fun ConfigPixTela(vm: MesasViewModel) {
    var pix by remember { mutableStateOf(vm.pix.value) }
    fun mudar(novo: br.com.mesasapp.data.preferences.PixConfig) {
        pix = novo
        vm.definirPix(novo)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("lista_pix")
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Voltar("Voltar") { vm.voltarDoMenu() }
        Titulo("Config. Pix")
        Cartao {
            T("Usado para gerar o QR Code e o copia e cola dos pedidos.", 13.sp, 400, Cores.Suave)
            Campo("Chave Pix", pix.chave, { mudar(pix.copy(chave = it)) }, placeholder = "CPF, CNPJ, e-mail, +55 celular ou aleatória", tag = "campo_chave_pix")
            Campo("Nome do recebedor (até 25 letras)", pix.nome, { mudar(pix.copy(nome = it)) }, placeholder = "Como aparece no banco", tag = "campo_recebedor")
            Campo("Cidade", pix.cidade, { mudar(pix.copy(cidade = it)) }, placeholder = "Sua cidade", tag = "campo_cidade")
        }
        Nota("As alterações valem na hora para todos os QR Codes gerados nos pedidos.")
    }
}
