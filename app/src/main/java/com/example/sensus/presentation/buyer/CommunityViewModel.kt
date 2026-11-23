package com.example.sensus.presentation.buyer

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.viewModelScope
import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.ContentReportInput
import com.example.sensus.domain.model.CreatePostInput
import com.example.sensus.domain.model.FeedPolicy
import com.example.sensus.domain.model.FeedPost
import com.example.sensus.domain.model.FeedQuery
import com.example.sensus.domain.model.PagedFeed
import com.example.sensus.domain.model.ReportReason
import com.example.sensus.domain.model.ReportTargetType
import com.example.sensus.domain.model.SubmittedReport
import com.example.sensus.domain.model.UserLocation
import com.example.sensus.domain.repository.LocationRepository
import com.example.sensus.domain.repository.OffersRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.usecase.AddFeedCommentUseCase
import com.example.sensus.domain.usecase.CreateFeedPostUseCase
import com.example.sensus.domain.usecase.DeleteFeedPostUseCase
import com.example.sensus.domain.usecase.GetFeedCommentsUseCase
import com.example.sensus.domain.usecase.GetNearbyFeedUseCase
import com.example.sensus.domain.usecase.SubmitContentReportUseCase
import com.example.sensus.domain.usecase.TogglePostLikeUseCase
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("TooManyFunctions", "LongParameterList")
@HiltViewModel
class CommunityViewModel @Inject constructor(
    private val getFeed: GetNearbyFeedUseCase,
    private val createPost: CreateFeedPostUseCase,
    private val locationRepository: LocationRepository,
    private val sessionRepository: SessionRepository,
    private val offersRepository: OffersRepository,
    private val togglePostLikeUseCase: TogglePostLikeUseCase? = null,
    private val getCommentsUseCase: GetFeedCommentsUseCase? = null,
    private val addCommentUseCase: AddFeedCommentUseCase? = null,
    private val deletePostUseCase: DeleteFeedPostUseCase? = null,
    private val submitReportUseCase: SubmitContentReportUseCase? = null
) : MviViewModel<CommunityViewState, CommunityViewIntent, CommunityViewEffect>(CommunityViewState()) {
    private var location: UserLocation? = null
    private var feedJob: Job? = null
    private var imageBytes: ByteArray? = null

    @VisibleForTesting
    internal var ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    init {
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
            CommunityViewIntent.NewPost,
            CommunityViewIntent.CloseComposer,
            is CommunityViewIntent.TextChanged,
            is CommunityViewIntent.MerchantSelected,
            is CommunityViewIntent.ImageSelected,
            CommunityViewIntent.ImageReadFailed,
            CommunityViewIntent.Submit -> processComposerIntent(intent)
            is CommunityViewIntent.ToggleLike -> toggleLike(intent.postId)
            is CommunityViewIntent.OpenComments,
            CommunityViewIntent.CloseComments,
            is CommunityViewIntent.CommentDraftChanged,
            CommunityViewIntent.SubmitComment -> processCommentIntent(intent)
            is CommunityViewIntent.RequestDeletePost,
            CommunityViewIntent.CancelDeletePost,
            CommunityViewIntent.ConfirmDeletePost,
            is CommunityViewIntent.OpenReportPost,
            CommunityViewIntent.CloseReport,
            is CommunityViewIntent.SubmitReport -> processModerationIntent(intent)
        }
    }

    private fun processComposerIntent(intent: CommunityViewIntent) {
        when (intent) {
            CommunityViewIntent.NewPost -> openComposer()
            CommunityViewIntent.CloseComposer -> handleCloseComposer()
            is CommunityViewIntent.TextChanged -> updateState { copy(draftText = intent.text, publishError = null) }
            is CommunityViewIntent.MerchantSelected -> updateState { copy(merchantId = intent.id) }
            is CommunityViewIntent.ImageSelected -> {
                imageBytes = intent.bytes
                updateState { copy(hasImage = intent.bytes != null, publishError = null) }
            }
            CommunityViewIntent.ImageReadFailed -> updateState {
                copy(publishError = AppError.Validation("No se pudo leer la foto. Selecciónala de nuevo."))
            }
            CommunityViewIntent.Submit -> submit()
            else -> Unit
        }
    }

    private fun handleCloseComposer() {
        if (!viewState.value.isPublishing) closeComposer()
    }

    private fun processCommentIntent(intent: CommunityViewIntent) {
        when (intent) {
            is CommunityViewIntent.OpenComments -> openComments(intent.post)
            CommunityViewIntent.CloseComments -> updateState {
                copy(activeCommentPost = null, comments = emptyList(), commentDraft = "", commentError = null)
            }
            is CommunityViewIntent.CommentDraftChanged -> updateState {
                copy(commentDraft = intent.text.take(FeedPolicy.MAX_COMMENT_LENGTH), commentError = null)
            }
            CommunityViewIntent.SubmitComment -> submitComment()
            else -> Unit
        }
    }

    private fun processModerationIntent(intent: CommunityViewIntent) {
        when (intent) {
            is CommunityViewIntent.RequestDeletePost -> updateState { copy(pendingDeletePost = intent.post) }
            CommunityViewIntent.CancelDeletePost -> updateState { copy(pendingDeletePost = null) }
            CommunityViewIntent.ConfirmDeletePost -> confirmDeletePost()
            is CommunityViewIntent.OpenReportPost -> openReportPost(intent.post)
            CommunityViewIntent.CloseReport -> updateState { copy(reportTarget = null, reportError = null) }
            is CommunityViewIntent.SubmitReport -> submitReport(intent.reason, intent.details)
            else -> Unit
        }
    }

    private fun loadPage(refresh: Boolean) {
        if (viewState.value.isLoading && !refresh) return
        feedJob?.cancel()
        val page = if (refresh) 1 else viewState.value.nextPage
        updateState { copy(isLoading = true, feedError = null) }
        feedJob = viewModelScope.launch {
            if (!locationRepository.hasLocationPermission()) {
                updateState {
                    copy(isLoading = false, feedError = AppError.Validation("Activa el permiso de ubicación para ver tu comunidad."))
                }
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

    private fun applyPage(result: Result<PagedFeed>, refresh: Boolean, page: Int) {
        when (result) {
            is Result.Success -> updateState {
                val items = if (refresh) result.data.posts else (posts + result.data.posts).distinctBy { it.id }
                copy(
                    posts = items,
                    isLoading = false,
                    hasMore = result.data.hasMore,
                    nextPage = page + 1,
                    isCached = result.data.isCached,
                    hasLoadedFeed = true
                )
            }
            is Result.Failure -> updateState { copy(isLoading = false, feedError = result.error) }
        }
    }

    private fun openComposer() {
        if (viewState.value.isGuest) {
            emitEffect(CommunityViewEffect.RequireAuth)
            return
        }
        updateState { copy(showComposer = true, publishError = null) }
        viewModelScope.launch {
            val merchants = withContext(ioDispatcher) {
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
        updateState { copy(showComposer = false, draftText = "", merchantId = null, hasImage = false, publishError = null) }
    }

    private fun submit() {
        if (viewState.value.isPublishing) return
        if (viewState.value.isGuest) {
            emitEffect(CommunityViewEffect.RequireAuth)
            return
        }
        val current = location
        if (current == null) {
            updateState { copy(publishError = AppError.Validation("Actualiza la comunidad para obtener tu ubicación antes de publicar.")) }
            return
        }
        val draft = viewState.value
        updateState { copy(isPublishing = true, publishError = null) }
        viewModelScope.launch {
            val input = CreatePostInput(draft.draftText, current.latitude, current.longitude, draft.merchantId)
            when (val result = createPost(input, imageBytes)) {
                is Result.Success -> {
                    closeComposer()
                    updateState { copy(posts = (listOf(result.data) + posts).distinctBy { it.id }, isPublishing = false) }
                }
                is Result.Failure -> updateState { copy(isPublishing = false, publishError = result.error) }
            }
        }
    }

    private fun toggleLike(postId: String) {
        if (viewState.value.isGuest) {
            emitEffect(CommunityViewEffect.RequireAuth)
            return
        }
        val useCase = togglePostLikeUseCase ?: return
        val previous = viewState.value.posts.firstOrNull { it.id == postId }
        if (previous != null && postId !in viewState.value.likingPostIds) {
            applyOptimisticLike(postId, previous)
            viewModelScope.launch {
                when (val result = useCase(postId)) {
                    is Result.Success -> reconcileLike(postId, result.data.isLikedByMe, result.data.likesCount)
                    is Result.Failure -> rollbackLike(postId, previous, result.error)
                }
            }
        }
    }

    private fun applyOptimisticLike(postId: String, previous: FeedPost) {
        val nextLiked = !previous.isLikedByMe
        val nextCount = (previous.likesCount + if (nextLiked) 1 else -1).coerceAtLeast(0)
        updateState {
            copy(
                likingPostIds = likingPostIds + postId,
                posts = posts.map { if (it.id == postId) it.copy(isLikedByMe = nextLiked, likesCount = nextCount) else it }
            )
        }
    }

    private fun reconcileLike(postId: String, isLikedByMe: Boolean, likesCount: Int) {
        updateState {
            copy(
                likingPostIds = likingPostIds - postId,
                posts = posts.map { if (it.id == postId) it.copy(isLikedByMe = isLikedByMe, likesCount = likesCount) else it }
            )
        }
    }

    private fun rollbackLike(postId: String, previous: FeedPost, error: AppError) {
        updateState {
            copy(likingPostIds = likingPostIds - postId, posts = posts.map { if (it.id == postId) previous else it })
        }
        val effect = if (error is AppError.Unauthorized) {
            CommunityViewEffect.RequireAuth
        } else {
            CommunityViewEffect.ShowMessage(error.message)
        }
        emitEffect(effect)
    }

    private fun openComments(post: FeedPost) {
        updateState {
            copy(
                activeCommentPost = post,
                comments = emptyList(),
                isLoadingComments = true,
                commentDraft = "",
                commentError = null
            )
        }
        val useCase = getCommentsUseCase ?: run {
            updateState { copy(isLoadingComments = false) }
            return
        }
        viewModelScope.launch {
            when (val result = useCase(post.id)) {
                is Result.Success -> updateState { copy(isLoadingComments = false, comments = result.data.items) }
                is Result.Failure -> updateState { copy(isLoadingComments = false, commentError = result.error.message) }
            }
        }
    }

    private fun submitComment() {
        if (viewState.value.isGuest) {
            emitEffect(CommunityViewEffect.RequireAuth)
            return
        }
        val useCase = addCommentUseCase
        val post = viewState.value.activeCommentPost
        if (useCase == null || post == null || viewState.value.isSubmittingComment) return
        val draft = viewState.value.commentDraft
        updateState { copy(isSubmittingComment = true, commentError = null) }
        viewModelScope.launch {
            when (val result = useCase(post.id, draft)) {
                is Result.Success -> updateState {
                    copy(
                        isSubmittingComment = false,
                        commentDraft = "",
                        comments = comments + result.data,
                        posts = posts.map { if (it.id == post.id) it.copy(commentsCount = it.commentsCount + 1) else it },
                        activeCommentPost = post.copy(commentsCount = post.commentsCount + 1)
                    )
                }
                is Result.Failure -> {
                    updateState { copy(isSubmittingComment = false, commentError = result.error.message) }
                    if (result.error is AppError.Unauthorized) emitEffect(CommunityViewEffect.RequireAuth)
                }
            }
        }
    }

    private fun confirmDeletePost() {
        val post = viewState.value.pendingDeletePost ?: return
        val useCase = deletePostUseCase ?: return
        updateState { copy(pendingDeletePost = null, deletingPostIds = deletingPostIds + post.id) }
        viewModelScope.launch {
            when (val result = useCase(post.id)) {
                is Result.Success -> {
                    updateState {
                        copy(
                            deletingPostIds = deletingPostIds - post.id,
                            posts = posts.filterNot { it.id == post.id },
                            activeCommentPost = if (activeCommentPost?.id == post.id) null else activeCommentPost
                        )
                    }
                    emitEffect(CommunityViewEffect.ShowMessage("Publicación eliminada."))
                }
                is Result.Failure -> {
                    updateState { copy(deletingPostIds = deletingPostIds - post.id) }
                    val effect = if (result.error is AppError.Unauthorized) {
                        CommunityViewEffect.RequireAuth
                    } else {
                        CommunityViewEffect.ShowMessage(result.error.message)
                    }
                    emitEffect(effect)
                }
            }
        }
    }

    private fun openReportPost(post: FeedPost) {
        if (viewState.value.isGuest) {
            emitEffect(CommunityViewEffect.RequireAuth)
            return
        }
        updateState {
            copy(
                reportTarget = ReportTargetUi(
                    targetType = ReportTargetType.POST,
                    targetId = post.id,
                    label = post.authorName
                ),
                reportError = null
            )
        }
    }

    private fun submitReport(reason: ReportReason, details: String) {
        val target = viewState.value.reportTarget ?: return
        val useCase = submitReportUseCase ?: return
        if (viewState.value.isSubmittingReport) return
        updateState { copy(isSubmittingReport = true, reportError = null) }
        viewModelScope.launch {
            val input = ContentReportInput(target.targetType, target.targetId, reason, details)
            handleReportResult(target, useCase(input))
        }
    }

    private fun handleReportResult(target: ReportTargetUi, result: Result<SubmittedReport>) {
        when (result) {
            is Result.Success -> {
                updateState {
                    val updatedPosts = if (target.targetType == ReportTargetType.POST) {
                        posts.filterNot { it.id == target.targetId }
                    } else {
                        posts
                    }
                    copy(isSubmittingReport = false, reportTarget = null, reportError = null, posts = updatedPosts)
                }
                emitEffect(CommunityViewEffect.ShowMessage(result.data.message))
            }
            is Result.Failure -> handleReportError(result.error)
        }
    }

    private fun handleReportError(err: AppError) {
        when (err) {
            is AppError.Unauthorized -> {
                updateState { copy(isSubmittingReport = false, reportTarget = null) }
                emitEffect(CommunityViewEffect.RequireAuth)
            }
            is AppError.Conflict -> {
                updateState { copy(isSubmittingReport = false, reportTarget = null) }
                emitEffect(CommunityViewEffect.ShowMessage(err.message))
            }
            else -> updateState { copy(isSubmittingReport = false, reportError = err.message) }
        }
    }

    private companion object {
        const val COMMUNITY_RADIUS_METERS = 2000
    }
}
