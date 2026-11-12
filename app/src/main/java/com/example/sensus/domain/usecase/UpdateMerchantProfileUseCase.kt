package com.example.sensus.domain.usecase

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.HorarioDia
import com.example.sensus.domain.model.MerchantProfile
import com.example.sensus.domain.repository.MerchantRepository

private const val MIN_ADDRESS_LENGTH = 5
private const val MAX_ADDRESS_LENGTH = 250

class UpdateMerchantProfileUseCase(private val merchantRepository: MerchantRepository) {
    suspend operator fun invoke(nombreComercial: String?, direccion: String, codigoPostal: String, horarios: List<HorarioDia>): Result<MerchantProfile> {
        val trimmedDireccion = direccion.trim()
        val trimmedCp = codigoPostal.trim()

        val error = when {
            trimmedDireccion.length !in MIN_ADDRESS_LENGTH..MAX_ADDRESS_LENGTH ->
                "La dirección debe contener entre $MIN_ADDRESS_LENGTH y $MAX_ADDRESS_LENGTH caracteres."
            !trimmedCp.matches(Regex("^\\d{5}$")) ->
                "El código postal debe tener exactamente 5 dígitos numéricos."
            else -> null
        }
        return if (error != null) {
            Result.Failure(AppError.Validation(error))
        } else {
            merchantRepository.updateMerchantProfile(
                nombreComercial = nombreComercial?.trim(),
                direccion = trimmedDireccion,
                codigoPostal = trimmedCp,
                horarios = horarios
            )
        }
    }
}
