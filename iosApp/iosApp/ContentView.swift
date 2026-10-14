import SwiftUI
import SharedLogic

struct ContentView: View {
    @StateObject private var auth = AuthModel()

    var body: some View {
        Group {
            switch auth.phase {
            case .loggedIn:
                HomeView(onLogout: { auth.logout() })
            default:
                LoginView(auth: auth)
            }
        }
        .sheet(isPresented: $auth.showSheet) {
            if let url = auth.authorizeURL {
                AuthSheet(
                    url: url,
                    onCode: { auth.onCode($0) },
                    onError: { auth.onError($0) },
                    onCancel: { auth.onError("Login cancelado") }
                )
            }
        }
    }
}

struct LoginView: View {
    @ObservedObject var auth: AuthModel

    var body: some View {
        LoginContent(
            isLoading: { if case .authenticating = auth.phase { return true } else { return false } }(),
            errorMessage: { if case .error(let message) = auth.phase { return message } else { return nil } }(),
            onLogin: { auth.login() }
        )
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
