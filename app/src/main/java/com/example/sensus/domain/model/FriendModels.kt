package com.example.sensus.domain.model

import java.net.URI
import java.net.URISyntaxException
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest

private const val INVITE_BASE_URL = "https://rodnix.com.mx/sensus"
private const val INVITE_CODE_HEX_BYTES = 3
private const val MIN_INVITE_CODE_LENGTH = 6
private const val BYTE_MASK = 0xFF
private const val RADIX_HEX = 16
private const val HEX_PAD_LEN = 2

const val MAX_OFFER_SHARE_FRIENDS = 10

data class ConnectedFriend(
    val inviteCode: String,
    val displayName: String,
    val addedAtEpochMs: Long,
    val addedVia: String,
    val userId: String = "",
    val photoUrl: String? = null
)

data class PendingFriendRequest(
    val requestId: String,
    val senderInviteCode: String,
    val senderName: String,
    val addedVia: String,
    val createdAtEpochMs: Long,
    val senderPhotoUrl: String? = null
)

data class SentFriendRequest(
    val requestId: String,
    val targetInviteCode: String,
    val targetName: String,
    val addedVia: String,
    val createdAtEpochMs: Long,
    val targetPhotoUrl: String? = null
)

data class SharedOfferMessage(
    val id: String,
    val offerId: String,
    val senderUserId: String,
    val recipientUserId: String,
    val offerTitle: String,
    val storeName: String,
    val merchantId: String?,
    val category: String?,
    val originalPrice: Double,
    val discountedPrice: Double,
    val discountPercentage: Int,
    val imageUrl: String?,
    val sharedAtEpochMs: Long
) {
    fun toProductOffer(): ProductOffer = ProductOffer(
        id = offerId,
        name = offerTitle,
        storeName = storeName,
        category = category?.takeIf { it.isNotBlank() }?.let { StoreCategory.displayNameFor(it, fallback = it) } ?: "Oferta",
        originalPrice = originalPrice,
        discountPrice = discountedPrice,
        discountPercent = discountPercentage,
        unitsLeft = 1,
        distanceMeters = 0,
        imageUrl = imageUrl,
        merchantId = merchantId
    )
}

sealed interface ShareOfferOutcome {
    data class Shared(val count: Int) : ShareOfferOutcome
    data object GuestNotAllowed : ShareOfferOutcome
    data class ValidationError(val message: String) : ShareOfferOutcome
    data class NetworkFailure(val message: String) : ShareOfferOutcome
}

enum class FriendNoticeType {
    INCOMING_REQUEST,
    ACCEPTED_FRIEND
}

data class ParsedFriendInvite(
    val inviteCode: String,
    val displayName: String?
)

sealed interface AddFriendResult {
    data class Added(val friend: ConnectedFriend) : AddFriendResult
    data class RequestSent(val request: SentFriendRequest) : AddFriendResult
    data class AlreadyPending(val targetName: String) : AddFriendResult
    data class AlreadyConnected(val friend: ConnectedFriend) : AddFriendResult
    data object SelfInviteNotAllowed : AddFriendResult
    data object InvalidCode : AddFriendResult
    data object GuestNotAllowed : AddFriendResult
    data class NetworkFailure(val message: String) : AddFriendResult
}

object FriendInviteCodec {
    private val validCodeRegex = Regex("^[A-Z0-9-]{$MIN_INVITE_CODE_LENGTH,32}$")

    fun generateInviteCode(email: String?, displayName: String?): String {
        val seed = email?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
            ?: displayName?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
            ?: return "SNS-VECINO"
        val digest = MessageDigest.getInstance("SHA-256").digest(seed.toByteArray(Charsets.UTF_8))
        val suffix = digest.take(INVITE_CODE_HEX_BYTES).joinToString("") { byte ->
            (byte.toInt() and BYTE_MASK).toString(RADIX_HEX).uppercase().padStart(HEX_PAD_LEN, '0')
        }
        return "SNS-$suffix"
    }

    fun buildInviteUrl(inviteCode: String, displayName: String?): String {
        val cleanCode = URLEncoder.encode(inviteCode.trim().uppercase(), Charsets.UTF_8.name())
        val cleanName = displayName?.trim()?.takeIf { it.isNotBlank() }
        return if (cleanName != null) {
            "$INVITE_BASE_URL?code=$cleanCode&name=${URLEncoder.encode(cleanName, Charsets.UTF_8.name())}"
        } else {
            "$INVITE_BASE_URL?code=$cleanCode"
        }
    }

    fun parseInvite(rawInput: String): ParsedFriendInvite? {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) return null

        val uriParsed = parseFromUri(trimmed)
        if (uriParsed != null) return uriParsed

        val normalizedCode = trimmed.uppercase()
        return if (validCodeRegex.matches(normalizedCode)) {
            ParsedFriendInvite(inviteCode = normalizedCode, displayName = null)
        } else {
            null
        }
    }

    private fun parseFromUri(raw: String): ParsedFriendInvite? {
        val uri = parseSafeUri(raw)?.takeIf(::isInviteUri) ?: return null
        val queryParams = parseQueryParams(uri.rawQuery.orEmpty()) ?: return null
        val rawCode = queryParams["code"]?.trim()?.uppercase().orEmpty()
        return if (validCodeRegex.matches(rawCode)) {
            val rawName = queryParams["name"]?.trim()?.takeIf { it.isNotBlank() }
            ParsedFriendInvite(inviteCode = rawCode, displayName = rawName)
        } else {
            null
        }
    }

    private fun parseSafeUri(raw: String): URI? = try {
        URI(raw)
    } catch (_: URISyntaxException) {
        null
    }

    private fun isInviteUri(uri: URI): Boolean {
        if (uri.rawUserInfo != null || uri.port != -1) return false
        val scheme = uri.scheme?.lowercase()
        val path = uri.rawPath.orEmpty()
        return when (scheme) {
            "sensus" -> uri.host.equals("invitacion", ignoreCase = true) && (path.isEmpty() || path == "/")
            "https", "http" -> uri.host.equals("rodnix.com.mx", ignoreCase = true) && path in inviteWebPaths
            else -> false
        }
    }

    private fun parseQueryParams(rawQuery: String): Map<String, String>? {
        val params = mutableMapOf<String, String>()
        for (pair in rawQuery.split("&")) {
            val separator = pair.indexOf('=')
            if (separator <= 0) continue
            val key = URLDecoder.decode(pair.substring(0, separator), Charsets.UTF_8.name())
            if (key == "code" || key == "name") {
                val decoded = URLDecoder.decode(pair.substring(separator + 1), Charsets.UTF_8.name())
                if (params.put(key, decoded) != null) return null
            }
        }
        return params
    }

    private val inviteWebPaths = setOf("/sensus", "/sensus/", "/sensus/invitacion", "/sensus/invitacion/")
}
