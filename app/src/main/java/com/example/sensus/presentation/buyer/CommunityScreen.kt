package com.example.sensus.presentation.buyer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sensus.R
import com.example.sensus.theme.sensusTextButtonColors

private enum class CommunitySubTab {
    ACTIVITY,
    FRIENDS
}

@Composable
fun CommunityScreen(
    viewModel: FriendsActivityViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
    callbacks: CommunityCallbacks = CommunityCallbacks(),
    openFriendsSignal: Int = 0
) {
    var selectedTab by rememberSaveable {
        mutableStateOf(if (callbacks.isFriendsSelected) CommunitySubTab.FRIENDS else CommunitySubTab.ACTIVITY)
    }
    LaunchedEffect(callbacks.isFriendsSelected) {
        selectedTab = when {
            callbacks.isFriendsSelected -> CommunitySubTab.FRIENDS
            selectedTab == CommunitySubTab.FRIENDS -> CommunitySubTab.ACTIVITY
            else -> selectedTab
        }
    }
    LaunchedEffect(openFriendsSignal) {
        if (openFriendsSignal > 0) {
            selectedTab = CommunitySubTab.FRIENDS
            callbacks.onSelectFriendsTab(true)
        }
    }
    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        CommunitySubTabBar(selectedTab) { tab ->
            selectedTab = tab
            callbacks.onSelectFriendsTab(tab == CommunitySubTab.FRIENDS)
        }
        CommunitySubTabBody(selectedTab, viewModel, callbacks, Modifier.weight(1f))
    }
}

@Composable
private fun CommunitySubTabBar(selectedTab: CommunitySubTab, onSelectTab: (CommunitySubTab) -> Unit) {
    Row(modifier = Modifier.statusBarsPadding().padding(horizontal = 8.dp)) {
        listOf(
            CommunitySubTab.ACTIVITY to R.string.community_activity_tab,
            CommunitySubTab.FRIENDS to R.string.community_friends_tab
        ).forEach { (tab, labelRes) ->
            TextButton(colors = sensusTextButtonColors(), onClick = { onSelectTab(tab) }) {
                Text(stringResource(labelRes), fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun CommunitySubTabBody(selectedTab: CommunitySubTab, activityViewModel: FriendsActivityViewModel, callbacks: CommunityCallbacks, modifier: Modifier) {
    val activityState by activityViewModel.viewState.collectAsStateWithLifecycle()
    when (selectedTab) {
        CommunitySubTab.ACTIVITY -> FriendsActivityFeedContent(
            state = activityState,
            onRefresh = { activityViewModel.dispatchIntent(FriendsActivityViewIntent.Refresh) },
            onRequireAuth = callbacks.onRequireAuth,
            modifier = modifier
        )
        CommunitySubTab.FRIENDS -> FriendsCommunityTab(
            onRequireAuth = callbacks.onRequireAuth,
            onFriendClick = callbacks.onFriendClick,
            modifier = modifier
        )
    }
}
