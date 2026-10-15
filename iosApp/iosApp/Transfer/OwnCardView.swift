import SwiftUI
import CoreImage.CIFilterBuiltins
import SharedLogic

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
        if case .finished = phase { return "Detalles de la transferencia" }
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
                    .modifier(ConfirmAlert(confirm: confirm, actions: actions))
            case .submitting:
                submitting
            case .finished(let detail):
                TransferDetailView(detail: detail, actions: actions)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Ez.Palette.background)
        .brandNavigationChrome(title: title)
        // Con una petición en vuelo no se sale: el resultado se mostraría a nadie y se podría repetir un pago por duda.
        .navigationBarBackButtonHidden(isSubmitting)
    }

    private var submitting: some View {
        VStack(spacing: Ez.Space.lg) {
            ProgressView().tint(Ez.Palette.primary)
            Text("Enviando la transferencia…").font(Ez.Typography.titleMedium).foregroundStyle(Ez.Palette.onSurface)
            Text("No cierres la pantalla.").font(Ez.Typography.bodyMedium).foregroundStyle(Ez.Palette.onSurfaceVariant)
        }
        .padding(Ez.Space.xl)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Enviando la transferencia. No cierres la pantalla.")
    }
}

/// Alerta de confirmación con la contraseña de pago. La contraseña vive solo aquí, en memoria, y se borra al usarla.
private struct ConfirmAlert: ViewModifier {
    let confirm: OwnCardConfirmData
    let actions: OwnCardActions
    @State private var password = ""
    // Se cierra con cualquiera de sus botones. Si el intento no sale (contraseña vacía...), el estado pasa por
    // "enviando" y vuelve a "confirmar": la vista se recrea y la alerta reaparece con el motivo.
    @State private var presented = true

    func body(content: Content) -> some View {
        content.alert(
            "Confirmar transferencia",
            isPresented: $presented
        ) {
            SecureField("Contraseña de pago", text: $password).keyboardType(.numberPad)
            Button("Cancelar", role: .cancel) {
                password = ""
                actions.back()
            }
            Button("Confirmar") {
                let entered = password
                password = ""
                actions.submit(entered)
            }
        } message: {
            Text(confirm.passwordError.map { "\(confirm.question)\n\n\($0)" } ?? confirm.question)
        }
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
                picker(label: "Tarjeta origen", card: form.source, placeholder: "", error: false,
                       options: form.cards, onPick: actions.selectSource)
                picker(label: "Tarjeta destino", card: form.destination, placeholder: "Seleccione la tarjeta destino",
                       error: form.destinationError, options: form.cards.filter { $0.id != form.sourceId },
                       onPick: actions.selectDestination)
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

    private func picker(label: String, card: CardChoice?, placeholder: String, error: Bool,
                        options: [CardChoice], onPick: @escaping (String) -> Void) -> some View {
        VStack(alignment: .leading, spacing: Ez.Space.xs) {
            Menu {
                ForEach(options) { option in
                    Button("\(option.label) · \(option.currency)") { onPick(option.id) }
                }
            } label: {
                HStack(spacing: Ez.Space.md) {
                    Image(card?.logoName ?? "avatar_placeholder").resizable().scaledToFit()
                        .frame(width: Ez.Size.avatarMd, height: Ez.Size.avatarMd).clipShape(Circle())
                        .accessibilityHidden(true)
                    VStack(alignment: .leading, spacing: 0) {
                        Text(label).font(Ez.Typography.labelMedium).foregroundStyle(Ez.Palette.onSurfaceVariant)
                        Text(card?.label ?? placeholder).font(Ez.Typography.bodyLarge)
                            .foregroundStyle(card == nil ? Ez.Palette.onSurfaceVariant : Ez.Palette.onSurface)
                    }
                    Spacer(minLength: 0)
                }
                .padding(.horizontal, Ez.Space.lg)
                .padding(.vertical, Ez.Space.md)
                .overlay(
                    RoundedRectangle(cornerRadius: Ez.Radius.md)
                        .stroke(error ? Ez.Palette.error : Ez.Palette.outline, lineWidth: error ? 2 : 1)
                )
            }
            .disabled(!enabled)
            .accessibilityLabel("\(label): \(card?.label ?? "sin elegir")")
            if error {
                Text(placeholder).font(Ez.Typography.bodySmall).foregroundStyle(Ez.Palette.error)
            }
        }
    }
}

/// "Detalles de la transferencia", como el comprobante de la app original. Tras un resultado que no es "confirmada" se
/// añaden los botones que hacen falta (corregir si no se movió dinero; mirar el historial si no se sabe).
struct TransferDetailView: View {
    let detail: TransferDetailData
    var actions = OwnCardActions()
    @Environment(\.dynamicTypeSize) private var typeSize

    private var statusColor: Color {
        switch detail.kind {
        case .success: return Ez.Palette.success
        case .failed: return Ez.Palette.error
        case .pending, .unknown: return Ez.Tone.warning.content
        }
    }

    var body: some View {
        ScrollView {
            VStack(spacing: Ez.Space.lg) {
                Image(detail.destinationLogo).resizable().scaledToFit().frame(width: 112, height: 112)
                    .clipShape(Circle()).accessibilityHidden(true)
                Text(detail.mainText).font(.title.bold()).foregroundStyle(Ez.Palette.onSurface)
                    .multilineTextAlignment(.center)
                Text(detail.statusTitle).font(.title3).foregroundStyle(statusColor).multilineTextAlignment(.center)
                Text(detail.amountText).font(.title3).foregroundStyle(Ez.Palette.onSurface)
                if let date = detail.dateText {
                    Text(date).font(.headline).foregroundStyle(Ez.Palette.onSurfaceVariant)
                }
                if let note = detail.note {
                    Text(note).font(Ez.Typography.bodyLarge).foregroundStyle(Ez.Palette.onSurfaceVariant)
                        .multilineTextAlignment(.center)
                }
                VStack(spacing: 0) {
                    ForEach(detail.rows, id: \.label) { row in
                        detailRow(row)
                        Rectangle().fill(Ez.Palette.divider).frame(height: Ez.Size.divider)
                    }
                }
                sourceCard
                if let content = detail.qrContent, let image = QRCode.image(content) {
                    Image(uiImage: image).interpolation(.none).resizable().scaledToFit().frame(width: 200, height: 200)
                        .accessibilityLabel("Código QR del comprobante")
                }
                buttons
            }
            .padding(.horizontal, Ez.Space.lg)
            .padding(.vertical, Ez.Space.xl)
        }
    }

    @ViewBuilder
    private var buttons: some View {
        switch detail.kind {
        case .success:
            EmptyView()
        // El dinero no se movió: se puede corregir y volver a intentar.
        case .failed:
            EzButton(title: "Corregir", action: actions.editAgain)
            EzButton(title: "Cerrar", kind: .tertiary, action: actions.close)
        // Pendiente o desconocido: puede que sí se movió, así que nada de reintentar; primero el historial.
        case .pending, .unknown:
            EzButton(title: "Ver historial", action: actions.openHistory)
            EzButton(title: "Cerrar", kind: .tertiary, action: actions.close)
        }
    }

    private func detailRow(_ row: OwnCardPresentation.Row) -> some View {
        Group {
            if typeSize.isAccessibilitySize {
                VStack(alignment: .leading, spacing: 0) {
                    Text(row.label).foregroundStyle(Ez.Palette.onSurfaceVariant)
                    Text(row.value).foregroundStyle(Ez.Palette.onSurface)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            } else {
                HStack {
                    Text(row.label).foregroundStyle(Ez.Palette.onSurfaceVariant)
                    Spacer(minLength: Ez.Space.md)
                    Text(row.value).foregroundStyle(Ez.Palette.onSurface).multilineTextAlignment(.trailing)
                }
            }
        }
        .font(Ez.Typography.bodyLarge)
        .padding(.vertical, Ez.Space.md)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(row.label): \(row.value)")
    }

    private var sourceCard: some View {
        Group {
            if typeSize.isAccessibilitySize {
                VStack(alignment: .leading, spacing: Ez.Space.xs) {
                    sourceLogo
                    Text(detail.sourceHolder.uppercased())
                    Text(detail.sourceSuffix)
                }
            } else {
                HStack(spacing: Ez.Space.md) {
                    sourceLogo
                    Text(detail.sourceHolder.uppercased())
                    Spacer(minLength: 0)
                    Text(detail.sourceSuffix)
                }
            }
        }
        .font(Ez.Typography.bodyLarge)
        .foregroundStyle(Ez.Palette.onSurfaceVariant)
        .padding(Ez.Space.md)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: Ez.Radius.sm).fill(Ez.Palette.surface).shadow(radius: 2, y: 1))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Tarjeta de origen: \(detail.sourceHolder), \(detail.sourceSuffix)")
    }

    private var sourceLogo: some View {
        Image(detail.sourceLogo).resizable().scaledToFit()
            .frame(width: Ez.Size.avatarMd, height: Ez.Size.avatarMd).clipShape(Circle()).accessibilityHidden(true)
    }
}

/// Dibuja un texto como código QR (negro sobre blanco, también en tema oscuro: así lo lee cualquier lector).
enum QRCode {
    static func image(_ content: String) -> UIImage? {
        guard !content.isEmpty else { return nil }
        let filter = CIFilter.qrCodeGenerator()
        filter.message = Data(content.utf8)
        filter.correctionLevel = "M"
        guard let output = filter.outputImage else { return nil }
        let scaled = output.transformed(by: CGAffineTransform(scaleX: 10, y: 10))
        guard let cgImage = CIContext().createCGImage(scaled, from: scaled.extent) else { return nil }
        return UIImage(cgImage: cgImage)
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
