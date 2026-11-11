package com.example.sensus.presentation.buyer

import androidx.lifecycle.viewModelScope
import com.example.sensus.common.Result
import com.example.sensus.domain.model.UserProfile
import com.example.sensus.domain.repository.ProfileRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import com.example.sensus.domain.usecase.UpdateProfileUseCase
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BuyerProfileViewModel @Inject constructor(
    val profileRepository: ProfileRepository,
    val updateProfileUseCase: UpdateProfileUseCase,
    private val sessionRepository: SessionRepository
) : MviViewModel<BuyerProfileViewState, BuyerProfileViewIntent, BuyerProfileViewEffect>(BuyerProfileViewState()) {

    private var lastLoadedSessionKey: String? = null

    init {
        observeSession()
        loadProfile()
    }

    private fun sessionKey(session: SessionState): String {
        return "${session.activeToken.orEmpty()}|${session.activeEmail.orEmpty()}|${session.isGuest}"
    }

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

    private fun observeSession() {
        viewModelScope.launch {
            sessionRepository.sessionState.collect { session ->
                if (session.isGuest) {
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
                            errorMessage = null
                        )
                    }
                    return@collect
                }

                val hasRegisteredHint = session.isSessionActive ||
                        !session.activeEmail.isNullOrBlank() ||
                        !session.activeToken.isNullOrBlank()

                if (!hasRegisteredHint) {
                    lastLoadedSessionKey = null
                    updateState {
                        copy(
                            isGuest = false,
                            profile = null,
                            isLoading = false
                        )
                    }
                    return@collect
                }

                val key = sessionKey(session)
                val cached = buildCachedRegisteredProfile(session)
                val sameSession = lastLoadedSessionKey == key
                updateState {
                    copy(
                        isGuest = false,
                        profile = if (sameSession) (profile ?: cached) else (cached ?: profile)
                    )
                }

                if (!sameSession) {
                    lastLoadedSessionKey = key
                    loadProfile()
                }
            }
        }
    }

    override fun processIntent(intent: BuyerProfileViewIntent) {
        when (intent) {
            is BuyerProfileViewIntent.LoadProfile -> loadProfile()
            is BuyerProfileViewIntent.UpdateName -> updateName(intent.newName)
        }
    }

    private fun loadProfile() {
        val currentSession = sessionRepository.sessionState.value
        if (currentSession.isGuest) {
            val guestProfile = UserProfile(
                id = "guest",
                nombreCompleto = "Invitado Sensus",
                correo = "invitado@sensus.app",
                roles = listOf("Invitado")
            )
            updateState { copy(isGuest = true, profile = guestProfile, isLoading = false, errorMessage = null) }
            return
        }

        val cachedFallback = buildCachedRegisteredProfile(currentSession)
        updateState {
            copy(
                isGuest = false,
                profile = profile ?: cachedFallback,
                isLoading = true,
                errorMessage = null
            )
        }
        viewModelScope.launch {
            when (val res = profileRepository.getProfile()) {
                is Result.Success -> updateState {
                    copy(
                        isGuest = false,
                        profile = res.data,
                        isLoading = false,
                        errorMessage = null
                    )
                }
                is Result.Failure -> {
                    val latestSession = sessionRepository.sessionState.value
                    val fallback = viewState.value.profile ?: buildCachedRegisteredProfile(latestSession)
                    updateState {
                        copy(
                            isGuest = latestSession.isGuest,
                            profile = fallback,
                            errorMessage = res.error.message,
                            isLoading = false
                        )
                    }
                }
            }
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
