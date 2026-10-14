import SwiftUI
import SharedLogic

/// Raíz de la zona autenticada: conecta el ViewModel de Kotlin con las pantallas.
struct HomeView: View {
    let onLogout: () -> Void
    @StateObject private var profile = ProfileModel()

    private var dashboardContent: DashboardView.Content {
        switch profile.phase {
        case .loading: return .loading
        case .content(let account): return .greeting("Hola, \(account.fullName)")
        case .error(let message): return .error(message)
        }
    }

    var body: some View {
        MainTabView(
            home: {
                // Escanear / Recibir llegan con el hito de QR (M5).
                DashboardView(content: dashboardContent, onRetry: { profile.load() })
            },
            profile: {
                NavigationStack { ProfileView(profile: profile, onLogout: onLogout) }
            }
        )
        .onAppear { profile.load() }
    }
}
