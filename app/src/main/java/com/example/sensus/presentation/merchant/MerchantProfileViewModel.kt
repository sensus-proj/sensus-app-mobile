package com.example.sensus.presentation.merchant

import androidx.lifecycle.viewModelScope
import com.example.sensus.common.Result
import com.example.sensus.domain.model.HorarioDia
import com.example.sensus.domain.model.MerchantProfile
import com.example.sensus.domain.repository.StoreRepository
import com.example.sensus.domain.usecase.GetMyMerchantUseCase
import com.example.sensus.domain.usecase.UpdateMerchantProfileUseCase
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class MerchantProfileViewModel @Inject constructor(
    private val storeRepository: StoreRepository,
    val updateMerchantProfileUseCase: UpdateMerchantProfileUseCase,
    private val getMyMerchantUseCase: GetMyMerchantUseCase
) : MviViewModel<MerchantProfileViewState, MerchantProfileViewIntent, MerchantProfileViewEffect>(
    MerchantProfileViewState()
) {

    private var fetchRealMerchantEnabled: Boolean = true

    constructor(
        storeRepository: StoreRepository,
        updateMerchantProfileUseCase: UpdateMerchantProfileUseCase
    ) : this(storeRepository, updateMerchantProfileUseCase, GetMyMerchantUseCase(NoOpProfileMerchantRepository)) {
        fetchRealMerchantEnabled = false
    }

    init {
        updateState { copy(store = storeRepository.getSampleStore()) }
        loadMerchantProfile()
    }

    override fun processIntent(intent: MerchantProfileViewIntent) {
        when (intent) {
            is MerchantProfileViewIntent.LoadMerchant -> loadMerchantProfile()
            is MerchantProfileViewIntent.UpdateStore -> updateState { copy(store = intent.store) }
        }
    }

    private fun loadMerchantProfile() {
        if (!fetchRealMerchantEnabled) return
        viewModelScope.launch {
            when (val res = getMyMerchantUseCase()) {
                is Result.Success -> applyLoadedMerchant(res.data)
                is Result.Failure -> Unit
            }
        }
    }

    private fun applyLoadedMerchant(profile: MerchantProfile) {
        updateState {
            val formattedSchedule = formatSchedulesSummary(profile.horarios).ifBlank { store.schedule }
            copy(
                store = store.copy(
                    name = profile.nombreComercial.ifBlank { store.name },
                    address = profile.direccion.ifBlank { store.address },
                    postalCode = profile.codigoPostal.ifBlank { store.postalCode },
                    latitude = profile.latitud,
                    longitude = profile.longitud,
                    schedule = formattedSchedule
                ),
                schedules = profile.horarios
            )
        }
    }
}

private fun formatSchedulesSummary(horarios: List<HorarioDia>): String {
    val openDays = horarios.filter { !it.estaCerrado && !it.horaApertura.isNullOrBlank() && !it.horaCierre.isNullOrBlank() }
    val first = openDays.firstOrNull() ?: return ""
    return "${first.dia}: ${first.horaApertura} - ${first.horaCierre}"
}

private object NoOpProfileMerchantRepository : com.example.sensus.domain.repository.MerchantRepository {
    override suspend fun getMyMerchant(): Result<MerchantProfile> = Result.Failure(com.example.sensus.common.AppError.NotFound("No configurado"))
    override suspend fun activateMerchant(
        nombreComercial: String,
        direccion: String,
        codigoPostal: String,
        latitud: Double?,
        longitud: Double?
    ): Result<MerchantProfile> = Result.Failure(com.example.sensus.common.AppError.NotFound("No configurado"))
    override suspend fun updateMerchantProfile(
        nombreComercial: String?,
        direccion: String,
        codigoPostal: String,
        horarios: List<HorarioDia>
    ): Result<MerchantProfile> = Result.Failure(com.example.sensus.common.AppError.NotFound("No configurado"))
}
