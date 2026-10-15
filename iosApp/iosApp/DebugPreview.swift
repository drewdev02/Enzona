#if DEBUG
import SwiftUI

/// Solo builds DEBUG. Muestra una pantalla con datos INVENTADOS, sin Koin ni red, para revisarla en el
/// simulador sin iniciar sesión. El modo oscuro y el tamaño de letra se cambian con el simulador:
///   xcrun simctl ui booted appearance dark
///   xcrun simctl ui booted content_size accessibility-extra-large
///
///   xcrun simctl launch booted <bundle id> -ezPreview <nombre>
/// Nombres: detail (por defecto), detail-empty, detail-partial, detail-loading, detail-error, balance.
enum DebugPreview {
    static var requested: String? {
        let args = ProcessInfo.processInfo.arguments
        guard let index = args.firstIndex(of: "-ezPreview") else { return nil }
        return index + 1 < args.count ? args[index + 1] : "detail"
    }

    @ViewBuilder
    static func view(_ name: String) -> some View {
        let label = "BANMET 6856"
        NavigationStack {
            switch name {
            case "detail-empty":
                CardDetailContent(phase: .content(.noOperations), cardLabel: label).balanceNavigationChrome()
            case "detail-partial":
                CardDetailContent(phase: .content(.partial), cardLabel: label).balanceNavigationChrome()
            case "detail-loading":
                CardDetailContent(phase: .loading, cardLabel: label).balanceNavigationChrome()
            case "detail-error":
                CardDetailContent(phase: .error("Sin conexión. Revisa tu red e inténtalo de nuevo."), cardLabel: label)
                    .balanceNavigationChrome()
            case "balance":
                BalanceContent(phase: .content(EzBankCardModel.samples)).balanceNavigationChrome()
            default:
                CardDetailContent(phase: .content(.sample), cardLabel: label).balanceNavigationChrome()
            }
        }
    }
}
#endif
