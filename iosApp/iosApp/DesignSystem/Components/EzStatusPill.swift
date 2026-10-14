import SwiftUI

/// Pastilla de estado (p. ej. "Aceptada", "Pendiente"). El color nunca es el único portador: lleva texto.
struct EzStatusPill: View {
    let text: String
    let tone: Ez.Tone

    var body: some View {
        Text(text)
            .font(Ez.Typography.labelMedium)
            .foregroundStyle(tone.content)
            .padding(.horizontal, Ez.Space.md)
            .padding(.vertical, Ez.Space.xs)
            .background(Capsule().fill(tone.container))
    }
}
