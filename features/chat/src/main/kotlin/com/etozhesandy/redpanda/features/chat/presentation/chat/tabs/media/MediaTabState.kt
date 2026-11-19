package com.etozhesandy.redpanda.features.chat.presentation.chat.tabs.media

import com.etozhesandy.redpanda.core.common.mvi.UiEffect
import com.etozhesandy.redpanda.core.common.mvi.UiEvent
import com.etozhesandy.redpanda.core.common.mvi.UiState
import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.core.model.MediaSort
import com.etozhesandy.redpanda.core.settings.AppSettings

/** MVI-контракт таба-сетки. Общий для «Фото» и «Видео» — они различаются только источником данных. */
object MediaTabState {

    data class State(
        val attachments: List<Attachment> = emptyList(),
        val imageWidthDp: Int = AppSettings.DEFAULT_MEDIA_IMAGE_WIDTH_DP,
        val sort: MediaSort = MediaSort.DATE,
        val sortAscending: Boolean = true,
        /** An attachment the grid must bring into view once, then report back via [Event.RevealHandled]. */
        val revealAttachmentId: String? = null,
        /** The attachment the reader bookmarked in this tab, if any. */
        val bookmarkAttachmentId: String? = null,
        /** The attachment the grid has just scrolled to; it blinks for a moment so it is easy to spot. */
        val highlightedAttachmentId: String? = null,
    ) : UiState

    sealed interface Event : UiEvent {
        data class AttachmentClicked(val attachmentId: String) : Event

        data object RevealHandled : Event

        /** Remembers [attachmentId] — the first one on screen — as the reader's place in this tab. */
        data class SaveBookmarkClicked(val attachmentId: String) : Event

        data object GoToBookmarkClicked : Event

        /** Picking the sort that's already active flips the direction instead of changing the key. */
        data class SortSelected(val sort: MediaSort) : Event
    }

    sealed interface Effect : UiEffect {
        data object ScrollToTop : Effect
    }
}
