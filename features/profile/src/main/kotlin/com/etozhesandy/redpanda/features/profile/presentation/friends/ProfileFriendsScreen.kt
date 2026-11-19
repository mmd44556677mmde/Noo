package com.etozhesandy.redpanda.features.profile.presentation.friends

import com.etozhesandy.redpanda.features.profile.presentation.friends.view.FriendRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.etozhesandy.redpanda.core.common.net.openExternally
import com.etozhesandy.redpanda.core.designsystem.components.BaseScreen
import com.etozhesandy.redpanda.core.designsystem.components.LoadableContent
import com.etozhesandy.redpanda.core.designsystem.components.SearchField
import com.etozhesandy.redpanda.features.profile.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest


@Composable
fun ProfileFriendsScreen(
    state: ProfileFriendsState.State,
    effect: Flow<ProfileFriendsState.Effect>,
    onEvent: (ProfileFriendsState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        effect.collectLatest { current ->
            when (current) {
                is ProfileFriendsState.Effect.OpenLink -> context.openExternally(current.url)
            }
        }
    }

    BaseScreen(
        title = stringResource(R.string.profile_friends),
        modifier = modifier,
        onBack = { onEvent(ProfileFriendsState.Event.BackClicked) },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SearchField(
                value = state.query,
                onValueChange = { onEvent(ProfileFriendsState.Event.QueryChanged(it)) },
                placeholder = stringResource(R.string.profile_search_by_name),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Box(modifier = Modifier.fillMaxSize()) {
                LoadableContent(
                    isLoading = state.isLoading,
                    isEmpty = state.friends.isEmpty(),
                    emptyText = stringResource(R.string.profile_empty_friends),
                ) {
                    LazyColumn {
                        items(state.friends, key = { it.id }) { friend ->
                            FriendRow(friend, onClick = { onEvent(ProfileFriendsState.Event.FriendClicked(friend)) })
                        }
                    }
                }
            }
        }
    }
}
