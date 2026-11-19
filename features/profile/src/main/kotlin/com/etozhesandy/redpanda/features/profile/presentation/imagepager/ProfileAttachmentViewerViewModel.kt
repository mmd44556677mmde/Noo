package com.etozhesandy.redpanda.features.profile.presentation.imagepager

import com.etozhesandy.redpanda.features.profile.domain.usecase.DownloadImageUseCase
import com.etozhesandy.redpanda.features.profile.mapper.toPages
import android.content.Context
import androidx.lifecycle.viewModelScope
import com.etozhesandy.redpanda.core.common.dispatcher.DefaultDispatcher
import com.etozhesandy.redpanda.core.common.mvi.BaseViewModel
import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.core.navigation.manager.INavigationManager
import com.etozhesandy.redpanda.features.profile.R
import com.etozhesandy.redpanda.features.profile.domain.usecase.ObserveProfileUseCase
import com.etozhesandy.redpanda.features.profile.domain.usecase.ObserveProfileAttachmentsUseCase
import com.etozhesandy.redpanda.features.profile.model.ProfileAttachmentViewerArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

@HiltViewModel
class ProfileAttachmentViewerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val nav: INavigationManager,
    private val args: ProfileAttachmentViewerArgs,
    private val observeProfile: ObserveProfileUseCase,
    private val observeAttachments: ObserveProfileAttachmentsUseCase,
    private val downloadImage: DownloadImageUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : BaseViewModel<ImagePagerState.State, ImagePagerState.Event, ImagePagerState.Effect>() {

    override fun createInitialState() = ImagePagerState.State()

    /** Folder name for saved files; the id is only a fallback until the profile loads. */
    private var profileName: String = args.profileId

    init {
        observeProfile(args.profileId)
            .onEach { profile -> profile?.displayName?.takeIf { it.isNotBlank() }?.let { profileName = it } }
            .launchIn(viewModelScope)

        observeAttachments(args.profileId)
            .map { attachments -> attachments.toPages() to attachments.indexOfFirst { it.id == args.startAttachmentId } }
            .flowOn(defaultDispatcher)
            .onEach { (pages, index) ->
                setState { copy(pages = pages, startIndex = index.coerceAtLeast(0), isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: ImagePagerState.Event) {
        when (event) {
            ImagePagerState.Event.BackClicked -> nav.back()

            // The grid this viewer was opened from stays on the stack, so going back from the
            // dialog returns to the photo and then to the grid.
            is ImagePagerState.Event.JumpToMessageClicked -> nav.navigate(
                Routes.Chat(
                    dialogId = event.anchor.dialogId,
                    profileId = event.anchor.profileId,
                    scrollToMessageId = event.anchor.messageId,
                ),
            )

            is ImagePagerState.Event.DownloadClicked -> launchSafe {
                val savedTo = downloadImage(event.url, profileName, context.getString(R.string.download_folder_attachments))
                setEffect { ImagePagerState.Effect.DownloadFinished(savedTo) }
            }
        }
    }
}
