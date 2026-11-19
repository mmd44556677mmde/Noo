package com.etozhesandy.redpanda.features.profile.presentation.mediaviewer

import com.etozhesandy.redpanda.features.profile.domain.usecase.DownloadImageUseCase
import androidx.lifecycle.viewModelScope
import com.etozhesandy.redpanda.core.common.dispatcher.DefaultDispatcher
import com.etozhesandy.redpanda.core.common.mvi.BaseViewModel
import com.etozhesandy.redpanda.core.navigation.manager.INavigationManager
import com.etozhesandy.redpanda.features.profile.domain.usecase.ObserveProfileUseCase
import com.etozhesandy.redpanda.features.profile.domain.usecase.ObserveArchiveFilesInFolderUseCase
import com.etozhesandy.redpanda.features.profile.model.ProfileMediaViewerArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@HiltViewModel
class ProfileMediaViewerViewModel @Inject constructor(
    private val nav: INavigationManager,
    private val args: ProfileMediaViewerArgs,
    private val observeProfile: ObserveProfileUseCase,
    private val observeArchiveFilesInFolder: ObserveArchiveFilesInFolderUseCase,
    private val downloadImage: DownloadImageUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : BaseViewModel<ProfileMediaViewerState.State, ProfileMediaViewerState.Event, ProfileMediaViewerState.Effect>() {

    override fun createInitialState() = ProfileMediaViewerState.State()

    /** Folder name for saved files; the id is only a fallback until the profile loads. */
    private var profileName: String = args.profileId

    init {
        observeProfile(args.profileId)
            .onEach { profile -> profile?.displayName?.takeIf { it.isNotBlank() }?.let { profileName = it } }
            .launchIn(viewModelScope)

        // The same source and order as the folder grid, so a swipe lands on the grid's neighbour.
        observeArchiveFilesInFolder(args.profileId, args.folder)
            .flowOn(defaultDispatcher)
            .onEach { attachments ->
                val startIndex = attachments.indexOfFirst { it.id == args.startAttachmentId }.coerceAtLeast(0)
                setState { copy(attachments = attachments, startIndex = startIndex, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: ProfileMediaViewerState.Event) {
        when (event) {
            ProfileMediaViewerState.Event.BackClicked -> nav.back()

            is ProfileMediaViewerState.Event.DownloadClicked -> launchSafe {
                // `args.folder` is the archive-relative folder path, so it nests as several levels.
                val savedTo = downloadImage(event.source, profileName, args.folder)
                setEffect { ProfileMediaViewerState.Effect.DownloadFinished(savedTo) }
            }
        }
    }
}
