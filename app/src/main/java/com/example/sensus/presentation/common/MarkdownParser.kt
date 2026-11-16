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
                line.isBlank() -> addBlankLine(blocks)
                ruleRegex.matches(line) -> Unit
                else -> blocks += parseLine(line)
            }
        }
        while (blocks.lastOrNull() == MarkdownBlock.Blank) blocks.removeAt(blocks.lastIndex)
        return blocks
    }

    private fun addBlankLine(blocks: MutableList<MarkdownBlock>) {
        if (blocks.isNotEmpty() && blocks.last() != MarkdownBlock.Blank) blocks += MarkdownBlock.Blank
    }

    private fun parseLine(line: String): MarkdownBlock {
        val heading = headingRegex.matchEntire(line)
        val numbered = numberedRegex.matchEntire(line)
        val bullet = bulletRegex.matchEntire(line)
        return when {
            heading != null -> MarkdownBlock.Heading(parseInline(heading.groupValues[1]))
            numbered != null -> MarkdownBlock.Numbered(numbered.groupValues[1].toInt(), parseInline(numbered.groupValues[2]))
            bullet != null -> MarkdownBlock.Bullet(parseInline(bullet.groupValues[1]))
            else -> MarkdownBlock.Paragraph(parseInline(line.trim()))
        }
    }

    fun parseInline(text: String): List<MarkdownSpan> {
        val scanner = InlineScanner(text)
        while (!scanner.finished) scanner.step()
        return scanner.finish()
    }

    /** Recorre el texto y mantiene el formato activo (negritas, cursivas, código). */
    private class InlineScanner(private val text: String) {
        private val spans = mutableListOf<MarkdownSpan>()
        private val buffer = StringBuilder()
        private var bold = false
        private var italic = false
        private var code = false
        private var index = 0

        val finished: Boolean get() = index >= text.length

        fun step() {
            when {
                isBacktickToggle() -> toggle(1) { code = !code }
                code -> appendCurrent()
                isBoldToggle() -> toggle(BOLD_MARKER.length) { bold = !bold }
                isItalicToggle() -> toggle(1) { italic = !italic }
                else -> appendCurrent()
            }
        }

        fun finish(): List<MarkdownSpan> {
            flush()
            return spans
        }

        private fun isBacktickToggle() = text[index] == '`' && (code || hasCloser("`", index + 1))

        private fun isBoldToggle() = text.startsWith(BOLD_MARKER, index) && (bold || hasCloser(BOLD_MARKER, index + BOLD_MARKER.length))

        private fun isItalicToggle() = text[index] == '*' && !text.startsWith(BOLD_MARKER, index) && (italic || hasSingleCloser(index + 1))

        private fun toggle(length: Int, change: () -> Unit) {
            flush()
            change()
            index += length
        }

        private fun appendCurrent() {
            buffer.append(text[index])
            index += 1
        }

        private fun flush() {
            if (buffer.isNotEmpty()) {
                spans += MarkdownSpan(buffer.toString(), bold, italic, code)
                buffer.clear()
            }
        }

        private fun hasCloser(marker: String, from: Int): Boolean = from < text.length && text.indexOf(marker, from) > from

        private fun hasSingleCloser(from: Int): Boolean {
            if (from >= text.length || text[from].isWhitespace()) return false
            var j = from
            while (j < text.length) {
                if (text[j] == '*') {
                    if (!text.startsWith(BOLD_MARKER, j)) return j > from
                    j += BOLD_MARKER.length
                } else {
                    j++
                }
            }
            return false
        }
    }

    private const val BOLD_MARKER = "**"
}
