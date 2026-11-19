package com.etozhesandy.redpanda.features.chat.domain.model

/**
 * One page of search results.
 *
 * [isRelaxed] tells that nothing held every word of the query, so these are the messages that hold
 * some of them — the screen says so, and asks the next pages in the same mode.
 */
data class SearchResultPage<T>(val items: List<T>, val isRelaxed: Boolean = false)
