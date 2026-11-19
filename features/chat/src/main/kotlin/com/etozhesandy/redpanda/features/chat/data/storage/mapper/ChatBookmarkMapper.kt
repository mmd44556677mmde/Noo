package com.etozhesandy.redpanda.features.chat.data.storage.mapper

import com.etozhesandy.redpanda.core.storage.db.chatbookmark.ChatBookmarkEntity
import com.etozhesandy.redpanda.features.chat.domain.model.BookmarkTab

/**
 * The tab goes into the table under a key spelled out here rather than the enum's `name`, so
 * renaming a constant can never orphan the bookmarks already saved under the old spelling. The
 * keys are the ones bookmarks have always been saved under.
 */
fun BookmarkTab.toStorageKey(): String = when (this) {
    BookmarkTab.MESSAGES -> "MESSAGES"
    BookmarkTab.PHOTOS -> "PHOTOS"
    BookmarkTab.VIDEOS -> "VIDEOS"
}

fun toChatBookmarkEntity(dialogId: String, tab: BookmarkTab, itemId: String): ChatBookmarkEntity =
    ChatBookmarkEntity(dialogId = dialogId, tab = tab.toStorageKey(), itemId = itemId)
