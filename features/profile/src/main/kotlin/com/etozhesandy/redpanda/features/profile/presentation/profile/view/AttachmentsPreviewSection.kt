package com.etozhesandy.redpanda.features.profile.presentation.profile.view

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.etozhesandy.redpanda.core.designsystem.media.AttachmentThumbnail
import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.features.profile.R

@Composable
fun AttachmentsPreviewSection(
    attachments: List<Attachment>,
    count: Int,
    onAllClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ProfilePreviewSection(
        title = stringResource(R.string.profile_attachments),
        count = count,
        onAllClick = onAllClick,
        modifier = modifier,
    ) {
        items(attachments, key = { it.id }) { attachment ->
            AttachmentThumbnail(attachment, onAllClick, Modifier.size(80.dp))
        }
    }
}
