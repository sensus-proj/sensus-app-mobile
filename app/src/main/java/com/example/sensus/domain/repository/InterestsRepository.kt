package com.example.sensus.domain.repository

import com.example.sensus.common.Result

interface InterestsRepository {
    suspend fun getMyInterests(): Result<List<String>>
    suspend fun updateMyInterests(categories: List<String>): Result<List<String>>
}
