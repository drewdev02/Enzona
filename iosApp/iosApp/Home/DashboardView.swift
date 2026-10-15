import SwiftUI

/// Pantalla de inicio. Sin estado ni dependencias del framework Kotlin: por eso se puede previsualizar.
struct DashboardView: View {
    let services: [EzServiceItem]
    var onScan: () -> Void = {}
    var onReceive: () -> Void = {}
    var onService: (EzServiceItem) -> Void = { _ in }

    var body: some View {
        ZStack(alignment: .top) {
            LinearGradient(
                colors: [Ez.Palette.brandBlueLight, Ez.Palette.brandNavy],
                startPoint: .leading, endPoint: .trailing
            )
            .ignoresSafeArea()

            VStack(spacing: 0) {
                HStack {
                    Spacer()
                    action("Escanear", symbol: "qrcode.viewfinder", handler: onScan)
                    Spacer()
                    action("Recibir", symbol: "qrcode", handler: onReceive)
                    Spacer()
                }
                .frame(height: Ez.Size.heroActionsHeight)
                .padding(.top, Ez.Space.md)

                ZStack(alignment: .top) {
                    UnevenRoundedRectangle(topLeadingRadius: Ez.Radius.sheet, topTrailingRadius: Ez.Radius.sheet)
                        .fill(Ez.Palette.surface)
                        .ignoresSafeArea(edges: .bottom)
                    ScrollView {
                        EzServiceGrid(items: services, onTap: onService)
                            .padding(.horizontal, Ez.Space.lg)
                            .padding(.top, Ez.Size.brandBadge / 2 + Ez.Space.xl)
                            .padding(.bottom, Ez.Space.lg)
                    }
                    BrandBadge()
                        .offset(y: -Ez.Size.brandBadge / 2)
                }
            }
        }
    }

    private func action(_ title: String, symbol: String, handler: @escaping () -> Void) -> some View {
        Button(action: handler) {
            VStack(spacing: Ez.Space.sm) {
                // Marco fijo: los dos símbolos miden distinto y sus etiquetas quedarían desalineadas.
                Image(systemName: symbol)
                    .font(.system(size: Ez.Size.heroGlyph, weight: .light))
                    .frame(width: Ez.Size.heroGlyph + Ez.Space.md, height: Ez.Size.heroGlyph + Ez.Space.md)
                Text(title).font(Ez.Typography.titleMedium)
            }
            .foregroundStyle(.white)
            // La cabecera tiene altura fija: con texto enorme la etiqueta chocaría con la insignia.
            .dynamicTypeSize(...DynamicTypeSize.xxxLarge)
        }
        .buttonStyle(.plain)
    }
}

#Preview("Inicio") { DashboardView(services: EzServiceItem.samples) }
#Preview("Inicio (oscuro)") { DashboardView(services: EzServiceItem.samples).preferredColorScheme(.dark) }
#Preview("Inicio (texto grande)") {
    DashboardView(services: EzServiceItem.samples).dynamicTypeSize(.accessibility2)
}
