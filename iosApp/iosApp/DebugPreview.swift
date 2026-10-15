#if DEBUG
import SwiftUI

/// Solo builds DEBUG. Muestra una pantalla con datos INVENTADOS, sin Koin ni red, para revisarla en el
/// simulador sin iniciar sesión. El modo oscuro y el tamaño de letra se cambian con el simulador:
///   xcrun simctl ui booted appearance dark
///   xcrun simctl ui booted content_size accessibility-extra-large
///
///   xcrun simctl launch booted <bundle id> -ezPreview <nombre>
/// Nombres: detail (por defecto), detail-empty, detail-partial, detail-loading, detail-error, balance,
/// transfer-menu, transfer-menu-empty, transfer-history, transfer-history-empty, find-account, find-account-notfound, friends,
/// own-form, own-errors, own-sending, own-accepted, own-pending, own-rejected, own-unknown.
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
            case "transfer-menu":
                TransferMenuContent(phase: .content(Array(TransferRowModel.samples.prefix(5)), skipped: 0))
            case "transfer-menu-empty":
                TransferMenuContent(phase: .content([], skipped: 0))
            case "transfer-history":
                TransferHistoryContent(
                    phase: .content(TransferHistoryData(rows: TransferRowModel.samples, skipped: 1, hasMore: false, isLoadingMore: false, moreError: nil)),
                    filter: .none)
            case "transfer-history-empty":
                TransferHistoryContent(
                    phase: .content(TransferHistoryData(rows: [], skipped: 0, hasMore: false, isLoadingMore: false, moreError: nil)),
                    filter: TransferFilterModel(startDay: "2026-10-01", endDay: nil, status: .failed))
            case "find-account":
                FindAccountContent(phase: .found(PersonModel.samples[0]))
            case "find-account-notfound":
                FindAccountContent(phase: .notFound)
            case "friends":
                TransferFriendsContent(phase: .content(PersonModel.samples))
            case "own-form":
                OwnCardContent(phase: .editing(.sample))
            case "own-errors":
                OwnCardContent(phase: .editing(.withProblems))
            case "own-sending":
                OwnCardContent(phase: .submitting)
            case "own-accepted":
                OwnCardContent(phase: .finished(.accepted))
            case "own-pending":
                OwnCardContent(phase: .finished(.pending))
            case "own-rejected":
                OwnCardContent(phase: .finished(.rejected))
            case "own-unknown":
                OwnCardContent(phase: .finished(.unknown))
            case "balance":
                BalanceContent(phase: .content(EzBankCardModel.samples)).balanceNavigationChrome()
            default:
                CardDetailContent(phase: .content(.sample), cardLabel: label).balanceNavigationChrome()
            }
        }
    }
}
#endif
