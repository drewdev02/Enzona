import SwiftUI

/// Los tres estados de cualquier pantalla con datos remotos: cargando, error y vacío.
struct EzLoadingState: View {
    var body: some View {
        ProgressView().tint(Ez.Palette.primary).frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

struct EzErrorState: View {
    let message: String
    let onRetry: () -> Void

    var body: some View {
        VStack(spacing: Ez.Space.lg) {
            Text(message)
                .font(Ez.Typography.bodyLarge)
                .foregroundStyle(Ez.Palette.error)
                .multilineTextAlignment(.center)
            EzButton(title: "Reintentar", kind: .secondary, fillWidth: false, action: onRetry)
        }
        .padding(Ez.Space.xl)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

/// Estado vacío sobre el componente nativo `ContentUnavailableView`.
struct EzEmptyState: View {
    let title: String
    var message: String? = nil
    var systemImage: String = "tray"

    var body: some View {
        ContentUnavailableView(title, systemImage: systemImage, description: message.map { Text($0) })
    }
}
