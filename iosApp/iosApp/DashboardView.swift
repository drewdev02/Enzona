import SwiftUI

/// Pantalla de inicio. Sin estado ni dependencias del framework Kotlin: por eso se puede previsualizar.
struct DashboardView: View {
    enum Content {
        case loading
        case greeting(String)
        case error(String)
    }

    let content: Content
    var onScan: () -> Void = {}
    var onReceive: () -> Void = {}
    var onRetry: () -> Void = {}

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
                    sheetContent
                        .padding(.top, Ez.Size.brandBadge / 2 + Ez.Space.sm)
                        .padding(.horizontal, Ez.Space.xl)
                    BrandBadge()
                        .offset(y: -Ez.Size.brandBadge / 2)
                }
            }
        }
    }

    private func action(_ title: String, symbol: String, handler: @escaping () -> Void) -> some View {
        Button(action: handler) {
            VStack(spacing: Ez.Space.sm) {
                Image(systemName: symbol).font(.system(size: Ez.Size.heroGlyph, weight: .light))
                Text(title).font(Ez.Typography.titleMedium)
            }
            .foregroundStyle(.white)
        }
        .buttonStyle(.plain)
    }

    @ViewBuilder
    private var sheetContent: some View {
        switch content {
        case .loading:
            EzLoadingState()
        case .greeting(let text):
            VStack(alignment: .leading, spacing: Ez.Space.sm) {
                Text(text).font(Ez.Typography.headlineMedium).foregroundStyle(Ez.Palette.onSurface)
                Text("Tus tarjetas y movimientos aparecerán aquí.")
                    .font(Ez.Typography.bodyMedium)
                    .foregroundStyle(Ez.Palette.onSurfaceVariant)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        case .error(let message):
            EzErrorState(message: message, onRetry: onRetry)
        }
    }
}

#Preview("Cargando") { DashboardView(content: .loading) }
#Preview("Con contenido") { DashboardView(content: .greeting("Hola, Test User")) }
#Preview("Con contenido (oscuro)") {
    DashboardView(content: .greeting("Hola, Test User")).preferredColorScheme(.dark)
}
#Preview("Error") { DashboardView(content: .error("Sin conexión. Revisa tu red e inténtalo de nuevo.")) }
