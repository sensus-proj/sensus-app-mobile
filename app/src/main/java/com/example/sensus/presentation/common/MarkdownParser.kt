package com.example.sensus.presentation.common

/** Fragmento de texto en línea con su formato (FT-24, respuestas de Sensi IA). */
data class MarkdownSpan(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val code: Boolean = false
)

/** Bloque de una respuesta Markdown ligera. */
sealed interface MarkdownBlock {
    data class Paragraph(val spans: List<MarkdownSpan>) : MarkdownBlock
    data class Heading(val spans: List<MarkdownSpan>) : MarkdownBlock
    data class Bullet(val spans: List<MarkdownSpan>) : MarkdownBlock
    data class Numbered(val number: Int, val spans: List<MarkdownSpan>) : MarkdownBlock
    data object Blank : MarkdownBlock
}

/**
 * Parser de un subconjunto seguro de Markdown, sin dependencias externas: `**negritas**`, `*cursivas*`,
 * `` `código` ``, encabezados `#`, viñetas (`-`, `*`, `•`) y listas numeradas. Los marcadores sin cierre se
 * muestran como texto literal. No interpreta HTML, enlaces ni imágenes.
 */
object MarkdownParser {
    private val bulletRegex = Regex("""^\s*[-*•]\s+(.*)$""")
    private val numberedRegex = Regex("""^\s*(\d{1,3})[.)]\s+(.*)$""")
    private val headingRegex = Regex("""^\s*#{1,6}\s+(.*)$""")
    private val ruleRegex = Regex("""^\s*(-{3,}|\*{3,}|_{3,})\s*$""")

    fun parse(markdown: String): List<MarkdownBlock> {
        val blocks = mutableListOf<MarkdownBlock>()
        for (rawLine in markdown.replace("\r\n", "\n").split('\n')) {
            val line = rawLine.trimEnd()
            when {
                line.isBlank() -> if (blocks.isNotEmpty() && blocks.last() != MarkdownBlock.Blank) {
                    blocks += MarkdownBlock.Blank
                }
                ruleRegex.matches(line) -> Unit
                headingRegex.matches(line) -> blocks += MarkdownBlock.Heading(
                    parseInline(headingRegex.matchEntire(line)!!.groupValues[1])
                )
                numberedRegex.matches(line) -> numberedRegex.matchEntire(line)!!.let {
                    blocks += MarkdownBlock.Numbered(it.groupValues[1].toInt(), parseInline(it.groupValues[2]))
                }
                bulletRegex.matches(line) -> blocks += MarkdownBlock.Bullet(
                    parseInline(bulletRegex.matchEntire(line)!!.groupValues[1])
                )
                else -> blocks += MarkdownBlock.Paragraph(parseInline(line.trim()))
            }
        }
        while (blocks.lastOrNull() == MarkdownBlock.Blank) blocks.removeAt(blocks.lastIndex)
        return blocks
    }

    fun parseInline(text: String): List<MarkdownSpan> {
        val spans = mutableListOf<MarkdownSpan>()
        val buffer = StringBuilder()
        var bold = false
        var italic = false
        var code = false

        fun flush() {
            if (buffer.isNotEmpty()) {
                spans += MarkdownSpan(buffer.toString(), bold, italic, code)
                buffer.clear()
            }
        }

        var i = 0
        while (i < text.length) {
            when {
                text[i] == '`' && (code || hasCloser(text, "`", i + 1)) -> {
                    flush(); code = !code; i += 1
                }
                code -> { buffer.append(text[i]); i += 1 }
                text.startsWith("**", i) && (bold || hasCloser(text, "**", i + 2)) -> {
                    flush(); bold = !bold; i += 2
                }
                text[i] == '*' && !text.startsWith("**", i) && (italic || hasSingleCloser(text, i + 1)) -> {
                    flush(); italic = !italic; i += 1
                }
                else -> { buffer.append(text[i]); i += 1 }
            }
        }
        flush()
        return spans
    }

    private fun hasCloser(text: String, marker: String, from: Int): Boolean =
        from < text.length && text.indexOf(marker, from) > from

    private fun hasSingleCloser(text: String, from: Int): Boolean {
        if (from >= text.length || text[from].isWhitespace()) return false
        var j = from
        while (j < text.length) {
            if (text[j] == '*') {
                if (text.startsWith("**", j)) { j += 2; continue }
                return j > from
            }
            j++
        }
        return false
    }
}
