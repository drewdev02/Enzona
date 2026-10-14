import SwiftUI
import SharedLogic

struct HomeView: View {
    let onLogout: () -> Void
    @StateObject private var profile = ProfileModel()

    private var greeting: String {
        if case .content(let account) = profile.phase { return "Hola, \(account.fullName)" }
        return "Sesión iniciada"
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 12) {
                Text(greeting).font(.title).bold()
                Text("Tarjetas y movimientos llegan en M3 y M4")
                    .font(.subheadline).foregroundColor(.secondary)
                NavigationLink("Mi perfil") { ProfileView(profile: profile) }
                    .buttonStyle(.borderedProminent)
                    .padding(.top, 12)
                Button("Cerrar sesión", role: .destructive, action: onLogout)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .padding()
        }
        .onAppear { profile.load() }
    }
}
