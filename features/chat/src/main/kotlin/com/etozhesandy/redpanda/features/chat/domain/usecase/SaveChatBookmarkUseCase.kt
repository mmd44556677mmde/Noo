package com.etozhesandy.redpanda.features.chat.domain.usecase

import com.etozhesandy.redpanda.features.chat.domain.model.BookmarkTab
import com.etozhesandy.redpanda.features.chat.domain.repository.ChatBookmarkRepository
import javax.inject.Inject

/** Remembers an item as the reader's place in one tab of a dialog, replacing the previous bookmark. */
class SaveChatBookmarkUseCase @Inject constructor(
    private val repository: ChatBookmarkRepository,
) {
    suspend operator fun invoke(dialogId: String, tab: BookmarkTab, itemId: String) =
        repository.save(dialogId, tab, itemId)
}
