package com.example.sensus.domain.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class SyncResult(
    val successCount: Int,
    val failedCount: Int
)

interface SyncManager {
    val isSyncing: StateFlow<Boolean>
    val pendingOperationsCount: StateFlow<Int> get() = MutableStateFlow(0)
    suspend fun enqueueOperation(operationType: String, payload: String): Long
    suspend fun syncPendingOperations(): SyncResult
    suspend fun clearQueue()
}
