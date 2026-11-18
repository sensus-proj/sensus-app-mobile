package com.example.sensus.presentation.buyer

import com.example.sensus.domain.model.ConnectedFriend
import com.example.sensus.domain.model.PendingFriendRequest
import com.example.sensus.domain.model.SentFriendRequest
import com.example.sensus.domain.model.SharedOfferMessage
import com.example.sensus.presentation.base.ViewEffect
import com.example.sensus.presentation.base.ViewIntent
import com.example.sensus.presentation.base.ViewState

data class FriendsViewState(
    val friends: List<ConnectedFriend> = emptyList(),
    val pendingRequests: List<PendingFriendRequest> = emptyList(),
    val sentRequests: List<SentFriendRequest> = emptyList(),
    val newlyArrivedRequest: PendingFriendRequest? = null,
    val newlyAcceptedFriend: ConnectedFriend? = null,
    val activeThreadFriendUserId: String? = null,
    val sharedOffers: List<SharedOfferMessage> = emptyList(),
    val isLoadingThread: Boolean = false,
    val isSharingOffer: Boolean = false,
    val myInviteCode: String = "",
    val myInviteUrl: String = "",
    val isGuest: Boolean = false,
    val pendingInviteTrigger: Int = 0
) : ViewState

sealed interface FriendsViewIntent : ViewIntent {
    data object Refresh : FriendsViewIntent
    data class AddFromInput(val rawInput: String, val addedVia: String = "Código") : FriendsViewIntent
    data class AcceptPendingRequest(val requestId: String) : FriendsViewIntent
    data class RejectPendingRequest(val requestId: String) : FriendsViewIntent
    data class RemoveFriend(val inviteCode: String) : FriendsViewIntent
    data class LoadSharedOffers(val friendUserId: String) : FriendsViewIntent
    data object ClearSharedOffers : FriendsViewIntent
    data class ShareOffer(val offerId: String, val recipientUserIds: List<String>) : FriendsViewIntent
    data object ConsumePendingDeepLink : FriendsViewIntent
    data object NotificationShown : FriendsViewIntent
    data object AcceptedNotificationShown : FriendsViewIntent
}

sealed interface FriendsViewEffect : ViewEffect {
    data class ShowMessage(val message: String, val openFriendsTab: Boolean = false) : FriendsViewEffect
    data class OfferShared(val count: Int) : FriendsViewEffect
    data object OpenFriendsTab : FriendsViewEffect
    data object RequireAuth : FriendsViewEffect
}
