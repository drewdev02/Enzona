import SwiftUI

/// Pestañas nativas (`TabView`): en iOS 26+ el sistema las dibuja como la cápsula flotante.
/// Genérica en sus pestañas con contenido real para poder previsualizar el conjunto sin Koin.
struct MainTabView<Home: View, Profile: View>: View {
    @ViewBuilder var home: () -> Home
    @ViewBuilder var profile: () -> Profile

    var body: some View {
        TabView {
            Tab("Inicio", systemImage: "house") { home() }
            Tab("Avisos", systemImage: "bell") {
                EzEmptyState(title: "Avisos", message: "Las notificaciones llegarán más adelante.", systemImage: "bell")
            }
            Tab("Amigos", systemImage: "person.2") {
                EzEmptyState(title: "Amigos", message: "Contactos y destinatarios llegarán más adelante.", systemImage: "person.2")
            }
            Tab("Perfil", systemImage: "person") { profile() }
        }
        .tint(Ez.Palette.primary)
    }
}

#Preview("Conjunto") {
    MainTabView(
        home: { DashboardView(content: .greeting("Hola, Test User")) },
        profile: { EzEmptyState(title: "Perfil", message: "(vista previa)", systemImage: "person") }
    )
}
