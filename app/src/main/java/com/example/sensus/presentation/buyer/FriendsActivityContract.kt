package com.example.sensus.presentation.buyer

import com.example.sensus.domain.model.CommunityActivityItem
import com.example.sensus.presentation.base.ViewEffect
import com.example.sensus.presentation.base.ViewIntent
import com.example.sensus.presentation.base.ViewState

data class FriendsActivityViewState(
    val isGuest: Boolean = true,
    val isLoading: Boolean = false,
    val items: List<CommunityActivityItem> = emptyList(),
    val errorMessage: String? = null
) : ViewState

sealed interface FriendsActivityViewIntent : ViewIntent {
    data object Refresh : FriendsActivityViewIntent
}

sealed interface FriendsActivityViewEffect : ViewEffect
