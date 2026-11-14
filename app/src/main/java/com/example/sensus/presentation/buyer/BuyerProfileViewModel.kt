package com.example.sensus.presentation.buyer

import androidx.lifecycle.viewModelScope
import com.example.sensus.common.Result
import com.example.sensus.domain.model.UserProfile
import com.example.sensus.domain.repository.ProfileRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import com.example.sensus.domain.usecase.GetMyMerchantUseCase
import com.example.sensus.domain.usecase.LogoutUseCase
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
    private val getMyMerchantUseCase: GetMyMerchantUseCase
) : MviViewModel<BuyerProfileViewState, BuyerProfileViewIntent, BuyerProfileViewEffect>(BuyerProfileViewState()) {

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
                merchantStoreName = resolvedStoreName
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
            is BuyerProfileViewIntent.Logout -> performLogout()
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
                    updateState { copy(hasActiveMerchant = true, merchantStoreName = storeName) }
                    val session = sessionRepository.sessionState.value
                    if (!session.isMerchantRegistered || session.merchantStoreName != storeName) {
                        sessionRepository.setMerchantStatus(isRegistered = true, storeName = storeName)
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
