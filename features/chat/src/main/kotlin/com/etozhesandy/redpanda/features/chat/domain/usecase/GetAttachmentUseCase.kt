package com.etozhesandy.redpanda.features.chat.domain.usecase

import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.features.chat.domain.repository.ChatRepository
import javax.inject.Inject

/** Finds one attachment by its id — for a viewer opened on something the dialog's media list does not hold. */
class GetAttachmentUseCase @Inject constructor(
    private val repository: ChatRepository,
) {
    suspend operator fun invoke(attachmentId: String): Attachment? = repository.getAttachment(attachmentId)
}
