package com.example.sensus.data.repository

import com.example.sensus.data.local.db.dao.UserPreferenceDao
import com.example.sensus.data.local.db.entity.UserPreferenceEntity
import com.example.sensus.domain.model.FeedPost
import com.example.sensus.domain.model.OfferLifecycleStatus
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.repository.SavedOffersRepository
import com.example.sensus.domain.repository.SessionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val KEY_OFFERS_PREFIX = "saved_interested_offers_"
private const val KEY_POSTS_PREFIX = "saved_interested_posts_"
private const val DEFAULT_SCOPE_KEY = "default"
private const val GUEST_SCOPE_KEY = "guest"
private val savedJson = Json { ignoreUnknownKeys = true }

@Serializable
private data class SavedOfferRecord(
    val id: String,
    val name: String,
    val storeName: String,
    val category: String,
    val originalPrice: Double,
    val discountPrice: Double,
    val discountPercent: Int,
    val unitsLeft: Int,
    val distanceMeters: Int,
    val timeToCollectMinutes: Int,
    val rating: Double,
    val reviewCount: Int,
    val address: String,
    val couponCode: String,
    val isVerified: Boolean,
    val latitude: Double,
    val longitude: Double,
    val postalCode: String = "",
    val description: String = "",
    val expirationDate: String = "",
    val imageUrl: String? = null,
    val merchantId: String? = null,
    val statusName: String = OfferLifecycleStatus.ACTIVE.name,
    val version: Int = 1,
    val expiresAtMillis: Long? = null
)

@Serializable
private data class SavedPostRecord(
    val id: String,
    val authorName: String,
    val text: String,
    val createdAt: String,
    val distanceMeters: Int = 0,
    val imageUrl: String? = null,
    val merchantId: String? = null,
    val merchantName: String? = null
)

/**
 * Persiste ofertas y publicaciones marcadas como "Me interesa" por el usuario
 * usando la tabla cifrada `user_preferences` en Room/SQLCipher.
 */
class SavedOffersRepositoryImpl(
    private val userPreferenceDao: UserPreferenceDao? = null,
    private val sessionRepository: SessionRepository? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + ioDispatcher)
) : SavedOffersRepository {

    private val lock = Any()
    private val _savedOffers = MutableStateFlow<List<ProductOffer>>(emptyList())
    override val savedOffers: StateFlow<List<ProductOffer>> = _savedOffers.asStateFlow()

    private val _savedPosts = MutableStateFlow<List<FeedPost>>(emptyList())
    override val savedPosts: StateFlow<List<FeedPost>> = _savedPosts.asStateFlow()

    init {
        if (sessionRepository != null) {
            scope.launch {
                sessionRepository.sessionState
                    .map { resolveScopeKey(it.isGuest, it.activeEmail) }
                    .distinctUntilChanged()
                    .collect { scopeKey -> loadForScope(scopeKey) }
            }
        } else if (userPreferenceDao != null) {
            scope.launch { loadForScope(DEFAULT_SCOPE_KEY) }
        }
    }

    override fun isOfferSaved(offerId: String): Boolean = _savedOffers.value.any { it.id == offerId }

    override fun isPostSaved(postId: String): Boolean = _savedPosts.value.any { it.id == postId }

    override suspend fun toggleSavedOffer(offer: ProductOffer): Boolean {
        val current = _savedOffers.value
        val alreadySaved = current.any { it.id == offer.id }
        val updated = if (alreadySaved) {
            current.filterNot { it.id == offer.id }
        } else {
            listOf(offer) + current.filterNot { it.id == offer.id }
        }
        synchronized(lock) { _savedOffers.value = updated }
        persistJson("$KEY_OFFERS_PREFIX${currentScopeKey()}") { savedJson.encodeToString(updated.map { it.toRecord() }) }
        return !alreadySaved
    }

    override suspend fun toggleSavedPost(post: FeedPost): Boolean {
        val current = _savedPosts.value
        val alreadySaved = current.any { it.id == post.id }
        val updated = if (alreadySaved) {
            current.filterNot { it.id == post.id }
        } else {
            listOf(post) + current.filterNot { it.id == post.id }
        }
        synchronized(lock) { _savedPosts.value = updated }
        persistJson("$KEY_POSTS_PREFIX${currentScopeKey()}") { savedJson.encodeToString(updated.map { it.toRecord() }) }
        return !alreadySaved
    }

    override suspend fun removeSavedOffer(offerId: String) {
        val updated = _savedOffers.value.filterNot { it.id == offerId }
        synchronized(lock) { _savedOffers.value = updated }
        persistJson("$KEY_OFFERS_PREFIX${currentScopeKey()}") { savedJson.encodeToString(updated.map { it.toRecord() }) }
    }

    override suspend fun removeSavedPost(postId: String) {
        val updated = _savedPosts.value.filterNot { it.id == postId }
        synchronized(lock) { _savedPosts.value = updated }
        persistJson("$KEY_POSTS_PREFIX${currentScopeKey()}") { savedJson.encodeToString(updated.map { it.toRecord() }) }
    }

    private fun currentScopeKey(): String {
        val session = sessionRepository?.sessionState?.value
        return if (session == null) DEFAULT_SCOPE_KEY else resolveScopeKey(session.isGuest, session.activeEmail)
    }

    private suspend fun loadForScope(scopeKey: String) {
        if (scopeKey == GUEST_SCOPE_KEY) {
            synchronized(lock) {
                _savedOffers.value = emptyList()
                _savedPosts.value = emptyList()
            }
            return
        }
        val dao = userPreferenceDao ?: return
        val loadedOffers = readRawJson(dao, "$KEY_OFFERS_PREFIX$scopeKey")?.let(::decodeOffers).orEmpty()
        val loadedPosts = readRawJson(dao, "$KEY_POSTS_PREFIX$scopeKey")?.let(::decodePosts).orEmpty()
        synchronized(lock) {
            _savedOffers.value = loadedOffers
            _savedPosts.value = loadedPosts
        }
    }

    private suspend fun readRawJson(dao: UserPreferenceDao, key: String): String? = withContext(ioDispatcher) {
        try {
            dao.getByKey(key)?.prefValue
        } catch (_: IllegalStateException) {
            null
        }
    }

    private suspend fun persistJson(key: String, encode: () -> String) {
        if (currentScopeKey() == GUEST_SCOPE_KEY) return
        val dao = userPreferenceDao ?: return
        withContext(ioDispatcher) {
            try {
                dao.upsert(UserPreferenceEntity(key, encode(), System.currentTimeMillis()))
            } catch (_: SerializationException) {
                // Evita falla si un campo transitorio no puede serializarse.
            } catch (_: IllegalStateException) {
                // Degradación segura si la base cifrada está cerrándose.
            }
        }
    }
}

private fun resolveScopeKey(isGuest: Boolean, userEmail: String?): String = when {
    isGuest -> GUEST_SCOPE_KEY
    !userEmail.isNullOrBlank() -> userEmail.trim().lowercase()
    else -> DEFAULT_SCOPE_KEY
}

private fun decodeOffers(raw: String): List<ProductOffer> = try {
    savedJson.decodeFromString<List<SavedOfferRecord>>(raw).map { it.toDomain() }
} catch (_: SerializationException) {
    emptyList()
} catch (_: IllegalArgumentException) {
    emptyList()
}

private fun decodePosts(raw: String): List<FeedPost> = try {
    savedJson.decodeFromString<List<SavedPostRecord>>(raw).map { it.toDomain() }
} catch (_: SerializationException) {
    emptyList()
} catch (_: IllegalArgumentException) {
    emptyList()
}

private fun ProductOffer.toRecord(): SavedOfferRecord = SavedOfferRecord(
    id = id,
    name = name,
    storeName = storeName,
    category = category,
    originalPrice = originalPrice,
    discountPrice = discountPrice,
    discountPercent = discountPercent,
    unitsLeft = unitsLeft,
    distanceMeters = distanceMeters,
    timeToCollectMinutes = timeToCollectMinutes,
    rating = rating,
    reviewCount = reviewCount,
    address = address,
    couponCode = couponCode,
    isVerified = isVerified,
    latitude = latitude,
    longitude = longitude,
    postalCode = postalCode,
    description = description,
    expirationDate = expirationDate,
    imageUrl = imageUrl,
    merchantId = merchantId,
    statusName = status.name,
    version = version,
    expiresAtMillis = expiresAtMillis
)

private fun SavedOfferRecord.toDomain(): ProductOffer {
    val resolvedStatus = try {
        OfferLifecycleStatus.valueOf(statusName)
    } catch (_: IllegalArgumentException) {
        OfferLifecycleStatus.ACTIVE
    }
    return ProductOffer(
        id = id,
        name = name,
        storeName = storeName,
        category = category,
        originalPrice = originalPrice,
        discountPrice = discountPrice,
        discountPercent = discountPercent,
        unitsLeft = unitsLeft,
        distanceMeters = distanceMeters,
        timeToCollectMinutes = timeToCollectMinutes,
        rating = rating,
        reviewCount = reviewCount,
        address = address,
        couponCode = couponCode,
        isVerified = isVerified,
        latitude = latitude,
        longitude = longitude,
        postalCode = postalCode,
        description = description,
        expirationDate = expirationDate,
        imageUrl = imageUrl,
        merchantId = merchantId,
        status = resolvedStatus,
        version = version,
        expiresAtMillis = expiresAtMillis
    )
}

private fun FeedPost.toRecord(): SavedPostRecord = SavedPostRecord(
    id = id,
    authorName = authorName,
    text = text,
    createdAt = createdAt,
    distanceMeters = distanceMeters,
    imageUrl = imageUrl,
    merchantId = merchantId,
    merchantName = merchantName
)

private fun SavedPostRecord.toDomain(): FeedPost = FeedPost(
    id = id,
    authorName = authorName,
    text = text,
    createdAt = createdAt,
    distanceMeters = distanceMeters,
    imageUrl = imageUrl,
    merchantId = merchantId,
    merchantName = merchantName
)
