package com.example.sensus.presentation.buyer

import com.example.sensus.domain.model.ConnectedFriend
import com.example.sensus.presentation.common.ComposeUiModel

@ComposeUiModel
data class CommunityCallbacks(
    val onRequireAuth: () -> Unit = {},
    val onFriendClick: (ConnectedFriend) -> Unit = {},
    val isFriendsSelected: Boolean = false,
    val onSelectFriendsTab: (Boolean) -> Unit = {}
)
