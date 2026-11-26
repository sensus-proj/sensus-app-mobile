package com.example.sensus.presentation.merchant

import androidx.lifecycle.viewModelScope
import com.example.sensus.common.Result
import com.example.sensus.domain.media.ImageCompressor
import com.example.sensus.domain.model.HorarioDia
import com.example.sensus.domain.model.MerchantCoordinates
import com.example.sensus.domain.model.MerchantProfile
import com.example.sensus.domain.model.MerchantProfileUpdateInput
import com.example.sensus.domain.repository.MediaRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.StoreRepository
import com.example.sensus.domain.usecase.GetMyMerchantUseCase
import com.example.sensus.domain.usecase.UpdateMerchantProfileUseCase
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

private const val POSTAL_CODE_LENGTH = 5

@HiltViewModel
class MerchantProfileViewModel @Inject constructor(
    private val storeRepository: StoreRepository,
    val updateMerchantProfileUseCase: UpdateMerchantProfileUseCase,
    private val getMyMerchantUseCase: GetMyMerchantUseCase,
    private val mediaRepository: MediaRepository?,
    private val imageCompressor: ImageCompressor?,
    private val sessionRepository: SessionRepository? = null
) : MviViewModel<MerchantProfileViewState, MerchantProfileViewIntent, MerchantProfileViewEffect>(
    MerchantProfileViewState()
) {

    private var fetchRealMerchantEnabled: Boolean = true
    private var profileRequest: Deferred<Result<MerchantProfile>>? = null
    private var profileLoad: Job? = null

    constructor(
        storeRepository: StoreRepository,
        updateMerchantProfileUseCase: UpdateMerchantProfileUseCase
    ) : this(storeRepository, updateMerchantProfileUseCase, GetMyMerchantUseCase(NoOpProfileMerchantRepository), null, null, null) {
        fetchRealMerchantEnabled = false
    }

    constructor(
        storeRepository: StoreRepository,
        updateMerchantProfileUseCase: UpdateMerchantProfileUseCase,
        getMyMerchantUseCase: GetMyMerchantUseCase
    ) : this(storeRepository, updateMerchantProfileUseCase, getMyMerchantUseCase, null, null, null)

    init {
        val sampleStore = storeRepository.getSampleStore()
        val cachedOpen = sessionRepository?.sessionState?.value?.isStoreOpen ?: sampleStore.isOpen
        updateState { copy(store = sampleStore.copy(isOpen = cachedOpen)) }
        loadMerchantProfile()
    }

    override fun processIntent(intent: MerchantProfileViewIntent) {
        when (intent) {
            is MerchantProfileViewIntent.LoadMerchant -> loadMerchantProfile()
            is MerchantProfileViewIntent.UpdateStore -> updateState { copy(store = intent.store) }
            is MerchantProfileViewIntent.UpdateFacadePhoto -> updateFacadePhoto(intent.imageBytes)
            is MerchantProfileViewIntent.ToggleStoreOpen -> toggleStoreOpen(intent.isOpen)
            is MerchantProfileViewIntent.ClearMessages -> updateState { copy(errorMessage = null, statusMessage = null) }
        }
    }

    private fun toggleStoreOpen(isOpen: Boolean) {
        if (viewState.value.isUpdatingStatus) return
        val currentStore = viewState.value.store
        updateState {
            copy(
                store = store.copy(isOpen = isOpen),
                isUpdatingStatus = true,
                errorMessage = null,
                statusMessage = null
            )
        }
        viewModelScope.launch {
            val input = buildProfileUpdateInput(currentStore = currentStore, activo = isOpen)
            when (val res = updateMerchantProfileUseCase(input)) {
                is Result.Success -> applyToggledStoreOpen(res.data, isOpen)
                is Result.Failure -> updateState {
                    copy(
                        isUpdatingStatus = false,
                        store = store.copy(isOpen = currentStore.isOpen),
                        errorMessage = res.error.message
                    )
                }
            }
        }
    }

    private suspend fun buildProfileUpdateInput(
        currentStore: com.example.sensus.domain.model.MerchantStore,
        fotoFachadaUrl: String? = null,
        activo: Boolean? = null
    ): MerchantProfileUpdateInput {
        val loaded = (awaitMerchantProfile() as? Result.Success)?.data
        val addr = (loaded?.direccion ?: currentStore.address).removePrefix("Dirección: ").trim().ifBlank { "Dirección local" }
        val cp = (loaded?.codigoPostal ?: currentStore.postalCode).trim().takeIf { it.length == POSTAL_CODE_LENGTH } ?: "42800"
        return MerchantProfileUpdateInput(
            nombreComercial = loaded?.nombreComercial ?: currentStore.name.takeIf { it.isNotBlank() },
            direccion = addr,
            codigoPostal = cp,
            horarios = loaded?.horarios ?: viewState.value.schedules,
            coordinates = loaded?.let { MerchantCoordinates(it.latitud, it.longitud) },
            fotoFachadaUrl = fotoFachadaUrl,
            activo = activo
        )
    }

    private suspend fun applyToggledStoreOpen(profile: MerchantProfile, requestedOpen: Boolean) {
        val actualOpen = profile.activo
        sessionRepository?.setStoreOpen(actualOpen)
        val statusSaved = actualOpen == requestedOpen
        val message = if (actualOpen) {
            "Tienda abierta y visible en el mapa."
        } else {
            "Tienda cerrada. Ya no aparecerá en el mapa hasta volver a abrirla."
        }
        updateState {
            copy(
                isUpdatingStatus = false,
                store = store.copy(isOpen = actualOpen),
                statusMessage = if (statusSaved) message else null,
                errorMessage = if (statusSaved) null else "El servidor aún no tiene desplegado el cambio de estado de tienda."
            )
        }
    }

    private fun updateFacadePhoto(bytes: ByteArray) {
        if (viewState.value.isUpdatingPhoto) return
        val currentStore = viewState.value.store
        updateState { copy(isUpdatingPhoto = true, errorMessage = null, statusMessage = null) }
        viewModelScope.launch {
            val uploadResult = uploadMerchantFacadePhotoIfProvided(bytes, mediaRepository, imageCompressor)
            if (uploadResult is Result.Failure) {
                updateState { copy(isUpdatingPhoto = false, errorMessage = uploadResult.error.message) }
                return@launch
            }
            val uploadedUrl = (uploadResult as Result.Success).data
            if (uploadedUrl.isNullOrBlank()) {
                updateState { copy(isUpdatingPhoto = false, errorMessage = "No se pudo subir la foto de fachada.") }
                return@launch
            }
            val updateInput = buildProfileUpdateInput(currentStore = currentStore, fotoFachadaUrl = uploadedUrl)
            when (val res = updateMerchantProfileUseCase(updateInput)) {
                is Result.Success -> applyUpdatedFacadePhoto(res.data, uploadedUrl)
                is Result.Failure -> updateState { copy(isUpdatingPhoto = false, errorMessage = res.error.message) }
            }
        }
    }

    private fun applyUpdatedFacadePhoto(profile: MerchantProfile, fallbackUrl: String) {
        updateState {
            copy(
                isUpdatingPhoto = false,
                store = store.copy(photoUrl = profile.fotoFachadaUrl ?: fallbackUrl),
                statusMessage = "¡Foto de la fachada actualizada exitosamente!"
            )
        }
    }

    private fun loadMerchantProfile() {
        if (!fetchRealMerchantEnabled || profileLoad?.isActive == true) return
        profileLoad = viewModelScope.launch {
            when (val res = awaitMerchantProfile()) {
                is Result.Success -> applyLoadedMerchant(res.data)
                is Result.Failure -> Unit
            }
        }
    }

    private suspend fun awaitMerchantProfile(): Result<MerchantProfile> {
        val request = profileRequest?.takeIf { it.isActive } ?: viewModelScope.async { getMyMerchantUseCase() }
            .also { profileRequest = it }
        return request.await()
    }

    private suspend fun applyLoadedMerchant(profile: MerchantProfile) {
        if (!viewState.value.isUpdatingStatus) sessionRepository?.setStoreOpen(profile.activo)
        updateState {
            val formattedSchedule = formatSchedulesSummary(profile.horarios).ifBlank { store.schedule }
            copy(
                store = store.copy(
                    name = profile.nombreComercial.ifBlank { store.name },
                    address = profile.direccion.ifBlank { store.address },
                    postalCode = profile.codigoPostal.ifBlank { store.postalCode },
                    latitude = profile.latitud,
                    longitude = profile.longitud,
                    schedule = formattedSchedule,
                    isOpen = if (isUpdatingStatus) store.isOpen else profile.activo,
                    photoUrl = profile.fotoFachadaUrl ?: store.photoUrl,
                    rating = profile.averageRating,
                    reviewCount = profile.reviewCount
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
