package org.andrew.enzona.ui.account

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import org.andrew.enzona.di.KoinHelper
import org.andrew.enzona.feature.auth.domain.SessionTokensForDebug
import org.andrew.enzona.ui.designsystem.EnzonaTheme
import org.andrew.enzona.ui.designsystem.components.EzButton
import org.andrew.enzona.ui.designsystem.components.EzButtonStyle
import org.andrew.enzona.ui.designsystem.components.EzDivider

/**
 * Herramienta de depuración: copia los tres tokens de la sesión en texto plano al portapapeles.
 * Solo se muestra en builds `debug` (bandera `debuggable`, igual que el logging): en release
 * queda oculta y los tokens no se llegan a leer. La inyecta la ruta (que ya usa Koin), así que
 * las previews no la ven.
 */
@Composable
fun DebugTokenSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val debuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    if (!debuggable) return
    DebugTokenSectionContent(modifier, onCopy = context::copySessionTokens)
}

/** Sin Koin ni portapapeles: se puede previsualizar. */
@Composable
private fun DebugTokenSectionContent(modifier: Modifier = Modifier, onCopy: () -> Unit) {
    val spacing = EnzonaTheme.spacing
    Column(modifier.fillMaxWidth()) {
        EzDivider()
        Text(
            "Depuración: copia los tokens de sesión en texto plano.",
            Modifier.padding(start = spacing.xl, end = spacing.xl, top = spacing.md, bottom = spacing.sm),
            style = MaterialTheme.typography.bodySmall,
            color = EnzonaTheme.colors.onSurfaceVariant,
        )
        EzButton(
            "Copiar tokens de sesión",
            onCopy,
            Modifier.padding(start = spacing.xl, end = spacing.xl, bottom = spacing.xl),
            style = EzButtonStyle.Secondary,
        )
    }
}

/** Lee los tokens al pulsar (no antes) y los copia. Los logs no los ven nunca. */
private fun Context.copySessionTokens() {
    val text = sessionTokensPlainText(KoinHelper.session.sessionTokensForDebug())
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Enzona: tokens de sesión", text))
    Toast.makeText(this, "Tokens copiados al portapapeles", Toast.LENGTH_SHORT).show()
}

private fun sessionTokensPlainText(tokens: SessionTokensForDebug): String = buildString {
    appendLine("access_token: ${tokens.accessToken ?: "(vacío)"}")
    appendLine("refresh_token: ${tokens.refreshToken ?: "(vacío)"}")
    append("id_token: ${tokens.idToken ?: "(vacío)"}")
}

@Preview(name = "Depuración: tokens", showBackground = true)
@Composable
private fun DebugTokenPreview() = EnzonaTheme(darkTheme = false) {
    DebugTokenSectionContent(onCopy = {})
}
