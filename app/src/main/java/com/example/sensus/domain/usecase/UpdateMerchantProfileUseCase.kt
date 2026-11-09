package com.example.sensus.domain.usecase

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.HorarioDia
import com.example.sensus.domain.model.MerchantProfile
import com.example.sensus.domain.repository.MerchantRepository

class UpdateMerchantProfileUseCase(private val merchantRepository: MerchantRepository) {
    suspend operator fun invoke(
        nombreComercial: String?,
        direccion: String,
        codigoPostal: String,
        horarios: List<HorarioDia>
    ): Result<MerchantProfile> {
        val trimmedDireccion = direccion.trim()
        val trimmedCp = codigoPostal.trim()

        if (trimmedDireccion.length !in 5..250) {
            return Result.Failure(AppError.Validation("La dirección debe contener entre 5 y 250 caracteres."))
        }

        if (!trimmedCp.matches(Regex("^\\d{5}$"))) {
            return Result.Failure(AppError.Validation("El código postal debe tener exactamente 5 dígitos numéricos."))
        }

        return merchantRepository.updateMerchantProfile(
            nombreComercial = nombreComercial?.trim(),
            direccion = trimmedDireccion,
            codigoPostal = trimmedCp,
            horarios = horarios
        )
    }
}
