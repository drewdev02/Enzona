package org.andrew.enzona.ui.transfer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.andrew.enzona.feature.transfer.domain.InvalidQuery
import org.andrew.enzona.feature.transfer.domain.TransferRecipient
import org.andrew.enzona.feature.transfer.presentation.FindRecipientUiState
import org.andrew.enzona.feature.transfer.presentation.FindRecipientViewModel
import org.andrew.enzona.ui.designsystem.EnzonaTheme
import org.andrew.enzona.ui.designsystem.components.EzAvatar
import org.andrew.enzona.ui.designsystem.components.EzButton
import org.andrew.enzona.ui.designsystem.components.EzListItem
import org.andrew.enzona.ui.designsystem.components.EzTextField
import org.andrew.enzona.ui.designsystem.components.EzGradientTopAppBar
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FindAccountRoute(
    onBack: () -> Unit,
    onRecipient: (TransferRecipient) -> Unit,
    vm: FindRecipientViewModel = koinViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    FindAccountScreen(state, onBack, onSearch = vm::search, onEdit = vm::reset, onRecipient = onRecipient)
}

internal fun InvalidQuery.message(): String = when (this) {
    InvalidQuery.EMPTY -> "Escribe el nombre de usuario de la cuenta."
    InvalidQuery.HAS_WHITESPACE -> "El nombre de usuario no lleva espacios."
    InvalidQuery.YOURSELF -> "Esa es tu cuenta. Para mover dinero entre tus tarjetas usa \"A una de mis tarjetas\"."
}

/** "Buscar una cuenta": solo una cuenta que el servidor devolvió permite seguir. */
@Composable
fun FindAccountScreen(
    state: FindRecipientUiState,
    onBack: () -> Unit,
    onSearch: (String) -> Unit,
    onEdit: () -> Unit,
    onRecipient: (TransferRecipient) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val spacing = EnzonaTheme.spacing
    val searching = state is FindRecipientUiState.Searching
    Scaffold(
        topBar = { EzGradientTopAppBar("Buscar una cuenta", onBack) },
        containerColor = EnzonaTheme.colors.background,
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            EzTextField(
                value = query,
                onValueChange = { query = it; if (state !is FindRecipientUiState.Idle) onEdit() },
                label = "Nombre de usuario",
                imeAction = ImeAction.Search,
                error = (state as? FindRecipientUiState.Invalid)?.reason?.message(),
            )
            EzButton("Buscar", onClick = { onSearch(query) }, loading = searching, enabled = !searching)
            when (state) {
                is FindRecipientUiState.Found -> {
                    EzListItem(
                        title = state.recipient.displayName,
                        subtitle = state.recipient.username,
                        leading = { EzAvatar(initialsOf(state.recipient.displayName)) },
                        onClick = { onRecipient(state.recipient) },
                    )
                }
                is FindRecipientUiState.NotFound -> Notice("No encontramos una cuenta con ese nombre de usuario.")
                is FindRecipientUiState.Error -> Notice(state.message)
                else -> Unit
            }
        }
    }
}

@Composable
private fun Notice(text: String) {
    Text(text, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, color = EnzonaTheme.colors.onSurfaceVariant)
}

@Preview(name = "Buscar: encontrada", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun FindAccountFoundPreview() = EnzonaTheme(darkTheme = false) {
    FindAccountScreen(FindRecipientUiState.Found(TransferPreviewData.recipient), {}, {}, {}, {})
}

@Preview(name = "Buscar: no existe", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun FindAccountNotFoundPreview() = EnzonaTheme(darkTheme = false) {
    FindAccountScreen(FindRecipientUiState.NotFound, {}, {}, {}, {})
}
