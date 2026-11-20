package com.example.sensus.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

private const val PREVIEW_WIDTH_PIXELS = 288
private const val CATALOG_WIDTH_PIXELS = 640
private val versionedPublicCloudinaryImage =
    Regex("""(https://res\.cloudinary\.com/[A-Za-z0-9_-]+/image/upload/)(v[0-9]+/[^?#]+)""")

internal enum class OfferThumbnailSize(val widthPixels: Int) {
    PREVIEW(PREVIEW_WIDTH_PIXELS),
    CATALOG(CATALOG_WIDTH_PIXELS)
}

internal fun publicOfferThumbnailUrl(url: String, size: OfferThumbnailSize): String {
    val match = versionedPublicCloudinaryImage.matchEntire(url) ?: return url
    val (prefix, resource) = match.destructured
    return "${prefix}c_limit,w_${size.widthPixels},q_auto/$resource"
}

@Composable
internal fun PublicOfferThumbnail(url: String, size: OfferThumbnailSize, description: String, modifier: Modifier = Modifier) {
    val thumbnail = remember(url, size) { publicOfferThumbnailUrl(url, size) }
    var useOriginal by remember(thumbnail, url) { mutableStateOf(false) }
    AsyncImage(
        model = if (useOriginal) url else thumbnail,
        contentDescription = description,
        contentScale = ContentScale.Crop,
        modifier = modifier,
        onError = { if (thumbnail != url) useOriginal = true }
    )
}
