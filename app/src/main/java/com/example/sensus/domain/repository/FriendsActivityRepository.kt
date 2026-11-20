package com.example.sensus.domain.repository

import com.example.sensus.common.Result
import com.example.sensus.domain.model.CommunityActivityItem

interface FriendsActivityRepository {
    suspend fun getFriendsActivity(limit: Int = DEFAULT_ACTIVITY_LIMIT): Result<List<CommunityActivityItem>>

    companion object {
        const val DEFAULT_ACTIVITY_LIMIT = 50
    }
}
