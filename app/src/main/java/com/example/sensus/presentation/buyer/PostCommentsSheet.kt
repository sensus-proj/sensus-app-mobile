package com.example.sensus.presentation.buyer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sensus.R
import com.example.sensus.domain.model.FeedComment
import com.example.sensus.domain.model.FeedPolicy
import com.example.sensus.domain.model.FeedPost
import com.example.sensus.presentation.common.ComposeUiModel
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusRed
import com.example.sensus.theme.SensusTextMuted
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.SensusTheme

private const val COMMENTS_LIST_MAX_DP = 300
private const val COMMENT_ACTION_SIZE_DP = 48

@ComposeUiModel
internal data class CommentsSheetModel(
    val post: FeedPost,
    val comments: List<FeedComment>,
    val isLoading: Boolean,
    val isSubmitting: Boolean,
    val draft: String,
    val errorMessage: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PostCommentsSheet(model: CommentsSheetModel, onDraftChange: (String) -> Unit, onSend: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        PostCommentsSheetBody(
            model = model,
            onDraftChange = onDraftChange,
            onSend = onSend
        )
    }
}

@Composable
private fun PostCommentsSheetBody(model: CommentsSheetModel, onDraftChange: (String) -> Unit, onSend: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(R.string.ft19_comments_title, model.post.authorName),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = SensusTextPrimary
        )
        HorizontalDivider()
        CommentsListSection(model)
        model.errorMessage?.let {
            Text(text = it, color = SensusRed, fontSize = 12.sp)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = model.draft,
                onValueChange = onDraftChange,
                label = { Text(stringResource(R.string.ft19_comment_placeholder)) },
                supportingText = {
                    Text("${model.draft.length}/${FeedPolicy.MAX_COMMENT_LENGTH}", color = SensusTextMuted)
                },
                modifier = Modifier.weight(1f),
                enabled = !model.isSubmitting
            )
            IconButton(
                onClick = onSend,
                enabled = !model.isSubmitting && model.draft.isNotBlank(),
                modifier = Modifier.size(COMMENT_ACTION_SIZE_DP.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.ft19_comment_send),
                    tint = SensusPurple
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun CommentsListSection(model: CommentsSheetModel) {
    when {
        model.isLoading -> CircularProgressIndicator(color = SensusPurple)
        model.comments.isEmpty() -> Text(
            text = stringResource(R.string.ft19_comments_empty),
            color = SensusTextSecondary,
            fontSize = 13.sp
        )
        else -> LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = COMMENTS_LIST_MAX_DP.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(model.comments, key = { it.id }) { comment ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = comment.authorName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SensusTextPrimary
                    )
                    Text(text = comment.text, fontSize = 14.sp, color = SensusTextSecondary)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
internal fun PostCommentsSheetBodyPreview() {
    SensusTheme {
        PostCommentsSheetBody(
            model = CommentsSheetModel(
                post = FeedPost("1", "Sofía", "Pan recién horneado!", "Hoy"),
                comments = listOf(
                    FeedComment("c1", "1", "u2", "Mateo", "¡Voy para allá!", "Ahora")
                ),
                isLoading = false,
                isSubmitting = false,
                draft = "",
                errorMessage = null
            ),
            onDraftChange = {},
            onSend = {}
        )
    }
}
