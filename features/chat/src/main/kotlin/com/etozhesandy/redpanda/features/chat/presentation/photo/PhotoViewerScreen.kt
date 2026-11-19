package com.etozhesandy.redpanda.features.chat.presentation.photo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.etozhesandy.redpanda.core.designsystem.components.DownloadIconButton
import com.etozhesandy.redpanda.core.designsystem.components.GoToMessageIconButton
import com.etozhesandy.redpanda.core.designsystem.components.rememberDownloadToast
import com.etozhesandy.redpanda.core.designsystem.media.AttachmentPage
import com.etozhesandy.redpanda.core.designsystem.media.MediaPagerScreen
import com.etozhesandy.redpanda.core.designsystem.media.VideoPlayer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

/** Full-screen swipeable viewer over everything the dialog holds, photos and videos alike. */
@Composable
fun PhotoViewerScreen(
    state: PhotoViewerState.State,
    effect: Flow<PhotoViewerState.Effect>,
    onEvent: (PhotoViewerState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    val showDownloadOutcome = rememberDownloadToast()
    LaunchedEffect(Unit) {
        effect.collectLatest { current ->
            when (current) {
                is PhotoViewerState.Effect.DownloadFinished -> showDownloadOutcome(current.savedTo)
            }
        }
    }

    MediaPagerScreen(
        items = state.attachments,
        startIndex = state.startIndex,
        key = { it.id },
        onBack = { onEvent(PhotoViewerState.Event.BackClicked) },
        modifier = modifier,
        actions = { current ->
            GoToMessageIconButton(enabled = current != null && current.id in state.jumpableIds) {
                onEvent(PhotoViewerState.Event.JumpToMessageClicked(current!!))
            }
            DownloadIconButton(source = current?.path?.takeIf { it.isNotBlank() }) { source ->
                onEvent(PhotoViewerState.Event.DownloadClicked(source))
            }
        },
    ) { attachment, isCurrentPage, zoom ->
        AttachmentPage(
            attachment = attachment,
            isCurrentPage = isCurrentPage,
            videoPlayer = { uri, autoPlay -> VideoPlayer(uri = uri, autoPlay = autoPlay) },
            zoom = zoom,
        )
    }
}
