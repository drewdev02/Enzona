package org.andrew.enzona

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.andrew.enzona.di.KoinHelper
import org.andrew.enzona.feature.auth.presentation.AuthUiState
import org.andrew.enzona.feature.auth.presentation.AuthViewModel
import org.andrew.enzona.ui.nav.AppNavigation
import org.andrew.enzona.ui.designsystem.EnzonaTheme
import org.andrew.enzona.ui.LoginScreen

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel get() = KoinHelper.authViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        authViewModel.restore()
        handleRedirect(intent)

        setContent { EnzonaTheme { AppRoot(authViewModel, ::openLogin) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleRedirect(intent)
    }

    /** Abre el portal OAuth en Custom Tabs (navegador del sistema). */
    private fun openLogin() {
        val url = authViewModel.buildAuthorizeUrl()
        CustomTabsIntent.Builder().build().launchUrl(this, Uri.parse(url))
    }

    /** Captura el redirect http://apk-callback?code=... y entrega el code al ViewModel. */
    private fun handleRedirect(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "http" && data.host == "apk-callback") {
            val code = data.getQueryParameter("code")
            if (code != null) authViewModel.onRedirectCode(code)
            else authViewModel.onAuthFailed(
                data.getQueryParameter("oauthErrorMsg") ?: "Login cancelado"
            )
        }
    }
}

@Composable
private fun AppRoot(vm: AuthViewModel, onLogin: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    when (state) {
        is AuthUiState.LoggedIn -> AppNavigation(onLogout = vm::logout)
        else -> LoginScreen(
            state = state,
            onLogin = onLogin,
        )
    }
}
