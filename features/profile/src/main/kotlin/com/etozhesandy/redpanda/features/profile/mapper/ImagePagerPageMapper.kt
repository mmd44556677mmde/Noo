package com.etozhesandy.redpanda.features.profile.mapper

import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.features.profile.presentation.imagepager.ImagePagerState

/**
 * Attachments from VK's flat gallery export carry no message of their own (and sometimes no
 * dialog either), but VK re-uses the same CDN URL for the copy that was sent inline — so a
 * twin sharing the [Attachment.path] is what the jump aims at.
 *
 * The twins are indexed once rather than searched per attachment: a profile's attachment list
 * is the whole archive's worth, and a scan inside the mapping would be quadratic over it.
 */
fun List<Attachment>.toPages(): List<ImagePagerState.Page> {
    val anchors = filter { it.messageId != null && it.dialogId.isNotBlank() && it.path.isNotBlank() }
        // Blank paths are excluded above: they are not one file, and would all collide on one key.
        .associate { it.path to ImagePagerState.MessageAnchor(it.dialogId, it.profileId, it.messageId!!) }
    return map { ImagePagerState.Page(url = it.path, anchor = anchors[it.path]) }
}
