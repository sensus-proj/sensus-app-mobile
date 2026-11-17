package com.example.sensus.data.repository

import com.example.sensus.data.local.db.dao.UserPreferenceDao
import com.example.sensus.data.remote.api.FriendsApiService
import com.example.sensus.data.remote.dto.FriendsOverviewDto
import com.example.sensus.data.remote.dto.SendFriendRequestBody
import com.example.sensus.data.remote.dto.SendFriendRequestResponseDto
import com.example.sensus.domain.model.AddFriendResult
import com.example.sensus.domain.model.ConnectedFriend
import com.example.sensus.domain.model.FriendInviteCodec
import com.example.sensus.domain.model.PendingFriendRequest
import com.example.sensus.domain.model.SentFriendRequest
import com.example.sensus.domain.repository.FriendsRepository
import com.example.sensus.domain.repository.SessionRepository
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import retrofit2.HttpException

private const val HTTP_NOT_FOUND = 404

class FriendsRepositoryImpl(
    userPreferenceDao: UserPreferenceDao? = null,
    private val sessionRepository: SessionRepository? = null,
    private val friendsApi: FriendsApiService? = null,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + ioDispatcher)
) : FriendsRepository {

    private val localStore = FriendsLocalStore(userPreferenceDao, ioDispatcher)
    private val lock = Any()

    private val _friends = MutableStateFlow<List<ConnectedFriend>>(emptyList())
    override val friends: StateFlow<List<ConnectedFriend>> = _friends.asStateFlow()

    private val _pendingRequests = MutableStateFlow<List<PendingFriendRequest>>(emptyList())
    override val pendingRequests: StateFlow<List<PendingFriendRequest>> = _pendingRequests.asStateFlow()

    private val _sentRequests = MutableStateFlow<List<SentFriendRequest>>(emptyList())
    override val sentRequests: StateFlow<List<SentFriendRequest>> = _sentRequests.asStateFlow()

    private val _newlyArrivedRequest = MutableStateFlow<PendingFriendRequest?>(null)
    override val newlyArrivedRequest: StateFlow<PendingFriendRequest?> = _newlyArrivedRequest.asStateFlow()

    private val _myInviteCode = MutableStateFlow(FriendInviteCodec.generateInviteCode(null, null))
    override val myInviteCode: StateFlow<String> = _myInviteCode.asStateFlow()

    private val _myInviteUrl = MutableStateFlow(FriendInviteCodec.buildInviteUrl(_myInviteCode.value, null))
    override val myInviteUrl: StateFlow<String> = _myInviteUrl.asStateFlow()

    private val pendingInviteInput = MutableStateFlow<String?>(null)
    private val _pendingInviteTrigger = MutableStateFlow(0)
    override val pendingInviteTrigger: StateFlow<Int> = _pendingInviteTrigger.asStateFlow()

    init {
        if (sessionRepository != null) {
            scope.launch {
                sessionRepository.sessionState
                    .map { Triple(it.isGuest, it.activeEmail, it.activeName) }
                    .distinctUntilChanged()
                    .collect { (isGuest, email, name) ->
                        _myInviteCode.value = FriendInviteCodec.generateInviteCode(email, name)
                        _myInviteUrl.value = FriendInviteCodec.buildInviteUrl(_myInviteCode.value, name)
                        val scopeKey = resolveFriendsScopeKey(isGuest, email)
                        val cached = localStore.loadFriends(scopeKey)
                        synchronized(lock) {
                            _friends.value = cached
                            if (isGuest) {
                                _pendingRequests.value = emptyList()
                                _sentRequests.value = emptyList()
                            }
                        }
                        if (!isGuest) syncFromBackend()
                    }
            }
        } else if (userPreferenceDao != null) {
            scope.launch {
                val cached = localStore.loadFriends(DEFAULT_FRIENDS_SCOPE)
                synchronized(lock) { _friends.value = cached }
            }
        }
    }

    override suspend fun syncFromBackend(): Boolean {
        val api = friendsApi
        val session = sessionRepository?.sessionState?.value
        val canSync = api != null && (session == null || (!session.isGuest && session.isSessionActive))
        var synced = false
        if (canSync) {
            synced = try {
                val response = api!!.getFriendsOverview()
                val body = if (response.isSuccessful) response.body() else null
                if (body != null) {
                    applySyncedOverview(body)
                    true
                } else {
                    false
                }
            } catch (_: IOException) {
                false
            } catch (_: HttpException) {
                false
            }
        }
        return synced
    }

    override suspend fun addFriendFromRaw(rawInput: String, addedVia: String): AddFriendResult {
        val isGuest = sessionRepository?.sessionState?.value?.isGuest == true
        val parsed = if (isGuest) null else FriendInviteCodec.parseInvite(rawInput)
        return when {
            isGuest -> AddFriendResult.GuestNotAllowed
            parsed == null -> AddFriendResult.InvalidCode
            parsed.inviteCode.equals(_myInviteCode.value, ignoreCase = true) -> AddFriendResult.SelfInviteNotAllowed
            friendsApi != null -> sendRequestToBackend(parsed.inviteCode, parsed.displayName, addedVia)
            else -> insertLocalFallback(parsed.inviteCode, parsed.displayName, addedVia)
        }
    }

    override suspend fun acceptPendingRequest(requestId: String): Boolean {
        val api = friendsApi ?: return false
        return try {
            val response = api.acceptFriendRequest(requestId)
            val dto = if (response.isSuccessful) response.body() else null
            if (dto != null) {
                val friend = dto.toDomain()
                val updated = listOf(friend) + _friends.value.filterNot { it.inviteCode.equals(friend.inviteCode, true) }
                synchronized(lock) {
                    _friends.value = updated
                    _pendingRequests.value = _pendingRequests.value.filterNot { it.requestId == requestId }
                }
                localStore.saveFriends(resolveCurrentFriendsScope(sessionRepository), updated)
                true
            } else {
                false
            }
        } catch (_: IOException) {
            false
        } catch (_: HttpException) {
            false
        }
    }

    override suspend fun rejectPendingRequest(requestId: String): Boolean {
        val api = friendsApi ?: return false
        return try {
            val response = api.rejectFriendRequest(requestId)
            if (response.isSuccessful) {
                synchronized(lock) {
                    _pendingRequests.value = _pendingRequests.value.filterNot { it.requestId == requestId }
                }
                true
            } else {
                false
            }
        } catch (_: IOException) {
            false
        } catch (_: HttpException) {
            false
        }
    }

    override suspend fun removeFriend(inviteCode: String) {
        val target = _friends.value.firstOrNull { it.inviteCode.equals(inviteCode, ignoreCase = true) }
        if (friendsApi != null && !target?.userId.isNullOrBlank()) {
            try {
                friendsApi.removeFriend(target!!.userId)
            } catch (_: IOException) {
                // Mantiene consistente el estado local ante pérdida temporal de red.
            } catch (_: HttpException) {
                // Evita falla si el vínculo ya no existía en servidor.
            }
        }
        val updated = _friends.value.filterNot { it.inviteCode.equals(inviteCode, ignoreCase = true) }
        synchronized(lock) { _friends.value = updated }
        localStore.saveFriends(resolveCurrentFriendsScope(sessionRepository), updated)
    }

    override suspend fun queuePendingInvite(rawInput: String) {
        if (rawInput.isNotBlank()) {
            pendingInviteInput.value = rawInput
            _pendingInviteTrigger.value += 1
        }
    }

    override suspend fun consumePendingInviteIfAuthenticated(): AddFriendResult? {
        val pending = pendingInviteInput.value
        val session = sessionRepository?.sessionState?.value
        var outcome: AddFriendResult? = null
        if (pending != null) {
            if (session != null && (session.isGuest || !session.isSessionActive)) {
                outcome = if (session.isGuest) AddFriendResult.GuestNotAllowed else null
            } else {
                pendingInviteInput.value = null
                if (pending.startsWith("sensus://amigos")) {
                    syncFromBackend()
                } else {
                    outcome = addFriendFromRaw(pending, addedVia = "Enlace")
                }
            }
        }
        return outcome
    }

    override fun markIncomingNotificationHandled() {
        _newlyArrivedRequest.value = null
    }

    private suspend fun applySyncedOverview(body: FriendsOverviewDto) {
        val syncedFriends = body.friends.map { it.toDomain() }
        val syncedPending = body.pendingReceived.map { it.toDomain() }
        val syncedSent = body.pendingSent.map { it.toDomain() }
        synchronized(lock) {
            _friends.value = syncedFriends
            _pendingRequests.value = syncedPending
            _sentRequests.value = syncedSent
        }
        val scopeKey = resolveCurrentFriendsScope(sessionRepository)
        localStore.saveFriends(scopeKey, syncedFriends)
        val unseen = localStore.filterUnseenAndMarkNotified(scopeKey, syncedPending)
        if (unseen != null) _newlyArrivedRequest.value = unseen
    }

    private suspend fun sendRequestToBackend(code: String, fallbackName: String?, addedVia: String): AddFriendResult {
        val api = friendsApi ?: return AddFriendResult.NetworkFailure("Servicio no disponible.")
        return try {
            val response = api.sendFriendRequest(SendFriendRequestBody(inviteCode = code, addedVia = addedVia))
            when {
                response.isSuccessful -> mapSuccessSend(response.body(), fallbackName, code)
                response.code() == HTTP_NOT_FOUND ->
                    AddFriendResult.NetworkFailure("No encontramos un usuario activo con ese código.")
                else -> AddFriendResult.NetworkFailure("No se pudo enviar la solicitud (${response.code()}).")
            }
        } catch (_: IOException) {
            AddFriendResult.NetworkFailure("Sin conexión con el servidor. Intenta nuevamente.")
        } catch (_: HttpException) {
            AddFriendResult.NetworkFailure("Error al enviar la solicitud de amistad.")
        }
    }

    private suspend fun mapSuccessSend(body: SendFriendRequestResponseDto?, fallbackName: String?, code: String): AddFriendResult = when (body?.outcome) {
        "AcceptedMutual" -> body.friend.toConnectedFriendOrDefault(code, fallbackName).let {
            insertConnectedFriend(it)
            AddFriendResult.Added(it)
        }
        "AlreadyConnected" -> body.friend.toConnectedFriendOrDefault(code, fallbackName).let {
            insertConnectedFriend(it)
            AddFriendResult.AlreadyConnected(it)
        }
        "AlreadyPending" -> AddFriendResult.AlreadyPending(
            body.pendingRequest.toSentRequestOrDefault(code, fallbackName).targetName
        )
        "Sent" -> body.pendingRequest.toSentRequestOrDefault(code, fallbackName).let { sent ->
            synchronized(lock) {
                _sentRequests.value = listOf(sent) + _sentRequests.value.filterNot {
                    it.targetInviteCode.equals(sent.targetInviteCode, true)
                }
            }
            AddFriendResult.RequestSent(sent)
        }
        else -> AddFriendResult.NetworkFailure("Respuesta inesperada del servidor.")
    }

    private suspend fun insertLocalFallback(inviteCode: String, displayName: String?, addedVia: String): AddFriendResult {
        val existing = _friends.value.firstOrNull { it.inviteCode.equals(inviteCode, ignoreCase = true) }
        if (existing != null) return AddFriendResult.AlreadyConnected(existing)
        val newFriend = ConnectedFriend(
            inviteCode = inviteCode,
            displayName = displayName ?: "Vecino ($inviteCode)",
            addedAtEpochMs = System.currentTimeMillis(),
            addedVia = addedVia
        )
        insertConnectedFriend(newFriend)
        return AddFriendResult.Added(newFriend)
    }

    private suspend fun insertConnectedFriend(friend: ConnectedFriend) {
        val updated = listOf(friend) + _friends.value.filterNot { it.inviteCode.equals(friend.inviteCode, true) }
        synchronized(lock) { _friends.value = updated }
        localStore.saveFriends(resolveCurrentFriendsScope(sessionRepository), updated)
    }
}

private fun resolveCurrentFriendsScope(sessionRepository: SessionRepository?): String {
    val session = sessionRepository?.sessionState?.value
    return if (session == null) DEFAULT_FRIENDS_SCOPE else resolveFriendsScopeKey(session.isGuest, session.activeEmail)
}
