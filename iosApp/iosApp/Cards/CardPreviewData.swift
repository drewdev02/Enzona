import SwiftUI

extension EzBankCardModel {
    /// Datos INVENTADOS para previews: un ejemplar de cada skin y uno genérico. Las tintas
    /// reproducen las de `CardSkin.kt` (solo para previews; la app las toma del modelo compartido).
    static let samples: [EzBankCardModel] = {
        let black = Color(red: 0, green: 0, blue: 0)
        let soft = Color(red: 0x20 / 255, green: 0x1F / 255, blue: 0x1F / 255)
        let white = Color.white
        func make(_ last4: String, _ skin: EzCardSkin, _ title: String, _ currency: String, _ expiry: String,
                  primary: Bool = false, number: Color, text: Color, menu: Color, scrim: Bool = false,
                  inset: CGFloat = Ez.Card.startInset) -> EzBankCardModel {
            EzBankCardModel(
                // Misma regla que `CardSkin.imageKey` (Kotlin): `card_skin_<nombre en minúsculas>`.
                id: last4, imageName: skin == .generic ? nil : "card_skin_\(skin.rawValue.lowercased())",
                fallbackTitle: skin == .generic ? title : nil,
                last4: last4, holder: "TEST USER", currency: currency, expiry: expiry, isPrimary: primary,
                numberInk: number, textInk: text, menuInk: menu, menuScrim: scrim, numberInset: inset,
                spoken: "Tarjeta \(title), \(currency), terminada en \(last4.map(String.init).joined(separator: " ")), titular test user, vence \(expiry.replacingOccurrences(of: "/", with: " "))\(primary ? ", predeterminada" : "")",
                menuLabel: "Más opciones de la tarjeta terminada en \(last4)"
            )
        }
        return [
            make("6856", .banmet, "Banco Metropolitano S.A", "CUP", "05/32", primary: true, number: black, text: soft, menu: black),
            make("3130", .clasica, "FINCIMEX CLASICA", "USD", "01/31", number: black, text: white, menu: black),
            make("5800", .bandec, "Banco de Crédito y Comercio", "CUP", "02/32", number: white, text: white, menu: white),
            make("4912", .bandecUsd, "Banco de Crédito y Comercio", "USD", "02/32", number: black, text: black, menu: white, scrim: true),
            make("1204", .bicsa, "BICSA", "USD", "11/30", number: black, text: soft, menu: black),
            make("7731", .bpa, "Banco Popular de Ahorro", "CUP", "09/29", number: black, text: soft, menu: black, inset: 0.115),
            make("9087", .ais, "American International Service", "USD", "03/33", number: white, text: black, menu: black),
            make("0042", .generic, "Banco Desconocido", "CUP", "12/30", number: white, text: white, menu: white),
        ]
    }()
}
