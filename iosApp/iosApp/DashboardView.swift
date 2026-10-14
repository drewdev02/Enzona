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

    private let actionsHeight: CGFloat = 172
    private let badgeSize: CGFloat = 80

    var body: some View {
        ZStack(alignment: .top) {
            LinearGradient(colors: [Brand.blueLight, Brand.navy], startPoint: .leading, endPoint: .trailing)
                .ignoresSafeArea()

            VStack(spacing: 0) {
                HStack {
                    Spacer()
                    action("Escanear", symbol: "qrcode.viewfinder", handler: onScan)
                    Spacer()
                    action("Recibir", symbol: "qrcode", handler: onReceive)
                    Spacer()
                }
                .frame(height: actionsHeight)
                .padding(.top, 12)

                ZStack(alignment: .top) {
                    UnevenRoundedRectangle(topLeadingRadius: 36, topTrailingRadius: 36)
                        .fill(Color(.systemBackground))
                        .ignoresSafeArea(edges: .bottom)
                    sheetContent
                        .padding(.top, badgeSize / 2 + 8)
                        .padding(.horizontal, 24)
                    BrandBadge(diameter: badgeSize)
                        .offset(y: -badgeSize / 2)
                }
            }
        }
    }

    private func action(_ title: String, symbol: String, handler: @escaping () -> Void) -> some View {
        Button(action: handler) {
            VStack(spacing: 8) {
                Image(systemName: symbol).font(.system(size: 64, weight: .light))
                Text(title).font(.headline)
            }
            .foregroundStyle(.white)
        }
        .buttonStyle(.plain)
    }

    @ViewBuilder
    private var sheetContent: some View {
        switch content {
        case .loading:
            ProgressView().tint(Brand.blue).frame(maxHeight: .infinity)
        case .greeting(let text):
            VStack(alignment: .leading, spacing: 8) {
                Text(text).font(.title2).bold()
                Text("Tus tarjetas y movimientos aparecerán aquí.").foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        case .error(let message):
            VStack(spacing: 16) {
                Text(message).foregroundStyle(.red).multilineTextAlignment(.center)
                Button("Reintentar", action: onRetry).buttonStyle(.borderedProminent)
            }
            .frame(maxHeight: .infinity)
        }
    }
}

#Preview("Cargando") { DashboardView(content: .loading) }
#Preview("Con contenido") { DashboardView(content: .greeting("Hola, Test User")) }
#Preview("Error") { DashboardView(content: .error("Sin conexión. Revisa tu red e inténtalo de nuevo.")) }
