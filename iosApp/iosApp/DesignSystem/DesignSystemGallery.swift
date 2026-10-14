import SwiftUI

/// Galería del sistema de diseño: cada token y cada componente en una pantalla. Se abre desde
/// los #Preview de abajo (Xcode). Si añades un componente, añádelo aquí.
struct DesignSystemGallery: View {
    /// Sección a la que desplazarse al abrir (p. ej. "Botones"); útil para previews de una sola sección.
    var scrollTo: String? = nil

    @State private var user = ""
    @State private var amount = "12345"
    @State private var secret = "secreto"

    var body: some View {
        ScrollViewReader { proxy in
        ScrollView {
            VStack(alignment: .leading, spacing: Ez.Space.lg) {
                section("Colores") { swatches }
                section("Tipografía") {
                    VStack(alignment: .leading) {
                        Text("displayMedium").font(Ez.Typography.displayMedium)
                        Text("headlineLarge").font(Ez.Typography.headlineLarge)
                        Text("headlineSmall").font(Ez.Typography.headlineSmall)
                        Text("titleMedium").font(Ez.Typography.titleMedium)
                        Text("bodyLarge — texto de lectura").font(Ez.Typography.bodyLarge)
                        Text("bodyMedium — texto secundario").font(Ez.Typography.bodyMedium)
                        Text("labelSmall — etiqueta").font(Ez.Typography.labelSmall)
                    }
                    .foregroundStyle(Ez.Palette.onSurface)
                }
                section("Botones") {
                    EzButton(title: "Primario") {}
                    EzButton(title: "Secundario", kind: .secondary) {}
                    EzButton(title: "Terciario", kind: .tertiary) {}
                    EzButton(title: "Destructivo", kind: .destructive) {}
                    EzButton(title: "Cargando", isLoading: true) {}
                    EzButton(title: "Deshabilitado") {}.disabled(true)
                }
                section("Campos de texto") {
                    EzTextField(label: "Usuario", text: $user, helper: "Tu nombre de usuario de EnZona")
                    EzTextField(label: "Monto", text: $amount, error: "Monto inválido", keyboard: .numberPad)
                    EzTextField(label: "Contraseña de pago", text: $secret, isSecure: true)
                }
                section("Estados y avatares") {
                    HStack {
                        EzStatusPill(text: "Aceptada", tone: .success)
                        EzStatusPill(text: "Pendiente", tone: .warning)
                        EzStatusPill(text: "Rechazada", tone: .error)
                    }
                    HStack {
                        EzStatusPill(text: "Info", tone: .info)
                        EzStatusPill(text: "Neutral", tone: .neutral)
                    }
                    HStack(spacing: Ez.Space.md) {
                        EzAvatar(initials: "TU", size: Ez.Size.avatarSm)
                        EzAvatar(initials: "TU", size: Ez.Size.avatarMd)
                        EzAvatar(initials: "TU", size: Ez.Size.avatarLg)
                    }
                }
                section("Importes") {
                    EzAmount(value: "1,400.00", currency: "CUP", large: true)
                    EzAmount(value: "350.00", currency: "CUP", tone: .positive)
                    EzAmount(value: "-90.50", currency: "CUP", tone: .negative)
                }
                section("Tarjeta") {
                    EzCard {
                        Text("Tarjeta").font(Ez.Typography.titleMedium).foregroundStyle(Ez.Palette.onSurface)
                        Text("Contenedor agrupado con esquinas lg.")
                            .font(Ez.Typography.bodyMedium).foregroundStyle(Ez.Palette.onSurfaceVariant)
                    }
                }
                EzSectionHeader(title: "Movimientos", actionText: "Ver todo", onAction: {}).id("Movimientos")
                EzListItem(title: "Transferencia a user2", subtitle: "Aceptada · 03 oct",
                           leading: { EzAvatar(initials: "U2") },
                           trailing: { EzAmount(value: "-1,400.00", currency: "CUP", tone: .negative) })
                EzDivider()
                EzListItem(title: "Pago ETECSA", subtitle: "Aceptada · 27 sep",
                           leading: { EzAvatar(initials: "ET") },
                           trailing: { EzAmount(value: "-250.00", currency: "CUP") })
            }
            .padding(.vertical, Ez.Space.lg)
        }
        .background(Ez.Palette.background)
        .onAppear { if let scrollTo { proxy.scrollTo(scrollTo, anchor: .top) } }
        }
    }

    private func section<Content: View>(_ title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: Ez.Space.sm) {
            Text(title).font(Ez.Typography.titleSmall).foregroundStyle(Ez.Palette.onSurfaceVariant)
            content()
        }
        .padding(.horizontal, Ez.Space.lg)
        .id(title)
    }

    private var swatches: some View {
        let items: [(String, Color)] = [
            ("brandBlue", Ez.Palette.brandBlue), ("brandNavy", Ez.Palette.brandNavy),
            ("primary", Ez.Palette.primary), ("primaryContainer", Ez.Palette.primaryContainer),
            ("surface", Ez.Palette.surface), ("surfaceVariant", Ez.Palette.surfaceVariant),
            ("outline", Ez.Palette.outline), ("divider", Ez.Palette.divider),
            ("success", Ez.Palette.success), ("warning", Ez.Palette.warning),
            ("error", Ez.Palette.error), ("info", Ez.Palette.info),
        ]
        return LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: Ez.Space.xs), count: 4), spacing: Ez.Space.xs) {
            ForEach(items, id: \.0) { name, color in
                VStack(alignment: .leading, spacing: Ez.Space.xxs) {
                    RoundedRectangle(cornerRadius: Ez.Radius.sm).fill(color).frame(height: 36)
                        // Borde: `surface` es blanco sobre blanco en claro y no se vería.
                        .overlay(RoundedRectangle(cornerRadius: Ez.Radius.sm).stroke(Ez.Palette.divider, lineWidth: 1))
                    Text(name).font(Ez.Typography.labelSmall).foregroundStyle(Ez.Palette.onSurfaceVariant)
                        .lineLimit(1).minimumScaleFactor(0.7)
                }
            }
        }
    }
}

#Preview("Galería (claro)") { DesignSystemGallery() }
#Preview("Galería (oscuro)") { DesignSystemGallery().preferredColorScheme(.dark) }
#Preview("Galería: botones") { DesignSystemGallery(scrollTo: "Botones") }
