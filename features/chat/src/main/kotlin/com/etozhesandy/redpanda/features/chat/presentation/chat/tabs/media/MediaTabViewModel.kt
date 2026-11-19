package com.etozhesandy.redpanda.features.chat.presentation.chat.tabs.media

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.etozhesandy.redpanda.core.common.dispatcher.DefaultDispatcher
import com.etozhesandy.redpanda.core.common.mvi.BaseViewModel
import com.etozhesandy.redpanda.core.common.mvi.sortPreference
import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.core.model.MediaSort
import com.etozhesandy.redpanda.core.model.naturalAscending
import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.core.navigation.manager.INavigationManager
import com.etozhesandy.redpanda.features.chat.domain.usecase.ObserveAppSettingsUseCase
import com.etozhesandy.redpanda.features.chat.domain.model.sortedBy
import com.etozhesandy.redpanda.features.chat.domain.usecase.ObserveChatBookmarkUseCase
import com.etozhesandy.redpanda.features.chat.domain.usecase.SaveChatBookmarkUseCase
import com.etozhesandy.redpanda.features.chat.mapper.toBookmarkTab
import com.etozhesandy.redpanda.features.chat.model.ChatArgs
import com.etozhesandy.redpanda.features.chat.model.ChatMediaTab
import com.etozhesandy.redpanda.features.chat.presentation.chat.ChatMediaScrollCache
import com.etozhesandy.redpanda.features.chat.presentation.chat.MediaScrollSlot
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

/**
 * The half of a media tab that doesn't depend on what it shows: its own ordering, the thumbnail
 * size from the settings, and the cell of the scroll cache it restores from.
 *
 * The ordering is per-tab: each subclass builds its own [sortPreference] over its own
 * [SavedStateHandle], so sorting the photos leaves the videos as the user left them.
 */
abstract class MediaTabViewModel(
    savedStateHandle: SavedStateHandle,
    observeAppSettings: ObserveAppSettingsUseCase,
    private val nav: INavigationManager,
    private val args: ChatArgs,
    scrollCache: ChatMediaScrollCache,
    observeBookmark: ObserveChatBookmarkUseCase,
    private val saveBookmark: SaveChatBookmarkUseCase,
    private val tab: ChatMediaTab,
    attachments: Flow<List<Attachment>>,
    @DefaultDispatcher defaultDispatcher: CoroutineDispatcher,
) : BaseViewModel<MediaTabState.State, MediaTabState.Event, MediaTabState.Effect>() {

    override fun createInitialState() = MediaTabState.State(
        revealAttachmentId = args.revealAttachmentId.takeIf { args.revealTab == tab },
    )

    /** Read once by the grid on its first composition, then written to as the user scrolls. */
    val scrollSlot: MediaScrollSlot = scrollCache.slot(args.dialogId, tab)

    private val sort = savedStateHandle.sortPreference<MediaSort>(
        keyPrefix = "media",
        defaults = observeAppSettings().map { it.defaultMediaSort to it.defaultMediaSortAscending },
        naturalAscending = { it.naturalAscending },
        memory = scrollCache.sortMemory(args.dialogId, tab),
    )

    init {
        sort.flow
            .onEach { (sort, ascending) -> setState { copy(sort = sort, sortAscending = ascending) } }
            .launchIn(viewModelScope)

        observeAppSettings()
            .onEach { settings -> setState { copy(imageWidthDp = settings.mediaImageWidthDp) } }
            .launchIn(viewModelScope)

        observeBookmark(args.dialogId, tab.toBookmarkTab())
            .onEach { id -> setState { copy(bookmarkAttachmentId = id) } }
            .launchIn(viewModelScope)

        attachments.sortedBy(sort.flow, defaultDispatcher)
            .onEach { sorted -> setState { copy(attachments = sorted) } }
            .launchIn(viewModelScope)
    }

    /** The grid is now at the attachment: the request is spent, and the attachment blinks for a moment. */
    private fun onRevealHandled() {
        val revealed = currentState.revealAttachmentId
        setState { copy(revealAttachmentId = null, highlightedAttachmentId = revealed) }
        if (revealed == null) return
        highlightJob?.cancel()
        highlightJob = viewModelScope.launch {
            delay(HIGHLIGHT_DURATION_MS)
            setState { copy(highlightedAttachmentId = null) }
        }
    }

    private var highlightJob: Job? = null

    override fun onEvent(event: MediaTabState.Event) {
        when (event) {
            is MediaTabState.Event.AttachmentClicked ->
                nav.navigate(Routes.PhotoViewer(args.dialogId, event.attachmentId))
            is MediaTabState.Event.SaveBookmarkClicked ->
                launchSafe { saveBookmark(args.dialogId, tab.toBookmarkTab(), event.attachmentId) }
            MediaTabState.Event.GoToBookmarkClicked ->
                currentState.bookmarkAttachmentId?.let { id -> setState { copy(revealAttachmentId = id) } }
            MediaTabState.Event.RevealHandled -> onRevealHandled()
            is MediaTabState.Event.SortSelected -> {
                val ascending = sort.select(
                    picked = event.sort,
                    current = currentState.sort,
                    currentAscending = currentState.sortAscending,
                )
                setState { copy(sort = event.sort, sortAscending = ascending) }
                setEffect { MediaTabState.Effect.ScrollToTop }
            }
        }
    }
}

/** How long the attachment a grid has scrolled to keeps blinking. */
private const val HIGHLIGHT_DURATION_MS = 2_000L
