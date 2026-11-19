package com.etozhesandy.redpanda.features.chat.presentation.photo

import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.core.common.mvi.UiEffect
import com.etozhesandy.redpanda.core.common.mvi.UiEvent
import com.etozhesandy.redpanda.core.common.mvi.UiState

/** MVI-контракт экрана: состояние, события и одноразовые эффекты. */
object PhotoViewerState {

    data class State(
        val attachments: List<Attachment> = emptyList(),
        val startIndex: Int = 0,
        /** Opened from a message: the jump button returns to the media tab, which is always possible. */
        val fromMessages: Boolean = false,
        /**
         * The attachments the jump button can act on. Gallery-sourced attachments carry no message
         * of their own, and neither does one whose twin was never sent inline — there would be
         * nowhere for the jump to land. Everything is jumpable when [fromMessages].
         */
        val jumpableIds: Set<String> = emptySet(),
    ) : UiState

    sealed interface Event : UiEvent {
        data class JumpToMessageClicked(val attachment: Attachment) : Event
        data class DownloadClicked(val source: String) : Event
        data object BackClicked : Event
    }

    sealed interface Effect : UiEffect {
        /** [savedTo] is the folder under Download/ the file landed in; null means it did not. */
        data class DownloadFinished(val savedTo: String?) : Effect
    }
}
