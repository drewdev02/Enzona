package org.andrew.enzona.ui.transfer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.andrew.enzona.feature.friends.domain.Friend
import org.andrew.enzona.feature.friends.presentation.FriendsUiState
import org.andrew.enzona.feature.friends.presentation.FriendsViewModel
import org.andrew.enzona.ui.designsystem.EnzonaTheme
import org.andrew.enzona.ui.designsystem.components.EzAvatar
import org.andrew.enzona.ui.designsystem.components.EzDivider
import org.andrew.enzona.ui.designsystem.components.EzEmptyState
import org.andrew.enzona.ui.designsystem.components.EzErrorState
import org.andrew.enzona.ui.designsystem.components.EzGradientTopAppBar
import org.andrew.enzona.ui.designsystem.components.EzListItem
import org.andrew.enzona.ui.designsystem.components.EzLoadingState
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TransferFriendsRoute(onBack: () -> Unit, onFriend: (Friend) -> Unit, vm: FriendsViewModel = koinViewModel()) {
    LaunchedEffect(Unit) { vm.load() }
    val state by vm.state.collectAsStateWithLifecycle()
    TransferFriendsScreen(state, onBack, onRetry = vm::load, onFriend = onFriend)
}

/** "Mis amigos": elegir a quién transferir. */
@Composable
fun TransferFriendsScreen(state: FriendsUiState, onBack: () -> Unit, onRetry: () -> Unit, onFriend: (Friend) -> Unit) {
    Scaffold(
        topBar = { EzGradientTopAppBar("Mis amigos", onBack) },
        containerColor = EnzonaTheme.colors.background,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (state) {
                is FriendsUiState.Loading -> EzLoadingState()
                is FriendsUiState.Error -> EzErrorState(state.message, onRetry)
                is FriendsUiState.Empty -> EzEmptyState("Aún no tienes amigos", message = "Cuando agregues amigos aparecerán aquí.")
                is FriendsUiState.Content -> LazyColumn(Modifier.fillMaxSize()) {
                    items(state.friends, key = { it.uuid }) { friend ->
                        EzListItem(
                            title = friend.displayName,
                            subtitle = friend.username.takeIf { friend.name != null },
                            leading = { EzAvatar(initialsOf(friend.displayName)) },
                            onClick = { onFriend(friend) },
                        )
                        EzDivider()
                    }
                }
            }
        }
    }
}

@Preview(name = "Amigos", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun TransferFriendsPreview() = EnzonaTheme(darkTheme = false) {
    TransferFriendsScreen(FriendsUiState.Content(TransferPreviewData.friends), {}, {}, {})
}
