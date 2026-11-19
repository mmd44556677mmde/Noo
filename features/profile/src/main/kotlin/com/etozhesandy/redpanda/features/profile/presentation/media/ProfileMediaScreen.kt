package com.etozhesandy.redpanda.features.profile.presentation.media

import com.etozhesandy.redpanda.features.profile.presentation.media.view.FolderRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.etozhesandy.redpanda.core.designsystem.components.BaseScreen
import com.etozhesandy.redpanda.core.designsystem.components.LoadableContent
import com.etozhesandy.redpanda.features.profile.R


@Composable
fun ProfileMediaScreen(
    state: ProfileMediaState.State,
    onEvent: (ProfileMediaState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    BaseScreen(
        title = stringResource(R.string.profile_archive_files),
        modifier = modifier,
        onBack = { onEvent(ProfileMediaState.Event.BackClicked) },
    ) {
        LoadableContent(
            isLoading = state.isLoading,
            isEmpty = state.folders.isEmpty(),
            emptyText = stringResource(R.string.profile_empty_archive_files),
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.folders, key = { it.path }) { folder ->
                    FolderRow(
                        folder = folder,
                        onClick = { onEvent(ProfileMediaState.Event.FolderClicked(folder.path)) },
                    )
                }
            }
        }
    }
}
