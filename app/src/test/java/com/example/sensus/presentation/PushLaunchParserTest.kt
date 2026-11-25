package com.example.sensus.presentation

import android.content.Intent
import android.os.Bundle
import com.example.sensus.domain.model.PushTarget
import com.example.sensus.domain.usecase.NotificationsDeviceSync
import com.example.sensus.presentation.push.PushLaunchParser
import com.example.sensus.presentation.push.PushNotificationPresenter
import com.example.sensus.presentation.push.PushOpenRequests
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PushLaunchParserTest {
    private fun intentWith(extras: Map<String, Any?>): Intent {
        val bundle = mockk<Bundle>()
        every { bundle.getBoolean(any(), any()) } answers { extras[firstArg()] as? Boolean ?: secondArg() }
        every { bundle.containsKey(any()) } answers { firstArg<String>() in extras }
        @Suppress("DEPRECATION")
        every { bundle.get(any()) } answers { extras[firstArg()] }
        return mockk {
            every { this@mockk.extras } returns bundle
            every { removeExtra(any()) } just runs
        }
    }

    @Test
    fun backgroundFcmTapIsParsedFromSystemExtras() {
        val intent = intentWith(mapOf("google.message_id" to "0:1", "type" to "nearby_offer", "offerId" to "offer-1", "from" to "123"))
        assertEquals(PushTarget.Offer("offer-1"), PushLaunchParser.parse(intent))
    }

    @Test
    fun foregroundNotificationTapUsesTheAppMarker() {
        val intent = intentWith(mapOf(PushNotificationPresenter.EXTRA_FROM_PUSH to true, "type" to "friend_request", "requestId" to "r-1"))
        assertEquals(PushTarget.Friends, PushLaunchParser.parse(intent))
    }

    @Test
    fun regularLaunchesAreNotTreatedAsNotifications() {
        assertNull(PushLaunchParser.parse(null))
        assertNull(PushLaunchParser.parse(mockk { every { extras } returns null }))
        assertNull(PushLaunchParser.parse(intentWith(mapOf("type" to "nearby_offer", "offerId" to "offer-1"))))
    }

    @Test
    fun consumeRemovesNotificationExtrasSoRotationDoesNotReopen() {
        val intent = intentWith(emptyMap())
        PushLaunchParser.consume(intent)
        PushLaunchParser.consume(null)
        verify { intent.removeExtra(PushNotificationPresenter.EXTRA_FROM_PUSH) }
        verify { intent.removeExtra("type") }
        verify { intent.removeExtra("offerId") }
        verify { intent.removeExtra("google.message_id") }
    }

    @Test
    fun openRequestsKeepOnlyTheLatestTargetAndConsumeOnce() {
        val requests = PushOpenRequests()
        requests.submit(PushTarget.Friends)
        requests.submit(PushTarget.Offer("offer-2"))
        assertEquals(PushTarget.Offer("offer-2"), requests.consume())
        assertNull(requests.consume())
    }

    @Test
    fun presenterIgnoresMessagesWithoutTitle() {
        val context = mockk<android.content.Context>()
        PushNotificationPresenter.show(context, "   ", "cuerpo", mapOf("type" to "nearby_offer"))
        PushNotificationPresenter.show(context, null, null, emptyMap())
        verify(exactly = 0) { context.getString(any()) }
    }

    @Test
    fun deviceSyncUsesProductionRetryScheduleByDefault() {
        assertEquals(listOf(30_000L, 120_000L, 600_000L), NotificationsDeviceSync.DEFAULT_RETRY_DELAYS_MILLIS)
    }
}
