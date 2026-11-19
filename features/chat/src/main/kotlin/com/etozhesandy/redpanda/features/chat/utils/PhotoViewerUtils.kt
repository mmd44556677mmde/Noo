package com.etozhesandy.redpanda.features.chat.utils

import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.core.model.AttachmentType

/**
 * The ids of [attachments] the viewer's jump button can act on.
 *
 * A jump from the grid lands on the attachment's message, which VK re-uses the same CDN URL for
 * when a photo also sits in the gallery: such a gallery item has no message of its own, but a
 * sibling with the same path does, so the path is what ties them together. A jump from a message
 * lands on the media tab, which only a photo or a video has.
 */
fun jumpableIdsOf(attachments: List<Attachment>, fromMessages: Boolean): Set<String> {
    // Photos and videos only: the jump lands on the media tab holding the attachment, and a
    // sticker or a drawing is in none of them.
    if (fromMessages) {
        return attachments.filter { it.type == AttachmentType.PHOTO || it.type == AttachmentType.VIDEO }
            .mapTo(HashSet()) { it.id }
    }
    val pathsWithMessage = attachments.filter { it.messageId != null }.mapTo(HashSet()) { it.path }
    return attachments.filter { it.messageId != null || it.path in pathsWithMessage }
        .mapTo(HashSet()) { it.id }
}
