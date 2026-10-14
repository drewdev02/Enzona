import SwiftUI

/// Contenedor de contenido agrupado.
struct EzCard<Content: View>: View {
    var padding: CGFloat = Ez.Space.lg
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 0, content: content)
            .padding(padding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Ez.Palette.surfaceVariant, in: RoundedRectangle(cornerRadius: Ez.Radius.lg))
    }
}
