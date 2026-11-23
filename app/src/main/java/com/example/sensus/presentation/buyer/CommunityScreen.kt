package com.example.sensus.presentation.buyer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.sensus.R
import com.example.sensus.common.AppError
import com.example.sensus.domain.model.ConnectedFriend
import com.example.sensus.domain.model.FeedPost
import com.example.sensus.presentation.common.ComposeUiModel
import com.example.sensus.presentation.components.states.ShimmerFeedList
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusRed
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.SensusVioletBg
import kotlinx.coroutines.flow.distinctUntilChanged

private const val COMMUNITY_NAV_CLEARANCE_DP = 96
private const val ACTION_TOUCH_MIN_DP = 48
private const val METER_ROUNDING_STEP = 100

@ComposeUiModel
internal data class CommunitySaveActions(
    val savedPostIds: Set<String> = emptySet(),
    val onToggleSavePost: (FeedPost) -> Unit = {}
)

@ComposeUiModel
data class CommunityCallbacks(
    val onRequireAuth: () -> Unit = {},
    val onMerchantClick: (String) -> Unit = {},
    val onFriendClick: (ConnectedFriend) -> Unit = {},
    val isFriendsSelected: Boolean = false,
    val onSelectFriendsTab: (Boolean) -> Unit = {}
)

@Composable
fun CommunityScreen(
    viewModel: FriendsActivityViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
    callbacks: CommunityCallbacks = CommunityCallbacks(),
    openFriendsSignal: Int = 0
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    var localShowFriends by rememberSaveable { mutableStateOf(callbacks.isFriendsSelected) }
    LaunchedEffect(callbacks.isFriendsSelected) { localShowFriends = callbacks.isFriendsSelected }
    LaunchedEffect(openFriendsSignal) {
        if (openFriendsSignal > 0) {
            localShowFriends = true
            callbacks.onSelectFriendsTab(true)
        }
    }
    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(modifier = Modifier.statusBarsPadding()) {
            TextButton(onClick = {
                localShowFriends = false
                callbacks.onSelectFriendsTab(false)
            }) { Text(stringResource(R.string.community_tab)) }
            TextButton(onClick = {
                localShowFriends = true
                callbacks.onSelectFriendsTab(true)
            }) { Text(stringResource(R.string.community_friends_tab)) }
        }
        if (localShowFriends) {
            FriendsCommunityTab(onRequireAuth = callbacks.onRequireAuth, onFriendClick = callbacks.onFriendClick, modifier = Modifier.weight(1f))
        } else {
            FriendsActivityFeedContent(
                state = state,
                onRefresh = { viewModel.dispatchIntent(FriendsActivityViewIntent.Refresh) },
                onRequireAuth = callbacks.onRequireAuth,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CommunityFeedContent(
    state: CommunityViewState,
    onIntent: (CommunityViewIntent) -> Unit,
    onMerchantClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    saveActions: CommunitySaveActions = CommunitySaveActions()
) {
    val listState = rememberLazyListState()
    LaunchedEffect(listState, state.hasMore, state.posts.size, state.isLoading) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged().collect { index ->
                if (shouldLoadMore(index, state)) {
                    onIntent(CommunityViewIntent.LoadMore)
                }
            }
    }
    PullToRefreshBox(isRefreshing = state.isLoading, onRefresh = { onIntent(CommunityViewIntent.Refresh) }, modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentPadding = PaddingValues(bottom = COMMUNITY_NAV_CLEARANCE_DP.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                CommunityFeedHeader(state, onIntent)
            }
            if (state.isLoading && state.posts.isEmpty()) {
                item {
                    ShimmerFeedList()
                }
            } else {
                items(state.posts, key = { it.id }) { post ->
                    CommunityPostCard(
                        post = post,
                        onMerchantClick = onMerchantClick,
                        actions = CommunityPostCardActions(
                            isSaved = post.id in saveActions.savedPostIds,
                            isLiking = post.id in state.likingPostIds,
                            onSaveClick = { saveActions.onToggleSavePost(post) },
                            onIntent = onIntent
                        )
                    )
                }
            }
            item {
                if (state.hasMore) {
                    TextButton(onClick = {
                        onIntent(CommunityViewIntent.LoadMore)
                    }, enabled = !state.isLoading) { Text(stringResource(R.string.community_more)) }
                }
                TextButton(onClick = { onIntent(CommunityViewIntent.Refresh) }, enabled = !state.isLoading) { Text(stringResource(R.string.community_refresh)) }
            }
        }
    }

    CommunityInteractionModals(state = state, onIntent = onIntent)
}

@Composable
private fun CommunityInteractionModals(state: CommunityViewState, onIntent: (CommunityViewIntent) -> Unit) {
    state.activeCommentPost?.let { post ->
        PostCommentsSheet(
            model = CommentsSheetModel(
                post = post,
                comments = state.comments,
                isLoading = state.isLoadingComments,
                isSubmitting = state.isSubmittingComment,
                draft = state.commentDraft,
                errorMessage = state.commentError
            ),
            onDraftChange = { onIntent(CommunityViewIntent.CommentDraftChanged(it)) },
            onSend = { onIntent(CommunityViewIntent.SubmitComment) },
            onDismiss = { onIntent(CommunityViewIntent.CloseComments) }
        )
    }

    state.reportTarget?.let { target ->
        ReportContentDialog(
            model = ReportDialogModel(
                targetLabel = target.label,
                isSubmitting = state.isSubmittingReport,
                errorMessage = state.reportError
            ),
            onDismiss = { onIntent(CommunityViewIntent.CloseReport) },
            onSubmit = { reason, details -> onIntent(CommunityViewIntent.SubmitReport(reason, details)) }
        )
    }

    state.pendingDeletePost?.let {
        AlertDialog(
            onDismissRequest = { onIntent(CommunityViewIntent.CancelDeletePost) },
            title = { Text(stringResource(R.string.ft19_delete_post_title)) },
            text = { Text(stringResource(R.string.ft19_delete_post_body)) },
            confirmButton = {
                Button(
                    onClick = { onIntent(CommunityViewIntent.ConfirmDeletePost) },
                    colors = ButtonDefaults.buttonColors(containerColor = SensusRed)
                ) {
                    Text(stringResource(R.string.ft19_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(CommunityViewIntent.CancelDeletePost) }) {
                    Text(stringResource(R.string.ft19_cancel))
                }
            }
        )
    }
}

@ComposeUiModel
internal data class CommunityPostCardActions(
    val isSaved: Boolean = false,
    val isLiking: Boolean = false,
    val onSaveClick: () -> Unit = {},
    val onIntent: (CommunityViewIntent) -> Unit = {}
)

@Composable
private fun CommunityPostCard(post: FeedPost, onMerchantClick: (String) -> Unit, actions: CommunityPostCardActions = CommunityPostCardActions()) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(post.authorName, fontWeight = FontWeight.SemiBold)
            Text(post.text)
            post.imageUrl?.let {
                AsyncImage(model = it, contentDescription = stringResource(R.string.community_post_photo), modifier = Modifier.fillMaxWidth())
            }
            Text("${post.distanceMeters / METER_ROUNDING_STEP * METER_ROUNDING_STEP} m aprox. · ${post.createdAt}")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                CommunityPostMerchantLabel(post = post, onMerchantClick = onMerchantClick)
                CommunityPostSavePill(isSaved = actions.isSaved, onSaveClick = actions.onSaveClick)
            }
            HorizontalDivider()
            CommunityPostActionsRow(post = post, isLiking = actions.isLiking, onIntent = actions.onIntent)
        }
    }
}

@Composable
private fun CommunityPostActionsRow(post: FeedPost, isLiking: Boolean, onIntent: (CommunityViewIntent) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PostEngagementActions(post = post, isLiking = isLiking, onIntent = onIntent)
        PostModerationActions(post = post, onIntent = onIntent)
    }
}

@Composable
private fun PostEngagementActions(post: FeedPost, isLiking: Boolean, onIntent: (CommunityViewIntent) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = Modifier
                .sizeIn(minWidth = ACTION_TOUCH_MIN_DP.dp, minHeight = ACTION_TOUCH_MIN_DP.dp)
                .clickable(enabled = !isLiking) { onIntent(CommunityViewIntent.ToggleLike(post.id)) }
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (post.isLikedByMe) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = stringResource(R.string.ft19_like_post),
                tint = if (post.isLikedByMe) SensusRed else SensusTextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("${post.likesCount}", fontSize = 13.sp, color = SensusTextSecondary)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Row(
            modifier = Modifier
                .sizeIn(minWidth = ACTION_TOUCH_MIN_DP.dp, minHeight = ACTION_TOUCH_MIN_DP.dp)
                .clickable { onIntent(CommunityViewIntent.OpenComments(post)) }
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.ChatBubbleOutline,
                contentDescription = stringResource(R.string.ft19_comment_post),
                tint = SensusTextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("${post.commentsCount}", fontSize = 13.sp, color = SensusTextSecondary)
        }
    }
}

@Composable
private fun PostModerationActions(post: FeedPost, onIntent: (CommunityViewIntent) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = { onIntent(CommunityViewIntent.OpenReportPost(post)) },
            modifier = Modifier.size(ACTION_TOUCH_MIN_DP.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Flag,
                contentDescription = stringResource(R.string.ft19_report_post),
                tint = SensusTextSecondary
            )
        }
        if (post.isMine) {
            IconButton(
                onClick = { onIntent(CommunityViewIntent.RequestDeletePost(post)) },
                modifier = Modifier.size(ACTION_TOUCH_MIN_DP.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.ft19_delete_post_cd),
                    tint = SensusRed
                )
            }
        }
    }
}

@Composable
private fun CommunityPostMerchantLabel(post: FeedPost, onMerchantClick: (String) -> Unit) {
    val merchantId = post.merchantId
    if (merchantId != null) {
        Text(
            text = post.merchantName ?: stringResource(R.string.community_merchant_offers),
            color = SensusPurple,
            modifier = Modifier.clickable { onMerchantClick(merchantId) }
        )
    } else {
        Spacer(modifier = Modifier.width(1.dp))
    }
}

@Composable
private fun CommunityPostSavePill(isSaved: Boolean, onSaveClick: () -> Unit) {
    val containerColor = if (isSaved) SensusPurple else SensusVioletBg
    val contentColor = if (isSaved) Color.White else SensusPurple
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onSaveClick)
    ) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isSaved) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(if (isSaved) R.string.saved_interests_post_saved else R.string.saved_interests_post_interest),
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun shouldLoadMore(index: Int?, state: CommunityViewState): Boolean {
    if (index == null || state.isLoading || state.feedError != null) return false
    return state.hasMore && index >= state.posts.size
}

@Composable
private fun CommunityFeedHeader(state: CommunityViewState, onIntent: (CommunityViewIntent) -> Unit) {
    Text(stringResource(R.string.community_title), color = SensusPurple)
    Button(onClick = { onIntent(CommunityViewIntent.NewPost) }, colors = ButtonDefaults.buttonColors(containerColor = SensusPurple)) {
        Text(stringResource(R.string.community_new_post))
    }
    state.feedError?.let {
        val message = if (it is AppError.NotFound) stringResource(R.string.community_feed_unavailable) else it.message
        Text(message, color = SensusRed)
    }
    if (state.isCached) Text(stringResource(R.string.community_cached))
    val hasSuccessfulResult = state.hasLoadedFeed && state.feedError == null
    if (hasSuccessfulResult && state.posts.isEmpty() && !state.isLoading) {
        Text(stringResource(R.string.community_empty))
    }
}
