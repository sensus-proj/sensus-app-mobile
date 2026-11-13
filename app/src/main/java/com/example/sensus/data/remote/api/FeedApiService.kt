package com.example.sensus.data.remote.api

import com.example.sensus.data.remote.dto.CreateFeedPostRequest
import com.example.sensus.data.remote.dto.FeedPostDto
import com.example.sensus.data.remote.dto.PagedFeedDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface FeedApiService {
    @GET("api/v1/feed/nearby")
    suspend fun nearby(
        @Query("lat") latitude: Double,
        @Query("lng") longitude: Double,
        @Query("radiusMeters") radiusMeters: Int,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int
    ): Response<PagedFeedDto>

    @POST("api/v1/feed/posts")
    suspend fun createPost(@Body request: CreateFeedPostRequest): Response<FeedPostDto>
}
