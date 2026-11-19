package com.etozhesandy.redpanda.features.profile.presentation.profile

import com.etozhesandy.redpanda.features.profile.domain.usecase.ObserveImportProgressUseCase
import androidx.lifecycle.viewModelScope
import com.etozhesandy.redpanda.core.common.mvi.BaseViewModel
import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.core.navigation.manager.INavigationManager
import com.etozhesandy.redpanda.core.common.net.UrlGuard
import com.etozhesandy.redpanda.features.profile.domain.usecase.ObserveProfileUseCase
import com.etozhesandy.redpanda.features.profile.domain.usecase.RenameProfileUseCase
import com.etozhesandy.redpanda.features.profile.domain.usecase.UpdateProfileNotesUseCase
import com.etozhesandy.redpanda.features.profile.domain.usecase.ObserveFriendsUseCase
import com.etozhesandy.redpanda.features.profile.domain.usecase.ObserveGroupsUseCase
import com.etozhesandy.redpanda.features.profile.domain.usecase.ObserveSavedPhotosUseCase
import com.etozhesandy.redpanda.features.profile.domain.usecase.ObserveProfileAttachmentsPreviewUseCase
import com.etozhesandy.redpanda.features.profile.domain.usecase.ObserveProfileAttachmentsCountUseCase
import com.etozhesandy.redpanda.features.profile.domain.usecase.ObserveArchiveFilesPreviewUseCase
import com.etozhesandy.redpanda.features.profile.domain.usecase.ObserveArchiveFilesCountUseCase
import com.etozhesandy.redpanda.features.profile.model.ProfileArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

private const val PREVIEW_LIMIT = 6

/** How long the notes field has to be left alone before what was typed is saved. */
private const val NOTES_SAVE_DELAY_MS = 600L

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val nav: INavigationManager,
    args: ProfileArgs,
    private val observeProfile: ObserveProfileUseCase,
    private val renameProfile: RenameProfileUseCase,
    private val updateNotes: UpdateProfileNotesUseCase,
    private val observeFriends: ObserveFriendsUseCase,
    private val observeGroups: ObserveGroupsUseCase,
    private val observeSavedPhotos: ObserveSavedPhotosUseCase,
    private val observeAttachmentsPreview: ObserveProfileAttachmentsPreviewUseCase,
    private val observeAttachmentsCount: ObserveProfileAttachmentsCountUseCase,
    private val observeArchiveFilesPreview: ObserveArchiveFilesPreviewUseCase,
    private val observeArchiveFilesCount: ObserveArchiveFilesCountUseCase,
    observeImportProgress: ObserveImportProgressUseCase,
) : BaseViewModel<ProfileState.State, ProfileState.Event, ProfileState.Effect>() {

    override fun createInitialState() = ProfileState.State()

    private val profileId = args.profileId

    init {
        val profileFriendsGroups = combine(
            observeProfile(profileId),
            observeFriends(profileId),
            observeGroups(profileId),
        ) { profile, friends, groups -> Triple(profile, friends, groups) }
        // Only the preview strips and their counts are read: the full lists of a profile run to
        // tens of thousands of rows and are only needed by the screens behind «Все».
        val attachmentsPreview = combine(
            observeAttachmentsPreview(profileId, PREVIEW_LIMIT),
            observeAttachmentsCount(profileId),
        ) { preview, count -> preview to count }
        val mediaPreview = combine(
            observeArchiveFilesPreview(profileId, PREVIEW_LIMIT),
            observeArchiveFilesCount(profileId),
        ) { preview, count -> preview to count }
        val savedPhotosAttachmentsMedia = combine(
            observeSavedPhotos(profileId),
            attachmentsPreview,
            mediaPreview,
        ) { savedPhotos, attachments, media -> Triple(savedPhotos, attachments, media) }

        profileFriendsGroups.combine(savedPhotosAttachmentsMedia) { first, second ->
            val (profile, friends, groups) = first
            val (savedPhotos, attachments, media) = second
            ProfileState.State(
                profile = profile,
                friendsPreview = friends.take(PREVIEW_LIMIT),
                friendsCount = friends.size,
                groupsPreview = groups.take(PREVIEW_LIMIT),
                groupsCount = groups.size,
                savedPhotosPreview = savedPhotos.take(PREVIEW_LIMIT),
                savedPhotosCount = savedPhotos.size,
                attachmentsPreview = attachments.first,
                attachmentsCount = attachments.second,
                mediaPreview = media.first,
                mediaCount = media.second,
                isLoading = false,
            )
        }
            // The database snapshot replaces the whole state, so the progress carried by the
            // current one is copied over rather than reset to null on every emission.
            .onEach { snapshot -> setState { snapshot.copy(importProgress = importProgress) } }
            .launchIn(viewModelScope)

        // Import lands here now, so the screen shows the same live counters the dialog list does.
        // Kept out of the combine above: the store is a separate, in-process source whose updates
        // must not wait on a database emission to reach the screen.
        observeImportProgress(profileId)
            .onEach { progress -> setState { copy(importProgress = progress) } }
            .launchIn(viewModelScope)
    }

    /** Typing is saved after a pause rather than per keystroke, and at once when the screen is left. */
    private var pendingNotes: String? = null
    private var notesSaveJob: Job? = null

    private fun flushNotes() {
        val notes = pendingNotes ?: return
        pendingNotes = null
        notesSaveJob?.cancel()
        // Leaving the screen clears the ViewModel and cancels its scope, and the last words must
        // still reach the database.
        viewModelScope.launch { withContext(NonCancellable) { updateNotes(profileId, notes) } }
    }

    override fun onEvent(event: ProfileState.Event) {
        when (event) {
            ProfileState.Event.BackClicked -> {
                flushNotes()
                nav.back()
            }
            is ProfileState.Event.RenameConfirmed -> launchSafe { renameProfile(profileId, event.name) }
            is ProfileState.Event.NotesChanged -> {
                pendingNotes = event.notes
                notesSaveJob?.cancel()
                notesSaveJob = viewModelScope.launch {
                    delay(NOTES_SAVE_DELAY_MS)
                    flushNotes()
                }
            }
            ProfileState.Event.DialogsClicked -> nav.navigate(Routes.Dialogs(profileId))
            ProfileState.Event.LinkClicked -> {
                // The screen name comes from the imported archive, so it is validated rather than
                // concatenated: "@evil.com/x" would make vk.com the userinfo of another host.
                val url = UrlGuard.vkProfileUrl(currentState.profile?.screenName) ?: return
                setEffect { ProfileState.Effect.OpenLink(url) }
            }
            ProfileState.Event.FriendsAllClicked -> nav.navigate(Routes.ProfileFriends(profileId))
            ProfileState.Event.GroupsAllClicked -> nav.navigate(Routes.ProfileGroups(profileId))
            ProfileState.Event.SavedPhotosAllClicked -> nav.navigate(Routes.ProfileSavedPhotos(profileId))
            ProfileState.Event.AttachmentsAllClicked -> nav.navigate(Routes.ProfileAttachments(profileId))
            ProfileState.Event.MediaAllClicked -> nav.navigate(Routes.ProfileMedia(profileId))
        }
    }
}
