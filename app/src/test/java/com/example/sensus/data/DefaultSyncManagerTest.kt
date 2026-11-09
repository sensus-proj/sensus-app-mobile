package com.example.sensus.data

import com.example.sensus.data.local.db.dao.PendingSyncDao
import com.example.sensus.data.local.db.entity.PendingSyncEntity
import com.example.sensus.data.sync.DefaultSyncManager
import com.example.sensus.domain.network.NetworkMonitor
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultSyncManagerTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Test
    fun enqueueOperation_insertsEntityIntoDao() = runTest(testDispatcher) {
        val dao = mockk<PendingSyncDao>(relaxed = true)
        val monitor = mockk<NetworkMonitor>(relaxed = true)
        every { dao.insert(any()) } returns 1L

        val manager = DefaultSyncManager(dao, monitor, testDispatcher, backgroundScope)
        val id = manager.enqueueOperation("CREATE_OFFER", "{\"name\":\"Pan\"}")

        assertEquals(1L, id)
        verify { dao.insert(match { it.operationType == "CREATE_OFFER" && it.status == "PENDING" }) }
    }

    @Test
    fun syncPendingOperations_processesAndDeletesItems() = runTest(testDispatcher) {
        val dao = mockk<PendingSyncDao>(relaxed = true)
        val monitor = mockk<NetworkMonitor>(relaxed = true)
        val sampleOp = PendingSyncEntity(1L, "APARTADO", "{}", System.currentTimeMillis(), "PENDING")

        every { dao.pendingOperations } returns listOf(sampleOp)

        val manager = DefaultSyncManager(dao, monitor, testDispatcher, backgroundScope)
        val result = manager.syncPendingOperations()

        assertEquals(1, result.successCount)
        assertEquals(0, result.failedCount)
        verify(exactly = 1) { dao.delete(sampleOp) }
        assertFalse(manager.isSyncing.value)
    }

    @Test
    fun clearQueue_callsDaoClearAll() = runTest(testDispatcher) {
        val dao = mockk<PendingSyncDao>(relaxed = true)
        val monitor = mockk<NetworkMonitor>(relaxed = true)

        val manager = DefaultSyncManager(dao, monitor, testDispatcher, backgroundScope)
        manager.clearQueue()

        verify { dao.clearAll() }
    }

    @Test
    fun autoSync_triggered_whenNetworkComesOnline() = runTest(testDispatcher) {
        val dao = mockk<PendingSyncDao>(relaxed = true)
        val monitor = mockk<NetworkMonitor>()
        val networkFlow = MutableStateFlow(false)
        every { monitor.isOnline } returns networkFlow
        every { dao.pendingOperations } returns emptyList()

        val manager = DefaultSyncManager(dao, monitor, testDispatcher, backgroundScope)

        networkFlow.value = true

        verify { dao.pendingOperations }
    }
}
