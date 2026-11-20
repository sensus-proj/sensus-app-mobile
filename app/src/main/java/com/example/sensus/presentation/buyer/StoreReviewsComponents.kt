package com.example.sensus.presentation.buyer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.sensus.domain.model.RatingBreakdownItem
import com.example.sensus.domain.model.StoreReview
import com.example.sensus.domain.model.StoreReviewReply
import com.example.sensus.domain.model.StoreReviewsSummary
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusCardBorder
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusTextMuted
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.SensusVioletBg
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val MAX_STARS = 5
private const val TRACK_COLOR_HEX = 0xFFE2E8F0
private const val AVATAR_BG_ALPHA = 0.14f
private val REVIEW_DATE_LOCALE = Locale.forLanguageTag("es-MX")
private val ISO_PATTERNS = listOf(
    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
    "yyyy-MM-dd'T'HH:mm:ss'Z'",
    "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
    "yyyy-MM-dd'T'HH:mm:ssXXX"
)

internal fun formatReviewDateWithYear(isoUtc: String): String {
    val parsed = ISO_PATTERNS.firstNotNullOfOrNull { pattern ->
        try {
            SimpleDateFormat(pattern, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(isoUtc)
        } catch (_: ParseException) {
            null
        }
    } ?: Date()
    return SimpleDateFormat("d MMM yyyy", REVIEW_DATE_LOCALE).format(parsed)
}

@Composable
internal fun PlayStoreSummaryCard(state: StoreReviewsViewState, summary: StoreReviewsSummary?) {
    val avg = summary?.averageRating ?: 0.0
    val total = summary?.totalReviews ?: 0
    val breakdown = summary?.breakdown ?: (MAX_STARS downTo 1).map { RatingBreakdownItem(it, 0) }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SensusCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StoreHeaderCompactRow(state.storeName, state.storeAddress, state.storePhotoUrl)
            HorizontalDivider(color = SensusBackground)
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                PlayStoreScoreColumn(average = avg, totalReviews = total)
                Spacer(modifier = Modifier.width(18.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    breakdown.forEach { item ->
                        RatingBreakdownBarRow(stars = item.stars, count = item.count, total = total)
                    }
                }
            }
        }
    }
}

@Composable
private fun StoreHeaderCompactRow(storeName: String, address: String, photoUrl: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StoreAvatarThumbnail(photoUrl = photoUrl, storeName = storeName, sizeDp = 46)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = storeName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (address.isNotBlank()) {
                Text(text = address, fontSize = 12.sp, color = SensusTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun PlayStoreScoreColumn(average: Double, totalReviews: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = String.format(Locale.US, "%.1f", average), fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, color = SensusTextPrimary)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = SensusPurple, modifier = Modifier.size(24.dp))
        }
        Text(text = "de $MAX_STARS", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SensusTextSecondary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = if (totalReviews == 1) "1 opinión" else "$totalReviews opiniones", fontSize = 11.sp, color = SensusTextMuted)
    }
}

@Composable
private fun RatingBreakdownBarRow(stars: Int, count: Int, total: Int) {
    val progress = if (total > 0) count.toFloat() / total.toFloat() else 0f
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "$stars", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SensusTextSecondary, modifier = Modifier.width(12.dp))
        LinearProgressIndicator(
            progress = { progress },
            color = SensusPurple,
            trackColor = Color(TRACK_COLOR_HEX),
            modifier = Modifier.weight(1f).height(7.dp).clip(RoundedCornerShape(4.dp))
        )
    }
}

@Composable
internal fun ReviewComposerCard(state: StoreReviewsViewState, onIntent: (StoreReviewsViewIntent) -> Unit, merchantId: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SensusCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = "Califica esta tienda", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (star in 1..MAX_STARS) {
                    val selected = star <= state.draftRating
                    IconButton(onClick = { onIntent(StoreReviewsViewIntent.SelectRating(star)) }, modifier = Modifier.size(38.dp)) {
                        Icon(
                            imageVector = if (selected) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "$star estrellas",
                            tint = if (selected) SensusPurple else SensusTextMuted,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
            OutlinedTextField(
                value = state.draftComment,
                onValueChange = { onIntent(StoreReviewsViewIntent.CommentChanged(it)) },
                placeholder = { Text("Cuéntale a otros cómo te fue en esta tienda...") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { onIntent(StoreReviewsViewIntent.SubmitReview(merchantId)) },
                enabled = !state.isSubmitting && state.draftComment.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = if (state.isSubmitting) "Publicando..." else "Publicar opinión", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun StoreReviewCard(
    review: StoreReview,
    isReplying: Boolean,
    replyDraft: String,
    isSendingReply: Boolean,
    onIntent: (StoreReviewsViewIntent) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SensusCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ReviewAuthorRow(
                name = review.authorName,
                photoUrl = review.authorPhotoUrl,
                stars = review.rating,
                dateText = formatReviewDateWithYear(review.createdAtUtc)
            )
            Text(text = review.comment, fontSize = 13.sp, color = SensusTextPrimary)
            ReviewActionRow(review = review, isReplying = isReplying, onIntent = onIntent)
            if (isReplying) {
                ReplyInputRow(reviewId = review.id, draft = replyDraft, isSending = isSendingReply, onIntent = onIntent)
            }
            if (review.replies.isNotEmpty()) {
                ReviewRepliesSection(replies = review.replies)
            }
        }
    }
}

@Composable
internal fun AuthorCircleAvatar(name: String, photoUrl: String?, sizeDp: Int) {
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(SensusPurple.copy(alpha = AVATAR_BG_ALPHA)),
        contentAlignment = Alignment.Center
    ) {
        if (!photoUrl.isNullOrBlank()) {
            AsyncImage(model = photoUrl, contentDescription = name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Text(text = name.trim().firstOrNull()?.uppercase() ?: "S", fontWeight = FontWeight.Bold, color = SensusPurple, fontSize = 14.sp)
        }
    }
}

@Composable
private fun ReviewAuthorRow(name: String, photoUrl: String?, stars: Int, dateText: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        AuthorCircleAvatar(name = name, photoUrl = photoUrl, sizeDp = 36)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Row {
                    for (idx in 1..MAX_STARS) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (idx <= stars) SensusPurple else Color(TRACK_COLOR_HEX),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
                Text(text = dateText, fontSize = 11.sp, color = SensusTextMuted)
            }
        }
    }
}

@Composable
private fun ReviewActionRow(review: StoreReview, isReplying: Boolean, onIntent: (StoreReviewsViewIntent) -> Unit) {
    val likeColor = if (review.isLikedByMe) SensusPurple else SensusTextSecondary
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            color = if (review.isLikedByMe) SensusVioletBg else Color.Transparent,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onIntent(StoreReviewsViewIntent.ToggleLikeReview(review.id)) }
        ) {
            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (review.isLikedByMe) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                    contentDescription = "Me gusta",
                    tint = likeColor,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(text = "${review.likesCount}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = likeColor)
            }
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onIntent(StoreReviewsViewIntent.OpenReply(if (isReplying) null else review.id)) }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.AutoMirrored.Filled.Reply, contentDescription = "Responder", tint = SensusTextSecondary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = if (isReplying) "Cancelar" else "Responder", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SensusTextSecondary)
        }
    }
}

@Composable
private fun ReplyInputRow(reviewId: String, draft: String, isSending: Boolean, onIntent: (StoreReviewsViewIntent) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = draft,
            onValueChange = { onIntent(StoreReviewsViewIntent.ReplyDraftChanged(it)) },
            placeholder = { Text("Escribe una respuesta...") },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = { onIntent(StoreReviewsViewIntent.SubmitReply(reviewId)) }, enabled = !isSending && draft.isNotBlank()) {
            Text(text = if (isSending) "..." else "Enviar", fontWeight = FontWeight.Bold, color = SensusPurple)
        }
    }
}

@Composable
private fun ReviewRepliesSection(replies: List<StoreReviewReply>) {
    Surface(color = SensusBackground, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            replies.forEach { reply ->
                Row(verticalAlignment = Alignment.Top) {
                    AuthorCircleAvatar(name = reply.authorName, photoUrl = reply.authorPhotoUrl, sizeDp = 28)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = reply.authorName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
                            Text(text = formatReviewDateWithYear(reply.createdAtUtc), fontSize = 10.sp, color = SensusTextMuted)
                        }
                        Text(text = reply.comment, fontSize = 12.sp, color = SensusTextSecondary)
                    }
                }
            }
        }
    }
}
