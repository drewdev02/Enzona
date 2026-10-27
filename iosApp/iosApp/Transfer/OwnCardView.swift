import SwiftUI
import SharedLogicSwift

/// Todo lo que la pantalla puede pedir al modelo, para pasarlo junto y poder previsualizar sin él.
struct OwnCardActions {
    var load: () -> Void = {}
    var selectSource: (String) -> Void = { _ in }
    var selectDestination: (String) -> Void = { _ in }
    var setAmount: (String) -> Void = { _ in }
    var setDescription: (String) -> Void = { _ in }
    var review: () -> Void = {}
    var back: () -> Void = {}
    var submit: (String) -> Void = { _ in }
    var editAgain: () -> Void = {}
    var openHistory: () -> Void = {}
    var close: () -> Void = {}

    var receipt: ReceiptActions { ReceiptActions(editAgain: editAgain, openHistory: openHistory, close: close) }
}

/// "A una de mis tarjetas" conectado al ViewModel. El contenido (`OwnCardContent`) no depende del framework Kotlin.
struct OwnCardView: View {
    @StateObject private var model = OwnCardModel()
    let onOpenHistory: () -> Void
    let onClose: () -> Void

    var body: some View {
        OwnCardContent(
            phase: model.phase,
            actions: OwnCardActions(
                load: { model.load() }, selectSource: { model.selectSource($0) },
                selectDestination: { model.selectDestination($0) }, setAmount: { model.setAmount($0) },
                setDescription: { model.setDescription($0) }, review: { model.review() }, back: { model.back() },
                submit: { model.submit($0) }, editAgain: { model.editAgain() }, openHistory: onOpenHistory, close: onClose)
        )
        .onAppear { model.load() }
    }
}

struct OwnCardContent: View {
    let phase: OwnCardPhase
    var actions = OwnCardActions()

    private var title: String {
        if case .finished = phase { return receiptTitle }
        return "Transferir a una de mis tarjetas"
    }

    private var isSubmitting: Bool {
        if case .submitting = phase { return true }
        return false
    }

    var body: some View {
        Group {
            switch phase {
            case .loading:
                EzLoadingState()
            case .loadError(let message):
                EzErrorState(message: message, onRetry: actions.load)
            case .needsTwoCards:
                EzEmptyState(title: "Necesitas al menos dos tarjetas",
                             message: "Para transferir entre tus tarjetas, primero agrega otra.", systemImage: "creditcard")
            case .editing(let form):
                OwnCardForm(form: form, enabled: true, actions: actions)
            case .confirming(let confirm):
                OwnCardForm(form: confirm.form, enabled: false, actions: actions)
                    .modifier(PasswordConfirmAlert(
                        question: confirm.question, passwordError: confirm.passwordError,
                        onCancel: actions.back, onSubmit: actions.submit))
            case .submitting:
                TransferSubmittingView()
            case .finished(let detail):
                TransferDetailView(detail: detail, actions: actions.receipt)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Ez.Palette.background)
        .brandNavigationChrome(title: title)
        // Con una petición en vuelo no se sale: el resultado se mostraría a nadie y se podría repetir un pago por duda.
        .navigationBarBackButtonHidden(isSubmitting)
    }
}

private struct OwnCardForm: View {
    let form: OwnCardFormData
    let enabled: Bool
    let actions: OwnCardActions
    // Copias locales de los textos: teclear no espera la vuelta del ViewModel (que llega después y puede perder letras).
    @State private var amount: String
    @State private var description: String

    init(form: OwnCardFormData, enabled: Bool, actions: OwnCardActions) {
        self.form = form
        self.enabled = enabled
        self.actions = actions
        _amount = State(initialValue: form.amountText)
        _description = State(initialValue: form.description)
    }

    var body: some View {
        ScrollView {
            VStack(spacing: Ez.Space.md) {
                CardPickerMenu(label: "Tarjeta origen", card: form.source, placeholder: "", enabled: enabled,
                               options: form.cards, onPick: actions.selectSource)
                CardPickerMenu(label: "Tarjeta destino", card: form.destination, placeholder: "Seleccione la tarjeta destino",
                               error: form.destinationError, enabled: enabled,
                               options: form.cards.filter { $0.id != form.sourceId }, onPick: actions.selectDestination)
                EzTextField(label: "Importe", text: $amount, error: form.amountError, keyboard: .decimalPad)
                    .disabled(!enabled)
                    .onChange(of: amount) { _, new in if new != form.amountText { actions.setAmount(new) } }
                    .onChange(of: form.amountText) { _, new in if new != amount { amount = new } }
                EzTextField(label: "Descripción", text: $description, error: form.descriptionError ? "Campo requerido" : nil)
                    .disabled(!enabled)
                    .onChange(of: description) { _, new in if new != form.description { actions.setDescription(new) } }
                    .onChange(of: form.description) { _, new in if new != description { description = new } }
                EzButton(title: "ACEPTAR", action: actions.review)
                    .disabled(!enabled)
            }
            .padding(Ez.Space.lg)
        }
    }
}

#Preview("Formulario") {
    NavigationStack { OwnCardContent(phase: .editing(.sample)) }
}
#Preview("Formulario con errores") {
    NavigationStack { OwnCardContent(phase: .editing(.withProblems)) }
}
#Preview("Detalle: confirmada") {
    NavigationStack { OwnCardContent(phase: .finished(.accepted)) }
}
#Preview("Detalle: fallida") {
    NavigationStack { OwnCardContent(phase: .finished(.rejected)) }
}
#Preview("Detalle (texto grande)") {
    NavigationStack { OwnCardContent(phase: .finished(.accepted)) }.dynamicTypeSize(.accessibility3)
}
