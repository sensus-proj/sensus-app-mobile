package com.example.sensus.presentation.buyer

import androidx.lifecycle.viewModelScope
import com.example.sensus.domain.repository.StoreRepository
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val storeRepository: StoreRepository
) : MviViewModel<NotificationsViewState, NotificationsViewIntent, NotificationsViewEffect>(NotificationsViewState()) {

    init {
        val list = storeRepository.getSampleNotifications()
        updateState { copy(notifications = list) }
    }

    override fun processIntent(intent: NotificationsViewIntent) {
        when (intent) {
            is NotificationsViewIntent.CategorySelected -> updateState { copy(selectedCategory = intent.category) }
        }
    }
}
