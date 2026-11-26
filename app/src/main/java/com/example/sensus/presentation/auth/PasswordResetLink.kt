package com.example.sensus.presentation.auth

import java.net.URI
import java.net.URISyntaxException
import java.net.URLDecoder

internal object PasswordResetLink {
    private const val MAX_LINK_LENGTH = 4096

    fun parse(raw: String): String? {
        if (raw.length > MAX_LINK_LENGTH) return null
        val uri = parseUri(raw) ?: return null
        return uri.takeIf(::isResetDestination)?.rawQuery?.let(::parseToken)
    }

    private fun parseToken(query: String): String? {
        val tokenParameter = query.split('&').singleOrNull { decode(it.substringBefore('=')) == "token" } ?: return null
        return decode(tokenParameter.substringAfter('=', missingDelimiterValue = ""))
            .takeIf { it.isNotEmpty() && it.none { char -> char.isWhitespace() || char.isISOControl() } }
    }

    private fun isResetDestination(uri: URI): Boolean {
        if (uri.rawFragment != null || uri.rawAuthority != uri.host) return false
        return when {
            uri.scheme.equals("sensus", ignoreCase = true) -> uri.host.equals("reset", ignoreCase = true) && uri.rawPath in setOf("", "/")
            uri.scheme.equals("https", ignoreCase = true) -> uri.host.equals("rodnix.com.mx", ignoreCase = true) &&
                uri.rawPath in setOf("/sensus/reset", "/sensus/reset/")
            else -> false
        }
    }

    private fun parseUri(raw: String): URI? = try {
        URI(raw)
    } catch (_: URISyntaxException) {
        null
    }

    private fun decode(raw: String): String = URLDecoder.decode(raw, Charsets.UTF_8.name())
}
