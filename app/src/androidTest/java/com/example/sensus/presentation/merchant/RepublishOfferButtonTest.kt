package com.example.sensus.presentation.merchant

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.sensus.domain.model.OfferLifecycleStatus
import com.example.sensus.theme.SensusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RepublishOfferButtonTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun expiredActionRequiresExplicitClick() {
        var clicks = 0
        compose.setContent { SensusTheme { RepublishOfferButton(OfferLifecycleStatus.EXPIRED, false) { clicks++ } } }
        assertEquals(0, clicks)
        compose.onNodeWithText("Republicar").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun archivedActionDisabledWhileBusy() {
        compose.setContent { SensusTheme { RepublishOfferButton(OfferLifecycleStatus.ARCHIVED, true) {} } }
        compose.onNodeWithText("Republicar").assertIsNotEnabled()
    }

    @Test
    fun unsupportedStatusesHaveNoRepublishAction() {
        compose.setContent {
            SensusTheme {
                RepublishOfferButton(OfferLifecycleStatus.ACTIVE, false) {}
                RepublishOfferButton(OfferLifecycleStatus.PAUSED, false) {}
                RepublishOfferButton(OfferLifecycleStatus.SOLD_OUT, false) {}
            }
        }
        compose.onAllNodesWithText("Republicar").assertCountEquals(0)
    }
}
