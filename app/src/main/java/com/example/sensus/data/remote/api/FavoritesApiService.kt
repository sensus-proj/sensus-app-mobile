package com.example.sensus.data.remote.api

import com.example.sensus.data.remote.dto.FavoritePageDto
import com.example.sensus.data.remote.dto.FavoriteToggleResultDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface FavoritesApiService {
    @GET("api/v1/users/favorites")
    suspend fun getFavorites(@Query("page") page: Int = 1, @Query("pageSize") pageSize: Int = 20): Response<FavoritePageDto>

    @POST("api/v1/users/favorites/{offerId}")
    suspend fun toggleFavorite(@Path("offerId") offerId: String): Response<FavoriteToggleResultDto>
}
