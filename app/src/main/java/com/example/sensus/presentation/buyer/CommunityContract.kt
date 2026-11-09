package com.example.sensus.presentation.buyer

import com.example.sensus.domain.model.FriendShare
import com.example.sensus.presentation.base.ViewEffect
import com.example.sensus.presentation.base.ViewIntent
import com.example.sensus.presentation.base.ViewState

data class CommunityViewState(
    val friends: List<FriendShare> = emptyList(),
    val currentUserName: String = "Usuario Sensus"
) : ViewState

sealed interface CommunityViewIntent : ViewIntent

sealed interface CommunityViewEffect : ViewEffect
