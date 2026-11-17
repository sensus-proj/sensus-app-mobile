package com.example.sensus.data.repository

import com.example.sensus.data.local.db.dao.UserPreferenceDao
import com.example.sensus.data.local.db.entity.UserPreferenceEntity
import com.example.sensus.data.remote.dto.FriendItemDto
import com.example.sensus.data.remote.dto.PendingFriendRequestDto
import com.example.sensus.data.remote.dto.SentFriendRequestDto
import com.example.sensus.domain.model.ConnectedFriend
import com.example.sensus.domain.model.PendingFriendRequest
import com.example.sensus.domain.model.SentFriendRequest
import java.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val KEY_FRIENDS_PREFIX = "connected_friends_"
private const val KEY_NOTIFIED_PREFIX = "notified_friend_req_ids_"
private const val MAX_SEEN_IDS = 50
internal const val DEFAULT_FRIENDS_SCOPE = "default"
internal const val GUEST_FRIENDS_SCOPE = "guest"
private val friendsJson = Json { ignoreUnknownKeys = true }

@Serializable
private data class ConnectedFriendRecord(
    val inviteCode: String,
    val displayName: String,
    val addedAtEpochMs: Long,
    val addedVia: String,
    val userId: String = ""
)

internal class FriendsLocalStore(
    private val dao: UserPreferenceDao?,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun loadFriends(scopeKey: String): List<ConnectedFriend> {
        if (scopeKey == GUEST_FRIENDS_SCOPE || dao == null) return emptyList()
        val raw = withContext(ioDispatcher) {
            try {
                dao.getByKey("$KEY_FRIENDS_PREFIX$scopeKey")?.prefValue
            } catch (_: IllegalStateException) {
                null
            }
        } ?: return emptyList()
        return try {
            friendsJson.decodeFromString<List<ConnectedFriendRecord>>(raw).map {
                ConnectedFriend(it.inviteCode, it.displayName, it.addedAtEpochMs, it.addedVia, it.userId)
            }
        } catch (_: SerializationException) {
            emptyList()
        } catch (_: IllegalArgumentException) {
            emptyList()
        }
    }

    suspend fun saveFriends(scopeKey: String, items: List<ConnectedFriend>) {
        if (scopeKey == GUEST_FRIENDS_SCOPE || dao == null) return
        withContext(ioDispatcher) {
            try {
                val payload = friendsJson.encodeToString(
                    items.map { ConnectedFriendRecord(it.inviteCode, it.displayName, it.addedAtEpochMs, it.addedVia, it.userId) }
                )
                dao.upsert(UserPreferenceEntity("$KEY_FRIENDS_PREFIX$scopeKey", payload, System.currentTimeMillis()))
            } catch (_: SerializationException) {
                // Evita falla si ocurre error al serializar caché local.
            } catch (_: IllegalStateException) {
                // Base cifrada cerrándose durante logout/teardown.
            }
        }
    }

    suspend fun filterUnseenAndMarkNotified(scopeKey: String, pending: List<PendingFriendRequest>): PendingFriendRequest? {
        if (pending.isEmpty() || scopeKey == GUEST_FRIENDS_SCOPE) return null
        val seen = loadSeenIds(scopeKey)
        val firstUnseen = pending.firstOrNull { it.requestId !in seen } ?: return null
        val updatedSeen = (seen + pending.map { it.requestId }).toList().takeLast(MAX_SEEN_IDS)
        saveSeenIds(scopeKey, updatedSeen)
        return firstUnseen
    }

    private suspend fun loadSeenIds(scopeKey: String): Set<String> {
        val localDao = dao ?: return emptySet()
        val raw = withContext(ioDispatcher) {
            try {
                localDao.getByKey("$KEY_NOTIFIED_PREFIX$scopeKey")?.prefValue
            } catch (_: IllegalStateException) {
                null
            }
        } ?: return emptySet()
        return try {
            friendsJson.decodeFromString<List<String>>(raw).toSet()
        } catch (_: SerializationException) {
            emptySet()
        } catch (_: IllegalArgumentException) {
            emptySet()
        }
    }

    private suspend fun saveSeenIds(scopeKey: String, ids: List<String>) {
        val localDao = dao ?: return
        withContext(ioDispatcher) {
            try {
                localDao.upsert(
                    UserPreferenceEntity(
                        "$KEY_NOTIFIED_PREFIX$scopeKey",
                        friendsJson.encodeToString(ids),
                        System.currentTimeMillis()
                    )
                )
            } catch (_: SerializationException) {
                // Evita falla al persistir IDs vistos.
            } catch (_: IllegalStateException) {
                // Ignora cierre de base cifrada.
            }
        }
    }
}

internal fun resolveFriendsScopeKey(isGuest: Boolean, email: String?): String = when {
    isGuest -> GUEST_FRIENDS_SCOPE
    !email.isNullOrBlank() -> email.trim().lowercase()
    else -> DEFAULT_FRIENDS_SCOPE
}

internal fun FriendItemDto.toDomain(): ConnectedFriend = ConnectedFriend(
    inviteCode = inviteCode,
    displayName = displayName.ifBlank { "Vecino ($inviteCode)" },
    addedAtEpochMs = parseEpochMs(connectedAtUtc),
    addedVia = addedVia,
    userId = userId
)

internal fun PendingFriendRequestDto.toDomain(): PendingFriendRequest = PendingFriendRequest(
    requestId = requestId,
    senderInviteCode = senderInviteCode,
    senderName = senderName.ifBlank { "Vecino ($senderInviteCode)" },
    addedVia = addedVia,
    createdAtEpochMs = parseEpochMs(createdAtUtc)
)

internal fun SentFriendRequestDto.toDomain(): SentFriendRequest = SentFriendRequest(
    requestId = requestId,
    targetInviteCode = targetInviteCode,
    targetName = targetName.ifBlank { "Vecino ($targetInviteCode)" },
    addedVia = addedVia,
    createdAtEpochMs = parseEpochMs(createdAtUtc)
)

internal fun FriendItemDto?.toConnectedFriendOrDefault(code: String, fallbackName: String?): ConnectedFriend = this?.toDomain() ?: ConnectedFriend(
    inviteCode = code,
    displayName = fallbackName?.takeIf { it.isNotBlank() } ?: "Vecino ($code)",
    addedAtEpochMs = System.currentTimeMillis(),
    addedVia = "QR"
)

internal fun SentFriendRequestDto?.toSentRequestOrDefault(code: String, fallbackName: String?): SentFriendRequest = this?.toDomain() ?: SentFriendRequest(
    requestId = "",
    targetInviteCode = code,
    targetName = fallbackName?.takeIf { it.isNotBlank() } ?: "Vecino ($code)",
    addedVia = "QR",
    createdAtEpochMs = System.currentTimeMillis()
)

private fun parseEpochMs(isoUtc: String): Long = try {
    if (isoUtc.isBlank()) System.currentTimeMillis() else Instant.parse(isoUtc).toEpochMilli()
} catch (_: java.time.format.DateTimeParseException) {
    System.currentTimeMillis()
}
