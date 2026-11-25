package com.example.sensus.presentation.buyer

import com.example.sensus.common.AppError
import com.example.sensus.domain.model.FeedComment
import com.example.sensus.domain.model.FeedPost
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.ReportReason
import com.example.sensus.domain.model.ReportTargetType
import com.example.sensus.presentation.base.ViewEffect
import com.example.sensus.presentation.base.ViewIntent
import com.example.sensus.presentation.base.ViewState
import com.example.sensus.presentation.common.ComposeUiModel

@ComposeUiModel
data class ReportTargetUi(
    val targetType: ReportTargetType,
    val targetId: String,
    val label: String
)

data class CommunityViewState(
    val posts: List<FeedPost> = emptyList(),
    val currentUserName: String = "Usuario Sensus",
    val isGuest: Boolean = true,
    val isLoading: Boolean = false,
    val isPublishing: Boolean = false,
    val isCached: Boolean = false,
    val hasMore: Boolean = false,
    val nextPage: Int = 1,
    val feedError: AppError? = null,
    val publishError: AppError? = null,
    val hasLoadedFeed: Boolean = false,
    val showComposer: Boolean = false,
    val draftText: String = "",
    val merchantId: String? = null,
    val hasImage: Boolean = false,
    val nearbyMerchants: List<ProductOffer> = emptyList(),
    val likingPostIds: Set<String> = emptySet(),
    val deletingPostIds: Set<String> = emptySet(),
    val activeCommentPost: FeedPost? = null,
    val comments: List<FeedComment> = emptyList(),
    val isLoadingComments: Boolean = false,
    val isSubmittingComment: Boolean = false,
    val commentDraft: String = "",
    val commentError: String? = null,
    val pendingDeletePost: FeedPost? = null,
    val reportTarget: ReportTargetUi? = null,
    val isSubmittingReport: Boolean = false,
    val reportError: String? = null
) : ViewState

sealed interface CommunityViewIntent : ViewIntent {
    data object Refresh : CommunityViewIntent
    data object LoadMore : CommunityViewIntent
    data object NewPost : CommunityViewIntent
    data object CloseComposer : CommunityViewIntent
    data class TextChanged(val text: String) : CommunityViewIntent
    data class MerchantSelected(val id: String?) : CommunityViewIntent
    class ImageSelected(val bytes: ByteArray?) : CommunityViewIntent
    data object ImageReadFailed : CommunityViewIntent
    data object Submit : CommunityViewIntent
    data class ToggleLike(val postId: String) : CommunityViewIntent
    data class OpenComments(val post: FeedPost) : CommunityViewIntent
    data object CloseComments : CommunityViewIntent
    data class CommentDraftChanged(val text: String) : CommunityViewIntent
    data object SubmitComment : CommunityViewIntent
    data class RequestDeletePost(val post: FeedPost) : CommunityViewIntent
    data object CancelDeletePost : CommunityViewIntent
    data object ConfirmDeletePost : CommunityViewIntent
    data class OpenReportPost(val post: FeedPost) : CommunityViewIntent
    data object CloseReport : CommunityViewIntent
    data class SubmitReport(val reason: ReportReason, val details: String) : CommunityViewIntent
}

sealed interface CommunityViewEffect : ViewEffect {
    data object RequireAuth : CommunityViewEffect
    data class ShowMessage(val message: String) : CommunityViewEffect
}
