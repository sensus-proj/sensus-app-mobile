package com.example.sensus.data.remote.api

import com.example.sensus.data.remote.dto.FriendItemDto
import com.example.sensus.data.remote.dto.FriendSharedOffersThreadDto
import com.example.sensus.data.remote.dto.FriendsOverviewDto
import com.example.sensus.data.remote.dto.PagedCommunityActivityResponseDto
import com.example.sensus.data.remote.dto.SendFriendRequestBody
import com.example.sensus.data.remote.dto.SendFriendRequestResponseDto
import com.example.sensus.data.remote.dto.ShareOfferRequestBody
import com.example.sensus.data.remote.dto.ShareOfferResultDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface FriendsApiService {
    @GET("api/v1/friends")
    suspend fun getFriendsOverview(): Response<FriendsOverviewDto>

    @POST("api/v1/friends/requests")
    suspend fun sendFriendRequest(@Body body: SendFriendRequestBody): Response<SendFriendRequestResponseDto>

    @POST("api/v1/friends/requests/{requestId}/accept")
    suspend fun acceptFriendRequest(@Path("requestId") requestId: String): Response<FriendItemDto>

    @POST("api/v1/friends/requests/{requestId}/reject")
    suspend fun rejectFriendRequest(@Path("requestId") requestId: String): Response<Unit>

    @DELETE("api/v1/friends/{friendUserId}")
    suspend fun removeFriend(@Path("friendUserId") friendUserId: String): Response<Unit>

    @GET("api/v1/friends/{friendUserId}/shared-offers")
    suspend fun getFriendSharedOffers(@Path("friendUserId") friendUserId: String): Response<FriendSharedOffersThreadDto>

    @POST("api/v1/offers/{offerId}/share")
    suspend fun shareOfferWithFriends(@Path("offerId") offerId: String, @Body body: ShareOfferRequestBody): Response<ShareOfferResultDto>

    @GET("api/v1/community/friends-activity")
    suspend fun getFriendsActivity(
        @Query("limit") limit: Int = 50,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = limit
    ): Response<PagedCommunityActivityResponseDto>
}
