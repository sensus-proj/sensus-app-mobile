package com.example.sensus.domain.usecase

import com.example.sensus.common.Result
import com.example.sensus.domain.model.MerchantProfile
import com.example.sensus.domain.repository.MerchantRepository

class GetMyMerchantUseCase(private val merchantRepository: MerchantRepository) {
    suspend operator fun invoke(): Result<MerchantProfile> {
        return merchantRepository.getMyMerchant()
    }
}
