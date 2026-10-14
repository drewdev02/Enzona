import SwiftUI
import SharedLogic

struct ProfileView: View {
    @ObservedObject var profile: ProfileModel

    var body: some View {
        Group {
            switch profile.phase {
            case .loading:
                ProgressView()
            case .error(let message):
                VStack(spacing: 16) {
                    Text(message).foregroundColor(.red).multilineTextAlignment(.center)
                    Button("Reintentar") { profile.load() }.buttonStyle(.borderedProminent)
                }
                .padding()
            case .content(let account):
                ProfileContent(account: account)
            }
        }
        .navigationTitle("Mi perfil")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { profile.load() }
    }
}

private struct ProfileContent: View {
    let account: Account

    var body: some View {
        List {
            Section {
                VStack(spacing: 8) {
                    // Iniciales: la foto remota (avatarUrl) llega con una carga de imágenes.
                    Text(account.initials)
                        .font(.largeTitle)
                        .frame(width: 88, height: 88)
                        .background(Circle().fill(Color.accentColor.opacity(0.2)))
                    Text(account.fullName).font(.title2).bold()
                    Text("@\(account.username)").foregroundColor(.secondary)
                }
                .frame(maxWidth: .infinity)
                .listRowBackground(Color.clear)
            }
            Section {
                row("Correo", account.email)
                row("Teléfono", account.phone)
                row("Moneda principal", account.primaryCurrency)
                row("Nivel de perfil", account.profileLevel.map { "\($0.intValue)" })
            }
            Section {
                row("Cuenta verificada", yesNo(account.isVerified))
                row("Teléfono verificado", yesNo(account.isPhoneVerified))
                row("Contraseña de pago", account.hasPaymentPassword ? "Configurada" : "Sin configurar")
                row("Biometría", yesNo(account.biometricEnabled))
                row("Cuenta de comercio", yesNo(account.isMerchant))
            }
        }
    }

    private func yesNo(_ value: Bool) -> String { value ? "Sí" : "No" }

    private func row(_ label: String, _ value: String?) -> some View {
        LabeledContent(label, value: value ?? "—")
    }
}
