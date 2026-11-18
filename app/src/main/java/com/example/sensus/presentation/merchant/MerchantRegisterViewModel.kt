package com.example.sensus.presentation.merchant

import androidx.lifecycle.viewModelScope
import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.media.ImageCompressor
import com.example.sensus.domain.media.ImageUploadPolicy
import com.example.sensus.domain.model.MerchantActivationInput
import com.example.sensus.domain.model.MerchantCoordinates
import com.example.sensus.domain.model.UserLocation
import com.example.sensus.domain.repository.MediaRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.usecase.ActivateMerchantUseCase
import com.example.sensus.domain.usecase.GetUserLocationUseCase
import com.example.sensus.domain.usecase.ResolveAddressFromCoordinatesUseCase
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val GEOCODE_DEBOUNCE_MS = 320L

@HiltViewModel
class MerchantRegisterViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val activateMerchantUseCase: ActivateMerchantUseCase,
    private val getUserLocationUseCase: GetUserLocationUseCase,
    private val resolveAddressFromCoordinatesUseCase: ResolveAddressFromCoordinatesUseCase,
    private val mediaRepository: MediaRepository?,
    private val imageCompressor: ImageCompressor?
) : MviViewModel<MerchantRegisterViewState, MerchantRegisterViewIntent, MerchantRegisterViewEffect>(
    MerchantRegisterViewState()
) {
    constructor(
        sessionRepository: SessionRepository,
        activateMerchantUseCase: ActivateMerchantUseCase,
        getUserLocationUseCase: GetUserLocationUseCase,
        resolveAddressFromCoordinatesUseCase: ResolveAddressFromCoordinatesUseCase
    ) : this(
        sessionRepository,
        activateMerchantUseCase,
        getUserLocationUseCase,
        resolveAddressFromCoordinatesUseCase,
        mediaRepository = null,
        imageCompressor = null
    )

    private var locationJob: Job? = null
    private var geocodeJob: Job? = null

    @Volatile
    private var hasUserMovedPin = false

    @Volatile
    private var hasConfirmedOrMovedPin = false

    init {
        preloadCurrentLocationIfPermitted()
    }

    private fun preloadCurrentLocationIfPermitted() {
        if (!getUserLocationUseCase.hasPermission()) return
        locationJob = viewModelScope.launch {
            val loc = getUserLocationUseCase()
            updateState {
                val keepManualCoords = hasUserMovedPin || hasConfirmedOrMovedPin
                copy(
                    userLocation = loc,
                    hasRealLocation = true,
                    pickerLatitude = if (keepManualCoords) pickerLatitude else loc.latitude,
                    pickerLongitude = if (keepManualCoords) pickerLongitude else loc.longitude,
                    selectedLatitude = if (keepManualCoords) selectedLatitude else loc.latitude,
                    selectedLongitude = if (keepManualCoords) selectedLongitude else loc.longitude,
                    recenterSignal = if (keepManualCoords) recenterSignal else recenterSignal + 1
                )
            }
        }
    }

    override fun processIntent(intent: MerchantRegisterViewIntent) {
        when (intent) {
            is MerchantRegisterViewIntent.OpenMapPickerWithCurrentLocation -> openWithCurrentLocation(openDialog = true)
            is MerchantRegisterViewIntent.OpenMapPickerManualFallback -> openManualFallbackPicker()
            is MerchantRegisterViewIntent.DismissMapPicker -> dismissPicker()
            is MerchantRegisterViewIntent.RecenterPickerOnCurrentLocation -> openWithCurrentLocation(openDialog = false)
            is MerchantRegisterViewIntent.MoveMapPin -> handleMovePin(intent.latitude, intent.longitude)
            is MerchantRegisterViewIntent.ConfirmMapLocation -> handleConfirmLocation(intent.latitude, intent.longitude)
            is MerchantRegisterViewIntent.ClearError -> updateState { copy(errorMessage = null) }
            is MerchantRegisterViewIntent.ActivateMerchant -> handleActivateMerchant(intent)
        }
    }

    private fun openWithCurrentLocation(openDialog: Boolean) {
        locationJob?.cancel()
        geocodeJob?.cancel()
        if (!openDialog) hasConfirmedOrMovedPin = false
        val savedCoords = savedConfirmedCoordsOrNull(openDialog)
        hasUserMovedPin = savedCoords != null
        applyPickerOpenState(openDialog = openDialog, savedCoords = savedCoords)
        locationJob = viewModelScope.launch {
            fetchAndApplyPickerGpsLocation(savedCoords)
        }
    }

    private fun savedConfirmedCoordsOrNull(openDialog: Boolean): Pair<Double, Double>? {
        val current = viewState.value
        val coords = current.selectedLatitude?.let { lat ->
            current.selectedLongitude?.let { lng -> lat to lng }
        }
        return coords.takeIf { openDialog && hasConfirmedOrMovedPin }
    }

    private fun applyPickerOpenState(openDialog: Boolean, savedCoords: Pair<Double, Double>?) {
        updateState {
            copy(
                showMapPicker = if (openDialog) true else showMapPicker,
                pickerLatitude = savedCoords?.first ?: pickerLatitude,
                pickerLongitude = savedCoords?.second ?: pickerLongitude,
                recenterSignal = if (savedCoords != null) recenterSignal + 1 else recenterSignal,
                isResolvingLocation = true,
                locationHelperHint = null,
                errorMessage = null
            )
        }
    }

    private suspend fun fetchAndApplyPickerGpsLocation(savedCoords: Pair<Double, Double>?) {
        if (savedCoords == null && !viewState.value.hasRealLocation && getUserLocationUseCase.hasPermission()) {
            val cachedLoc = getUserLocationUseCase()
            if (!hasUserMovedPin) applyDetectedGpsLocation(cachedLoc)
        }
        val freshLoc = getUserLocationUseCase.fresh()
        if (savedCoords != null || hasUserMovedPin) {
            updateState { copy(userLocation = freshLoc, hasRealLocation = true, isResolvingLocation = false) }
            if (savedCoords != null) resolveAndPreview(savedCoords.first, savedCoords.second)
        } else {
            applyDetectedGpsLocation(freshLoc)
            resolveAndPreview(freshLoc.latitude, freshLoc.longitude)
        }
    }

    private fun applyDetectedGpsLocation(location: UserLocation) {
        updateState {
            copy(
                userLocation = location,
                hasRealLocation = true,
                selectedLatitude = location.latitude,
                selectedLongitude = location.longitude,
                pickerLatitude = location.latitude,
                pickerLongitude = location.longitude,
                recenterSignal = recenterSignal + 1
            )
        }
    }

    private fun openManualFallbackPicker() {
        locationJob?.cancel()
        geocodeJob?.cancel()
        val current = viewState.value
        val fallbackLat = current.selectedLatitude ?: current.pickerLatitude
        val fallbackLng = current.selectedLongitude ?: current.pickerLongitude
        updateState {
            copy(
                showMapPicker = true,
                pickerLatitude = fallbackLat,
                pickerLongitude = fallbackLng,
                recenterSignal = recenterSignal + 1,
                isResolvingLocation = true,
                locationHelperHint = "Mueve el pin en el mapa para elegir tu ubicación exacta."
            )
        }
        geocodeJob = viewModelScope.launch {
            resolveAndPreview(fallbackLat, fallbackLng)
        }
    }

    private fun dismissPicker() {
        locationJob?.cancel()
        geocodeJob?.cancel()
        updateState { copy(showMapPicker = false, isResolvingLocation = false) }
    }

    private fun handleMovePin(latitude: Double, longitude: Double) {
        hasUserMovedPin = true
        hasConfirmedOrMovedPin = true
        locationJob?.cancel()
        geocodeJob?.cancel()
        updateState {
            copy(
                pickerLatitude = latitude,
                pickerLongitude = longitude,
                isResolvingLocation = true,
                locationHelperHint = null
            )
        }
        geocodeJob = viewModelScope.launch {
            delay(GEOCODE_DEBOUNCE_MS)
            resolveAndPreview(latitude, longitude)
        }
    }

    private fun handleConfirmLocation(latitude: Double, longitude: Double) {
        hasConfirmedOrMovedPin = true
        geocodeJob?.cancel()
        viewModelScope.launch {
            updateState { copy(isResolvingLocation = true) }
            val currentPreview = viewState.value.resolvedAddressPreview
            val currentPostal = viewState.value.resolvedPostalPreview
            val resolved = resolveAddressFromCoordinatesUseCase(latitude, longitude)
            val finalAddress = (resolved as? Result.Success)?.data?.direccion ?: currentPreview.orEmpty()
            val finalPostal = (resolved as? Result.Success)?.data?.codigoPostal
                ?.takeIf { it.isNotBlank() }
                ?: currentPostal.orEmpty()

            updateState {
                copy(
                    showMapPicker = false,
                    isResolvingLocation = false,
                    selectedLatitude = latitude,
                    selectedLongitude = longitude,
                    pickerLatitude = latitude,
                    pickerLongitude = longitude,
                    resolvedAddressPreview = finalAddress.ifBlank { resolvedAddressPreview },
                    resolvedPostalPreview = finalPostal.ifBlank { resolvedPostalPreview },
                    locationHelperHint = (resolved as? Result.Failure)?.error?.message
                )
            }
            if (finalAddress.isNotBlank() || finalPostal.isNotBlank()) {
                emitEffect(MerchantRegisterViewEffect.ApplyResolvedAddress(finalAddress, finalPostal))
            }
        }
    }

    private suspend fun resolveAndPreview(latitude: Double, longitude: Double) {
        when (val result = resolveAddressFromCoordinatesUseCase(latitude, longitude)) {
            is Result.Success -> updateState {
                copy(
                    isResolvingLocation = false,
                    resolvedAddressPreview = result.data.direccion,
                    resolvedPostalPreview = result.data.codigoPostal.takeIf { it.isNotBlank() },
                    locationHelperHint = null
                )
            }
            is Result.Failure -> updateState {
                copy(
                    isResolvingLocation = false,
                    locationHelperHint = result.error.message
                )
            }
        }
    }

    private fun handleActivateMerchant(intent: MerchantRegisterViewIntent.ActivateMerchant) {
        if (viewState.value.isRegistering) return
        val stateSnapshot = viewState.value
        updateState { copy(isRegistering = true, errorMessage = null) }

        viewModelScope.launch {
            val photoResult = uploadMerchantFacadePhotoIfProvided(intent.facadePhotoBytes, mediaRepository, imageCompressor)
            if (photoResult is Result.Failure) {
                updateState { copy(isRegistering = false, errorMessage = photoResult.error.message) }
                return@launch
            }
            val fotoFachadaUrl = (photoResult as Result.Success).data
            val coords = stateSnapshot.selectedLatitude?.let { lat ->
                stateSnapshot.selectedLongitude?.let { lng -> MerchantCoordinates(lat, lng) }
            }
            val result = activateMerchantUseCase(
                MerchantActivationInput(
                    nombreComercial = intent.storeName,
                    direccion = intent.address,
                    codigoPostal = intent.postalCode,
                    coordinates = coords,
                    fotoFachadaUrl = fotoFachadaUrl
                )
            )
            when (result) {
                is Result.Success -> {
                    sessionRepository.setMerchantStatus(
                        isRegistered = true,
                        storeName = result.data.nombreComercial
                    )
                    sessionRepository.syncSession()
                    updateState { copy(isRegistering = false, errorMessage = null) }
                    emitEffect(MerchantRegisterViewEffect.ActivationSuccess(result.data.nombreComercial))
                }
                is Result.Failure -> updateState {
                    copy(isRegistering = false, errorMessage = result.error.message)
                }
            }
        }
    }
}

internal suspend fun uploadMerchantFacadePhotoIfProvided(
    bytes: ByteArray?,
    mediaRepository: MediaRepository?,
    imageCompressor: ImageCompressor?
): Result<String?> {
    val repo = mediaRepository.takeIf { bytes != null && bytes.isNotEmpty() } ?: return Result.Success(null)
    val rawBytes = bytes ?: return Result.Success(null)
    val compressed = if (imageCompressor != null) {
        imageCompressor.compressForUpload(rawBytes)
    } else {
        rawBytes
    }
    return if (compressed == null) {
        Result.Failure(AppError.Validation(ImageUploadPolicy.TOO_LARGE_MESSAGE))
    } else {
        when (val upload = repo.uploadAndModerate(compressed, ImageUploadPolicy.JPEG_MIME_TYPE)) {
            is Result.Success -> Result.Success(upload.data.url)
            is Result.Failure -> upload
        }
    }
}
