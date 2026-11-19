package com.etozhesandy.redpanda.features.chat.mapper

import com.etozhesandy.redpanda.features.chat.domain.model.BookmarkTab
import com.etozhesandy.redpanda.features.chat.model.ChatMediaTab

fun ChatMediaTab.toBookmarkTab(): BookmarkTab = when (this) {
    ChatMediaTab.PHOTOS -> BookmarkTab.PHOTOS
    ChatMediaTab.VIDEOS -> BookmarkTab.VIDEOS
}
