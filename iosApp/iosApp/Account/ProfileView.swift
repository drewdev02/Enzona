import SwiftUI
import SharedLogicSwift

struct ProfileView: View {
    @ObservedObject var profile: ProfileModel
    let onLogout: () -> Void

    var body: some View {
        Group {
            switch profile.phase {
            case .loading:
                EzLoadingState()
            case .error(let message):
                EzErrorState(message: message, onRetry: { profile.load() })
            case .content(let account):
                ProfileContent(
                    fullName: account.fullName, username: account.username, initials: account.initials,
                    rows: [
                        ("Correo", account.email ?? "—"),
                        ("Teléfono", account.phone ?? "—"),
                        ("Moneda principal", account.primaryCurrency ?? "—"),
                        ("Nivel de perfil", account.profileLevel.map { String($0) } ?? "—"),
                    ],
                    statusRows: [
                        ("Cuenta verificada", account.isVerified ? "Sí" : "No"),
                        ("Teléfono verificado", account.isPhoneVerified ? "Sí" : "No"),
                        ("Contraseña de pago", account.hasPaymentPassword ? "Configurada" : "Sin configurar"),
                        ("Biometría", account.biometricEnabled ? "Sí" : "No"),
                        ("Cuenta de comercio", account.isMerchant ? "Sí" : "No"),
                    ],
                    onLogout: onLogout,
                    debugSection: debugSection
                )
            }
        }
        .navigationTitle("Mi perfil")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { profile.load() }
    }

    /// Solo builds debug: sección para copiar los tokens de sesión (ver `DebugTokenSection.swift`).
    private var debugSection: AnyView? {
        #if DEBUG
        return AnyView(DebugTokenSection())
        #else
        return nil
        #endif
    }
}

/// Sin dependencias del framework Kotlin: se puede previsualizar.
struct ProfileContent: View {
    let fullName: String
    let username: String
    let initials: String
    let rows: [(String, String)]
    let statusRows: [(String, String)]
    let onLogout: () -> Void
    /// Solo builds debug: la inyecta `ProfileView`; las previews no la pasan.
    var debugSection: AnyView? = nil

    var body: some View {
        List {
            Section {
                VStack(spacing: Ez.Space.sm) {
                    EzAvatar(initials: initials, size: Ez.Size.avatarXl)
                    Text(fullName).font(Ez.Typography.headlineSmall).foregroundStyle(Ez.Palette.onSurface)
                    Text("@\(username)").font(Ez.Typography.bodyMedium).foregroundStyle(Ez.Palette.onSurfaceVariant)
                }
                .frame(maxWidth: .infinity)
                .listRowBackground(Color.clear)
            }
            Section { ForEach(rows, id: \.0) { EzListItem(overline: $0.0, title: $0.1) } }
            Section { ForEach(statusRows, id: \.0) { EzListItem(overline: $0.0, title: $0.1) } }
            Section {
                EzButton(title: "Cerrar sesión", kind: .secondary, action: onLogout)
                    .listRowBackground(Color.clear)
                    .listRowInsets(EdgeInsets())
            }
            debugSection
        }
        .scrollContentBackground(.hidden)
        .background(Ez.Palette.background)
    }
}

#Preview("Perfil") {
    NavigationStack {
        ProfileContent(
            fullName: "Test User", username: "user1", initials: "TU",
            rows: [("Correo", "test@example.com"), ("Teléfono", "55555555"), ("Moneda principal", "CUP"), ("Nivel de perfil", "8")],
            statusRows: [("Cuenta verificada", "Sí"), ("Contraseña de pago", "Configurada"), ("Biometría", "No")],
            onLogout: {}
        )
        .navigationTitle("Mi perfil")
        .navigationBarTitleDisplayMode(.inline)
    }
}
