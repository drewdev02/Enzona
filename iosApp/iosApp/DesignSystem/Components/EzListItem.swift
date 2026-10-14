import SwiftUI

/// Fila de lista: leading · textos · trailing. Sirve para movimientos, tarjetas y ajustes.
/// `overline` es una etiqueta pequeña sobre el título (útil en filas "campo: valor").
struct EzListItem<Leading: View, Trailing: View>: View {
    var overline: String?
    let title: String
    var subtitle: String?
    let leading: Leading
    let trailing: Trailing

    init(
        overline: String? = nil, title: String, subtitle: String? = nil,
        @ViewBuilder leading: () -> Leading, @ViewBuilder trailing: () -> Trailing
    ) {
        self.overline = overline
        self.title = title
        self.subtitle = subtitle
        self.leading = leading()
        self.trailing = trailing()
    }

    var body: some View {
        HStack(spacing: Ez.Space.md) {
            leading
            VStack(alignment: .leading, spacing: 0) {
                if let overline {
                    Text(overline).font(Ez.Typography.labelMedium).foregroundStyle(Ez.Palette.onSurfaceVariant)
                }
                Text(title).font(Ez.Typography.bodyLarge).foregroundStyle(Ez.Palette.onSurface)
                if let subtitle {
                    Text(subtitle).font(Ez.Typography.bodyMedium).foregroundStyle(Ez.Palette.onSurfaceVariant)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            trailing
        }
        .padding(.horizontal, Ez.Space.lg)
        .padding(.vertical, Ez.Space.md)
        .frame(minHeight: Ez.Size.listItemMinHeight)
        .accessibilityElement(children: .combine)
    }
}

extension EzListItem where Leading == EmptyView, Trailing == EmptyView {
    init(overline: String? = nil, title: String, subtitle: String? = nil) {
        self.init(overline: overline, title: title, subtitle: subtitle,
                  leading: { EmptyView() }, trailing: { EmptyView() })
    }
}

extension EzListItem where Trailing == EmptyView {
    init(overline: String? = nil, title: String, subtitle: String? = nil, @ViewBuilder leading: () -> Leading) {
        self.init(overline: overline, title: title, subtitle: subtitle, leading: leading, trailing: { EmptyView() })
    }
}
