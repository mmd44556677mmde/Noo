package com.etozhesandy.redpanda.features.chat.presentation.chat.tabs.media

import androidx.lifecycle.SavedStateHandle
import com.etozhesandy.redpanda.core.common.dispatcher.DefaultDispatcher
import com.etozhesandy.redpanda.core.navigation.manager.INavigationManager
import com.etozhesandy.redpanda.features.chat.domain.usecase.ObserveAppSettingsUseCase
import com.etozhesandy.redpanda.features.chat.domain.usecase.ObserveChatBookmarkUseCase
import com.etozhesandy.redpanda.features.chat.domain.usecase.ObserveDialogVideosUseCase
import com.etozhesandy.redpanda.features.chat.domain.usecase.SaveChatBookmarkUseCase
import com.etozhesandy.redpanda.features.chat.model.ChatArgs
import com.etozhesandy.redpanda.features.chat.model.ChatMediaTab
import com.etozhesandy.redpanda.features.chat.presentation.chat.ChatMediaScrollCache
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher

@HiltViewModel
class VideosTabViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeAppSettings: ObserveAppSettingsUseCase,
    nav: INavigationManager,
    args: ChatArgs,
    scrollCache: ChatMediaScrollCache,
    observeBookmark: ObserveChatBookmarkUseCase,
    saveBookmark: SaveChatBookmarkUseCase,
    observeDialogVideos: ObserveDialogVideosUseCase,
    @DefaultDispatcher defaultDispatcher: CoroutineDispatcher,
) : MediaTabViewModel(
    savedStateHandle = savedStateHandle,
    observeAppSettings = observeAppSettings,
    nav = nav,
    args = args,
    scrollCache = scrollCache,
    observeBookmark = observeBookmark,
    saveBookmark = saveBookmark,
    tab = ChatMediaTab.VIDEOS,
    attachments = observeDialogVideos(args.dialogId),
    defaultDispatcher = defaultDispatcher,
)
