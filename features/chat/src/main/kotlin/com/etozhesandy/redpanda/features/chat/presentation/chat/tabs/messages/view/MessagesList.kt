package com.etozhesandy.redpanda.features.chat.presentation.chat.tabs.messages.view

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.features.chat.model.MessageUi
import com.etozhesandy.redpanda.features.chat.presentation.chat.tabs.messages.MessagesTabState
import com.etozhesandy.redpanda.features.chat.presentation.chat.utils.formatMessageDate
import com.etozhesandy.redpanda.features.chat.presentation.chat.utils.isSameDay
import com.etozhesandy.redpanda.features.chat.presentation.chat.view.DateSeparator
import com.etozhesandy.redpanda.features.chat.presentation.chat.view.MessageBubble

@Composable
fun MessagesList(
    audioContent: @Composable (Attachment) -> Unit,
    favoriteIds: Set<String>,
    bookmarkMessageId: String?,
    pagingItems: LazyPagingItems<MessageUi>,
    listState: LazyListState,
    onEvent: (MessagesTabState.Event) -> Unit,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        items(
            count = pagingItems.itemCount,
            key = pagingItems.itemKey { it.message.id },
            // Plain text and a bubble full of media measure nothing alike; keeping them apart lets
            // a scrolled-off row be reused by one of its own kind.
            contentType = pagingItems.itemContentType { item ->
                if (item.attachments.isEmpty()) MESSAGE_TEXT else MESSAGE_WITH_ATTACHMENTS
            },
        ) { index ->
            val item = pagingItems[index] ?: return@items
            val message = item.message
            val previous = if (index > 0) pagingItems.peek(index - 1)?.message else null
            if (previous == null || !isSameDay(previous.timestampEpoch, message.timestampEpoch)) {
                DateSeparator(date = formatMessageDate(message.timestampEpoch))
            }
            val isFavorite = message.id in favoriteIds
            MessageBubble(
                message = message,
                attachments = item.attachments,
                isFavorite = isFavorite,
                isBookmarked = message.id == bookmarkMessageId,
                onFavoriteToggle = {
                    onEvent(MessagesTabState.Event.FavoriteToggled(message.id, !isFavorite))
                },
                onAttachmentClick = { attachment ->
                    onEvent(MessagesTabState.Event.AttachmentClicked(attachment))
                },
                onLinkClick = { url -> onEvent(MessagesTabState.Event.LinkClicked(url)) },
                audioContent = audioContent,
            )
        }
    }
}

private const val MESSAGE_TEXT = "text"
private const val MESSAGE_WITH_ATTACHMENTS = "attachments"
