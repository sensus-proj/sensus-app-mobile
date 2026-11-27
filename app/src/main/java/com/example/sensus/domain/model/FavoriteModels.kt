package com.example.sensus.domain.model

enum class FavoriteFilter {
    ALL,
    AVAILABLE_ONLY
}

data class FavoriteOffer(
    val offer: ProductOffer,
    val favoritedAtUtc: String = "",
    val likesCount: Int = 0
)

data class FavoriteToggleResult(
    val offerId: String,
    val favorited: Boolean,
    val favoriteCount: Int
)
