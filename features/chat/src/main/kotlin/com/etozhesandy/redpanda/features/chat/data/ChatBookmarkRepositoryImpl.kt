package com.etozhesandy.redpanda.features.chat.data

import com.etozhesandy.redpanda.core.storage.db.chatbookmark.ChatBookmarkDao
import com.etozhesandy.redpanda.features.chat.data.storage.mapper.toChatBookmarkEntity
import com.etozhesandy.redpanda.features.chat.data.storage.mapper.toStorageKey
import com.etozhesandy.redpanda.features.chat.domain.model.BookmarkTab
import com.etozhesandy.redpanda.features.chat.domain.repository.ChatBookmarkRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ChatBookmarkRepositoryImpl @Inject constructor(
    private val dao: ChatBookmarkDao,
) : ChatBookmarkRepository {

    override fun observe(dialogId: String, tab: BookmarkTab): Flow<String?> = dao.observe(dialogId, tab.toStorageKey())

    override suspend fun save(dialogId: String, tab: BookmarkTab, itemId: String) =
        dao.upsert(toChatBookmarkEntity(dialogId, tab, itemId))
}
