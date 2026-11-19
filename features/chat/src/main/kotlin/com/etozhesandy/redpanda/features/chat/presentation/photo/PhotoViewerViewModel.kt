package com.etozhesandy.redpanda.features.chat.presentation.photo

import com.etozhesandy.redpanda.features.chat.domain.usecase.DownloadImageUseCase
import androidx.lifecycle.viewModelScope
import com.etozhesandy.redpanda.core.common.mvi.BaseViewModel
import com.etozhesandy.redpanda.core.model.AttachmentType
import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.core.navigation.manager.INavigationManager
import com.etozhesandy.redpanda.core.navigation.manager.PopUpTo
import com.etozhesandy.redpanda.features.chat.domain.usecase.GetAttachmentUseCase
import com.etozhesandy.redpanda.features.chat.domain.usecase.ObserveChatProfileUseCase
import com.etozhesandy.redpanda.features.chat.domain.usecase.ObserveDialogMediaUseCase
import com.etozhesandy.redpanda.features.chat.domain.usecase.ObserveDialogUseCase
import com.etozhesandy.redpanda.features.chat.model.PhotoViewerArgs
import com.etozhesandy.redpanda.features.chat.utils.jumpableIdsOf
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@HiltViewModel
class PhotoViewerViewModel @Inject constructor(
    private val args: PhotoViewerArgs,
    private val nav: INavigationManager,
    observeDialogMedia: ObserveDialogMediaUseCase,
    private val getAttachment: GetAttachmentUseCase,
    observeDialog: ObserveDialogUseCase,
    private val observeProfile: ObserveChatProfileUseCase,
    private val downloadImage: DownloadImageUseCase,
) : BaseViewModel<PhotoViewerState.State, PhotoViewerState.Event, PhotoViewerState.Effect>() {

    override fun createInitialState() = PhotoViewerState.State(fromMessages = args.fromMessages)

    // Folder names for saved files; ids are only a fallback until the dialog and profile load.
    private var profileName: String = ""
    private var dialogName: String = args.dialogId
    private var isProfileObserved = false

    init {
        observeDialog(args.dialogId)
            .onEach { dialog ->
                if (dialog == null) return@onEach
                dialog.peerName.takeIf { it.isNotBlank() }?.let { dialogName = it }
                observeProfileName(dialog.profileId)
            }
            .launchIn(viewModelScope)

        observeDialogMedia(args.dialogId)
            .onEach { attachments ->
                val index = attachments.indexOfFirst { it.id == args.startAttachmentId }
                if (index >= 0) {
                    setState {
                        copy(
                            attachments = attachments,
                            startIndex = index,
                            jumpableIds = jumpableIdsOf(attachments, args.fromMessages),
                        )
                    }
                } else {
                    showStartAttachmentAlone()
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * The dialog's media list holds photos and videos only, so a sticker or a drawing opened from a
     * message is not in it. It is shown on its own: falling back to the first picture of the list
     * would open the wrong one, and there is nothing to swipe to from a sticker.
     */
    private fun showStartAttachmentAlone() {
        launchSafe {
            val attachment = getAttachment(args.startAttachmentId) ?: return@launchSafe
            val attachments = listOf(attachment)
            setState {
                copy(
                    attachments = attachments,
                    startIndex = 0,
                    jumpableIds = jumpableIdsOf(attachments, args.fromMessages),
                )
            }
        }
    }

    override fun onEvent(event: PhotoViewerState.Event) {
        when (event) {
            is PhotoViewerState.Event.JumpToMessageClicked -> {
                if (args.fromMessages) {
                    // Opened from a message, so the jump goes the other way: to the media tab
                    // holding this attachment, which the rebuilt chat scrolls into view.
                    nav.navigate(
                        Routes.Chat(
                            dialogId = event.attachment.dialogId,
                            profileId = event.attachment.profileId,
                            revealAttachmentId = event.attachment.id,
                            revealTab = if (event.attachment.type == AttachmentType.VIDEO) {
                                Routes.Chat.TAB_VIDEOS
                            } else {
                                Routes.Chat.TAB_PHOTOS
                            },
                        ),
                        PopUpTo(Routes.Chat::class, inclusive = true),
                    )
                    return
                }
                // Gallery-sourced attachments (VK's flat photos.html/videos.html export) carry no
                // messageId of their own — fall back to another attachment with the same path that
                // was also sent inline in a message, since VK re-uses the same CDN URL for both.
                val messageId = event.attachment.messageId
                    ?: currentState.attachments.firstOrNull { it.path == event.attachment.path && it.messageId != null }?.messageId
                    ?: return
                // The chat is rebuilt around the target message, so the old chat entry goes with
                // this viewer rather than staying behind it.
                nav.navigate(
                    Routes.Chat(
                        dialogId = event.attachment.dialogId,
                        profileId = event.attachment.profileId,
                        scrollToMessageId = messageId,
                    ),
                    PopUpTo(Routes.Chat::class, inclusive = true),
                )
            }

            PhotoViewerState.Event.BackClicked -> nav.back()

            is PhotoViewerState.Event.DownloadClicked -> launchSafe {
                val savedTo = downloadImage(event.source, profileName, dialogName)
                setEffect { PhotoViewerState.Effect.DownloadFinished(savedTo) }
            }
        }
    }

    /** The route carries only a dialogId, so the profile is reached through the dialog. */
    private fun observeProfileName(profileId: String) {
        if (isProfileObserved) return
        isProfileObserved = true
        observeProfile(profileId)
            .onEach { profile -> profile?.displayName?.takeIf { it.isNotBlank() }?.let { profileName = it } }
            .launchIn(viewModelScope)
    }
}
