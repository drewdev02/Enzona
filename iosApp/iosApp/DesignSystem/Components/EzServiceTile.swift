import SwiftUI

/// Datos de una casilla. Modelo propio de la UI (sin depender del framework Kotlin) para que
/// las vistas se puedan previsualizar.
struct EzServiceItem: Identifiable, Hashable {
    let id: String
    let title: String
    /// Nombre del imageset en el catálogo de assets.
    let imageName: String
}

/// Casilla de servicio: icono grande con la etiqueta debajo. El título es el nombre accesible
/// de todo el botón (el icono es decorativo).
struct EzServiceTile: View {
    let item: EzServiceItem
    let action: () -> Void
    @Environment(\.dynamicTypeSize) private var typeSize

    var body: some View {
        Button(action: action) {
            VStack(spacing: 0) {
                Image(item.imageName)
                    .resizable()
                    .scaledToFit()
                    .frame(width: Ez.Size.serviceIcon, height: Ez.Size.serviceIcon)
                    .accessibilityHidden(true)
                Text(item.title)
                    .font(Ez.Typography.tileLabel)
                    .foregroundStyle(Ez.Palette.onSurface)
                    // Tamaños normales: una línea que se encoge un poco, para no partir palabras
                    // ("Organizaciones"). Tamaños de accesibilidad: no se encoge y puede ocupar
                    // varias líneas (la cuadrícula pasa a 2 columnas, donde las palabras caben).
                    .lineLimit(typeSize.isAccessibilitySize ? 3 : 1)
                    .minimumScaleFactor(typeSize.isAccessibilitySize ? 1 : 0.85)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)
            }
            .padding(.vertical, Ez.Space.xs)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

/// Cuadrícula de casillas (4 columnas por defecto). Se desplaza si no cabe.
struct EzServiceGrid: View {
    let items: [EzServiceItem]
    var columns = 4
    let onTap: (EzServiceItem) -> Void
    @Environment(\.dynamicTypeSize) private var typeSize

    var body: some View {
        // Con texto de accesibilidad, 4 columnas no caben: pasa a 2. `alignment: .top` mantiene
        // los iconos de una fila alineados aunque una etiqueta ocupe más líneas.
        let count = typeSize.isAccessibilitySize ? 2 : columns
        LazyVGrid(
            columns: Array(repeating: GridItem(.flexible(), spacing: Ez.Space.xs, alignment: .top), count: count),
            spacing: Ez.Space.md
        ) {
            ForEach(items) { item in
                EzServiceTile(item: item) { onTap(item) }
            }
        }
    }
}

extension EzServiceItem {
    /// Solo para previews: reproduce el orden y las etiquetas del inicio.
    static let samples: [EzServiceItem] = [
        ("transfer", "Transferir"), ("balance", "Balance"), ("bulevar", "Bulevar"), ("gift", "Regalo"),
        ("donate", "Donar"), ("electricity", "Electricidad"), ("etecsa", "Etecsa"), ("taxes", "Impuestos"),
        ("gas", "Gas"), ("mail", "Correos"), ("extra_cash", "Caja extra"), ("purchases", "Mis compras"),
        ("create_account", "Crear cuenta"), ("organizations", "Organizaciones"),
    ].map { EzServiceItem(id: $0.0, title: $0.1, imageName: "service_\($0.0)") }
}
