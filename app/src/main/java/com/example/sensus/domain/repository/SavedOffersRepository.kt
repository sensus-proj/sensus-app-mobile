package com.example.sensus.domain.repository

import com.example.sensus.domain.model.FeedPost
import com.example.sensus.domain.model.ProductOffer
import kotlinx.coroutines.flow.StateFlow

interface SavedOffersRepository {
    val savedOffers: StateFlow<List<ProductOffer>>
    val savedPosts: StateFlow<List<FeedPost>>
    fun isOfferSaved(offerId: String): Boolean
    fun isPostSaved(postId: String): Boolean
    suspend fun toggleSavedOffer(offer: ProductOffer): Boolean
    suspend fun toggleSavedPost(post: FeedPost): Boolean
    suspend fun removeSavedOffer(offerId: String)
    suspend fun removeSavedPost(postId: String)
}
