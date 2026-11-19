package com.etozhesandy.redpanda.features.profile.presentation.mediafolder

import androidx.lifecycle.viewModelScope
import com.etozhesandy.redpanda.core.common.mvi.BaseViewModel
import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.core.navigation.manager.INavigationManager
import com.etozhesandy.redpanda.features.profile.domain.usecase.ObserveAppSettingsUseCase
import com.etozhesandy.redpanda.features.profile.domain.usecase.ObserveArchiveFilesInFolderUseCase
import com.etozhesandy.redpanda.features.profile.utils.folderDisplayName
import com.etozhesandy.redpanda.features.profile.model.ProfileMediaFolderArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@HiltViewModel
class ProfileMediaFolderViewModel @Inject constructor(
    private val nav: INavigationManager,
    private val args: ProfileMediaFolderArgs,
    private val observeArchiveFilesInFolder: ObserveArchiveFilesInFolderUseCase,
    observeAppSettings: ObserveAppSettingsUseCase,
) : BaseViewModel<ProfileMediaFolderState.State, ProfileMediaFolderState.Event, ProfileMediaFolderState.Effect>() {

    override fun createInitialState() = ProfileMediaFolderState.State()

    init {
        setState { copy(folderName = folderDisplayName(args.folder)) }
        observeArchiveFilesInFolder(args.profileId, args.folder)
            .onEach { attachments -> setState { copy(attachments = attachments, isLoading = false) } }
            .launchIn(viewModelScope)
        observeAppSettings()
            .onEach { settings -> setState { copy(imageWidthDp = settings.mediaImageWidthDp) } }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: ProfileMediaFolderState.Event) {
        when (event) {
            ProfileMediaFolderState.Event.BackClicked -> nav.back()
            is ProfileMediaFolderState.Event.AttachmentClicked ->
                nav.navigate(Routes.ProfileMediaViewer(args.profileId, args.folder, event.attachment.id))
        }
    }
}
