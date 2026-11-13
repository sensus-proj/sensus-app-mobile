package com.example.sensus.presentation.buyer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.sensus.R
import com.example.sensus.domain.model.FeedPost
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusRed
import kotlinx.coroutines.flow.distinctUntilChanged

private const val COMMUNITY_NAV_CLEARANCE_DP = 96

@Composable
fun CommunityScreen(
    viewModel: CommunityViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
    onRequireAuth: () -> Unit = {},
    onMerchantClick: (String) -> Unit = {}
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.viewEffect.collect { if (it == CommunityViewEffect.RequireAuth) onRequireAuth() }
    }
    var showFriends by rememberSaveable { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxSize().background(SensusBackground)) {
        Row(modifier = Modifier.statusBarsPadding()) {
            TextButton(onClick = { showFriends = false }) { Text(stringResource(R.string.community_tab)) }
            TextButton(onClick = { showFriends = true }) { Text(stringResource(R.string.community_friends_tab)) }
        }
        if (showFriends) {
            FriendsCommunityScreen(state.friends, Modifier.weight(1f))
        } else {
            CommunityFeedContent(state, viewModel::dispatchIntent, onMerchantClick, Modifier.weight(1f))
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
    modifier: Modifier = Modifier
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
            items(state.posts, key = { it.id }) { post -> CommunityPostCard(post, onMerchantClick) }
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
private fun CommunityPostCard(post: FeedPost, onMerchantClick: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(post.authorName)
            Text(post.text)
            post.imageUrl?.let {
                AsyncImage(
                    model = it,
                    contentDescription = stringResource(R.string.community_post_photo),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Text("${post.distanceMeters / 100 * 100} m aprox. · ${post.createdAt}")
            post.merchantId?.let { id ->
                Text(
                    post.merchantName ?: stringResource(R.string.community_merchant_offers),
                    color = SensusPurple,
                    modifier = Modifier.clickable { onMerchantClick(id) }
                )
            }
        }
    }
}

private fun shouldLoadMore(index: Int?, state: CommunityViewState): Boolean {
    if (index == null || state.isLoading) return false
    return state.hasMore && index >= state.posts.size
}

@Composable
private fun CommunityFeedHeader(state: CommunityViewState, onIntent: (CommunityViewIntent) -> Unit) {
    Text(stringResource(R.string.community_title), color = SensusPurple)
    Button(onClick = { onIntent(CommunityViewIntent.NewPost) }, colors = ButtonDefaults.buttonColors(containerColor = SensusPurple)) {
        Text(stringResource(R.string.community_new_post))
    }
    state.errorMessage?.let { Text(it, color = SensusRed) }
    if (state.isCached) Text(stringResource(R.string.community_cached))
    if (state.posts.isEmpty() && !state.isLoading) Text(stringResource(R.string.community_empty))
}
