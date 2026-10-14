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
import org.andrew.enzona.feature.auth.domain.SessionRepository
import org.andrew.enzona.feature.auth.domain.SessionState
import org.andrew.enzona.feature.auth.presentation.LoginViewModel
import org.andrew.enzona.ui.nav.AppNavigation
import org.andrew.enzona.ui.designsystem.EnzonaTheme
import org.andrew.enzona.ui.LoginScreen
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    /** La sesión es de la app; la Activity solo la observa. */
    private val session: SessionRepository by inject()

    /**
     * Acotado a la Activity porque el redirect OAuth llega a `onNewIntent`, antes de que exista
     * (o aunque se recree) la pantalla de login. El PKCE pendiente vive en [SessionRepository].
     */
    private val loginViewModel: LoginViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Al recrearse (rotación) el intent original sigue ahí: su code ya se canjeó.
        if (savedInstanceState == null) handleRedirect(intent)

        setContent { EnzonaTheme { AppRoot(session, loginViewModel, ::openLogin) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleRedirect(intent)
    }

    /** Abre el portal OAuth en Custom Tabs (navegador del sistema). */
    private fun openLogin() {
        val url = loginViewModel.buildAuthorizeUrl()
        CustomTabsIntent.Builder().build().launchUrl(this, Uri.parse(url))
    }

    /** Captura el redirect http://apk-callback?code=... y entrega el code al ViewModel. */
    private fun handleRedirect(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "http" && data.host == "apk-callback") {
            val code = data.getQueryParameter("code")
            if (code != null) loginViewModel.onRedirectCode(code)
            else loginViewModel.onAuthFailed(
                data.getQueryParameter("oauthErrorMsg") ?: "Login cancelado"
            )
        }
    }
}

/**
 * Raíz: la sesión decide qué se muestra. Al pasar a LoggedOut la zona autenticada sale de la
 * composición y sus ViewModels (cuenta, tarjetas) se destruyen con ella.
 */
@Composable
private fun AppRoot(session: SessionRepository, loginVm: LoginViewModel, onLogin: () -> Unit) {
    val sessionState by session.state.collectAsStateWithLifecycle()
    val loginState by loginVm.state.collectAsStateWithLifecycle()
    when (val s = sessionState) {
        is SessionState.LoggedIn -> AppNavigation(onLogout = session::logout)
        is SessionState.LoggedOut -> LoginScreen(
            state = loginState,
            sessionExpired = s.expired,
            onLogin = onLogin,
        )
    }
}
