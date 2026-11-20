package com.example.sensus.domain.usecase

import com.example.sensus.common.Result
import com.example.sensus.domain.model.CommunityActivityItem
import com.example.sensus.domain.repository.FriendsActivityRepository

class GetFriendsCommunityActivityUseCase(private val repository: FriendsActivityRepository) {
    suspend operator fun invoke(limit: Int = FriendsActivityRepository.DEFAULT_ACTIVITY_LIMIT): Result<List<CommunityActivityItem>> =
        repository.getFriendsActivity(limit)
}
