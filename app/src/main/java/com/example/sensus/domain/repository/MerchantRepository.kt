package com.example.sensus.domain.repository

import com.example.sensus.common.Result
import com.example.sensus.domain.model.HorarioDia
import com.example.sensus.domain.model.MerchantActivationInput
import com.example.sensus.domain.model.MerchantCoordinates
import com.example.sensus.domain.model.MerchantProfile
import com.example.sensus.domain.model.MerchantProfileUpdateInput

interface MerchantRepository {
    suspend fun getMyMerchant(): Result<MerchantProfile>

    suspend fun activateMerchant(
        nombreComercial: String,
        direccion: String,
        codigoPostal: String,
        latitud: Double? = null,
        longitud: Double? = null
    ): Result<MerchantProfile>

    suspend fun activateMerchant(input: MerchantActivationInput): Result<MerchantProfile> = activateMerchant(
        input.nombreComercial,
        input.direccion,
        input.codigoPostal,
        input.coordinates?.latitude,
        input.coordinates?.longitude
    )

    suspend fun updateMerchantProfile(nombreComercial: String?, direccion: String, codigoPostal: String, horarios: List<HorarioDia>): Result<MerchantProfile>

    suspend fun updateMerchantProfile(
        nombreComercial: String?,
        direccion: String,
        codigoPostal: String,
        horarios: List<HorarioDia>,
        coordinates: MerchantCoordinates?
    ): Result<MerchantProfile> = updateMerchantProfile(nombreComercial, direccion, codigoPostal, horarios)

    suspend fun updateMerchantProfile(input: MerchantProfileUpdateInput): Result<MerchantProfile> = if (input.coordinates == null) {
        updateMerchantProfile(input.nombreComercial, input.direccion, input.codigoPostal, input.horarios)
    } else {
        updateMerchantProfile(input.nombreComercial, input.direccion, input.codigoPostal, input.horarios, input.coordinates)
    }

    suspend fun getNearbyMerchants(latitude: Double, longitude: Double, radiusMeters: Int): Result<List<MerchantProfile>> = Result.Success(emptyList())
}
