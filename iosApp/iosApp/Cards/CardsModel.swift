import SwiftUI
import SharedLogicSwift

private extension CardInk {
    /// Debe coincidir con INK en tools/check_card_skins.py: el verificador mide contra estos valores.
    var color: Color {
        switch self {
        case .SOFT_BLACK: return Color(red: 0x20 / 255, green: 0x1F / 255, blue: 0x1F / 255)
        case .WHITE: return .white
        default: return Color(red: 0, green: 0, blue: 0)
        }
    }
}

extension BankCard {
    /// Traduce el dominio a los datos de presentación de `EzBankCard`.
    func displayModel() -> EzBankCardModel {
        let skin = self.skin
        let kind = EzCardSkin(skin)
        return EzBankCardModel(
            id: id,
            imageName: skin.imageKey,
            fallbackTitle: (kind == .generic && !bankName.isEmpty) ? bankName : nil,
            last4: last4, holder: holder, currency: currency, expiry: expiry, isPrimary: isPrimary,
            numberInk: skin.number.color, textInk: skin.text.color, menuInk: skin.menu.color,
            menuScrim: skin.menuScrim,
            numberInset: CGFloat(skin.numberInset),
            spoken: spokenDescription(),
            menuLabel: "Más opciones de la tarjeta terminada en \(last4)",
            detailLabel: OperationPresentation.cardDetailLabel(skin: kind, bankName: bankName, last4: last4)
        )
    }
}

/// Envuelve el CardsViewModel de Kotlin para SwiftUI (observa `state` con `asAsyncSequence()` de Swift Export).
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
    private var observation: Task<Void, Never>?

    init() {
        let state = holder.viewModel.state
        observation = Task { [weak self] in
            do {
                for try await value in state.asAsyncSequence() { self?.apply(value) }
            } catch {}
        }
    }

    deinit {
        observation?.cancel()
        holder.clear()
    }

    private func apply(_ state: CardsUiState) {
        switch state.sealedType() {
        case .content(let content): phase = .content(content.value.cards.map { $0.displayModel() })
        case .empty: phase = .empty
        case .error(let error): phase = .error(error.value.message)
        case .loading: phase = .loading
        }
    }

    func load() { vm.load() }
}
