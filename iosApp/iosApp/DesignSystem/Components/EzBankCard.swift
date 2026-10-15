import SwiftUI

/// Datos de presentación de una tarjeta. Modelo propio de la UI (sin depender del framework Kotlin)
/// para que las vistas se puedan previsualizar. Quien lo usa traduce el dominio (imagen y tintas).
struct EzBankCardModel: Identifiable {
    let id: String
    /// Nombre del imageset, o nil para dibujar un fondo propio.
    let imageName: String?
    let fallbackTitle: String?
    let last4: String
    let holder: String
    let currency: String
    let expiry: String?
    let isPrimary: Bool
    let numberInk: Color
    let textInk: Color
    let menuInk: Color
    let menuScrim: Bool
    /// Dónde empieza el número, en fracción del ancho (algunos skins traen un adorno a la izquierda).
    var numberInset: CGFloat = Ez.Card.startInset
    /// Lo que leen los lectores de pantalla (la imagen no se lee y el número quedaría en trozos).
    let spoken: String
    let menuLabel: String
    /// Texto de la fila "Tarjeta" del detalle ("BANMET 6856"). Lo calcula quien traduce el dominio.
    var detailLabel: String = ""
}

/// Tarjeta bancaria dibujada sobre la imagen de su banco. Los tamaños de letra salen del ANCHO de la
/// tarjeta y no de Dynamic Type: el texto está colocado sobre arte con zonas fijas; el contenido
/// completo lo cubre `model.spoken`.
struct EzBankCard: View {
    let model: EzBankCardModel
    var onMenu: () -> Void = {}
    /// Toque sobre la tarjeta (el menú tiene el suyo). `nil` la deja sin acción, como en las previews.
    var onTap: (() -> Void)? = nil

    var body: some View {
        GeometryReader { geo in
            let w = geo.size.width
            let h = geo.size.height
            let inset = w * Ez.Card.startInset

            ZStack(alignment: .topLeading) {
                background(width: w, height: h)

                // Todo el texto es UN elemento para los lectores de pantalla (el menú queda aparte).
                ZStack(alignment: .topLeading) {
                    if let title = model.fallbackTitle {
                        Text(title)
                            .font(.system(size: w * 0.055, weight: .bold))
                            .foregroundStyle(model.textInk)
                            .lineLimit(1)
                            .frame(width: w * 0.65, alignment: .leading)
                            .offset(x: inset, y: h * Ez.Card.iconCenterY - 14)
                    }
                    row(width: w, height: h * 0.14, centerY: h * Ez.Card.numberCenter, inset: w * model.numberInset) {
                        Text("**** **** **** ")
                            .font(.system(size: w * 0.07, weight: .regular))
                            .foregroundStyle(model.numberInk)
                        Text(model.last4)
                            .font(.system(size: w * 0.085, weight: .bold).monospacedDigit())
                            .foregroundStyle(model.numberInk)
                    }
                    if !model.holder.isEmpty {
                        row(width: w, height: h * 0.1, centerY: h * Ez.Card.nameCenter, inset: inset) {
                            Text(model.holder)
                                .font(.system(size: w * 0.047, weight: .medium))
                                .foregroundStyle(model.textInk)
                                .lineLimit(1)
                                .frame(width: w * 0.68, alignment: .leading)
                        }
                    }
                    row(width: w, height: h * 0.1, centerY: h * Ez.Card.infoCenter, inset: inset) {
                        if !model.currency.isEmpty {
                            Text(model.currency)
                                .font(.system(size: w * 0.042, weight: .bold))
                                .foregroundStyle(model.textInk)
                        }
                        if let expiry = model.expiry {
                            Text("Vence: \(expiry)")
                                .font(.system(size: w * 0.042))
                                .foregroundStyle(model.textInk)
                                .padding(.leading, model.currency.isEmpty ? 0 : w * 0.04)
                        }
                    }
                    if model.isPrimary {
                        Image(systemName: "checkmark")
                            .font(.system(size: 20, weight: .semibold))
                            .foregroundStyle(model.menuInk)
                            .frame(width: 24, height: 24)
                            .offset(x: w * Ez.Card.checkCenterX - 12, y: h * Ez.Card.iconCenterY - 12)
                    }
                }
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(model.spoken)
                .accessibilityAddTraits(onTap == nil ? [] : .isButton)
                .accessibilityAction { onTap?() }

                Button(action: onMenu) {
                    ZStack {
                        if model.menuScrim {
                            Circle().fill(Color.black.opacity(Ez.Card.menuScrimAlpha)).frame(width: 32, height: 32)
                        }
                        Image(systemName: "ellipsis")
                            .rotationEffect(.degrees(90))   // el original usa tres puntos verticales
                            .font(.system(size: 20, weight: .bold))
                            .foregroundStyle(model.menuInk)
                    }
                    .frame(width: Ez.Size.minTouchTarget + 4, height: Ez.Size.minTouchTarget + 4)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(model.menuLabel)
                .offset(x: w * Ez.Card.menuCenterX - 24, y: h * Ez.Card.iconCenterY - 24)
            }
            .frame(width: w, height: h, alignment: .topLeading)
            .clipShape(RoundedRectangle(cornerRadius: w * Ez.Card.cornerRadius))
            .contentShape(RoundedRectangle(cornerRadius: w * Ez.Card.cornerRadius))
            .onTapGesture { onTap?() }
            .shadow(color: .black.opacity(0.18), radius: Ez.Elevation.medium, y: 1)
        }
        .aspectRatio(1 / Ez.Card.heightRatio, contentMode: .fit)
    }

    @ViewBuilder
    private func background(width: CGFloat, height: CGFloat) -> some View {
        if let name = model.imageName {
            Image(name)
                .resizable()
                .scaledToFill()          // recorta (BoxFit.cover), no estira
                .frame(width: width, height: height)
                .clipped()
                .accessibilityHidden(true)
        } else {
            LinearGradient(colors: [Ez.Palette.brandBlue, Ez.Palette.brandNavy], startPoint: .topLeading, endPoint: .bottomTrailing)
                .frame(width: width, height: height)
        }
    }

    private func row<Content: View>(width: CGFloat, height: CGFloat, centerY: CGFloat, inset: CGFloat,
                                    @ViewBuilder content: () -> Content) -> some View {
        HStack(spacing: 0, content: content)
            .frame(height: height)
            .offset(x: inset, y: centerY - height / 2)
    }
}
