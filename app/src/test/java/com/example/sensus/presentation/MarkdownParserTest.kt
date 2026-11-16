package com.example.sensus.presentation

import com.example.sensus.presentation.common.MarkdownBlock
import com.example.sensus.presentation.common.MarkdownParser
import com.example.sensus.presentation.common.MarkdownSpan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserTest {

    @Test
    fun `negritas se convierten en span bold sin asteriscos`() {
        val spans = MarkdownParser.parseInline("**Producto:** Teclado")

        assertEquals(
            listOf(MarkdownSpan("Producto:", bold = true), MarkdownSpan(" Teclado")),
            spans
        )
    }

    @Test
    fun `cursiva y codigo en linea`() {
        val spans = MarkdownParser.parseInline("Precio *150* y `1` unidad")

        assertEquals(
            listOf(
                MarkdownSpan("Precio "),
                MarkdownSpan("150", italic = true),
                MarkdownSpan(" y "),
                MarkdownSpan("1", code = true),
                MarkdownSpan(" unidad")
            ),
            spans
        )
    }

    @Test
    fun `marcadores sin cierre se muestran literales`() {
        assertEquals(listOf(MarkdownSpan("2 ** 3 y *a")), MarkdownParser.parseInline("2 ** 3 y *a"))
        assertEquals(listOf(MarkdownSpan("precio `sin cierre")), MarkdownParser.parseInline("precio `sin cierre"))
    }

    @Test
    fun `asterisco aislado entre espacios no activa cursiva`() {
        assertEquals(listOf(MarkdownSpan("3 * 4 * 5")), MarkdownParser.parseInline("3 * 4 * 5"))
    }

    @Test
    fun `el contenido dentro de codigo no se interpreta`() {
        val spans = MarkdownParser.parseInline("`**no negrita**`")

        assertEquals(listOf(MarkdownSpan("**no negrita**", code = true)), spans)
    }

    @Test
    fun `lineas con viñetas y numeradas se detectan`() {
        val blocks = MarkdownParser.parse("- uno\n* dos\n• tres\n1. cuatro\n2) cinco")

        assertEquals(
            listOf(
                MarkdownBlock.Bullet(listOf(MarkdownSpan("uno"))),
                MarkdownBlock.Bullet(listOf(MarkdownSpan("dos"))),
                MarkdownBlock.Bullet(listOf(MarkdownSpan("tres"))),
                MarkdownBlock.Numbered(1, listOf(MarkdownSpan("cuatro"))),
                MarkdownBlock.Numbered(2, listOf(MarkdownSpan("cinco")))
            ),
            blocks
        )
    }

    @Test
    fun `encabezados pierden los numerales`() {
        val blocks = MarkdownParser.parse("## Ofertas cercanas")

        assertEquals(listOf(MarkdownBlock.Heading(listOf(MarkdownSpan("Ofertas cercanas")))), blocks)
    }

    @Test
    fun `lineas en blanco se colapsan y se recortan en los extremos`() {
        val blocks = MarkdownParser.parse("\n\nHola\n\n\n\nAdios\n\n")

        assertEquals(
            listOf(
                MarkdownBlock.Paragraph(listOf(MarkdownSpan("Hola"))),
                MarkdownBlock.Blank,
                MarkdownBlock.Paragraph(listOf(MarkdownSpan("Adios")))
            ),
            blocks
        )
    }

    @Test
    fun `separadores horizontales se omiten`() {
        val blocks = MarkdownParser.parse("Antes\n---\nDespues")

        assertEquals(2, blocks.size)
        assertTrue(blocks.all { it is MarkdownBlock.Paragraph })
    }

    @Test
    fun `respuesta real de Sensi se estructura por lineas`() {
        val blocks = MarkdownParser.parse(
            "He encontrado una oferta cercana:\r\n\r\n**Producto:** Teclado Gaming\r\n" +
                "**Precio con Descuento:** \$150.00 MXN (25% de descuento)\r\n\r\nSi te interesa, avísame."
        )

        assertEquals(6, blocks.size)
        val producto = blocks[2] as MarkdownBlock.Paragraph
        assertEquals(MarkdownSpan("Producto:", bold = true), producto.spans.first())
        assertTrue(blocks.flatMap { (it as? MarkdownBlock.Paragraph)?.spans.orEmpty() }.none { "**" in it.text })
    }

    @Test
    fun `texto vacio no genera bloques`() {
        assertTrue(MarkdownParser.parse("").isEmpty())
        assertTrue(MarkdownParser.parse("  \n \n").isEmpty())
    }

    @Test
    fun `html y enlaces se muestran como texto sin interpretar`() {
        val blocks = MarkdownParser.parse("<b>hola</b> [enlace](http://x)")

        assertEquals(
            listOf(MarkdownBlock.Paragraph(listOf(MarkdownSpan("<b>hola</b> [enlace](http://x)")))),
            blocks
        )
    }
}
