import SwiftUI
import SharedLogic

struct ContentView: View {
    @StateObject private var session = SessionModel()

    var body: some View {
        // La sesión decide qué se muestra. Al cerrarla, HomeView sale del árbol y sus
        // modelos (cuenta, tarjetas) se destruyen: no queda nada del usuario anterior.
        switch session.phase {
        case .loggedIn:
            HomeView(onLogout: { session.logout() })
        case .loggedOut(let expired):
            LoginView(sessionExpired: expired)
        }
    }
}

struct LoginView: View {
    let sessionExpired: Bool
    @StateObject private var login = LoginModel()

    var body: some View {
        LoginContent(
            isLoading: { if case .authenticating = login.phase { return true } else { return false } }(),
            errorMessage: notice,
            onLogin: { login.login() }
        )
        .sheet(isPresented: $login.showSheet) {
            if let url = login.authorizeURL {
                AuthSheet(
                    url: url,
                    onCode: { login.onCode($0) },
                    onError: { login.onError($0) },
                    onCancel: { login.onError("Login cancelado") }
                )
            }
        }
    }

    /// Un error del intento actual tiene prioridad sobre el aviso de sesión caducada.
    private var notice: String? {
        switch login.phase {
        case .error(let message): return message
        case .idle where sessionExpired: return "Tu sesión expiró. Inicia sesión de nuevo."
        default: return nil
        }
    }
}

/// Sin dependencias del framework Kotlin: se puede previsualizar.
struct LoginContent: View {
    let isLoading: Bool
    let errorMessage: String?
    let onLogin: () -> Void

    var body: some View {
        VStack(spacing: Ez.Space.md) {
            Text("EnZona").font(Ez.Typography.headlineLarge).foregroundStyle(Ez.Palette.onSurface)
            Text("Inicia sesión con tu cuenta")
                .font(Ez.Typography.bodyMedium)
                .foregroundStyle(Ez.Palette.onSurfaceVariant)
                .padding(.bottom, Ez.Space.lg)
            EzButton(title: "Iniciar sesión", isLoading: isLoading, action: onLogin)
            if let errorMessage {
                Text(errorMessage)
                    .font(Ez.Typography.bodySmall)
                    .foregroundStyle(Ez.Palette.error)
                    .multilineTextAlignment(.center)
            }
        }
        .padding(Ez.Space.xl)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Ez.Palette.background)
    }
}

#Preview("Login") { LoginContent(isLoading: false, errorMessage: nil, onLogin: {}) }
#Preview("Login: autenticando") { LoginContent(isLoading: true, errorMessage: nil, onLogin: {}) }
#Preview("Login: error (oscuro)") {
    LoginContent(isLoading: false, errorMessage: "Tu sesión expiró. Inicia sesión de nuevo.", onLogin: {})
        .preferredColorScheme(.dark)
}
