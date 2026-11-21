package com.example.sensus.presentation.buyer

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.sensus.domain.model.FeedComment
import com.example.sensus.domain.model.FeedPost
import com.example.sensus.domain.model.ReportReason
import com.example.sensus.theme.SensusTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class Ft19FeedInteractionsComposeTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun postCard_triggersLikeCommentReportAndDeleteIntents() {
        val intents = mutableListOf<CommunityViewIntent>()
        val post = FeedPost(
            id = "p1",
            authorName = "Sofía",
            text = "Promoción especial hoy",
            createdAt = "Hoy",
            likesCount = 3,
            commentsCount = 2,
            isMine = true
        )

        compose.setContent {
            SensusTheme {
                CommunityFeedContent(
                    state = CommunityViewState(posts = listOf(post), hasLoadedFeed = true),
                    onIntent = { intents.add(it) },
                    onMerchantClick = {}
                )
            }
        }

        compose.onNodeWithContentDescription("Me gusta").performClick()
        compose.onNodeWithContentDescription("Comentarios").performClick()
        compose.onNodeWithContentDescription("Reportar publicación").performClick()
        compose.onNodeWithContentDescription("Eliminar publicación").performClick()

        assertTrue(intents.any { it is CommunityViewIntent.ToggleLike && it.postId == "p1" })
        assertTrue(intents.any { it is CommunityViewIntent.OpenComments })
        assertTrue(intents.any { it is CommunityViewIntent.OpenReportPost })
        assertTrue(intents.any { it is CommunityViewIntent.RequestDeletePost })
    }

    @Test
    fun reportContentDialog_submitsSelectedReasonAndDetails() {
        var submittedReason: ReportReason? = null
        compose.setContent {
            SensusTheme {
                ReportContentDialog(
                    model = ReportDialogModel(targetLabel = "Oferta pan 2x1"),
                    onDismiss = {},
                    onSubmit = { reason, _ -> submittedReason = reason }
                )
            }
        }

        compose.onNodeWithText(ReportReason.FRAUD.displayLabel).performClick()
        compose.onNodeWithText("Enviar reporte").performClick()
        assertEquals(ReportReason.FRAUD, submittedReason)
    }

    @Test
    fun commentsSheet_rendersExistingComments() {
        val post = FeedPost("p1", "Sofía", "Texto", "Hoy")
        compose.setContent {
            SensusTheme {
                PostCommentsSheet(
                    model = CommentsSheetModel(
                        post = post,
                        comments = listOf(FeedComment("c1", "p1", "u2", "Mateo", "Muy bueno", "Ahora")),
                        isLoading = false,
                        isSubmitting = false,
                        draft = "",
                        errorMessage = null
                    ),
                    onDraftChange = {},
                    onSend = {},
                    onDismiss = {}
                )
            }
        }

        compose.onNodeWithText("Muy bueno").assertExists()
    }
}
