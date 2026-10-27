import SwiftUI
import SharedLogicSwift

/// Pantalla "Balance" conectada al ViewModel. El cromo de navegación vive aquí; el contenido
/// (`BalanceContent`) no depende del framework Kotlin y se puede previsualizar.
struct BalanceView: View {
    @ObservedObject var cards: CardsModel
    let onCardMenu: () -> Void
    let onCardTap: (EzBankCardModel) -> Void

    private var phase: BalanceContent.Phase {
        switch cards.phase {
        case .loading: return .loading
        case .empty: return .empty
        case .error(let message): return .error(message)
        case .content(let list): return .content(list)
        }
    }

    var body: some View {
        BalanceContent(phase: phase, onRetry: { cards.load() }, onCardMenu: onCardMenu, onCardTap: onCardTap)
            .balanceNavigationChrome()
            .onAppear { cards.load() }
    }
}

extension View {
    /// Barra azul de la marca, título "Balance" y sin barra de pestañas (como el original).
    ///
    /// El original usa un degradado azul-marino. La barra nativa de iOS no lo admite (con un
    /// degradado se queda en un color plano), y la hora y los iconos de la barra de estado siguen
    /// negros aunque se pida esquema oscuro; sobre el azul de marca esos iconos cumplen contraste
    /// (~6:1), pero sobre el marino del degradado no. Por eso el color es plano.
    func balanceNavigationChrome() -> some View { brandNavigationChrome(title: "Balance") }

    /// La misma barra para cualquier pantalla de segundo nivel.
    func brandNavigationChrome(title: String) -> some View {
        self
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(Ez.Palette.brandBlueLight, for: .navigationBar)
            .toolbarBackgroundVisibility(.visible, for: .navigationBar)
            .toolbarColorScheme(.dark, for: .navigationBar)
            .toolbar(.hidden, for: .tabBar)
    }
}

/// Sin dependencias del framework Kotlin: se puede previsualizar.
struct BalanceContent: View {
    enum Phase {
        case loading
        case empty
        case error(String)
        case content([EzBankCardModel])
    }

    let phase: Phase
    var onRetry: () -> Void = {}
    var onCardMenu: () -> Void = {}
    var onCardTap: (EzBankCardModel) -> Void = { _ in }

    var body: some View {
        Group {
            switch phase {
            case .loading:
                EzLoadingState()
            case .error(let message):
                EzErrorState(message: message, onRetry: onRetry)
            case .empty:
                EzEmptyState(title: "No tienes tarjetas",
                             message: "Cuando agregues una tarjeta, aparecerá aquí con su saldo.",
                             systemImage: "creditcard")
            case .content(let cards):
                ScrollView {
                    LazyVStack(spacing: Ez.Space.lg) {
                        ForEach(cards) { card in
                            EzBankCard(model: card, onMenu: onCardMenu, onTap: { onCardTap(card) })
                        }
                    }
                    .padding(.horizontal, Ez.Space.lg)
                    .padding(.vertical, Ez.Space.lg)
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Ez.Palette.surfaceVariant)
    }
}

#Preview("Balance: todos los skins") {
    NavigationStack { BalanceContent(phase: .content(EzBankCardModel.samples)).balanceNavigationChrome() }
}
#Preview("Balance (oscuro)") {
    NavigationStack { BalanceContent(phase: .content(Array(EzBankCardModel.samples.prefix(3)))).balanceNavigationChrome() }
        .preferredColorScheme(.dark)
}
#Preview("Balance: cargando") { NavigationStack { BalanceContent(phase: .loading).balanceNavigationChrome() } }
#Preview("Balance: vacío") { NavigationStack { BalanceContent(phase: .empty).balanceNavigationChrome() } }
#Preview("Balance: error") {
    NavigationStack { BalanceContent(phase: .error("Sin conexión. Revisa tu red e inténtalo de nuevo.")).balanceNavigationChrome() }
}
#Preview("Una tarjeta") {
    EzBankCard(model: EzBankCardModel.samples[3]).padding()
}
