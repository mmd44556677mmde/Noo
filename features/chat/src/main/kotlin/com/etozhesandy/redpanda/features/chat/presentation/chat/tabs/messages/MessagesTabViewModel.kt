package com.etozhesandy.redpanda.features.chat.presentation.chat.tabs.messages

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.etozhesandy.redpanda.core.common.dispatcher.DefaultDispatcher
import com.etozhesandy.redpanda.core.common.mvi.BaseViewModel
import com.etozhesandy.redpanda.core.common.net.UrlGuard
import com.etozhesandy.redpanda.core.designsystem.media.isVisualMedia
import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.core.model.AttachmentType
import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.core.navigation.manager.INavigationManager
import com.etozhesandy.redpanda.core.navigation.manager.PopUpTo
import com.etozhesandy.redpanda.features.chat.domain.usecase.ObserveAppSettingsUseCase
import com.etozhesandy.redpanda.features.chat.domain.model.BookmarkTab
import com.etozhesandy.redpanda.features.chat.domain.usecase.GetMessagePositionUseCase
import com.etozhesandy.redpanda.features.chat.domain.usecase.ObserveChatBookmarkUseCase
import com.etozhesandy.redpanda.features.chat.domain.usecase.SaveChatBookmarkUseCase
import com.etozhesandy.redpanda.features.chat.domain.usecase.ObserveFavoriteMessageIdsUseCase
import com.etozhesandy.redpanda.features.chat.domain.usecase.ObserveMessagesUseCase
import com.etozhesandy.redpanda.features.chat.domain.usecase.ToggleFavoriteUseCase
import com.etozhesandy.redpanda.features.chat.mapper.toOrderOverride
import com.etozhesandy.redpanda.features.chat.mapper.toUi
import com.etozhesandy.redpanda.features.chat.model.ChatArgs
import com.etozhesandy.redpanda.features.chat.model.MessageUi
import com.etozhesandy.redpanda.features.chat.presentation.chat.handler.AudioHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MessagesTabViewModel @Inject constructor(
    private val nav: INavigationManager,
    private val args: ChatArgs,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val saveBookmark: SaveChatBookmarkUseCase,
    private val audio: AudioHandler,
    observeMessages: ObserveMessagesUseCase,
    observeFavoriteIds: ObserveFavoriteMessageIdsUseCase,
    observeBookmark: ObserveChatBookmarkUseCase,
    getMessagePosition: GetMessagePositionUseCase,
    observeAppSettings: ObserveAppSettingsUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : BaseViewModel<MessagesTabState.State, MessagesTabState.Event, MessagesTabState.Effect>() {

    override fun createInitialState() = MessagesTabState.State()

    /**
     * Resolved once for the whole tab so the paging source and the order toggle can't end up
     * disagreeing if the setting changes while the chat is open.
     */
    private val reversed: Deferred<Boolean> = viewModelScope.async {
        args.orderReversed ?: observeAppSettings().first().defaultChatReversed
    }

    val pagingMessages: Flow<PagingData<MessageUi>> = flow {
        val isReversed = reversed.await()
        val position = args.scrollToMessageId?.let { messageId ->
            getMessagePosition(args.dialogId, messageId, isReversed)
        }
        emit(isReversed to position)
    }
        .flatMapLatest { (isReversed, initialPosition) ->
            observeMessages(args.dialogId, isReversed, initialPosition)
        }
        .map { pagingData -> pagingData.map { it.toUi() } }
        // Mapping each page is dispatched from wherever this is collected, and `cachedIn` collects
        // in `viewModelScope` — i.e. on the main thread without this.
        .flowOn(defaultDispatcher)
        .cachedIn(viewModelScope)

    init {
        launchSafe {
            val isReversed = reversed.await()
            setState { copy(isReversed = isReversed) }
        }
        observeFavoriteIds(args.dialogId)
            .onEach { ids -> setState { copy(favoriteIds = ids) } }
            .launchIn(viewModelScope)
        observeBookmark(args.dialogId, BookmarkTab.MESSAGES)
            .onEach { id -> setState { copy(bookmarkMessageId = id) } }
            .launchIn(viewModelScope)
        audio.prepared
            .onEach { prepared -> setState { copy(preparedAudio = prepared) } }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: MessagesTabState.Event) {
        when (event) {
            is MessagesTabState.Event.FavoriteToggled ->
                launchSafe { toggleFavorite(event.messageId, event.isFavorite) }
            is MessagesTabState.Event.AttachmentClicked -> onAttachmentClicked(event.attachment)
            is MessagesTabState.Event.LinkClicked -> onLinkClicked(event.url)
            is MessagesTabState.Event.AudioShown -> audio.request(viewModelScope, event.attachment)
            is MessagesTabState.Event.SaveBookmarkClicked ->
                launchSafe { saveBookmark(args.dialogId, BookmarkTab.MESSAGES, event.messageId) }
            MessagesTabState.Event.GoToBookmarkClicked -> currentState.bookmarkMessageId?.let(::openAt)
            MessagesTabState.Event.ToggleOrderReversed -> recreateChat()
        }
    }

    private fun onLinkClicked(url: String) {
        when {
            UrlGuard.isVkUrl(url) -> nav.navigate(Routes.WebView(url))
            UrlGuard.isWebUrl(url) -> setEffect { MessagesTabState.Effect.OpenExternally(url) }
        }
    }

    private fun onAttachmentClicked(attachment: Attachment) {
        when {
            attachment.type.isVisualMedia ->
                nav.navigate(Routes.PhotoViewer(args.dialogId, attachment.id, fromMessages = true))
            // A wall post is a link to a page on VK, which stays inside the app's WebView; any
            // other link is the browser's business.
            UrlGuard.isVkUrl(attachment.path) -> nav.navigate(Routes.WebView(attachment.path))
            attachment.type != AttachmentType.AUDIO && attachment.path.startsWith("http") ->
                setEffect { MessagesTabState.Effect.OpenExternally(attachment.path) }
        }
    }

    /** Reopens the chat anchored at [messageId], in the order the reader is already using. */
    private fun openAt(messageId: String) {
        nav.navigate(
            Routes.Chat(
                dialogId = args.dialogId,
                profileId = args.profileId,
                scrollToMessageId = messageId,
                orderOverride = currentState.isReversed.toOrderOverride(),
            ),
            PopUpTo(Routes.Chat::class, inclusive = true),
        )
    }

    /**
     * Reopens the chat from scratch: the paging source is built around one order and one anchor
     * message, so changing either means a new screen rather than a new state.
     */
    private fun recreateChat() {
        nav.navigate(
            Routes.Chat(
                dialogId = args.dialogId,
                profileId = args.profileId,
                orderOverride = (!currentState.isReversed).toOrderOverride(),
            ),
            PopUpTo(Routes.Chat::class, inclusive = true),
        )
    }
}
