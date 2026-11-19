package com.etozhesandy.redpanda.core.storage.db.chatbookmark

import androidx.room.Entity

/**
 * The place the reader left in one tab of one dialog, so they can come back to it in a tap.
 *
 * [itemId] is the id of a message or an attachment rather than a list index: an index shifts with
 * the sort order and with whatever has been paged in, an id keeps pointing at the same thing.
 * [tab] is the name of the tab the bookmark belongs to; there is one bookmark per tab per dialog.
 */
@Entity(tableName = "chat_bookmarks", primaryKeys = ["dialogId", "tab"])
data class ChatBookmarkEntity(
    val dialogId: String,
    val tab: String,
    val itemId: String,
)
