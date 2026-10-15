import Foundation

/// Textos de "A una cuenta" y "A un amigo" (los mismos que `RecipientTransferPresentation.kt` en Android). Lo común a
/// todos los formularios que mueven dinero está en `OwnCardPresentation`. Sin dependencias del framework Kotlin.
enum RecipientPresentation {
    /// Cuál de los dos formularios es: solo cambia el título, el envío es el mismo.
    static func title(isFriend: Bool) -> String { isFriend ? "Transferir a un amigo" : "Transferir a una cuenta" }

    /// La pregunta de confirmación. Solo avisa del cambio de moneda cuando se SABE que difiere (`exchangeApplies` viene
    /// del dominio): con la moneda del destinatario desconocida no se afirma nada.
    static func confirmation(amount: String, source: String, recipientName: String, recipientCurrency: String?,
                             exchangeApplies: Bool) -> String {
        if exchangeApplies, let recipientCurrency, !recipientCurrency.trimmingCharacters(in: .whitespaces).isEmpty {
            return "¿Desea transferir \(amount) de la tarjeta \(source) a una tarjeta en "
                + "\(OperationPresentation.displayCurrency(recipientCurrency)) aplicando la tasa de cambio vigente?"
        }
        return "¿Está seguro que desea transferir \(amount) de la tarjeta \(source) a la cuenta de \(recipientName)?"
    }
}
