import SwiftUI
import CoreImage.CIFilterBuiltins

// Piezas que comparten los formularios que mueven dinero ("A una de mis tarjetas", "A una cuenta", "A un amigo"): el
// selector de tarjeta, la alerta de contraseña, la pantalla de "enviando" y el comprobante. Una sola versión de cada una.

/// Título de la pantalla cuando ya hay un desenlace: el del comprobante de la app original.
let receiptTitle = "Detalles de la transferencia"

/// Qué puede pedir el comprobante: corregir (si no se movió dinero), mirar el historial (si no se sabe) o cerrar.
struct ReceiptActions {
    var editAgain: () -> Void = {}
    var openHistory: () -> Void = {}
    var close: () -> Void = {}
}

/// Pantalla de "enviando": no se sale hasta que termine.
struct TransferSubmittingView: View {
    var body: some View {
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
struct PasswordConfirmAlert: ViewModifier {
    let question: String
    let passwordError: String?
    let onCancel: () -> Void
    let onSubmit: (String) -> Void
    @State private var password = ""
    // Se cierra con cualquiera de sus botones. Si el intento no sale (contraseña vacía...), el estado pasa por
    // "enviando" y vuelve a "confirmar": la vista se recrea y la alerta reaparece con el motivo.
    @State private var presented = true

    func body(content: Content) -> some View {
        content.alert("Confirmar transferencia", isPresented: $presented) {
            SecureField("Contraseña de pago", text: $password).keyboardType(.numberPad)
            Button("Cancelar", role: .cancel) {
                password = ""
                onCancel()
            }
            Button("Confirmar") {
                let entered = password
                password = ""
                onSubmit(entered)
            }
        } message: {
            Text(passwordError.map { "\(question)\n\n\($0)" } ?? question)
        }
    }
}

/// Fila con borde que abre el menú de tarjetas. Sin tarjeta elegida muestra [placeholder].
struct CardPickerMenu: View {
    let label: String
    let card: CardChoice?
    let placeholder: String
    var error = false
    let enabled: Bool
    let options: [CardChoice]
    let onPick: (String) -> Void

    var body: some View {
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
    var actions = ReceiptActions()
    @Environment(\.dynamicTypeSize) private var typeSize

    private var statusColor: Color {
        switch detail.kind {
        case .success: return Ez.Palette.success
        case .failed, .notSent: return Ez.Palette.error
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
        // Nunca se muestra aquí (un intento que no salió vuelve a confirmar), pero el `switch` es exhaustivo.
        case .notSent:
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
