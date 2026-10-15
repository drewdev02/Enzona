package org.andrew.enzona.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.andrew.enzona.ui.designsystem.EnzonaTheme
import org.andrew.enzona.ui.designsystem.components.EzEmptyState

/** Pestaña cuyo contenido aún no existe en el roadmap actual. */
@Composable
fun ComingSoonScreen(title: String, note: String) {
    EzEmptyState(title = title, message = note)
}

@Preview(showBackground = true)
@Composable
private fun ComingSoonPreview() = EnzonaTheme(darkTheme = false) {
    ComingSoonScreen("Avisos", "Las notificaciones llegarán más adelante.")
}
