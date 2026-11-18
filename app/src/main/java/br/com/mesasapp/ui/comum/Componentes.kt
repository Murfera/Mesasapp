package br.com.mesasapp.ui.comum

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.mesasapp.domain.StatusPedido
import br.com.mesasapp.ui.centavosParaCampo
import br.com.mesasapp.ui.theme.Cores
import br.com.mesasapp.ui.theme.Inter
import br.com.mesasapp.ui.theme.SpaceGrotesk
import br.com.mesasapp.util.toCentsOrNull

/** Texto no estilo do prototipo: tamanho em sp, peso numerico. */
@Composable
fun T(
    texto: String,
    tamanho: TextUnit = 14.sp,
    peso: Int = 400,
    cor: Color = Cores.Texto,
    modifier: Modifier = Modifier,
    fonte: FontFamily = Inter,
    alinhar: TextAlign? = null,
    umaLinha: Boolean = false
) {
    Text(
        texto, modifier = modifier, fontSize = tamanho, fontWeight = FontWeight(peso), color = cor,
        fontFamily = fonte, textAlign = alinhar, lineHeight = tamanho * 1.3f,
        maxLines = if (umaLinha) 1 else Int.MAX_VALUE, softWrap = !umaLinha,
        overflow = if (umaLinha) TextOverflow.Ellipsis else TextOverflow.Clip
    )
}

@Composable
fun Titulo(texto: String, tamanho: TextUnit = 26.sp, modifier: Modifier = Modifier) =
    T(texto, tamanho, 600, fonte = SpaceGrotesk, modifier = modifier)

@Composable
fun Numero(texto: String, tamanho: TextUnit = 30.sp, cor: Color = Cores.Texto, modifier: Modifier = Modifier) =
    T(texto, tamanho, 600, cor = cor, fonte = SpaceGrotesk, modifier = modifier)

/** Cartao branco com borda clara e canto de 14. */
@Composable
fun Cartao(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(16.dp),
    espaco: Dp = 14.dp,
    borda: BorderStroke = BorderStroke(1.dp, Cores.Borda),
    fundo: Color = Cores.Superficie,
    aoTocar: (() -> Unit)? = null,
    conteudo: @Composable ColumnScope.() -> Unit
) {
    val forma = RoundedCornerShape(14.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(forma)
            .background(fundo)
            .border(borda, forma)
            .then(if (aoTocar != null) Modifier.clickable(role = Role.Button, onClick = aoTocar) else Modifier)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(espaco),
        content = conteudo
    )
}

@Composable
fun Pilula(texto: String, fundo: Color, cor: Color, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(999.dp)).background(fundo).padding(horizontal = 10.dp, vertical = 4.dp)) {
        T(texto, 12.sp, 600, cor, umaLinha = true)
    }
}

@Composable
fun corStatus(s: StatusPedido): Pair<Color, Color> = when (s) {
    StatusPedido.ORCAMENTO -> Cores.RoxoClaro to Cores.Roxo
    StatusPedido.CONFIRMADO -> Cores.AzulClaro to Cores.Azul
    StatusPedido.ENTREGUE -> Cores.LaranjaClaro to Cores.Laranja
    StatusPedido.DEVOLVIDO -> Cores.CinzaStatus to Cores.CinzaStatusTexto
}

@Composable
fun PilulaStatus(s: StatusPedido, modifier: Modifier = Modifier) {
    val (f, c) = corStatus(s)
    Pilula(s.rotulo, f, c, modifier)
}

/** Chip de escolha: verde claro com borda verde quando marcado, branco quando nao. */
@Composable
fun Chip(
    texto: String,
    marcado: Boolean,
    aoTocar: () -> Unit,
    modifier: Modifier = Modifier,
    altura: Dp = 40.dp,
    escuro: Boolean = false
) {
    val forma = RoundedCornerShape(altura / 2)
    val (fundo, cor, borda) = when {
        marcado && escuro -> Triple(Cores.Texto, Cores.Superficie, Cores.Texto)
        marcado -> Triple(Cores.VerdeClaro, Cores.VerdeEscuro, Cores.Destaque)
        else -> Triple(Cores.Superficie, Cores.Texto, Cores.BordaCampo)
    }
    Box(
        modifier
            .height(altura)
            .clip(forma)
            .background(fundo)
            .border(1.dp, borda, forma)
            .clickable(role = Role.Button, onClick = aoTocar)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) { T(texto, 13.sp, 600, cor, umaLinha = true) }
}

@Composable
fun Bolinha(cor: Color, tamanho: Dp = 14.dp) {
    Box(Modifier.size(tamanho).clip(CircleShape).background(cor).border(1.dp, Cores.Bolinha, CircleShape))
}

@Composable
fun Rotulo(texto: String) = T(texto, 13.sp, 500, Cores.Texto2)

/** Caixa do campo: 48 de altura, borda cinza, verde quando focado. */
@Composable
fun CampoBase(
    valor: String,
    aoMudar: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    teclado: KeyboardType = KeyboardType.Text,
    altura: Dp = 48.dp,
    tamanho: TextUnit = 16.sp,
    peso: Int = 400,
    alinhar: TextAlign = TextAlign.Start,
    paddingH: Dp = 14.dp,
    aoFocar: (Boolean) -> Unit = {}
) {
    var focado by remember { mutableStateOf(false) }
    val forma = RoundedCornerShape(10.dp)
    BasicTextField(
        value = valor,
        onValueChange = aoMudar,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        textStyle = TextStyle(fontFamily = Inter, fontSize = tamanho, fontWeight = FontWeight(peso), color = Cores.Texto, textAlign = alinhar),
        cursorBrush = SolidColor(Cores.Destaque),
        modifier = modifier
            .height(altura)
            .onFocusChanged { focado = it.isFocused; aoFocar(it.isFocused) },
        decorationBox = { campo ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(altura)
                    .clip(forma)
                    .background(Cores.Superficie)
                    .border(if (focado) 2.dp else 1.dp, if (focado) Cores.Destaque else Cores.BordaCampo, forma)
                    .padding(horizontal = paddingH),
                contentAlignment = Alignment.CenterStart
            ) {
                if (valor.isEmpty() && placeholder.isNotEmpty()) {
                    T(placeholder, tamanho, 400, Cores.Placeholder, Modifier.fillMaxWidth(), alinhar = alinhar, umaLinha = true)
                }
                campo()
            }
        }
    )
}

/** Rotulo em cima, campo embaixo. */
@Composable
fun Campo(
    rotulo: String,
    valor: String,
    aoMudar: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    teclado: KeyboardType = KeyboardType.Text,
    tag: String? = null
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Rotulo(rotulo)
        CampoBase(valor, aoMudar, Modifier.fillMaxWidth().then(tag?.let { Modifier.testTag(it) } ?: Modifier), placeholder, teclado)
    }
}

/**
 * Campo numerico ligado a um valor salvo (estoque, precos).
 *
 * - **Zero aparece vazio**, e vazio vale zero: o estoque comeca limpo, sem uma
 *   fileira de "0" para apagar.
 * - Pode ser apagado enquanto a pessoa digita; [minimo] so vale para o que foi
 *   digitado (vazio continua zero).
 * - [placeholder] aparece em cinza no campo vazio, para mostrar o padrao
 *   (ex.: "4" cadeiras por mesa).
 * - O texto so e reescrito de fora quando o campo nao esta em uso, para a volta
 *   do valor salvo nao atropelar a digitacao.
 */
@Composable
fun CampoNumero(
    valor: Long,
    aoMudar: (Long) -> Unit,
    modifier: Modifier = Modifier,
    dinheiro: Boolean = false,
    minimo: Long = 0,
    placeholder: String = "",
    altura: Dp = 48.dp,
    paddingH: Dp = 10.dp,
    alinhar: TextAlign = TextAlign.Start,
    tamanho: TextUnit = 16.sp,
    peso: Int = 400
) {
    val formatar: (Long) -> String = { v -> if (v == 0L) "" else if (dinheiro) centavosParaCampo(v) else v.toString() }
    val ler: (String) -> Long = { s ->
        if (s.isBlank()) 0L
        else ((if (dinheiro) s.toCentsOrNull() else s.trim().toLongOrNull()) ?: 0L).coerceAtLeast(minimo)
    }
    var texto by remember { mutableStateOf(formatar(valor)) }
    var focado by remember { mutableStateOf(false) }
    LaunchedEffect(valor, focado) {
        if (!focado && ler(texto) != valor) texto = formatar(valor)
    }
    CampoBase(
        texto,
        { novo ->
            val limpo = if (dinheiro) novo.filter { it.isDigit() || it == ',' || it == '.' } else novo.filter { it.isDigit() }
            texto = limpo
            aoMudar(ler(limpo))
        },
        modifier,
        placeholder = placeholder,
        teclado = if (dinheiro) KeyboardType.Decimal else KeyboardType.Number,
        altura = altura, paddingH = paddingH, alinhar = alinhar, tamanho = tamanho, peso = peso,
        aoFocar = { focado = it }
    )
}

@Composable
fun Botao(
    texto: String,
    aoTocar: () -> Unit,
    modifier: Modifier = Modifier,
    altura: Dp = 48.dp,
    estilo: EstiloBotao = EstiloBotao.PRIMARIO,
    tamanho: TextUnit = 14.sp,
    raio: Dp = 12.dp,
    icone: (@Composable RowScope.() -> Unit)? = null
) {
    val forma = RoundedCornerShape(raio)
    val (fundo, cor, borda) = when (estilo) {
        EstiloBotao.PRIMARIO -> Triple(Cores.Verde, Cores.SobreVerde, null)
        EstiloBotao.CONTORNO -> Triple(Cores.Superficie, Cores.Destaque, Cores.Destaque)
        EstiloBotao.SUAVE -> Triple(Cores.VerdeClaro, Cores.VerdeEscuro, Cores.Destaque)
        EstiloBotao.SUAVE_SEM_BORDA -> Triple(Cores.VerdeClaro, Cores.VerdeEscuro, null)
        EstiloBotao.NEUTRO -> Triple(Cores.Superficie, Cores.Texto, Cores.BordaCampo)
    }
    Row(
        modifier
            .height(altura)
            .clip(forma)
            .background(fundo)
            .then(if (borda != null) Modifier.border(1.dp, borda, forma) else Modifier)
            .clickable(role = Role.Button, onClick = aoTocar)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icone?.invoke(this)
        T(texto, tamanho, 600, cor, umaLinha = true)
    }
}

enum class EstiloBotao { PRIMARIO, CONTORNO, SUAVE, SUAVE_SEM_BORDA, NEUTRO }

/** Botao quadrado de 44 com - ou +. */
@Composable
fun BotaoPasso(sinal: String, descricao: String, aoTocar: () -> Unit, modifier: Modifier = Modifier) {
    val forma = RoundedCornerShape(10.dp)
    Box(
        modifier
            .size(44.dp)
            .clip(forma)
            .background(Cores.Superficie)
            .border(1.dp, Cores.BordaCampo, forma)
            .clickable(role = Role.Button, onClick = aoTocar)
            .semantics(mergeDescendants = true) { contentDescription = descricao },
        contentAlignment = Alignment.Center
    ) { T(sinal, 20.sp, 400) }
}

/** Linha "rotulo ......... valor". */
@Composable
fun LinhaValor(rotulo: String, valor: String, modifier: Modifier = Modifier, tamanho: TextUnit = 13.sp, peso: Int = 400, cor: Color = Cores.Texto2, pesoValor: Int = peso) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        T(rotulo, tamanho, peso, cor, Modifier.weight(1f, fill = false))
        T(valor, tamanho, pesoValor, cor, Modifier.padding(start = 10.dp))
    }
}

@Composable
fun Tracejado(modifier: Modifier = Modifier) {
    val cor = Cores.Tracejado
    Canvas(modifier.fillMaxWidth().height(1.dp)) {
        drawLine(cor, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
    }
}

@Composable
fun Divisor(modifier: Modifier = Modifier, cor: Color = Cores.Linha) {
    Box(modifier.fillMaxWidth().height(1.dp).background(cor))
}

/** Faixa de aviso: verde para sucesso, laranja para erro. */
@Composable
fun Aviso(texto: String, ok: Boolean, modifier: Modifier = Modifier, vertical: Dp = 10.dp) {
    Box(
        modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .background(if (ok) Cores.VerdeClaro else Cores.LaranjaClaro)
            .padding(horizontal = if (vertical < 10.dp) 10.dp else 12.dp, vertical = vertical)
    ) { T(texto, 13.sp, 600, if (ok) Cores.VerdeEscuro else Cores.Laranja) }
}

/** Faixa cinza de informacao. */
@Composable
fun Nota(conteudo: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Cores.Cinza).padding(horizontal = 12.dp, vertical = 10.dp)) {
        T(conteudo, 13.sp, 400, Cores.Texto2)
    }
}

/** "‹ Pedidos" / "‹ Voltar" no alto das telas que abrem por cima. */
@Composable
fun Voltar(texto: String, aoTocar: () -> Unit) {
    Row(
        Modifier.height(40.dp).clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClick = aoTocar)
            .padding(start = 0.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = Cores.Destaque, modifier = Modifier.size(24.dp))
        T(texto, 14.sp, 600, Cores.Destaque)
    }
}

/** Espaco fixo, para nao repetir Modifier.width. */
@Composable
fun Largura(dp: Dp) = Box(Modifier.width(dp))
