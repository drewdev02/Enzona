import SwiftUI
import SharedLogic

private extension CardInk {
    /// Debe coincidir con INK en tools/check_card_skins.py: el verificador mide contra estos valores.
    var color: Color {
        switch name {
        case "SOFT_BLACK": return Color(red: 0x20 / 255, green: 0x1F / 255, blue: 0x1F / 255)
        case "WHITE": return .white
        default: return Color(red: 0, green: 0, blue: 0)
        }
    }
}

extension BankCard {
    /// Traduce el dominio a los datos de presentación de `EzBankCard`.
    func displayModel() -> EzBankCardModel {
        let skin = self.skin
        return EzBankCardModel(
            id: id,
            imageName: skin.imageKey,
            fallbackTitle: (skin.name == "GENERIC" && !bankName.isEmpty) ? bankName : nil,
            last4: last4, holder: holder, currency: currency, expiry: expiry, isPrimary: isPrimary,
            numberInk: skin.number.color, textInk: skin.text.color, menuInk: skin.menu.color,
            menuScrim: skin.menuScrim,
            numberInset: CGFloat(skin.numberInset),
            spoken: spokenDescription(),
            menuLabel: "Más opciones de la tarjeta terminada en \(last4)"
        )
    }
}

/// Envuelve el CardsViewModel de Kotlin para SwiftUI (puente `watchState`, en lugar de SKIE).
@MainActor
final class CardsModel: ObservableObject {
    enum Phase {
        case loading
        case empty
        case error(String)
        case content([EzBankCardModel])
    }

    @Published var phase: Phase = .loading

    /// Un ViewModel por pantalla: `deinit` lo limpia y cancela sus cargas en vuelo.
    private let holder = KoinHelper.shared.cardsViewModelHolder()
    private var vm: CardsViewModel { holder.viewModel }
    private var subscription: Cancellable?

    init() {
        subscription = holder.viewModel.watchState { [weak self] state in
            self?.apply(state)
        }
    }

    deinit {
        subscription?.cancel()
        holder.clear()
    }

    private func apply(_ state: CardsUiState) {
        switch state {
        case let content as CardsUiStateContent: phase = .content(content.cards.map { $0.displayModel() })
        case is CardsUiStateEmpty: phase = .empty
        case let error as CardsUiStateError: phase = .error(error.message)
        default: phase = .loading
        }
    }

    func load() { vm.load() }
}
