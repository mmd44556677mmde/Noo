package com.etozhesandy.redpanda.features.chat.domain.usecase

import com.etozhesandy.redpanda.core.model.Message
import com.etozhesandy.redpanda.features.chat.domain.repository.ChatRepository
import com.etozhesandy.redpanda.features.chat.domain.model.SearchResultPage
import com.etozhesandy.redpanda.features.chat.utils.asPrefixQuery
import com.etozhesandy.redpanda.features.chat.utils.asRelaxedPrefixQuery
import com.etozhesandy.redpanda.features.chat.utils.searchTermCount
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Full-text searches messages; optionally limited to one dialog. Each word of the query is reduced
 * to its stem and matched as a prefix, so other forms of the word are found too.
 */
class SearchMessagesUseCase @Inject constructor(
    private val repository: ChatRepository,
) {
    /**
     * One page of matches for [rawQuery] in [dialogId], newest first; empty when the query has no
     * searchable term. [limit] and [offset] page through the same ordered result set.
     *
     * A query of several words that no message answers whole is asked again for messages holding
     * some of its words; the page then says so, and [relaxed] carries that mode to the next pages.
     */
    suspend fun searchPage(
        profileId: String,
        rawQuery: String,
        dialogId: String,
        limit: Int,
        offset: Int,
        relaxed: Boolean = false,
    ): SearchResultPage<Message> {
        val canRelax = rawQuery.searchTermCount() > 1
        if (!relaxed || !canRelax) {
            val ftsQuery = rawQuery.asPrefixQuery()
            if (ftsQuery.isBlank()) return SearchResultPage(emptyList())
            val page = repository.searchMessagesPage(profileId, ftsQuery, dialogId, limit, offset)
            if (page.isNotEmpty() || offset > 0 || !canRelax) return SearchResultPage(page)
        }
        val page = repository.searchMessagesPage(profileId, rawQuery.asRelaxedPrefixQuery(), dialogId, limit, offset)
        return SearchResultPage(page, isRelaxed = true)
    }

    operator fun invoke(
        profileId: String,
        rawQuery: String,
        dialogId: String? = null,
    ): Flow<List<Message>> {
        val ftsQuery = rawQuery.asPrefixQuery()
        if (ftsQuery.isBlank()) return flowOf(emptyList())
        return repository.searchMessages(profileId, ftsQuery, dialogId)
    }
}
