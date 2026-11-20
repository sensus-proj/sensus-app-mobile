package com.example.sensus.domain.usecase

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.HorarioDia
import com.example.sensus.domain.model.MerchantCoordinates
import com.example.sensus.domain.model.MerchantProfile
import com.example.sensus.domain.model.MerchantProfileUpdateInput
import com.example.sensus.domain.moderation.ProfanityFilter
import com.example.sensus.domain.repository.MerchantRepository

private const val MIN_ADDRESS_LENGTH = 5
private const val MAX_ADDRESS_LENGTH = 250

class UpdateMerchantProfileUseCase(private val merchantRepository: MerchantRepository) {
    suspend operator fun invoke(
        nombreComercial: String?,
        direccion: String,
        codigoPostal: String,
        horarios: List<HorarioDia>,
        coordinates: MerchantCoordinates? = null
    ): Result<MerchantProfile> {
        val trimmedDireccion = direccion.trim()
        val trimmedCp = codigoPostal.trim()
        val error = validateFields(nombreComercial?.trim(), trimmedDireccion, trimmedCp)
        return when {
            error != null -> Result.Failure(AppError.Validation(error))
            coordinates == null -> merchantRepository.updateMerchantProfile(
                nombreComercial = nombreComercial?.trim(),
                direccion = trimmedDireccion,
                codigoPostal = trimmedCp,
                horarios = horarios
            )
            else -> merchantRepository.updateMerchantProfile(
                nombreComercial = nombreComercial?.trim(),
                direccion = trimmedDireccion,
                codigoPostal = trimmedCp,
                horarios = horarios,
                coordinates = coordinates
            )
        }
    }

    suspend operator fun invoke(input: MerchantProfileUpdateInput): Result<MerchantProfile> {
        val trimmedDireccion = input.direccion.trim()
        val trimmedCp = input.codigoPostal.trim()
        val error = validateFields(input.nombreComercial?.trim(), trimmedDireccion, trimmedCp)
        return if (error != null) {
            Result.Failure(AppError.Validation(error))
        } else {
            merchantRepository.updateMerchantProfile(
                input.copy(
                    nombreComercial = input.nombreComercial?.trim(),
                    direccion = trimmedDireccion,
                    codigoPostal = trimmedCp,
                    fotoFachadaUrl = input.fotoFachadaUrl?.trim()
                )
            )
        }
    }

    private fun validateFields(trimmedNombre: String?, trimmedDireccion: String, trimmedCp: String): String? = when {
        ProfanityFilter.containsProfanity(trimmedNombre) ->
            "El nombre del comercio contiene lenguaje ofensivo o inapropiado."
        trimmedDireccion.length !in MIN_ADDRESS_LENGTH..MAX_ADDRESS_LENGTH ->
            "La dirección debe contener entre $MIN_ADDRESS_LENGTH y $MAX_ADDRESS_LENGTH caracteres."
        ProfanityFilter.containsProfanity(trimmedDireccion) ->
            "La dirección contiene lenguaje ofensivo o inapropiado."
        !trimmedCp.matches(Regex("^\\d{5}$")) ->
            "El código postal debe tener exactamente 5 dígitos numéricos."
        else -> null
    }
}
