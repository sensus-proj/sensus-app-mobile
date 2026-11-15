package com.example.sensus.data

import android.content.Context
import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.data.push.NotificationsFirebaseDeviceProvider
import com.example.sensus.domain.repository.NotificationsPushDeviceProvider
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class NotificationPreferencesFirebaseProviderTest {
    private val context = mockk<Context>()

    @Before
    fun setup() {
        mockkStatic(FirebaseApp::class)
        mockkStatic(FirebaseMessaging::class)
        mockkStatic(FirebaseInstallations::class)
    }

    @After
    fun teardown() {
        unmockkStatic(FirebaseApp::class)
        unmockkStatic(FirebaseMessaging::class)
        unmockkStatic(FirebaseInstallations::class)
    }

    @Test
    fun absentResourcesReturnExplicitUnavailableWithoutMessaging() = runTest {
        every { FirebaseApp.initializeApp(context) } returns null
        val expected = Result.Failure(AppError.NotFound(NotificationsPushDeviceProvider.CONFIGURATION_UNAVAILABLE))
        assertEquals(expected, NotificationsFirebaseDeviceProvider(context).getDevice())
        verify(exactly = 0) { FirebaseMessaging.getInstance() }
    }

    @Test
    fun configuredAppUsesInstallationsIdAndMessagingToken() = runTest {
        val app = mockk<FirebaseApp>()
        val installations = mockk<FirebaseInstallations>()
        val messaging = mockk<FirebaseMessaging>()
        every { FirebaseApp.initializeApp(context) } returns app
        every { FirebaseInstallations.getInstance(app) } returns installations
        every { FirebaseMessaging.getInstance() } returns messaging
        every { installations.id } returns successfulTask("installation-from-sdk")
        every { messaging.token } returns successfulTask("token-issued-by-sdk-fixture")
        val result = NotificationsFirebaseDeviceProvider(context).getDevice()
        assertTrue(result is Result.Success)
        val device = (result as Result.Success).data
        assertEquals("installation-from-sdk", device.deviceId)
        assertEquals("token-issued-by-sdk-fixture", device.token)
    }

    @Test
    fun sdkFailureIsSanitizedAndDoesNotReturnCredentials() = runTest {
        every { FirebaseApp.initializeApp(context) } throws IllegalStateException("private-sdk-detail")
        assertEquals(Result.Failure(AppError.Generic()), NotificationsFirebaseDeviceProvider(context).getDevice())
    }

    @Test
    fun transportAndFirebaseFailuresAreMappedWithoutExposingSdkDetails() = runTest {
        every { FirebaseApp.initializeApp(context) } throws IOException("private-transport-detail")
        assertEquals(Result.Failure(AppError.Network()), NotificationsFirebaseDeviceProvider(context).getDevice())
        every { FirebaseApp.initializeApp(context) } throws mockk<FirebaseException>(relaxed = true)
        assertEquals(Result.Failure(AppError.Generic()), NotificationsFirebaseDeviceProvider(context).getDevice())
        every { FirebaseApp.initializeApp(context) } throws IllegalArgumentException("private-config-detail")
        assertEquals(Result.Failure(AppError.Generic()), NotificationsFirebaseDeviceProvider(context).getDevice())
    }

    @Test
    fun cancellationPropagatesInsteadOfBecomingAnOperationFailure() = runTest {
        val cancellation = CancellationException("cancelled")
        every { FirebaseApp.initializeApp(context) } throws cancellation
        try {
            NotificationsFirebaseDeviceProvider(context).getDevice()
            fail("Cancellation must propagate")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
    }
    private fun successfulTask(value: String): Task<String> {
        val task = mockk<Task<String>>(relaxed = true)
        every { task.addOnSuccessListener(any<OnSuccessListener<String>>()) } answers {
            firstArg<OnSuccessListener<String>>().onSuccess(value)
            task
        }
        return task
    }
}
