import SwiftUI

/// Encabezado de sección centrado entre dos líneas ("—— Últimas operaciones ——").
struct EzLabeledDivider: View {
    let title: String

    var body: some View {
        HStack(spacing: Ez.Space.md) {
            line
            Text(title)
                .font(Ez.Typography.titleMedium)
                .foregroundStyle(Ez.Palette.onSurfaceVariant)
                // Las líneas ceden primero: el texto solo se parte si de verdad no cabe (texto muy grande).
                .layoutPriority(1)
                .multilineTextAlignment(.center)
                .accessibilityAddTraits(.isHeader)
            line
        }
        .padding(.vertical, Ez.Space.sm)
    }

    private var line: some View {
        Rectangle().fill(Ez.Palette.divider).frame(height: Ez.Size.divider)
    }
}

#Preview("Divisor con título") { EzLabeledDivider(title: "Últimas operaciones").padding() }
#Preview("Divisor (oscuro)") {
    EzLabeledDivider(title: "Últimas operaciones").padding().background(Ez.Palette.background).preferredColorScheme(.dark)
}
