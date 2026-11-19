package com.etozhesandy.redpanda.features.chat.presentation.chat.tabs.messages

import com.etozhesandy.redpanda.core.common.net.openExternally
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.etozhesandy.redpanda.features.chat.presentation.chat.tabs.messages.view.MessagesList
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.paging.compose.LazyPagingItems
import com.etozhesandy.redpanda.features.chat.R
import com.etozhesandy.redpanda.features.chat.model.MessageUi
import com.etozhesandy.redpanda.features.chat.presentation.chat.view.AudioListItem
import com.etozhesandy.redpanda.features.chat.presentation.chat.view.BookmarkActions
import com.etozhesandy.redpanda.features.chat.presentation.chat.view.TabActionsHeight
import com.etozhesandy.redpanda.features.chat.presentation.chat.view.TabActionsRow
import com.etozhesandy.redpanda.features.chat.presentation.chat.view.rememberAudioPlayback
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first

@Composable
fun MessagesTabScreen(
    state: MessagesTabState.State,
    effect: Flow<MessagesTabState.Effect>,
    pagingItems: LazyPagingItems<MessageUi>,
    listState: LazyListState,
    onEvent: (MessagesTabState.Event) -> Unit,
    modifier: Modifier = Modifier,
    targetMessageId: String? = null,
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        effect.collectLatest { current ->
            when (current) {
                is MessagesTabState.Effect.OpenExternally -> context.openExternally(current.url)
            }
        }
    }

    // A database paging anchor is not a LazyColumn index: refresh may load earlier rows,
    // and prepends can shift the target. Locate the actual message in the loaded snapshot.
    var targetReached by rememberSaveable(targetMessageId) { mutableStateOf(false) }
    LaunchedEffect(targetMessageId, pagingItems, listState) {
        if (targetMessageId == null || targetReached) return@LaunchedEffect
        val index = snapshotFlow {
            pagingItems.itemSnapshotList.items.indexOfFirst { it.message.id == targetMessageId }
        }.first { it >= 0 }
        listState.scrollToItem(index)
        targetReached = true
    }
    // One player for the whole chat, so starting a voice message stops the previous one.
    val playback = rememberAudioPlayback()
    Box(modifier = modifier.fillMaxSize()) {
        MessagesList(
            audioContent = { attachment ->
                LaunchedEffect(attachment.id) { onEvent(MessagesTabState.Event.AudioShown(attachment)) }
                val prepared = state.preparedAudio[attachment.id]
                AudioListItem(
                    attachment = attachment,
                    isPlaying = playback.isPlaying(attachment),
                    onClick = { playback.toggle(attachment, prepared?.localPath) },
                    durationMs = prepared?.durationMs,
                    progress = playback.progressOf(attachment, prepared?.durationMs),
                    onSeek = { playback.seekTo(attachment, it) },
                )
            },
            favoriteIds = state.favoriteIds,
            bookmarkMessageId = state.bookmarkMessageId,
            pagingItems = pagingItems,
            listState = listState,
            onEvent = onEvent,
            contentPadding = PaddingValues(top = TabActionsHeight),
        )
        TabActionsRow(modifier = Modifier.align(Alignment.TopEnd)) {
            BookmarkActions(
                hasBookmark = state.bookmarkMessageId != null,
                onSave = {
                    pagingItems.peek(listState.firstVisibleItemIndex)?.let {
                        onEvent(MessagesTabState.Event.SaveBookmarkClicked(it.message.id))
                    }
                },
                onGoTo = { onEvent(MessagesTabState.Event.GoToBookmarkClicked) },
            )
            // Flipping the order re-anchors paging instead of reordering a loaded list, so this
            // is a toggle rather than one of the sort menus the other tabs draw here.
            IconButton(onClick = { onEvent(MessagesTabState.Event.ToggleOrderReversed) }) {
                Icon(Icons.Default.SwapVert, contentDescription = stringResource(R.string.chat_action_reverse_order))
            }
        }
    }
}
