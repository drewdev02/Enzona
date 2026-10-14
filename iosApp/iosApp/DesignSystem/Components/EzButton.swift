import SwiftUI

enum EzButtonKind { case primary, secondary, tertiary, destructive }

/// Botón estándar. `isLoading` muestra un indicador y bloquea los toques, pero conserva el
/// aspecto activo (con `.disabled(true)` el botón se vería "apagado" durante la carga).
struct EzButton: View {
    let title: String
    var kind: EzButtonKind = .primary
    var isLoading = false
    var fillWidth = true
    let action: () -> Void

    var body: some View {
        Button {
            if !isLoading { action() }
        } label: {
            ZStack {
                Text(title).opacity(isLoading ? 0 : 1)
                if isLoading { ProgressView() }
            }
            .frame(maxWidth: fillWidth ? .infinity : nil, minHeight: Ez.Size.buttonHeight)
            .padding(.horizontal, Ez.Space.xl)
        }
        .buttonStyle(EzButtonStyle(kind: kind))
    }
}

private struct EzButtonStyle: ButtonStyle {
    let kind: EzButtonKind
    @Environment(\.isEnabled) private var isEnabled

    private var foreground: Color {
        switch kind {
        case .primary: Ez.Palette.onPrimary
        case .secondary: Ez.Palette.onPrimaryContainer
        case .tertiary: Ez.Palette.primary
        case .destructive: Ez.Palette.onError
        }
    }

    private var background: Color {
        switch kind {
        case .primary: Ez.Palette.primary
        case .secondary: Ez.Palette.primaryContainer
        case .tertiary: .clear
        case .destructive: Ez.Palette.error
        }
    }

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(Ez.Typography.labelLarge)
            .foregroundStyle(foreground)
            .tint(foreground)
            .background(Capsule().fill(background))
            .contentShape(Capsule())
            .opacity(isEnabled ? (configuration.isPressed ? Ez.Opacity.pressed : 1) : Ez.Opacity.disabled)
    }
}
