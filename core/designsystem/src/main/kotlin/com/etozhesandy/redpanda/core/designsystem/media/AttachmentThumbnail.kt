package com.etozhesandy.redpanda.core.designsystem.media

import com.etozhesandy.redpanda.core.designsystem.utils.isVideoPageUrl
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import com.etozhesandy.redpanda.core.designsystem.R
import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.core.model.AttachmentType
import java.io.File
import kotlinx.coroutines.delay

private const val MAX_LOAD_RETRIES = 2
private const val RETRY_DELAY_MS = 1_500L

/**
 * The size is caller-controlled via [modifier] (default 120dp square, used inline in message
 * bubbles). The media grid overrides it with a fillMaxWidth/aspectRatio modifier so thumbnails
 * fit the configured number of columns.
 */
@Composable
fun AttachmentThumbnail(
    attachment: Attachment,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.size(120.dp),
) = MediaThumbnail(
    path = attachment.path,
    type = attachment.type,
    caption = attachment.caption,
    onClick = onClick,
    modifier = modifier,
)

/**
 * The same tile addressed by its parts, for callers holding only a path and a kind — folder
 * previews come from an aggregate query rather than a whole [Attachment].
 */
@Composable
fun MediaThumbnail(
    path: String,
    type: AttachmentType,
    caption: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.size(120.dp),
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // Deliberately no `else`: this is the one place every attachment tile goes through, so a
        // future AttachmentType constant should break the build here rather than render as blank.
        when (type) {
            AttachmentType.PHOTO,
            AttachmentType.VIDEO,
            AttachmentType.STICKER,
            AttachmentType.GRAFFITI,
            -> if (path.isBlank() || (type == AttachmentType.VIDEO && path.isVideoPageUrl())) {
                // Media the source no longer has — a removed video still has a record but nothing
                // to load — and a video that is only a link to a player page: neither has an
                // image to fetch, and without this they would render as empty tiles.
                AttachmentPlaceholder(type, caption, Modifier.matchParentSize())
            } else {
                val model = if (path.startsWith("http")) path else File(path)
                var failed by remember(model) { mutableStateOf(false) }
                var attempt by remember(model) { mutableIntStateOf(0) }
                // The CDN now and then answers a request with a timeout or a 5xx that a second try
                // gets past, and a grid fires dozens at once. Only a link that kept failing is
                // shown as broken; local files have nothing to retry.
                val canRetry = model is String && attempt < MAX_LOAD_RETRIES
                LaunchedEffect(failed, attempt) {
                    if (failed && canRetry) {
                        delay(RETRY_DELAY_MS * (attempt + 1))
                        failed = false
                        attempt++
                    }
                }
                val context = LocalContext.current
                // Built once per model and attempt: a tile recomposes while the grid scrolls, and
                // a fresh request each time made Coil re-resolve what it already had in hand.
                val request = remember(context, model, attempt) {
                    ImageRequest.Builder(context)
                        .data(model)
                        // Kept out of the cache key: a retry is the same picture, only a new attempt.
                        .setParameter("attempt", attempt, memoryCacheKey = null)
                        .build()
                }
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    // Stickers are transparent artwork sized to their own aspect ratio; cropping
                    // them to a square tile cuts the drawing.
                    contentScale = if (type == AttachmentType.STICKER) {
                        ContentScale.Fit
                    } else {
                        ContentScale.Crop
                    },
                    onState = { failed = it is AsyncImagePainter.State.Error },
                    modifier = Modifier.matchParentSize(),
                )
                // A signed link that has since expired answers 404; saying so beats a tile that
                // looks like it is still loading.
                if (failed && !canRetry) {
                    Icon(
                        Icons.Default.BrokenImage,
                        contentDescription = stringResource(R.string.attachment_load_failed),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (type == AttachmentType.VIDEO) {
                    Icon(
                        Icons.Default.PlayCircle,
                        contentDescription = stringResource(R.string.attachment_type_video),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            AttachmentType.FILE,
            AttachmentType.AUDIO,
            AttachmentType.LINK,
            AttachmentType.WALL,
            AttachmentType.CALL,
            AttachmentType.OTHER,
            -> AttachmentPlaceholder(type, caption, Modifier.matchParentSize())
        }
    }
}
