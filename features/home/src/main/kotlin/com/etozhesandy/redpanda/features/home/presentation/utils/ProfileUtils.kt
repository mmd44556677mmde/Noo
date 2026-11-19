package com.etozhesandy.redpanda.features.home.presentation.utils

import com.etozhesandy.redpanda.core.model.SourceType

/** The source name is a brand, so it is the same in every language. */
fun sourceLabel(sourceType: SourceType): String = when (sourceType) {
    SourceType.VK -> "VK"
}
