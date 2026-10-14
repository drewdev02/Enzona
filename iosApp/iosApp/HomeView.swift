import SwiftUI
import SharedLogic

/// Raíz de la zona autenticada: conecta el ViewModel y el modelo compartido con las pantallas.
struct HomeView: View {
    let onLogout: () -> Void
    @StateObject private var profile = ProfileModel()
    @State private var comingSoon: String?

    /// Orden y etiquetas vienen del enum compartido `HomeService` (una sola fuente).
    private let services: [EzServiceItem] = HomeServices.shared.all.map {
        EzServiceItem(id: $0.iconKey, title: $0.label, imageName: $0.iconKey)
    }

    var body: some View {
        MainTabView(
            home: {
                // Escanear / Recibir llegan con el hito de QR (M5).
                DashboardView(services: services, onService: { item in
                    // La navegación de cada servicio se añade con su hito; hasta entonces avisa.
                    comingSoon = item.title
                })
            },
            profile: {
                NavigationStack { ProfileView(profile: profile, onLogout: onLogout) }
            }
        )
        .alert(
            "Próximamente",
            isPresented: Binding(get: { comingSoon != nil }, set: { if !$0 { comingSoon = nil } })
        ) {
            Button("Entendido", role: .cancel) {}
        } message: {
            Text("\(comingSoon ?? "") estará disponible próximamente.")
        }
    }
}
