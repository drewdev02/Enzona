import SwiftUI

/// Campo de texto con etiqueta, ayuda y error. `error` tiene prioridad sobre `helper`.
/// Con `isSecure` oculta el valor y ofrece "Mostrar/Ocultar".
struct EzTextField: View {
    let label: String
    @Binding var text: String
    var placeholder: String = ""
    var helper: String? = nil
    var error: String? = nil
    var isSecure = false
    var keyboard: UIKeyboardType = .default

    @State private var revealed = false
    @FocusState private var focused: Bool

    private var borderColor: Color {
        if error != nil { return Ez.Palette.error }
        return focused ? Ez.Palette.primary : Ez.Palette.outline
    }

    var body: some View {
        VStack(alignment: .leading, spacing: Ez.Space.xs) {
            Text(label)
                .font(Ez.Typography.labelMedium)
                .foregroundStyle(error != nil ? Ez.Palette.error : Ez.Palette.onSurfaceVariant)

            HStack {
                Group {
                    if isSecure && !revealed {
                        SecureField(placeholder, text: $text)
                    } else {
                        TextField(placeholder, text: $text).keyboardType(keyboard)
                    }
                }
                .focused($focused)
                .font(Ez.Typography.bodyLarge)
                .foregroundStyle(Ez.Palette.onSurface)
                .accessibilityLabel(label)

                if isSecure {
                    Button(revealed ? "Ocultar" : "Mostrar") { revealed.toggle() }
                        .font(Ez.Typography.labelLarge)
                        .buttonStyle(.plain)
                        .foregroundStyle(Ez.Palette.primary)
                }
            }
            .padding(.horizontal, Ez.Space.lg)
            .frame(minHeight: Ez.Size.fieldHeight)
            .background(RoundedRectangle(cornerRadius: Ez.Radius.md).fill(Ez.Palette.surface))
            .overlay(
                RoundedRectangle(cornerRadius: Ez.Radius.md)
                    .stroke(borderColor, lineWidth: (focused || error != nil) ? 2 : 1)
            )

            if let message = error ?? helper {
                Text(message)
                    .font(Ez.Typography.bodySmall)
                    .foregroundStyle(error != nil ? Ez.Palette.error : Ez.Palette.onSurfaceVariant)
            }
        }
    }
}
