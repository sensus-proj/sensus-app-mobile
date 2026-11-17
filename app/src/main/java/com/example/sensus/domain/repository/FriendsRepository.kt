package com.example.sensus.domain.repository

import com.example.sensus.domain.model.AddFriendResult
import com.example.sensus.domain.model.ConnectedFriend
import com.example.sensus.domain.model.PendingFriendRequest
import com.example.sensus.domain.model.SentFriendRequest
import kotlinx.coroutines.flow.StateFlow

interface FriendsRepository {
    val friends: StateFlow<List<ConnectedFriend>>
    val pendingRequests: StateFlow<List<PendingFriendRequest>>
    val sentRequests: StateFlow<List<SentFriendRequest>>
    val newlyArrivedRequest: StateFlow<PendingFriendRequest?>
    val myInviteCode: StateFlow<String>
    val myInviteUrl: StateFlow<String>
    val pendingInviteTrigger: StateFlow<Int>
    suspend fun syncFromBackend(): Boolean
    suspend fun addFriendFromRaw(rawInput: String, addedVia: String = "Código"): AddFriendResult
    suspend fun acceptPendingRequest(requestId: String): Boolean
    suspend fun rejectPendingRequest(requestId: String): Boolean
    suspend fun removeFriend(inviteCode: String)
    suspend fun queuePendingInvite(rawInput: String)
    suspend fun consumePendingInviteIfAuthenticated(): AddFriendResult?
    fun markIncomingNotificationHandled()
}
