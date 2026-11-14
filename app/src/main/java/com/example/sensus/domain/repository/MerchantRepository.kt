package com.example.sensus.domain.repository

import com.example.sensus.common.Result
import com.example.sensus.domain.model.HorarioDia
import com.example.sensus.domain.model.MerchantCoordinates
import com.example.sensus.domain.model.MerchantProfile

interface MerchantRepository {
    suspend fun getMyMerchant(): Result<MerchantProfile>

    suspend fun activateMerchant(
        nombreComercial: String,
        direccion: String,
        codigoPostal: String,
        latitud: Double? = null,
        longitud: Double? = null
    ): Result<MerchantProfile>

    suspend fun updateMerchantProfile(nombreComercial: String?, direccion: String, codigoPostal: String, horarios: List<HorarioDia>): Result<MerchantProfile>

    suspend fun updateMerchantProfile(
        nombreComercial: String?,
        direccion: String,
        codigoPostal: String,
        horarios: List<HorarioDia>,
        coordinates: MerchantCoordinates?
    ): Result<MerchantProfile> = updateMerchantProfile(nombreComercial, direccion, codigoPostal, horarios)

    suspend fun getNearbyMerchants(latitude: Double, longitude: Double, radiusMeters: Int): Result<List<MerchantProfile>> = Result.Success(emptyList())
}
