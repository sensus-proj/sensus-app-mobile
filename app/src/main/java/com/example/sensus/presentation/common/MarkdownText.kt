package com.example.sensus.presentation.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BLOCK_SPACING = 4.dp
private val BLANK_SPACING = 8.dp
private val LIST_INDENT = 18.dp
private const val CODE_BACKGROUND_ALPHA = 0.12f

/**
 * Pinta texto con Markdown ligero (negritas, cursivas, código, viñetas y listas numeradas) usando solo
 * Compose, sin dependencias externas. Se usa para las respuestas de Sensi IA.
 */
@Composable
fun MarkdownText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 14.sp,
    lineHeight: TextUnit = 20.sp
) {
    val blocks = remember(text) { MarkdownParser.parse(text) }
    Column(modifier = modifier) {
        blocks.forEachIndexed { index, block ->
            if (index > 0 && block !is MarkdownBlock.Blank && blocks[index - 1] !is MarkdownBlock.Blank) {
                Spacer(modifier = Modifier.height(BLOCK_SPACING))
            }
            when (block) {
                is MarkdownBlock.Blank -> Spacer(modifier = Modifier.height(BLANK_SPACING))
                is MarkdownBlock.Paragraph -> BlockText(block.spans, color, fontSize, lineHeight)
                is MarkdownBlock.Heading -> BlockText(block.spans, color, fontSize, lineHeight, forceBold = true)
                is MarkdownBlock.Bullet -> ListRow("•", block.spans, color, fontSize, lineHeight)
                is MarkdownBlock.Numbered -> ListRow("${block.number}.", block.spans, color, fontSize, lineHeight)
            }
        }
    }
}

@Composable
private fun BlockText(
    spans: List<MarkdownSpan>,
    color: Color,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    modifier: Modifier = Modifier,
    forceBold: Boolean = false
) {
    Text(
        text = remember(spans, color, forceBold) { spans.toAnnotatedString(color, forceBold) },
        fontSize = fontSize,
        lineHeight = lineHeight,
        color = color,
        modifier = modifier
    )
}

@Composable
private fun ListRow(
    marker: String,
    spans: List<MarkdownSpan>,
    color: Color,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    indent: Dp = LIST_INDENT
) {
    Row(modifier = Modifier.padding(start = 4.dp)) {
        Text(
            text = marker,
            fontSize = fontSize,
            lineHeight = lineHeight,
            color = color,
            modifier = Modifier.width(indent)
        )
        BlockText(spans, color, fontSize, lineHeight)
    }
}

private fun List<MarkdownSpan>.toAnnotatedString(color: Color, forceBold: Boolean): AnnotatedString =
    buildAnnotatedString {
        for (span in this@toAnnotatedString) {
            val style = SpanStyle(
                fontWeight = if (span.bold || forceBold) FontWeight.Bold else null,
                fontStyle = if (span.italic) FontStyle.Italic else null,
                fontFamily = if (span.code) FontFamily.Monospace else null,
                background = if (span.code) color.copy(alpha = CODE_BACKGROUND_ALPHA) else Color.Unspecified
            )
            withStyle(style) { append(span.text) }
        }
    }

@Preview(showBackground = true)
@Composable
private fun MarkdownTextPreview() {
    MarkdownText(
        text = "He encontrado una oferta cercana:\n\n**Producto:** Teclado Gaming\n- Precio: *$150.00 MXN*\n" +
            "- Stock: `1` unidad\n1. Apártala\n2. Recógela",
        color = Color.Black
    )
}
