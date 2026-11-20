package com.example.sensus.presentation.splash

import org.junit.Assert.assertEquals
import org.junit.Test

private const val DELTA = 0.001f
private const val PORTRAIT_WIDTH = 1080
private const val PORTRAIT_HEIGHT = 1920
private const val TALL_VIEW_HEIGHT = 2400
private const val WIDE_VIEW_WIDTH = 1920
private const val WIDE_VIEW_HEIGHT = 1080

class SplashScreenTimingTest {

    @Test
    fun splashSafetyTimeout_allowsThreeSecondVideoToComplete() {
        assertEquals(4_500L, SPLASH_SAFETY_TIMEOUT_MS)
    }

    @Test
    fun computeFitCenterScale_preservesAspectRatioForPortraitAndLandscapeContainers() {
        val (sameX, sameY) = computeFitCenterScale(PORTRAIT_WIDTH, PORTRAIT_HEIGHT, PORTRAIT_WIDTH, PORTRAIT_HEIGHT)
        assertEquals(1f, sameX, DELTA)
        assertEquals(1f, sameY, DELTA)

        val (tallX, tallY) = computeFitCenterScale(PORTRAIT_WIDTH, TALL_VIEW_HEIGHT, PORTRAIT_WIDTH, PORTRAIT_HEIGHT)
        assertEquals(1f, tallX, DELTA)
        assertEquals(0.8f, tallY, DELTA)

        val (wideX, wideY) = computeFitCenterScale(WIDE_VIEW_WIDTH, WIDE_VIEW_HEIGHT, PORTRAIT_WIDTH, PORTRAIT_HEIGHT)
        assertEquals((9f / 16f) / (16f / 9f), wideX, DELTA)
        assertEquals(1f, wideY, DELTA)
    }
}
