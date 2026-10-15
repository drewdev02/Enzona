import SwiftUI
import SharedLogic

/// Las cuatro formas de transferir del menú, en el orden del original.
enum TransferOption: CaseIterable, Identifiable {
    case friend, account, card, ownCard

    var id: Self { self }

    var label: String {
        switch self {
        case .friend: "A un amigo"
        case .account: "A una cuenta"
        case .card: "A una tarjeta"
        case .ownCard: "A una de mis tarjetas"
        }
    }

    var imageName: String {
        switch self {
        case .friend: "transfer_option_friend"
        case .account: "transfer_option_account"
        case .card: "transfer_option_card"
        case .ownCard: "transfer_option_own_card"
        }
    }
}

/// Menú "Transferir" conectado al ViewModel. El contenido (`TransferMenuContent`) no depende del framework Kotlin.
struct TransferMenuView: View {
    @StateObject private var model = TransferMenuModel()
    let onOption: (TransferOption) -> Void
    let onHistory: () -> Void

    var body: some View {
        TransferMenuContent(phase: model.phase, onOption: onOption, onHistory: onHistory, onRetry: { model.load() })
            .onAppear { model.load() }
    }
}

struct TransferMenuContent: View {
    let phase: TransferMenuPhase
    var onOption: (TransferOption) -> Void = { _ in }
    var onHistory: () -> Void = {}
    var onRetry: () -> Void = {}

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 0) {
                ForEach(TransferOption.allCases) { option in
                    Button { onOption(option) } label: {
                        EzListItem(title: option.label, leading: {
                            Image(option.imageName).resizable().scaledToFit()
                                .frame(width: Ez.Size.avatarLg, height: Ez.Size.avatarLg)
                                .accessibilityHidden(true)
                        })
                    }
                    .buttonStyle(.plain)
                    Rectangle().fill(Ez.Palette.divider).frame(height: Ez.Size.divider)
                }
                EzLabeledDivider(title: "Recientes")
                recents
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Ez.Palette.background)
        .brandNavigationChrome(title: "Transferir")
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button("Historial", action: onHistory).foregroundStyle(.white)
            }
        }
    }

    @ViewBuilder
    private var recents: some View {
        switch phase {
        case .loading:
            ProgressView().tint(Ez.Palette.primary).frame(maxWidth: .infinity).padding(Ez.Space.xl)
        case .error(let message):
            EzErrorState(message: message, onRetry: onRetry).frame(minHeight: 200)
        case .content(let rows, let skipped):
            if rows.isEmpty {
                TransferNotice(text: "Aún no has hecho transferencias")
            } else {
                ForEach(rows) { TransferRowView(row: $0) }
            }
            if skipped > 0 { TransferNotice(text: "No se pudieron mostrar \(skipped) transferencias.") }
        }
    }
}

#Preview("Menú") {
    NavigationStack { TransferMenuContent(phase: .content(Array(TransferRowModel.samples.prefix(4)), skipped: 0)) }
}
#Preview("Menú (oscuro)") {
    NavigationStack { TransferMenuContent(phase: .content(Array(TransferRowModel.samples.prefix(4)), skipped: 0)) }
        .preferredColorScheme(.dark)
}
#Preview("Menú (texto grande)") {
    NavigationStack { TransferMenuContent(phase: .content(Array(TransferRowModel.samples.prefix(3)), skipped: 0)) }
        .dynamicTypeSize(.accessibility3)
}
#Preview("Menú: sin transferencias") {
    NavigationStack { TransferMenuContent(phase: .content([], skipped: 0)) }
}
