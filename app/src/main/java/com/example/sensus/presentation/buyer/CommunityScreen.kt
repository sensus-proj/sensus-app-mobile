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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusRed
import com.example.sensus.theme.SensusVioletBg
import kotlinx.coroutines.flow.distinctUntilChanged

private const val COMMUNITY_NAV_CLEARANCE_DP = 96

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
    viewModel: CommunityViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
    callbacks: CommunityCallbacks = CommunityCallbacks(),
    openFriendsSignal: Int = 0
) {
    val savedViewModel: SavedInterestsViewModel = hiltViewModel()
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    val savedState by savedViewModel.viewState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.viewEffect.collect { if (it == CommunityViewEffect.RequireAuth) callbacks.onRequireAuth() }
    }
    SavedInterestsEffectHandler(savedViewModel = savedViewModel, onRequireAuth = callbacks.onRequireAuth)
    var localShowFriends by rememberSaveable { mutableStateOf(callbacks.isFriendsSelected) }
    LaunchedEffect(callbacks.isFriendsSelected) { localShowFriends = callbacks.isFriendsSelected }
    LaunchedEffect(openFriendsSignal) {
        if (openFriendsSignal > 0) {
            localShowFriends = true
            callbacks.onSelectFriendsTab(true)
        }
    }
    Column(modifier = modifier.fillMaxSize().background(SensusBackground)) {
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
            CommunityFeedContent(
                state = state,
                onIntent = viewModel::dispatchIntent,
                onMerchantClick = callbacks.onMerchantClick,
                saveActions = CommunitySaveActions(
                    savedPostIds = savedState.savedPostIds,
                    onToggleSavePost = { savedViewModel.dispatchIntent(SavedInterestsViewIntent.TogglePost(it)) }
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
    if (state.showComposer) CreateFeedPostSheet(state, viewModel::dispatchIntent)
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
            items(state.posts, key = { it.id }) { post ->
                CommunityPostCard(
                    post = post,
                    onMerchantClick = onMerchantClick,
                    isSaved = post.id in saveActions.savedPostIds,
                    onSaveClick = { saveActions.onToggleSavePost(post) }
                )
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
}

@Composable
private fun CommunityPostCard(post: FeedPost, onMerchantClick: (String) -> Unit, isSaved: Boolean = false, onSaveClick: () -> Unit = {}) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(post.authorName)
            Text(post.text)
            post.imageUrl?.let {
                AsyncImage(model = it, contentDescription = stringResource(R.string.community_post_photo), modifier = Modifier.fillMaxWidth())
            }
            Text("${post.distanceMeters / 100 * 100} m aprox. · ${post.createdAt}")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                CommunityPostMerchantLabel(post = post, onMerchantClick = onMerchantClick)
                CommunityPostSavePill(isSaved = isSaved, onSaveClick = onSaveClick)
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
