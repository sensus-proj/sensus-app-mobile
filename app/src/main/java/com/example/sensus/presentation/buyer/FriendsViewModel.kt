package com.example.sensus.presentation.buyer

import androidx.lifecycle.viewModelScope
import com.example.sensus.domain.model.AddFriendResult
import com.example.sensus.domain.model.ConnectedFriend
import com.example.sensus.domain.model.FriendNoticeType
import com.example.sensus.domain.model.PendingFriendRequest
import com.example.sensus.domain.model.SentFriendRequest
import com.example.sensus.domain.model.ShareOfferOutcome
import com.example.sensus.domain.model.SharedOfferMessage
import com.example.sensus.domain.repository.FriendsRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val friendsRepository: FriendsRepository,
    private val sessionRepository: SessionRepository
) : MviViewModel<FriendsViewState, FriendsViewIntent, FriendsViewEffect>(FriendsViewState()) {

    private val threadState = MutableStateFlow(ThreadUiSnapshot())

    init {
        viewModelScope.launch {
            val listsFlow = combine(
                friendsRepository.friends,
                friendsRepository.pendingRequests,
                friendsRepository.sentRequests,
                friendsRepository.newlyArrivedRequest,
                friendsRepository.newlyAcceptedFriend
            ) { friends, pending, sent, arrived, accepted ->
                FriendListsSnapshot(friends, pending, sent, arrived, accepted)
            }
            val identityFlow = combine(
                friendsRepository.myInviteCode,
                friendsRepository.myInviteUrl,
                sessionRepository.sessionState,
                friendsRepository.pendingInviteTrigger,
                threadState
            ) { code, url, session, pendingTrigger, thread ->
                FriendIdentitySnapshot(code, url, session.isGuest, pendingTrigger, thread)
            }
            combine(listsFlow, identityFlow) { lists, identity ->
                FriendsViewState(
                    friends = lists.friends,
                    pendingRequests = lists.pending,
                    sentRequests = lists.sent,
                    newlyArrivedRequest = lists.arrived,
                    newlyAcceptedFriend = lists.accepted,
                    activeThreadFriendUserId = identity.thread.activeFriendUserId,
                    sharedOffers = identity.thread.offers,
                    isLoadingThread = identity.thread.isLoading,
                    isSharingOffer = identity.thread.isSharing,
                    myInviteCode = identity.code,
                    myInviteUrl = identity.url,
                    isGuest = identity.isGuest,
                    pendingInviteTrigger = identity.pendingTrigger
                )
            }.collect { next -> updateState { next } }
        }
    }

    override fun processIntent(intent: FriendsViewIntent) {
        when (intent) {
            FriendsViewIntent.Refresh -> viewModelScope.launch { friendsRepository.syncFromBackend() }
            is FriendsViewIntent.AddFromInput -> addFromInput(intent.rawInput, intent.addedVia)
            is FriendsViewIntent.AcceptPendingRequest -> respondToRequest(intent.requestId, accept = true)
            is FriendsViewIntent.RejectPendingRequest -> respondToRequest(intent.requestId, accept = false)
            else -> processSecondaryIntent(intent)
        }
    }

    private fun processSecondaryIntent(intent: FriendsViewIntent) {
        when (intent) {
            is FriendsViewIntent.RemoveFriend -> viewModelScope.launch {
                friendsRepository.removeFriend(intent.inviteCode)
                emitEffect(FriendsViewEffect.ShowMessage("Amigo eliminado de tu red."))
            }
            is FriendsViewIntent.LoadSharedOffers -> loadSharedOffers(intent.friendUserId)
            FriendsViewIntent.ClearSharedOffers -> threadState.value = threadState.value.copy(
                activeFriendUserId = null,
                offers = emptyList(),
                isLoading = false
            )
            is FriendsViewIntent.ShareOffer -> shareOffer(intent.offerId, intent.recipientUserIds)
            FriendsViewIntent.ConsumePendingDeepLink -> consumePendingDeepLink()
            FriendsViewIntent.NotificationShown -> friendsRepository.markNoticeHandled(FriendNoticeType.INCOMING_REQUEST)
            FriendsViewIntent.AcceptedNotificationShown -> friendsRepository.markNoticeHandled(FriendNoticeType.ACCEPTED_FRIEND)
            else -> Unit
        }
    }

    private fun loadSharedOffers(friendUserId: String) {
        val switchedFriend = threadState.value.activeFriendUserId != friendUserId
        threadState.value = if (switchedFriend) {
            threadState.value.copy(activeFriendUserId = friendUserId, offers = emptyList(), isLoading = true)
        } else {
            threadState.value.copy(isLoading = true)
        }
        viewModelScope.launch {
            val offers = friendsRepository.getSharedOffersWithFriend(friendUserId)
            if (threadState.value.activeFriendUserId == friendUserId) {
                threadState.value = threadState.value.copy(offers = offers, isLoading = false)
            }
        }
    }

    private fun shareOffer(offerId: String, recipientIds: List<String>) {
        if (sessionRepository.sessionState.value.isGuest) {
            emitEffect(FriendsViewEffect.RequireAuth)
            return
        }
        viewModelScope.launch {
            threadState.value = threadState.value.copy(isSharing = true)
            val outcome = friendsRepository.shareOfferWithFriends(offerId, recipientIds)
            threadState.value = threadState.value.copy(isSharing = false)
            when (outcome) {
                is ShareOfferOutcome.Shared -> {
                    emitEffect(FriendsViewEffect.OfferShared(outcome.count))
                    val label = if (outcome.count == 1) "1 amigo" else "${outcome.count} amigos"
                    emitEffect(FriendsViewEffect.ShowMessage("Oferta compartida con $label."))
                }
                ShareOfferOutcome.GuestNotAllowed -> emitEffect(FriendsViewEffect.RequireAuth)
                is ShareOfferOutcome.ValidationError -> emitEffect(FriendsViewEffect.ShowMessage(outcome.message))
                is ShareOfferOutcome.NetworkFailure -> emitEffect(FriendsViewEffect.ShowMessage(outcome.message))
            }
        }
    }

    private fun respondToRequest(requestId: String, accept: Boolean) {
        viewModelScope.launch {
            val ok = if (accept) {
                friendsRepository.acceptPendingRequest(requestId)
            } else {
                friendsRepository.rejectPendingRequest(requestId)
            }
            val msg = when {
                !ok -> "No se pudo actualizar la solicitud. Verifica tu conexión."
                accept -> "Solicitud aceptada. Ya están conectados como amigos."
                else -> "Solicitud cancelada."
            }
            emitEffect(FriendsViewEffect.ShowMessage(msg))
        }
    }

    private fun addFromInput(rawInput: String, addedVia: String) {
        if (sessionRepository.sessionState.value.isGuest) {
            emitEffect(FriendsViewEffect.RequireAuth)
            return
        }
        viewModelScope.launch {
            emitOutcome(friendsRepository.addFriendFromRaw(rawInput, addedVia), openFriendsTab = false)
        }
    }

    private fun consumePendingDeepLink() {
        viewModelScope.launch {
            val outcome = friendsRepository.consumePendingInviteIfAuthenticated()
            if (outcome == null) {
                if (friendsRepository.pendingRequests.value.isNotEmpty()) {
                    emitEffect(FriendsViewEffect.OpenFriendsTab)
                }
                return@launch
            }
            emitOutcome(outcome, openFriendsTab = true)
        }
    }

    private fun emitOutcome(outcome: AddFriendResult, openFriendsTab: Boolean) {
        when (outcome) {
            is AddFriendResult.Added -> emitEffect(
                FriendsViewEffect.ShowMessage("${outcome.friend.displayName} ahora está en tus amigos.", openFriendsTab)
            )
            is AddFriendResult.RequestSent -> emitEffect(
                FriendsViewEffect.ShowMessage("Solicitud enviada a ${outcome.request.targetName}.", openFriendsTab)
            )
            is AddFriendResult.AlreadyPending -> emitEffect(
                FriendsViewEffect.ShowMessage("Ya enviaste una solicitud pendiente a ${outcome.targetName}.", openFriendsTab)
            )
            is AddFriendResult.AlreadyConnected -> emitEffect(
                FriendsViewEffect.ShowMessage("${outcome.friend.displayName} ya es tu amigo.", openFriendsTab)
            )
            AddFriendResult.SelfInviteNotAllowed -> emitEffect(
                FriendsViewEffect.ShowMessage("No puedes agregarte a ti mismo como amigo.")
            )
            AddFriendResult.InvalidCode -> emitEffect(
                FriendsViewEffect.ShowMessage("Código o QR de invitación inválido.")
            )
            is AddFriendResult.NetworkFailure -> emitEffect(
                FriendsViewEffect.ShowMessage(outcome.message, openFriendsTab)
            )
            AddFriendResult.GuestNotAllowed -> emitEffect(FriendsViewEffect.RequireAuth)
        }
    }
}

private data class ThreadUiSnapshot(
    val activeFriendUserId: String? = null,
    val offers: List<SharedOfferMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isSharing: Boolean = false
)

private data class FriendListsSnapshot(
    val friends: List<ConnectedFriend>,
    val pending: List<PendingFriendRequest>,
    val sent: List<SentFriendRequest>,
    val arrived: PendingFriendRequest?,
    val accepted: ConnectedFriend?
)

private data class FriendIdentitySnapshot(
    val code: String,
    val url: String,
    val isGuest: Boolean,
    val pendingTrigger: Int,
    val thread: ThreadUiSnapshot
)
