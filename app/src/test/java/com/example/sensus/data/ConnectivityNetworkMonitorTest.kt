package com.example.sensus.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import app.cash.turbine.test
import com.example.sensus.data.network.ConnectivityNetworkMonitor
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectivityNetworkMonitorTest {

    @Test
    fun isOnline_nullConnectivityManager_emitsFalse() = runTest {
        val context = mockk<Context>()
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns null

        val monitor = ConnectivityNetworkMonitor(context)
        monitor.isOnline.test {
            assertFalse(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun isOnline_withInternetCapability_emitsTrue() = runTest {
        val context = mockk<Context>()
        val connectivityManager = mockk<ConnectivityManager>(relaxed = true)
        val capabilities = mockk<NetworkCapabilities>()

        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { connectivityManager.activeNetwork } returns mockk()
        every { connectivityManager.getNetworkCapabilities(any()) } returns capabilities
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true

        val monitor = ConnectivityNetworkMonitor(context)
        monitor.isOnline.test {
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun isOnline_withoutInternetCapability_emitsFalse() = runTest {
        val context = mockk<Context>()
        val connectivityManager = mockk<ConnectivityManager>(relaxed = true)

        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { connectivityManager.activeNetwork } returns null
        every { connectivityManager.getNetworkCapabilities(any()) } returns null

        val monitor = ConnectivityNetworkMonitor(context)
        monitor.isOnline.test {
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
