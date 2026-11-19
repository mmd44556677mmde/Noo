package com.etozhesandy.redpanda.features.profile.presentation.profile.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.etozhesandy.redpanda.features.profile.R

/**
 * The reader's notes on a profile, edited in place and saved as they type.
 *
 * The text belongs to the field from the first composition on: it is seeded from [notes] once and
 * never overwritten by it afterwards, because every save makes the database emit the same text
 * back, and applying that to a field the reader is still typing in would move their cursor.
 */
@Composable
fun ProfileNotesSection(
    profileId: String,
    notes: String,
    onNotesChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by rememberSaveable(profileId) { mutableStateOf(notes) }
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(stringResource(R.string.profile_notes), style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                onNotesChange(it)
            },
            placeholder = { Text(stringResource(R.string.profile_notes_hint)) },
            minLines = 3,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
    }
}
