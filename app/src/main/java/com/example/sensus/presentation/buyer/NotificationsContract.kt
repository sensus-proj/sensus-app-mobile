package com.example.sensus.presentation.buyer

import com.example.sensus.domain.model.NotificationItem
import com.example.sensus.presentation.base.ViewEffect
import com.example.sensus.presentation.base.ViewIntent
import com.example.sensus.presentation.base.ViewState

data class NotificationsViewState(
    val notifications: List<NotificationItem> = emptyList(),
    val selectedCategory: String = "Todas"
) : ViewState

sealed interface NotificationsViewIntent : ViewIntent {
    data class CategorySelected(val category: String) : NotificationsViewIntent
}

sealed interface NotificationsViewEffect : ViewEffect
