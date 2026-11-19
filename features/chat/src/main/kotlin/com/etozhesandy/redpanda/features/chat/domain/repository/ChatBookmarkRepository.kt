package com.etozhesandy.redpanda.features.chat.domain.repository

import com.etozhesandy.redpanda.features.chat.domain.model.BookmarkTab
import kotlinx.coroutines.flow.Flow

interface ChatBookmarkRepository {
    /** The id of the message or attachment bookmarked in [tab], or null when nothing is saved. */
    fun observe(dialogId: String, tab: BookmarkTab): Flow<String?>

    suspend fun save(dialogId: String, tab: BookmarkTab, itemId: String)
}
