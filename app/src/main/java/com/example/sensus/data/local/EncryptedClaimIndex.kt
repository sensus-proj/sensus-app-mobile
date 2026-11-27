package com.example.sensus.data.local

import androidx.security.crypto.EncryptedSharedPreferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private const val MAX_CLAIMS_PER_ACCOUNT = 50
private const val CLAIM_PREFIX = "claim:"
private const val HISTORY_PREFIX = "claim_history:"

@Serializable
private data class StoredClaimEntry(val offerId: String, val claimId: String)

class EncryptedClaimIndex internal constructor(private val encryptedPreferences: () -> EncryptedSharedPreferences?) {
    private val json = Json { ignoreUnknownKeys = true }

    fun rememberClaimId(account: String, offerId: String, claimId: String) {
        val normalizedAccount = account.trim()
        if (normalizedAccount.isBlank() || offerId.isBlank() || claimId.isBlank()) return
        val prefs = safePrefs() ?: return
        try {
            val history = readHistory(prefs, normalizedAccount).filterNot { it.claimId == claimId }
            val updated = listOf(StoredClaimEntry(offerId, claimId)) + history
            val kept = updated.take(MAX_CLAIMS_PER_ACCOUNT)
            val evicted = updated.drop(MAX_CLAIMS_PER_ACCOUNT)
            val editor = prefs.edit()
                .putString(claimKey(normalizedAccount, offerId), claimId)
                .putString(historyKey(normalizedAccount), encodeHistory(kept))
            evicted.forEach { old ->
                val key = claimKey(normalizedAccount, old.offerId)
                if (kept.none { it.offerId == old.offerId } && prefs.getString(key, null) == old.claimId) {
                    editor.remove(key)
                }
            }
            editor.apply()
        } catch (_: RuntimeException) {
            // A cache failure must not turn a confirmed server reservation into a retry.
        }
    }

    fun findClaimId(account: String, offerId: String): String? {
        val normalizedAccount = account.trim()
        var found: String? = null
        if (normalizedAccount.isNotBlank() && offerId.isNotBlank()) {
            found = try {
                safePrefs()?.getString(claimKey(normalizedAccount, offerId), null)
            } catch (_: RuntimeException) {
                null
            }
        }
        return found
    }

    fun getClaimIds(account: String): List<String> {
        val normalizedAccount = account.trim()
        val prefs = if (normalizedAccount.isBlank()) null else safePrefs()
        if (prefs == null) return emptyList()
        return try {
            val ordered = readHistory(prefs, normalizedAccount).map { it.claimId }.toMutableList()
            appendLegacyClaimIds(prefs, normalizedAccount, ordered)
            ordered.take(MAX_CLAIMS_PER_ACCOUNT)
        } catch (_: RuntimeException) {
            emptyList()
        }
    }

    private fun appendLegacyClaimIds(prefs: EncryptedSharedPreferences, account: String, ordered: MutableList<String>) {
        val legacySuffix = ":$account"
        prefs.all.forEach { (key, value) ->
            if (!key.startsWith(CLAIM_PREFIX) || !key.endsWith(legacySuffix)) return@forEach
            val id = value as? String
            if (!id.isNullOrBlank() && id !in ordered) {
                ordered.add(id)
            }
        }
    }

    fun removeClaimId(account: String, claimId: String) {
        val normalizedAccount = account.trim()
        if (normalizedAccount.isBlank() || claimId.isBlank()) return
        val prefs = safePrefs() ?: return
        try {
            val remaining = readHistory(prefs, normalizedAccount).filterNot { it.claimId == claimId }
            val editor = prefs.edit().putString(historyKey(normalizedAccount), encodeHistory(remaining))
            val legacySuffix = ":$normalizedAccount"
            prefs.all.forEach { (key, value) ->
                if (key.startsWith(CLAIM_PREFIX) && key.endsWith(legacySuffix) && value == claimId) {
                    editor.remove(key)
                }
            }
            editor.apply()
        } catch (_: RuntimeException) {
            // Best-effort pruning of stale claim identifiers.
        }
    }

    private fun safePrefs(): EncryptedSharedPreferences? = try {
        encryptedPreferences()
    } catch (_: RuntimeException) {
        null
    }

    private fun readHistory(prefs: EncryptedSharedPreferences, account: String): List<StoredClaimEntry> {
        val raw = prefs.getString(historyKey(account), null) ?: return emptyList()
        return try {
            json.decodeFromString(ListSerializer(StoredClaimEntry.serializer()), raw)
        } catch (_: SerializationException) {
            emptyList()
        } catch (_: IllegalArgumentException) {
            emptyList()
        }
    }

    private fun encodeHistory(entries: List<StoredClaimEntry>): String = json.encodeToString(ListSerializer(StoredClaimEntry.serializer()), entries)

    private fun claimKey(account: String, offerId: String): String = "$CLAIM_PREFIX$offerId:$account"
    private fun historyKey(account: String): String = "$HISTORY_PREFIX$account"
}
