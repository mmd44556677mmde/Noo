package com.etozhesandy.redpanda.features.chat.presentation.chat.tabs.messages

import com.etozhesandy.redpanda.core.common.mvi.UiEffect
import com.etozhesandy.redpanda.core.common.mvi.UiEvent
import com.etozhesandy.redpanda.core.common.mvi.UiState
import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.features.chat.domain.model.PreparedAudio

/**
 * MVI-контракт таба «Сообщения».
 *
 * Самих сообщений в состоянии нет: `PagingData` — это поток, который Compose собирает сам, и
 * снимок такого потока был бы новым списком на каждой рекомпозиции.
 */
object MessagesTabState {

    /**
     * [favoriteIds] rather than a flag on each message: the messages arrive as `PagingData`, whose
     * pages are read once and never re-read, so the stars have to be watched separately.
     */
    data class State(
        val isReversed: Boolean = false,
        val favoriteIds: Set<String> = emptySet(),
        /** Audio prepared for playback — a local copy and its length — by attachment id. */
        val preparedAudio: Map<String, PreparedAudio> = emptyMap(),
        /** The message the reader bookmarked in this chat, if any. */
        val bookmarkMessageId: String? = null,
    ) : UiState

    sealed interface Event : UiEvent {
        data class FavoriteToggled(val messageId: String, val isFavorite: Boolean) : Event
        /**
         * Only media belongs in the photo viewer; a document or a wall post opens where it actually
         * lives, and metadata-only kinds have nowhere to go at all.
         */
        data class AttachmentClicked(val attachment: Attachment) : Event

        /** A link in a message's text: a VK page opens inside the app, any other goes to the browser. */
        data class LinkClicked(val url: String) : Event

        /** A voice message came into view, so its audio is worth preparing. */
        data class AudioShown(val attachment: Attachment) : Event

        /** Remembers [messageId] — the first message on screen — as the reader's place in the chat. */
        data class SaveBookmarkClicked(val messageId: String) : Event

        data object GoToBookmarkClicked : Event

        /** Flips the reading order, which re-anchors paging rather than reordering a loaded list. */
        data object ToggleOrderReversed : Event
    }

    sealed interface Effect : UiEffect {
        /** Documents live on VK's servers, so the only thing to do with one is hand it to the browser. */
        data class OpenExternally(val url: String) : Effect
    }
}
