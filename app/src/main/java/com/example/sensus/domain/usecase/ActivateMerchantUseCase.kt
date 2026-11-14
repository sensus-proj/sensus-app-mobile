package com.example.sensus.domain.usecase

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.GeoCoordinatePolicy
import com.example.sensus.domain.model.MerchantProfile
import com.example.sensus.domain.repository.MerchantRepository

private const val MIN_STORE_NAME_LENGTH = 2
private const val MAX_STORE_NAME_LENGTH = 150
private const val MIN_ADDRESS_LENGTH = 5
private const val MAX_ADDRESS_LENGTH = 250
private val POSTAL_CODE_REGEX = Regex("^[0-9a-zA-Z\\s\\-]{4,10}$")

class ActivateMerchantUseCase(private val merchantRepository: MerchantRepository) {

    suspend operator fun invoke(
        nombreComercial: String,
        direccion: String,
        codigoPostal: String,
        latitud: Double? = null,
        longitud: Double? = null
    ): Result<MerchantProfile> {
        val trimmedName = nombreComercial.trim()
        val trimmedAddress = direccion.trim()
        val trimmedPostalCode = codigoPostal.trim()

        val validationError = validateInputs(trimmedName, trimmedAddress, trimmedPostalCode, latitud, longitud)
        return if (validationError != null) {
            Result.Failure(AppError.Validation(validationError))
        } else {
            merchantRepository.activateMerchant(
                nombreComercial = trimmedName,
                direccion = trimmedAddress,
                codigoPostal = trimmedPostalCode,
                latitud = latitud,
                longitud = longitud
            )
        }
    }

    private fun validateInputs(name: String, address: String, postalCode: String, latitud: Double?, longitud: Double?): String? {
        val fieldError = when {
            name.length !in MIN_STORE_NAME_LENGTH..MAX_STORE_NAME_LENGTH ->
                "El nombre comercial debe tener entre $MIN_STORE_NAME_LENGTH y $MAX_STORE_NAME_LENGTH caracteres."
            address.length !in MIN_ADDRESS_LENGTH..MAX_ADDRESS_LENGTH ->
                "La dirección debe contener entre $MIN_ADDRESS_LENGTH y $MAX_ADDRESS_LENGTH caracteres."
            !POSTAL_CODE_REGEX.matches(postalCode) ->
                "El código postal debe tener entre 4 y 10 caracteres alfanuméricos."
            else -> null
        }
        return fieldError ?: GeoCoordinatePolicy.validatePair(latitud, longitud)
    }
}
