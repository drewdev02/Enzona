import SwiftUI

/// Todo lo que la pantalla puede pedir al modelo, para pasarlo junto y poder previsualizar sin él.
struct RecipientActions {
    var load: () -> Void = {}
    var selectSource: (String) -> Void = { _ in }
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

/// "A una cuenta" / "A un amigo" conectado al ViewModel (uno por pantalla, con el destinatario ya verificado). El
/// contenido (`RecipientContent`) no depende del framework Kotlin.
struct RecipientView: View {
    @StateObject private var model: RecipientModel
    let isFriend: Bool
    let onOpenHistory: () -> Void
    let onClose: () -> Void

    init(recipient: RecipientIdentity, isFriend: Bool, onOpenHistory: @escaping () -> Void, onClose: @escaping () -> Void) {
        // `wrappedValue` es un autoclosure: el modelo (y su ViewModel) se crea una vez por pantalla.
        _model = StateObject(wrappedValue: RecipientModel(recipient: recipient))
        self.isFriend = isFriend
        self.onOpenHistory = onOpenHistory
        self.onClose = onClose
    }

    var body: some View {
        RecipientContent(
            phase: model.phase, isFriend: isFriend,
            actions: RecipientActions(
                load: { model.load() }, selectSource: { model.selectSource($0) }, setAmount: { model.setAmount($0) },
                setDescription: { model.setDescription($0) }, review: { model.review() }, back: { model.back() },
                submit: { model.submit($0) }, editAgain: { model.editAgain() }, openHistory: onOpenHistory, close: onClose)
        )
        .onAppear { model.load() }
    }
}

struct RecipientContent: View {
    let phase: RecipientPhase
    var isFriend = false
    var actions = RecipientActions()

    private var title: String {
        if case .finished = phase { return receiptTitle }
        return RecipientPresentation.title(isFriend: isFriend)
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
            case .noCards:
                EzEmptyState(title: "No tienes tarjetas", message: "Para transferir necesitas una tarjeta de origen.",
                             systemImage: "creditcard")
            case .editing(let form):
                RecipientForm(form: form, enabled: true, actions: actions)
            case .confirming(let confirm):
                RecipientForm(form: confirm.form, enabled: false, actions: actions)
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

private struct RecipientForm: View {
    let form: RecipientFormData
    let enabled: Bool
    let actions: RecipientActions
    // Copias locales de los textos: teclear no espera la vuelta del ViewModel (que llega después y puede perder letras).
    @State private var amount: String
    @State private var description: String

    init(form: RecipientFormData, enabled: Bool, actions: RecipientActions) {
        self.form = form
        self.enabled = enabled
        self.actions = actions
        _amount = State(initialValue: form.amountText)
        _description = State(initialValue: form.description)
    }

    var body: some View {
        ScrollView {
            VStack(spacing: Ez.Space.md) {
                // A quién se envía: el destinatario verificado por el servidor, no editable.
                EzListItem(title: form.recipient.displayName, subtitle: form.recipient.username, leading: {
                    EzAvatar(initials: TransferPresentation.initials(form.recipient.displayName))
                })
                CardPickerMenu(label: "Tarjeta origen", card: form.source, placeholder: "", enabled: enabled,
                               options: form.cards, onPick: actions.selectSource)
                EzTextField(label: "Importe", text: $amount, error: form.amountError, keyboard: .decimalPad)
                    .disabled(!enabled)
                    .onChange(of: amount) { _, new in if new != form.amountText { actions.setAmount(new) } }
                    .onChange(of: form.amountText) { _, new in if new != amount { amount = new } }
                EzTextField(label: "Descripción (Opcional)", text: $description)
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

#Preview("Cuenta: formulario") {
    NavigationStack { RecipientContent(phase: .editing(.sample)) }
}
#Preview("Amigo: confirmar con cambio de moneda") {
    NavigationStack { RecipientContent(phase: .confirming(.withExchange), isFriend: true) }
}
#Preview("Cuenta: comprobante") {
    NavigationStack { RecipientContent(phase: .finished(.acceptedToPerson)) }
}
