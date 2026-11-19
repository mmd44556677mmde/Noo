package com.etozhesandy.redpanda.features.chat.presentation.search.view

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.etozhesandy.redpanda.features.chat.R

/** Says above the results that none of them holds every word of the query. */
@Composable
fun RelaxedSearchHint(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.search_relaxed_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
