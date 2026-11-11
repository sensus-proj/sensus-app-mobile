package com.example.sensus.domain.repository

import com.example.sensus.common.Result
import com.example.sensus.domain.model.HorarioDia
import com.example.sensus.domain.model.MerchantProfile

interface MerchantRepository {
    suspend fun getMyMerchant(): Result<MerchantProfile>

    suspend fun updateMerchantProfile(nombreComercial: String?, direccion: String, codigoPostal: String, horarios: List<HorarioDia>): Result<MerchantProfile>
}
