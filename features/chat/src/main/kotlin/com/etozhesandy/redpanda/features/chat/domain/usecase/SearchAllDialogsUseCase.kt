package com.etozhesandy.redpanda.features.chat.domain.usecase

import com.etozhesandy.redpanda.core.model.DialogMessage
import com.etozhesandy.redpanda.features.chat.domain.repository.ChatRepository
import com.etozhesandy.redpanda.features.chat.domain.model.SearchResultPage
import com.etozhesandy.redpanda.features.chat.utils.asPrefixQuery
import com.etozhesandy.redpanda.features.chat.utils.asRelaxedPrefixQuery
import com.etozhesandy.redpanda.features.chat.utils.searchTermCount
import javax.inject.Inject

/**
 * Full-text searches the messages of every dialog of one profile, each result carrying the name of
 * the dialog it belongs to. The query is read exactly as in the single-dialog search: stems of its
 * words as prefixes, and some of the words instead of all when no message holds them all.
 */
class SearchAllDialogsUseCase @Inject constructor(
    private val repository: ChatRepository,
) {
    suspend operator fun invoke(
        profileId: String,
        rawQuery: String,
        limit: Int,
        offset: Int,
        relaxed: Boolean = false,
    ): SearchResultPage<DialogMessage> {
        val canRelax = rawQuery.searchTermCount() > 1
        if (!relaxed || !canRelax) {
            val ftsQuery = rawQuery.asPrefixQuery()
            if (ftsQuery.isBlank()) return SearchResultPage(emptyList())
            val page = repository.searchAllDialogs(profileId, ftsQuery, limit, offset)
            if (page.isNotEmpty() || offset > 0 || !canRelax) return SearchResultPage(page)
        }
        val page = repository.searchAllDialogs(profileId, rawQuery.asRelaxedPrefixQuery(), limit, offset)
        return SearchResultPage(page, isRelaxed = true)
    }
}
