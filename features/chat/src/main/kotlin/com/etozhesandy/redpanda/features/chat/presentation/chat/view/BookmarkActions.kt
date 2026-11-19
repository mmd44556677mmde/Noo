package com.etozhesandy.redpanda.features.chat.presentation.chat.view

import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.etozhesandy.redpanda.features.chat.R

/**
 * The two buttons every bookmarkable tab draws in its [TabActionsRow]: save the place the reader
 * is at, and jump back to the saved one. The jump is disabled until something has been saved.
 */
@Composable
fun BookmarkActions(
    hasBookmark: Boolean,
    onSave: () -> Unit,
    onGoTo: () -> Unit,
) {
    val context = LocalContext.current
    val savedText = stringResource(R.string.chat_bookmark_saved)
    IconButton(
        onClick = {
            onSave()
            Toast.makeText(context, savedText, Toast.LENGTH_SHORT).show()
        },
    ) {
        Icon(Icons.Default.BookmarkAdd, contentDescription = stringResource(R.string.chat_action_save_bookmark))
    }
    IconButton(onClick = onGoTo, enabled = hasBookmark) {
        Icon(Icons.Default.Bookmark, contentDescription = stringResource(R.string.chat_action_go_to_bookmark))
    }
}
