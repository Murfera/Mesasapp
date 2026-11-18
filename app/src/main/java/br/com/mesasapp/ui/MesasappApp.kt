package br.com.mesasapp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.mesasapp.data.preferences.TemaApp
import br.com.mesasapp.di.AppContainer
import br.com.mesasapp.domain.FiltroMaterial
import br.com.mesasapp.ui.comum.Divisor
import br.com.mesasapp.ui.comum.T
import br.com.mesasapp.ui.telas.AgendaTela
import br.com.mesasapp.ui.telas.ConfigPagamentoTela
import br.com.mesasapp.ui.telas.ConfigPixTela
import br.com.mesasapp.ui.telas.DetalheTela
import br.com.mesasapp.ui.telas.EstoqueTela
import br.com.mesasapp.ui.telas.InicioTela
import br.com.mesasapp.ui.telas.NovoTela
import br.com.mesasapp.ui.telas.PedidosTela
import br.com.mesasapp.ui.telas.PrecosTela
import br.com.mesasapp.ui.theme.Cores
import br.com.mesasapp.ui.theme.SpaceGrotesk

private data class Aba(val tela: Tela, val titulo: String, val icone: ImageVector)

private val ABAS = listOf(
    Aba(Tela.INICIO, "Início", Icons.Outlined.Home),
    Aba(Tela.PEDIDOS, "Pedidos", Icons.Outlined.Assignment),
    Aba(Tela.NOVO, "Novo", Icons.Outlined.AddCircleOutline),
    Aba(Tela.AGENDA, "Agenda", Icons.Outlined.CalendarMonth),
    Aba(Tela.ESTOQUE, "Estoque", Icons.Outlined.Inventory2)
)

@Composable
fun MesasappApp(container: AppContainer) {
    val vm: MesasViewModel = viewModel(factory = MesasViewModel.Fabrica(container))

    BackHandler(enabled = vm.tela == Tela.DETALHE || vm.tela == Tela.PRECOS || vm.tela == Tela.PIX || vm.tela == Tela.PAGAMENTO) { vm.voltar() }

    Column(
        Modifier
            .fillMaxSize()
            .background(Cores.Fundo)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .imePadding()
    ) {
        BarraSuperior(vm)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (vm.tela) {
                Tela.INICIO -> InicioTela(vm)
                Tela.PEDIDOS -> PedidosTela(vm)
                Tela.NOVO -> NovoTela(vm)
                Tela.AGENDA -> AgendaTela(vm)
                Tela.ESTOQUE -> EstoqueTela(vm)
                Tela.DETALHE -> DetalheTela(vm)
                Tela.PRECOS -> PrecosTela(vm)
                Tela.PIX -> ConfigPixTela(vm)
                Tela.PAGAMENTO -> ConfigPagamentoTela(vm)
            }
        }
        BarraDeAbas(vm)
    }
}

@Composable
private fun BarraSuperior(vm: MesasViewModel) {
    val filtro by vm.filtro.collectAsState()
    Column(Modifier.background(Cores.Superficie).windowInsetsPadding(WindowInsets.statusBars)) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(start = 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                T("Locação de festas", 17.sp, 600, fonte = SpaceGrotesk)
                if (filtro != FiltroMaterial.TODOS) {
                    Box(
                        Modifier.height(24.dp).clip(RoundedCornerShape(12.dp)).background(Cores.VerdeClaro).padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) { T(filtro.rotulo, 12.sp, 600, Cores.VerdeEscuro) }
                }
            }
            Box {
                Box(
                    Modifier.size(44.dp).clip(CircleShape)
                        .clickable(role = Role.Button) { vm.menuAberto = !vm.menuAberto; vm.menuMaterialAberto = false }
                        .semantics { contentDescription = "Mais opções" }
                        .testTag("botao_menu"),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.MoreVert, null, tint = Cores.Texto, modifier = Modifier.size(22.dp)) }
                Menu(vm, filtro)
            }
        }
        Divisor(cor = Cores.Borda)
    }
}

@Composable
private fun Menu(vm: MesasViewModel, filtro: FiltroMaterial) {
    val tema by vm.tema.collectAsState()
    DropdownMenu(
        expanded = vm.menuAberto,
        onDismissRequest = { vm.menuAberto = false; vm.menuMaterialAberto = false; vm.menuTemaAberto = false },
        offset = DpOffset((-4).dp, 0.dp),
        shape = RoundedCornerShape(12.dp),
        containerColor = Cores.Superficie,
        modifier = Modifier.width(220.dp).padding(horizontal = 6.dp)
    ) {
        ItemMenu(Icons.Outlined.Layers, "Material", filtro.rotulo, Modifier.testTag("menu_material"), vm.menuMaterialAberto) {
            vm.menuMaterialAberto = !vm.menuMaterialAberto
        }
        if (vm.menuMaterialAberto) {
            Opcoes(FiltroMaterial.entries, filtro, { it.rotulo }, { "filtro_${it.name}" }) { vm.escolherFiltro(it) }
        }
        Divisor(Modifier.padding(horizontal = 4.dp, vertical = 6.dp), Cores.Borda)
        ItemMenu(Icons.Outlined.LocalOffer, "Preços", null, Modifier.testTag("menu_precos")) { vm.abrirDoMenu(Tela.PRECOS) }
        ItemMenu(Icons.Outlined.QrCode2, "Config. Pix", null, Modifier.testTag("menu_pix")) { vm.abrirDoMenu(Tela.PIX) }
        ItemMenu(Icons.Outlined.Payments, "Exibir", null, Modifier.testTag("menu_pagamento")) { vm.abrirDoMenu(Tela.PAGAMENTO) }
        Divisor(Modifier.padding(horizontal = 4.dp, vertical = 6.dp), Cores.Borda)
        ItemMenu(Icons.Outlined.DarkMode, "Tema", tema.rotulo, Modifier.testTag("menu_tema"), vm.menuTemaAberto) {
            vm.menuTemaAberto = !vm.menuTemaAberto
        }
        if (vm.menuTemaAberto) {
            Opcoes(TemaApp.entries, tema, { it.rotulo }, { "tema_${it.name}" }) { vm.escolherTema(it) }
        }
    }
}

/** Sub-lista do menu com o marcador na opcao atual (Material, Tema). */
@Composable
private fun <T> Opcoes(lista: List<T>, atual: T, rotulo: (T) -> String, tag: (T) -> String, aoEscolher: (T) -> Unit) {
    Column(Modifier.padding(start = 28.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        lista.forEach { item ->
            val sel = item == atual
            Row(
                Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(8.dp))
                    .background(if (sel) Cores.VerdeClaro else Color.Transparent)
                    .clickable(role = Role.Button) { aoEscolher(item) }
                    .padding(horizontal = 10.dp)
                    .testTag(tag(item)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.Check, null, tint = if (sel) Cores.Destaque else Color.Transparent, modifier = Modifier.size(16.dp))
                T(rotulo(item), 14.sp, if (sel) 600 else 500)
            }
        }
    }
}

@Composable
private fun ItemMenu(icone: ImageVector, texto: String, atual: String?, modifier: Modifier = Modifier, aberto: Boolean = false, aoTocar: () -> Unit) {
    Row(
        modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = aoTocar).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icone, null, tint = Cores.Texto, modifier = Modifier.size(18.dp))
        T(texto, 14.sp, 600, modifier = Modifier.weight(1f))
        if (atual != null) {
            T(atual, 13.sp, 500, Cores.Suave)
            Icon(
                Icons.Outlined.ExpandMore, null, tint = Cores.Suave,
                modifier = Modifier.size(16.dp).rotate(if (aberto) 180f else 0f)
            )
        }
    }
}

@Composable
private fun BarraDeAbas(vm: MesasViewModel) {
    Column(Modifier.background(Cores.Superficie)) {
        Divisor(cor = Cores.Borda)
        Row(Modifier.fillMaxWidth().height(74.dp).padding(bottom = 6.dp)) {
            ABAS.forEach { aba ->
                val ativa = vm.tela == aba.tela || (aba.tela == Tela.PEDIDOS && vm.tela == Tela.DETALHE)
                val cor = if (ativa) Cores.Destaque else Cores.Suave
                Column(
                    Modifier.weight(1f).fillMaxSize().clickable(role = Role.Tab) { vm.irPara(aba.tela) }.testTag("aba_${aba.titulo}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
                ) {
                    Icon(aba.icone, null, tint = cor, modifier = Modifier.size(22.dp))
                    T(aba.titulo, 12.sp, if (ativa) 600 else 500, cor)
                }
            }
        }
    }
}
