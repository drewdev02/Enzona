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
        VStack(spacing: 12) {
            Text("EnZona").font(.largeTitle).bold()
            Text("Inicia sesión con tu cuenta")
                .font(.subheadline).foregroundColor(.secondary)
                .padding(.bottom, 16)

            switch auth.phase {
            case .authenticating:
                ProgressView()
            case .error(let message):
                Button("Iniciar sesión") { auth.login() }
                Text(message).font(.footnote).foregroundColor(.red)
                    .multilineTextAlignment(.center).padding(.top, 8)
            default:
                Button("Iniciar sesión") { auth.login() }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding()
    }
}
