package com.etozhesandy.redpanda.features.chat.mapper

import com.etozhesandy.redpanda.features.chat.model.ChatMediaTab

/** Positions of the chat's tabs in its pager, in the order the tab row shows them. */
const val CHAT_PAGE_MESSAGES = 0
const val CHAT_PAGE_PHOTOS = 1
const val CHAT_PAGE_VIDEOS = 2

/** The pager page holding the grid of this media tab. */
fun ChatMediaTab.toChatPage(): Int = when (this) {
    ChatMediaTab.PHOTOS -> CHAT_PAGE_PHOTOS
    ChatMediaTab.VIDEOS -> CHAT_PAGE_VIDEOS
}
