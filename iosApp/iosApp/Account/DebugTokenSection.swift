#if DEBUG
import SwiftUI
import UIKit
import SharedLogicSwift

/// Herramienta de depuración: copia los tres tokens de la sesión en texto plano al portapapeles.
/// Solo existe en builds debug (este archivo entero queda fuera de release). Los tokens se leen
/// al pulsar el botón (nunca antes) y los logs no los ven.
struct DebugTokenSection: View {
    /// Cómo construir el texto copiado; inyectable solo para las previews.
    var plainText: () -> String = DebugTokenSection.sessionTokensPlainText
    @State private var copied = false

    var body: some View {
        Section {
            EzButton(title: copied ? "Tokens copiados" : "Copiar tokens de sesión", kind: .secondary) {
                UIPasteboard.general.string = plainText()
                copied = true
                Task {
                    try? await Task.sleep(for: .seconds(2))
                    copied = false
                }
            }
            .listRowBackground(Color.clear)
            .listRowInsets(EdgeInsets())
        } footer: {
            Text("Depuración: copia los tokens de sesión en texto plano.")
                .font(Ez.Typography.bodySmall)
                .foregroundStyle(Ez.Palette.onSurfaceVariant)
        }
    }

    /// `access_token`, `refresh_token` e `id_token` de la sesión actual, en claro.
    static func sessionTokensPlainText() -> String {
        let tokens = KoinHelper.shared.session.sessionTokensForDebug()
        return """
        access_token: \(tokens.accessToken ?? "(vacío)")
        refresh_token: \(tokens.refreshToken ?? "(vacío)")
        id_token: \(tokens.idToken ?? "(vacío)")
        """
    }
}

#Preview("Depuración: tokens") {
    List {
        DebugTokenSection(plainText: {
            "access_token: eyJhbGciOi…\nrefresh_token: eyJhbGciOi…\nid_token: eyJhbGciOi…"
        })
    }
    .scrollContentBackground(.hidden)
    .background(Ez.Palette.background)
}
#endif
