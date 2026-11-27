package com.example.sensus.data.remote.api

import com.example.sensus.data.remote.dto.CreateFeedCommentRequest
import com.example.sensus.data.remote.dto.CreateFeedPostRequest
import com.example.sensus.data.remote.dto.FeedCommentDto
import com.example.sensus.data.remote.dto.FeedPostDto
import com.example.sensus.data.remote.dto.PagedFeedCommentsDto
import com.example.sensus.data.remote.dto.PagedFeedDto
import com.example.sensus.data.remote.dto.SetFeedPostLikeRequest
import com.example.sensus.data.remote.dto.TogglePostLikeDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface FeedApiService {
    @GET("api/v1/feed/nearby")
    suspend fun nearby(
        @Query("lat") latitude: Double,
        @Query("lng") longitude: Double,
        @Query("radiusKm") radiusKm: Double,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int
    ): Response<PagedFeedDto>

    @POST("api/v1/feed/posts")
    suspend fun createPost(@Body request: CreateFeedPostRequest): Response<FeedPostDto>

    @POST("api/v1/feed/posts/{id}/like")
    suspend fun toggleLike(@Path("id") postId: String, @Body request: SetFeedPostLikeRequest = SetFeedPostLikeRequest(true)): Response<TogglePostLikeDto>

    @GET("api/v1/feed/posts/{id}/comments")
    suspend fun getComments(@Path("id") postId: String, @Query("page") page: Int, @Query("pageSize") pageSize: Int): Response<PagedFeedCommentsDto>

    @POST("api/v1/feed/posts/{id}/comments")
    suspend fun addComment(@Path("id") postId: String, @Body request: CreateFeedCommentRequest): Response<FeedCommentDto>

    @DELETE("api/v1/feed/posts/{id}")
    suspend fun deletePost(@Path("id") postId: String): Response<Unit>
}
