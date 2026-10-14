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
                ComingSoonView(title: "Avisos", note: "Las notificaciones llegarán más adelante.")
            }
            Tab("Amigos", systemImage: "person.2") {
                ComingSoonView(title: "Amigos", note: "Contactos y destinatarios llegarán más adelante.")
            }
            Tab("Perfil", systemImage: "person") { profile() }
        }
        .tint(Brand.blue)
    }
}

/// Pestaña cuyo contenido aún no existe en el roadmap actual.
struct ComingSoonView: View {
    let title: String
    let note: String

    var body: some View {
        VStack(spacing: 8) {
            Text(title).font(.title2).bold()
            Text(note).foregroundStyle(.secondary).multilineTextAlignment(.center)
        }
        .padding(32)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

#Preview("Conjunto") {
    MainTabView(
        home: { DashboardView(content: .greeting("Hola, Test User")) },
        profile: { ComingSoonView(title: "Perfil", note: "(vista previa)") }
    )
}
