package com.example.sensus.data.remote.api

import com.example.sensus.data.remote.dto.FriendItemDto
import com.example.sensus.data.remote.dto.FriendsOverviewDto
import com.example.sensus.data.remote.dto.SendFriendRequestBody
import com.example.sensus.data.remote.dto.SendFriendRequestResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

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
}
