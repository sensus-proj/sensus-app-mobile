package com.example.sensus.domain.moderation

import java.text.Normalizer

/**
 * Filtro determinístico local de lenguaje ofensivo / groserías (español e inglés).
 * Complementa la moderación de servidor bloqueando inmediatamente texto ofensivo en nombres, comercios, ofertas y reseñas.
 */
object ProfanityFilter {

    const val DEFAULT_REJECTION_MESSAGE =
        "El texto contiene lenguaje ofensivo o inapropiado. Por favor usa un lenguaje respetuoso."

    private val combiningMarksRegex = Regex("\\p{InCombiningDiacriticalMarks}+")
    private val tokenSplitRegex = Regex("[^a-z0-9ñ]+")
    private val tripleCharRegex = Regex("([a-z0-9ñ])\\1{2,}")
    private val separatedSingleCharsRegex = Regex("(?:\\b[a-z0-9ñ]\\b[\\s._\\-*]+){2,}\\b[a-z0-9ñ]\\b")
    private val nonAlnumRegex = Regex("[^a-z0-9ñ]+")

    private val bannedWords: Set<String> = setOf(
        "pito", "pitos", "pitote", "pitotes", "pitito",
        "puta", "putas", "puto", "putos", "putita", "putitas", "putito", "putitos",
        "putazo", "putazos", "puteria", "putero", "putamadre", "hijoputa", "hijaputa", "hijueputa",
        "pendejo", "pendeja", "pendejos", "pendejas", "pendejada", "pendejadas",
        "verga", "vergas", "vergazo", "vergazos", "verguiza", "vergon", "vergota", "averga",
        "chinga", "chingar", "chingada", "chingado", "chingados", "chingadas", "chingatumadre",
        "chingon", "chingona", "chingadera", "chingaderas", "chinguen", "chingo",
        "cabron", "cabrona", "cabrones", "cabronas",
        "culero", "culera", "culeros", "culeras", "culo", "culos", "culon", "culona",
        "mierda", "mierdas", "mierdero",
        "mamon", "mamona", "mamones", "mamonas", "mamada", "mamadas",
        "marica", "maricas", "maricon", "maricones", "maricona",
        "joto", "jota", "jotos", "jotas",
        "zorra", "zorras", "perra", "perras",
        "pinche", "pinches",
        "cagada", "cagadas", "cagado", "cagados", "cagar", "cagon", "cagona",
        "cono", "conos", "cojones", "cojon",
        "pija", "pijas", "pijazo", "chupapija",
        "panocha", "panochas", "concha", "conchatumadre",
        "huevon", "huevona", "huevones", "webon", "webona", "webones",
        "boludo", "boluda", "boludos", "pelotudo", "pelotuda", "pelotudos",
        "malparido", "malparida", "malparidos", "gonorrea",
        "gilipollas", "capullo", "idiota", "idiotas", "imbecil", "imbeciles",
        "estupido", "estupida", "estupidos", "estupidas",
        "baboso", "babosa", "ojete", "ojetes", "punetas", "puneton",
        "nalga", "nalgas", "tetas", "chichis", "pene", "penes", "vagina", "vaginas",
        "masturbar", "masturbacion", "porno", "pornografia", "xxx",
        "fuck", "fucker", "fucking", "fucked", "motherfucker", "motherfucking", "wtf",
        "shit", "shitty", "bullshit",
        "bitch", "bitches", "bitching",
        "asshole", "assholes", "bastard", "bastards",
        "dick", "dicks", "dickhead", "cock", "cocks", "pussy", "pussies",
        "whore", "whores", "slut", "sluts", "cunt", "cunts",
        "faggot", "faggots", "fag", "nigger", "niggers", "nigga", "niggas",
        "retard", "retarded"
    )

    private val bannedPhrases: List<String> = listOf(
        "hijo de puta",
        "hija de puta",
        "hijos de puta",
        "tu puta madre",
        "chinga tu madre",
        "chinga a tu madre",
        "vete a la verga",
        "vale verga",
        "vales verga",
        "me vale verga",
        "come mierda",
        "son of a bitch"
    )

    fun containsProfanity(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        val normalized = normalizeBasic(text)
        if (normalized.isEmpty()) return false

        val tokens = tokenSplitRegex.split(normalized).filter { it.isNotEmpty() }
        val hasBadToken = tokens.any { isBannedWord(it) }
        val spaced = tokens.joinToString(" ")
        val hasBadPhrase = bannedPhrases.any { containsWholePhrase(spaced, it) }
        val hasObfuscated = separatedSingleCharsRegex.findAll(normalized)
            .any { isBannedWord(nonAlnumRegex.replace(it.value, "")) }

        return hasBadToken || hasBadPhrase || hasObfuscated
    }

    private fun isBannedWord(token: String): Boolean = token in bannedWords ||
        tripleCharRegex.replace(token, "$1") in bannedWords ||
        tripleCharRegex.replace(token, "$1$1") in bannedWords

    private fun containsWholePhrase(text: String, phrase: String): Boolean {
        var idx = text.indexOf(phrase)
        while (idx >= 0) {
            val leftOk = idx == 0 || text[idx - 1] == ' '
            val end = idx + phrase.length
            val rightOk = end == text.length || text[end] == ' '
            if (leftOk && rightOk) return true
            idx = text.indexOf(phrase, idx + 1)
        }
        return false
    }

    private fun normalizeBasic(input: String): String {
        val decomposed = Normalizer.normalize(input, Normalizer.Form.NFD)
        val withoutMarks = combiningMarksRegex.replace(decomposed, "")
        return buildString(withoutMarks.length) {
            for (ch in withoutMarks) {
                val lower = ch.lowercaseChar()
                append(mapLeetChar(lower))
            }
        }
    }

    private fun mapLeetChar(ch: Char): Char = when (ch) {
        '0' -> 'o'
        '1', '!', '|' -> 'i'
        '3' -> 'e'
        '4', '@' -> 'a'
        '5', '$' -> 's'
        '7' -> 't'
        else -> ch
    }
}
