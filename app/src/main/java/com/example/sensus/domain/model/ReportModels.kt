package com.example.sensus.domain.model

import java.text.Normalizer

enum class ReportTargetType(val apiCode: String) {
    OFFER("offer"),
    POST("post"),
    COMMENT("comment")
}

enum class ReportReason(val apiCode: String, val displayLabel: String) {
    SPAM("spam", "Spam o publicidad engañosa"),
    OFFENSIVE("ofensivo", "Contenido ofensivo o agresivo"),
    FALSE_OFFER("false_offer", "Oferta falsa o inexistente"),
    PRICE_MISMATCH("price_mismatch", "El precio en tienda no coincide"),
    FRAUD("fraude", "Posible fraude o suplantación"),
    OTHER("otro", "Otro motivo")
}

data class ContentReportInput(
    val targetType: ReportTargetType,
    val targetId: String,
    val reason: ReportReason,
    val details: String? = null
)

data class SubmittedReport(
    val id: String,
    val targetType: String,
    val targetId: String,
    val reason: String,
    val targetMarkedForReview: Boolean,
    val createdAtUtc: String,
    val message: String
)

object ReportPolicy {
    const val MAX_DETAILS_LENGTH = 500
    const val LEGAL_EXCLUSION_MESSAGE =
        "Este tipo de contenido excede la moderación de la plataforma. " +
            "Por tu seguridad, repórtalo directamente ante las autoridades competentes (ej. NCMEC, Fiscalía o línea 911 / 089)."

    private val combiningMarksRegex = Regex("\\p{InCombiningDiacriticalMarks}+")
    private val prohibitedTerms = listOf(
        "csam",
        "abuso infantil",
        "abuso sexual infantil",
        "explotacion infantil",
        "explotacion sexual infantil",
        "pornografia infantil",
        "child abuse",
        "child sexual",
        "child exploitation",
        "child pornography",
        "underage sexual"
    )

    fun isProhibitedLegalContent(details: String?): Boolean {
        val raw = details?.trim().orEmpty()
        if (raw.isEmpty()) return false
        val normalized = combiningMarksRegex.replace(
            Normalizer.normalize(raw, Normalizer.Form.NFD),
            ""
        ).lowercase()
        return prohibitedTerms.any { normalized.contains(it) }
    }
}
