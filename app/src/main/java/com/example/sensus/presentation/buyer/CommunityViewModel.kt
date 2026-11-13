package com.example.sensus.presentation.buyer

import androidx.lifecycle.viewModelScope
import com.example.sensus.common.Result
import com.example.sensus.domain.model.CreatePostInput
import com.example.sensus.domain.model.FeedQuery
import com.example.sensus.domain.model.UserLocation
import com.example.sensus.domain.repository.LocationRepository
import com.example.sensus.domain.repository.OffersRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.usecase.CreateFeedPostUseCase
import com.example.sensus.domain.usecase.GetNearbyFeedUseCase
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class CommunityViewModel @Inject constructor(
    private val getFeed: GetNearbyFeedUseCase,
    private val createPost: CreateFeedPostUseCase,
    private val locationRepository: LocationRepository,
    private val sessionRepository: SessionRepository,
    private val offersRepository: OffersRepository,
    private val storeRepository: com.example.sensus.domain.repository.StoreRepository? = null
) : MviViewModel<CommunityViewState, CommunityViewIntent, CommunityViewEffect>(CommunityViewState()) {
    private var imageBytes: ByteArray? = null
    private var location: UserLocation? = null
    private var feedJob: Job? = null

    init {
        updateState { copy(friends = storeRepository?.getSampleFriends().orEmpty()) }
        viewModelScope.launch {
            sessionRepository.sessionState.collect { session ->
                updateState {
                    copy(
                        isGuest = session.isGuest || !session.isSessionActive,
                        currentUserName = session.activeName ?: session.activeEmail ?: "Usuario Sensus"
                    )
                }
                if (session.isGuest || !session.isSessionActive) closeComposer()
            }
        }
        loadPage(refresh = true)
    }

    override fun processIntent(intent: CommunityViewIntent) {
        when (intent) {
            CommunityViewIntent.Refresh -> loadPage(refresh = true)
            CommunityViewIntent.LoadMore -> if (viewState.value.hasMore) loadPage(refresh = false)
            CommunityViewIntent.NewPost -> openComposer()
            CommunityViewIntent.CloseComposer -> if (!viewState.value.isPublishing) closeComposer()
            CommunityViewIntent.Submit -> submit()
            else -> editDraft(intent)
        }
    }

    private fun editDraft(intent: CommunityViewIntent) {
        when (intent) {
            is CommunityViewIntent.TextChanged -> updateState { copy(draftText = intent.text, errorMessage = null) }
            is CommunityViewIntent.MerchantSelected -> updateState { copy(merchantId = intent.id) }
            is CommunityViewIntent.ImageSelected -> {
                imageBytes = intent.bytes
                updateState { copy(hasImage = intent.bytes != null, errorMessage = null) }
            }
            CommunityViewIntent.ImageReadFailed -> updateState { copy(errorMessage = "No se pudo leer la foto. Selecciónala de nuevo.") }
            else -> Unit
        }
    }

    private fun loadPage(refresh: Boolean) {
        if (viewState.value.isLoading && !refresh) return
        feedJob?.cancel()
        val page = if (refresh) 1 else viewState.value.nextPage
        updateState { copy(isLoading = true, errorMessage = null) }
        feedJob = viewModelScope.launch {
            if (!locationRepository.hasLocationPermission()) {
                updateState { copy(isLoading = false, errorMessage = "Activa el permiso de ubicación para ver tu comunidad.") }
                return@launch
            }
            val current = resolveLocation(refresh)
            applyPage(getFeed(FeedQuery(current.latitude, current.longitude, page = page)), refresh, page)
        }
    }

    private suspend fun resolveLocation(refresh: Boolean): UserLocation {
        val cached = location
        val current = if (refresh || cached == null) locationRepository.getCurrentLocation() else cached
        location = current
        return current
    }

    private fun applyPage(result: Result<com.example.sensus.domain.model.PagedFeed>, refresh: Boolean, page: Int) {
        when (result) {
            is Result.Success -> updateState {
                val items = if (refresh) result.data.posts else (posts + result.data.posts).distinctBy { it.id }
                copy(posts = items, isLoading = false, hasMore = result.data.hasMore, nextPage = page + 1, isCached = result.data.isCached)
            }
            is Result.Failure -> updateState { copy(isLoading = false, errorMessage = result.error.message) }
        }
    }

    private fun openComposer() {
        if (viewState.value.isGuest) {
            emitEffect(CommunityViewEffect.RequireAuth)
            return
        }
        updateState { copy(showComposer = true, errorMessage = null) }
        viewModelScope.launch {
            val merchants = withContext(Dispatchers.IO) {
                val current = location ?: return@withContext emptyList()
                com.example.sensus.domain.model.PublishingMerchantPolicy.nearby(
                    offersRepository.getOffers(),
                    current,
                    COMMUNITY_RADIUS_METERS
                )
            }
            updateState { copy(nearbyMerchants = merchants) }
        }
    }

    private fun closeComposer() {
        imageBytes = null
        updateState { copy(showComposer = false, draftText = "", merchantId = null, hasImage = false) }
    }

    private fun submit() {
        if (viewState.value.isPublishing) return
        if (viewState.value.isGuest) {
            emitEffect(CommunityViewEffect.RequireAuth)
            return
        }
        val current = location
        if (current == null) {
            updateState { copy(errorMessage = "Actualiza la comunidad para obtener tu ubicación antes de publicar.") }
            return
        }
        val draft = viewState.value
        updateState { copy(isPublishing = true, errorMessage = null) }
        viewModelScope.launch {
            val input = CreatePostInput(draft.draftText, current.latitude, current.longitude, draft.merchantId)
            when (val result = createPost(input, imageBytes)) {
                is Result.Success -> {
                    closeComposer()
                    updateState { copy(posts = (listOf(result.data) + posts).distinctBy { it.id }, isPublishing = false) }
                }
                is Result.Failure -> updateState { copy(isPublishing = false, errorMessage = result.error.message) }
            }
        }
    }

    private companion object {
        const val COMMUNITY_RADIUS_METERS = 2000
    }
}
