package com.example.sensus.presentation.buyer

import androidx.lifecycle.viewModelScope
import com.example.sensus.domain.model.AddFriendResult
import com.example.sensus.domain.model.PendingFriendRequest
import com.example.sensus.domain.repository.FriendsRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val friendsRepository: FriendsRepository,
    private val sessionRepository: SessionRepository
) : MviViewModel<FriendsViewState, FriendsViewIntent, FriendsViewEffect>(FriendsViewState()) {

    init {
        viewModelScope.launch {
            val listsFlow = combine(
                friendsRepository.friends,
                friendsRepository.pendingRequests,
                friendsRepository.sentRequests,
                friendsRepository.newlyArrivedRequest
            ) { friends, pending, sent, arrived ->
                FriendListsSnapshot(friends, pending, sent, arrived)
            }
            combine(
                listsFlow,
                friendsRepository.myInviteCode,
                friendsRepository.myInviteUrl,
                sessionRepository.sessionState,
                friendsRepository.pendingInviteTrigger
            ) { lists, code, url, session, pendingTrigger ->
                FriendsViewState(
                    friends = lists.friends,
                    pendingRequests = lists.pending,
                    sentRequests = lists.sent,
                    newlyArrivedRequest = lists.arrived,
                    myInviteCode = code,
                    myInviteUrl = url,
                    isGuest = session.isGuest,
                    pendingInviteTrigger = pendingTrigger
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
            is FriendsViewIntent.RemoveFriend -> viewModelScope.launch {
                friendsRepository.removeFriend(intent.inviteCode)
                emitEffect(FriendsViewEffect.ShowMessage("Amigo eliminado de tu red."))
            }
            FriendsViewIntent.ConsumePendingDeepLink -> consumePendingDeepLink()
            FriendsViewIntent.NotificationShown -> friendsRepository.markIncomingNotificationHandled()
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

private data class FriendListsSnapshot(
    val friends: List<com.example.sensus.domain.model.ConnectedFriend>,
    val pending: List<PendingFriendRequest>,
    val sent: List<com.example.sensus.domain.model.SentFriendRequest>,
    val arrived: PendingFriendRequest?
)
