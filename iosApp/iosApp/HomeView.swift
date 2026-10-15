import SwiftUI
import SharedLogic

/// Segundo nivel (no son pestañas): pantallas que se abren desde el inicio.
enum HomeRoute: Hashable {
    case balance
    /// Tercer nivel: saldo y operaciones de una tarjeta. `label` es la fila "Tarjeta" ("BANMET 6856").
    case cardDetail(id: String, label: String)
}

/// Raíz de la zona autenticada: conecta los ViewModels y el modelo compartido con las pantallas.
struct HomeView: View {
    let onLogout: () -> Void
    @StateObject private var profile = ProfileModel()
    @StateObject private var cards = CardsModel()
    @State private var path: [HomeRoute] = []
    @State private var comingSoon: String?

    /// Orden y etiquetas vienen del enum compartido `HomeService` (una sola fuente).
    private let services: [EzServiceItem] = HomeServices.shared.all.map {
        EzServiceItem(id: $0.iconKey, title: $0.label, imageName: $0.iconKey)
    }

    var body: some View {
        MainTabView(
            home: {
                NavigationStack(path: $path) {
                    // Escanear / Recibir llegan con el hito de QR (M5).
                    DashboardView(services: services, onService: open)
                        // El inicio dibuja su propia cabecera; `toolbarColorScheme` además pone la
                        // barra de estado en blanco sobre el degradado.
                        .toolbar(.hidden, for: .navigationBar)
                        .toolbarColorScheme(.dark, for: .navigationBar)
                        .navigationDestination(for: HomeRoute.self) { route in
                            switch route {
                            case .balance:
                                BalanceView(
                                    cards: cards,
                                    onCardMenu: {
                                        // Eliminar / predeterminada / PIN / operaciones: llegan en su hito.
                                        comingSoon = "Las acciones de la tarjeta estarán disponibles próximamente."
                                    },
                                    onCardTap: { card in
                                        path.append(.cardDetail(id: card.id, label: card.detailLabel))
                                    }
                                )
                            case .cardDetail(let id, let label):
                                CardDetailView(cardId: id, cardLabel: label)
                            }
                        }
                }
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
            Text(comingSoon ?? "")
        }
    }

    /// La navegación de cada servicio se añade con su hito; hasta entonces avisa.
    private func open(_ item: EzServiceItem) {
        let service = HomeServices.shared.all.first { $0.iconKey == item.id }
        if service?.isAvailable == true {
            if service?.name == "BALANCE" { path.append(.balance) }
        } else {
            comingSoon = "\(item.title) estará disponible próximamente."
        }
    }
}
