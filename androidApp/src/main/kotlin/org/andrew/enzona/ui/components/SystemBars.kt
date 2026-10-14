package org.andrew.enzona.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Iconos BLANCOS en la barra de estado mientras esta pantalla esté visible (para pantallas con
 * cabecera oscura). Al salir restaura el valor anterior: otras pantallas tienen barra clara.
 */
@Composable
fun WhiteStatusBarIcons() {
    val view = LocalView.current
    if (view.isInEditMode) return // en @Preview no hay Activity
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previous = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false
        onDispose { if (previous != null) controller.isAppearanceLightStatusBars = previous }
    }
}
