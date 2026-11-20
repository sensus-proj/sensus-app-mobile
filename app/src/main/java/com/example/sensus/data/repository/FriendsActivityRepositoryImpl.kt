package com.example.sensus.data.repository

import com.example.sensus.common.Result
import com.example.sensus.data.remote.api.FriendsApiService
import com.example.sensus.data.remote.dto.CommunityActivityItemDto
import com.example.sensus.domain.model.CommunityActivityItem
import com.example.sensus.domain.model.CommunityActivityType
import com.example.sensus.domain.repository.FriendsActivityRepository

class FriendsActivityRepositoryImpl(private val api: FriendsApiService) : FriendsActivityRepository {

    override suspend fun getFriendsActivity(limit: Int): Result<List<CommunityActivityItem>> =
        safeNetworkCall("No se pudo cargar la actividad de tus amigos.") {
            val response = api.getFriendsActivity(limit)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.Success(body.mapNotNull { it.toDomainOrNull() })
            } else {
                Result.Failure(
                    mapCommonHttpStatus(
                        response.code(),
                        parseBackendErrorMessage(response.errorBody()?.string()),
                        "Inicia sesión para ver los comentarios y me gusta de tus amigos.",
                        "No se encontró actividad reciente."
                    )
                )
            }
        }

    private fun CommunityActivityItemDto.toDomainOrNull(): CommunityActivityItem? {
        val type = when (activityType) {
            "OfferLike" -> CommunityActivityType.OFFER_LIKE
            "StoreReview" -> CommunityActivityType.STORE_REVIEW
            "ReviewReply" -> CommunityActivityType.REVIEW_REPLY
            else -> return null
        }
        return CommunityActivityItem(
            id = id,
            type = type,
            actorUserId = actorUserId,
            actorName = actorName,
            actorPhotoUrl = actorPhotoUrl,
            offerId = offerId,
            merchantId = merchantId,
            targetTitle = targetTitle,
            targetSubtitle = targetSubtitle,
            commentText = commentText,
            rating = rating,
            createdAtUtc = createdAtUtc
        )
    }
}
