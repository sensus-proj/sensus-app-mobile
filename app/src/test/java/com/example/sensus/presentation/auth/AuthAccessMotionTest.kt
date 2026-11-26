package com.example.sensus.presentation.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.LayoutDirection
import com.example.sensus.theme.SensusPurple
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthAccessMotionTest {
    @Test
    fun indicatorStartsUnderActiveTabAndMirrorsInRtl() {
        val track = Size(300f, 48f)
        val login = authIndicatorGeometry(track, authTabPosition(isRegister = false), LayoutDirection.Ltr)
        val register = authIndicatorGeometry(track, authTabPosition(isRegister = true), LayoutDirection.Ltr)
        val mirroredRegister = authIndicatorGeometry(track, authTabPosition(isRegister = true), LayoutDirection.Rtl)

        assertEquals(Offset.Zero, login.offset)
        assertEquals(Offset(150f, 0f), register.offset)
        assertEquals(login, mirroredRegister)
        assertEquals(Size(150f, 48f), login.size)
        assertEquals(CornerRadius(24f), login.radius)
    }

    @Test
    fun indicatorAndLabelsFollowTransitionProgress() {
        val track = Size(320f, 48f)

        assertEquals(Offset(40f, 0f), authIndicatorGeometry(track, 0.25f, LayoutDirection.Ltr).offset)
        assertEquals(Offset(120f, 0f), authIndicatorGeometry(track, 0.25f, LayoutDirection.Rtl).offset)
        assertFalse(authHighlightsRegister(0.25f))
        assertTrue(authHighlightsRegister(0.75f))
    }

    @Test
    fun switchNavigatesOnceAfterSlidingInEitherDirection() = runTest {
        for (isRegister in listOf(false, true)) {
            val clock = BroadcastFrameClock()
            val start = authTabPosition(isRegister)
            val position = Animatable(start)
            var navigations = 0
            launch(clock) { switchAuthTab(isRegister, position) { navigations++ } }
            runCurrent()

            clock.sendFrame(0L)
            runCurrent()
            launch(clock) { switchAuthTab(isRegister, position) { navigations++ } }
            runCurrent()
            clock.sendFrame(90_000_000L)
            runCurrent()
            assertTrue(position.value in 0f..1f && position.value != start)
            assertEquals(0, navigations)

            clock.sendFrame(200_000_000L)
            runCurrent()
            assertEquals(authTabPosition(!isRegister), position.value)
            assertEquals(1, navigations)
        }
    }

    @Test
    fun cancelledSlideDoesNotNavigate() = runTest {
        val clock = BroadcastFrameClock()
        val transition = launch(clock) {
            switchAuthTab(isRegister = false, position = Animatable(0f)) { error("Unexpected navigation") }
        }
        runCurrent()
        transition.cancelAndJoin()
        assertTrue(transition.isCancelled)
    }

    @Test
    fun drawsThePurplePillAtThePositionComputedForTheCurrentDirection() {
        val scope = mockk<DrawScope>(relaxed = true)
        every { scope.size } returns Size(300f, 48f)
        every { scope.layoutDirection } returns LayoutDirection.Rtl

        scope.drawAuthIndicator(0.25f)

        verify {
            scope.drawRoundRect(
                color = SensusPurple,
                topLeft = Offset(112.5f, 0f),
                size = Size(150f, 48f),
                cornerRadius = CornerRadius(24f)
            )
        }
    }
}
