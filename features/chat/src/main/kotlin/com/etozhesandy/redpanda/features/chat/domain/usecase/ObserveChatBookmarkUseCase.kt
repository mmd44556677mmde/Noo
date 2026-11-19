package com.etozhesandy.redpanda.features.chat.domain.usecase

import com.etozhesandy.redpanda.features.chat.domain.model.BookmarkTab
import com.etozhesandy.redpanda.features.chat.domain.repository.ChatBookmarkRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Streams the id of the item the reader bookmarked in one tab of a dialog, or null if none. */
class ObserveChatBookmarkUseCase @Inject constructor(
    private val repository: ChatBookmarkRepository,
) {
    operator fun invoke(dialogId: String, tab: BookmarkTab): Flow<String?> = repository.observe(dialogId, tab)
}
