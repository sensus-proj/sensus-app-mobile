package com.example.sensus.data.sync

import com.example.sensus.data.local.db.dao.PendingSyncDao
import com.example.sensus.data.local.db.entity.PendingSyncEntity
import com.example.sensus.domain.network.NetworkMonitor
import com.example.sensus.domain.sync.SyncManager
import com.example.sensus.domain.sync.SyncResult
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DefaultSyncManager @Inject constructor(
    private val pendingSyncDao: PendingSyncDao,
    private val networkMonitor: NetworkMonitor,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    externalScope: CoroutineScope? = null
) : SyncManager {

    private val _isSyncing = MutableStateFlow(false)
    override val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    init {
        val scope = externalScope ?: CoroutineScope(ioDispatcher)
        scope.launch {
            networkMonitor.isOnline.collect { isOnline ->
                if (isOnline) {
                    syncPendingOperations()
                }
            }
        }
    }

    override suspend fun enqueueOperation(operationType: String, payload: String): Long = withContext(ioDispatcher) {
        val entity = PendingSyncEntity(
            0,
            operationType,
            payload,
            System.currentTimeMillis(),
            "PENDING"
        )
        pendingSyncDao.insert(entity)
    }

    override suspend fun syncPendingOperations(): SyncResult = withContext(ioDispatcher) {
        if (_isSyncing.value) return@withContext SyncResult(0, 0)
        _isSyncing.value = true
        var successCount = 0
        var failedCount = 0

        try {
            val pendingList = pendingSyncDao.pendingOperations
            for (op in pendingList) {
                try {
                    processOperation(op)
                    pendingSyncDao.delete(op)
                    successCount++
                } catch (_: Exception) {
                    failedCount++
                }
            }
        } finally {
            _isSyncing.value = false
        }

        SyncResult(successCount, failedCount)
    }

    override suspend fun clearQueue() = withContext(ioDispatcher) {
        pendingSyncDao.clearAll()
    }

    private fun processOperation(operation: PendingSyncEntity) {
        // En Fase 1 valida la operación; listo para despachar a APIs remotas en Fase 4
        require(operation.operationType.isNotBlank()) { "Pending sync operationType cannot be blank" }
    }
}
