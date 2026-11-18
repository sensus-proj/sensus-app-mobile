package com.example.sensus.presentation.buyer

import androidx.lifecycle.viewModelScope
import com.example.sensus.common.Result
import com.example.sensus.domain.model.MerchantCoordinates
import com.example.sensus.domain.model.MerchantProfileUpdateInput
import com.example.sensus.domain.model.UserProfile
import com.example.sensus.domain.repository.ProfileRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import com.example.sensus.domain.usecase.GetMyMerchantUseCase
import com.example.sensus.domain.usecase.LogoutUseCase
import com.example.sensus.domain.usecase.UpdateMerchantProfileUseCase
import com.example.sensus.domain.usecase.UpdateProfileUseCase
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class BuyerProfileViewModel @Inject constructor(
    val profileRepository: ProfileRepository,
    val updateProfileUseCase: UpdateProfileUseCase,
    private val sessionRepository: SessionRepository,
    private val logoutUseCase: LogoutUseCase,
    private val getMyMerchantUseCase: GetMyMerchantUseCase,
    private val updateMerchantProfileUseCase: UpdateMerchantProfileUseCase? = null
) : MviViewModel<BuyerProfileViewState, BuyerProfileViewIntent, BuyerProfileViewEffect>(BuyerProfileViewState()) {

    constructor(
        profileRepository: ProfileRepository,
        updateProfileUseCase: UpdateProfileUseCase,
        sessionRepository: SessionRepository,
        logoutUseCase: LogoutUseCase,
        getMyMerchantUseCase: GetMyMerchantUseCase
    ) : this(profileRepository, updateProfileUseCase, sessionRepository, logoutUseCase, getMyMerchantUseCase, null)

    private var lastLoadedSessionKey: String? = null

    init {
        handleSessionChange(sessionRepository.sessionState.value)
        observeSession()
    }

    private fun observeSession() {
        viewModelScope.launch {
            sessionRepository.sessionState.collect { session -> handleSessionChange(session) }
        }
    }

    private fun handleSessionChange(session: SessionState) {
        if (session.isGuest) {
            applyGuestSessionState()
            return
        }
        if (!hasRegisteredSessionHint(session)) {
            lastLoadedSessionKey = null
            updateState {
                copy(isGuest = false, profile = null, isLoading = false, hasActiveMerchant = false, merchantStoreName = null)
            }
            return
        }
        applyRegisteredSessionState(session)
    }

    private fun applyGuestSessionState() {
        lastLoadedSessionKey = "GUEST"
        val guestProfile = UserProfile(
            id = "guest",
            nombreCompleto = "Invitado Sensus",
            correo = "invitado@sensus.app",
            roles = listOf("Invitado")
        )
        updateState {
            copy(
                isGuest = true,
                profile = guestProfile,
                isLoading = false,
                errorMessage = null,
                hasActiveMerchant = false,
                merchantStoreName = null
            )
        }
    }

    private fun applyRegisteredSessionState(session: SessionState) {
        val key = sessionKey(session)
        val cached = buildCachedRegisteredProfile(session)
        val sameSession = lastLoadedSessionKey == key
        updateState {
            val resolvedStoreName = session.merchantStoreName ?: merchantStoreName.takeIf { session.isMerchantRegistered }
            copy(
                isGuest = false,
                profile = if (sameSession) (profile ?: cached) else (cached ?: profile),
                hasActiveMerchant = session.isMerchantRegistered,
                merchantStoreName = resolvedStoreName,
                isStoreOpen = session.isStoreOpen
            )
        }
        if (!sameSession) {
            lastLoadedSessionKey = key
            loadProfile()
        }
    }

    override fun processIntent(intent: BuyerProfileViewIntent) {
        when (intent) {
            is BuyerProfileViewIntent.LoadProfile -> loadProfile()
            is BuyerProfileViewIntent.UpdateName -> updateName(intent.newName)
            is BuyerProfileViewIntent.ReopenStore -> reopenStore()
            is BuyerProfileViewIntent.Logout -> performLogout()
        }
    }

    private fun reopenStore() {
        if (viewState.value.isReopeningStore) return
        val updater = updateMerchantProfileUseCase ?: run {
            emitEffect(BuyerProfileViewEffect.StoreReopened)
            return
        }
        updateState { copy(isReopeningStore = true) }
        viewModelScope.launch {
            val loaded = (getMyMerchantUseCase() as? Result.Success)?.data
            if (loaded == null) {
                updateState { copy(isReopeningStore = false) }
                emitEffect(BuyerProfileViewEffect.ShowMessage("No se pudo cargar tu tienda para reabrirla."))
                return@launch
            }
            val input = MerchantProfileUpdateInput(
                nombreComercial = loaded.nombreComercial,
                direccion = loaded.direccion,
                codigoPostal = loaded.codigoPostal,
                horarios = loaded.horarios,
                coordinates = MerchantCoordinates(loaded.latitud, loaded.longitud),
                activo = true
            )
            when (val res = updater(input)) {
                is Result.Success -> handleReopenSuccess(res.data.activo)
                is Result.Failure -> {
                    updateState { copy(isReopeningStore = false) }
                    emitEffect(BuyerProfileViewEffect.ShowMessage(res.error.message))
                }
            }
        }
    }

    private suspend fun handleReopenSuccess(actualActive: Boolean) {
        sessionRepository.setStoreOpen(actualActive)
        updateState { copy(isStoreOpen = actualActive, isReopeningStore = false) }
        if (actualActive) {
            emitEffect(BuyerProfileViewEffect.StoreReopened)
        } else {
            emitEffect(BuyerProfileViewEffect.ShowMessage("El servidor aún no guardó la reapertura de la tienda."))
        }
    }

    private fun loadProfile() {
        val currentSession = sessionRepository.sessionState.value
        if (currentSession.isGuest) {
            applyGuestSessionState()
            return
        }
        val cachedFallback = buildCachedRegisteredProfile(currentSession)
        updateState { copy(isGuest = false, profile = profile ?: cachedFallback, isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val res = profileRepository.getProfile()) {
                is Result.Success -> handleProfileSuccess(res.data, currentSession)
                is Result.Failure -> handleProfileFailure(res.error.message)
            }
        }
    }

    private fun handleProfileSuccess(user: UserProfile, sessionSnapshot: SessionState) {
        val hasMerchantRole = user.hasMerchantRole() || sessionSnapshot.isMerchantRegistered
        updateState {
            copy(
                isGuest = false,
                profile = user,
                isLoading = false,
                errorMessage = null,
                hasActiveMerchant = hasMerchantRole,
                merchantStoreName = sessionSnapshot.merchantStoreName ?: merchantStoreName
            )
        }
        syncMerchantStoreIfEligible(hasMerchantRole)
    }

    private fun handleProfileFailure(errorMessage: String) {
        val latestSession = sessionRepository.sessionState.value
        val fallback = viewState.value.profile ?: buildCachedRegisteredProfile(latestSession)
        updateState {
            copy(
                isGuest = latestSession.isGuest,
                profile = fallback,
                errorMessage = errorMessage,
                isLoading = false,
                hasActiveMerchant = latestSession.isMerchantRegistered || hasActiveMerchant,
                merchantStoreName = latestSession.merchantStoreName ?: merchantStoreName
            )
        }
    }

    private fun syncMerchantStoreIfEligible(isMerchantEligible: Boolean) {
        if (!isMerchantEligible) return
        viewModelScope.launch {
            when (val merchantRes = getMyMerchantUseCase()) {
                is Result.Success -> {
                    val storeName = merchantRes.data.nombreComercial
                    val isOpen = merchantRes.data.activo
                    updateState { copy(hasActiveMerchant = true, merchantStoreName = storeName, isStoreOpen = isOpen) }
                    val session = sessionRepository.sessionState.value
                    if (!session.isMerchantRegistered || session.merchantStoreName != storeName) {
                        sessionRepository.setMerchantStatus(isRegistered = true, storeName = storeName)
                    }
                    if (session.isStoreOpen != isOpen) {
                        sessionRepository.setStoreOpen(isOpen)
                    }
                }
                is Result.Failure -> Unit
            }
        }
    }

    private fun performLogout() {
        viewModelScope.launch {
            logoutUseCase()
        }
    }

    private fun updateName(newName: String) {
        if (viewState.value.isGuest) {
            updateState { copy(errorMessage = "Inicia sesión para editar tu perfil.") }
            return
        }
        updateState { copy(isLoading = true) }
        viewModelScope.launch {
            when (val res = updateProfileUseCase(newName, viewState.value.profile?.fotoPerfilUrl)) {
                is Result.Success -> updateState { copy(profile = res.data, isLoading = false, errorMessage = null) }
                is Result.Failure -> updateState { copy(errorMessage = res.error.message, isLoading = false) }
            }
        }
    }
}

private fun sessionKey(session: SessionState): String = "${session.activeToken.orEmpty()}|${session.activeEmail.orEmpty()}|${session.isGuest}"

private fun hasRegisteredSessionHint(session: SessionState): Boolean =
    session.isSessionActive || !session.activeEmail.isNullOrBlank() || !session.activeToken.isNullOrBlank()

private fun UserProfile.hasMerchantRole(): Boolean = roles.any { it.equals("Comerciante", ignoreCase = true) || it.equals("Merchant", ignoreCase = true) }

private fun buildCachedRegisteredProfile(session: SessionState): UserProfile? {
    val email = session.activeEmail?.takeIf { it.isNotBlank() }
    val name = session.activeName?.takeIf { it.isNotBlank() }
    if (email == null && name == null) return null
    val displayName = name ?: email!!.substringBefore("@").replaceFirstChar { it.uppercase() }
    return UserProfile(
        id = "local",
        nombreCompleto = displayName,
        correo = email.orEmpty(),
        fotoPerfilUrl = session.activePhotoUrl,
        roles = listOf("Consumidor")
    )
}
