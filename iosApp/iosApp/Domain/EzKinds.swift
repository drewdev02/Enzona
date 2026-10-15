import Foundation

/// Espejos en Swift de los enums del dominio (Kotlin), SIN dependencias del framework: así la presentación se verifica
/// con `swiftc` y se previsualiza. Cada valor Kotlin se convierte a su espejo UNA vez, en `EzKinds+Kotlin.swift`, y a
/// partir de ahí todo es tipado: ningún `switch` compara nombres en texto.
///
/// El valor crudo de cada caso es el nombre EXACTO de la entrada en Kotlin. `tools/check_enum_parity.py` comprueba que
/// cada espejo tiene las mismas entradas que su enum Kotlin; si el dominio añade una, el script falla en vez de caer
/// en silencio en `fallback`.
protocol KotlinNamed: RawRepresentable, CaseIterable where RawValue == String {
    /// Lo que se usa si llega un nombre que no conocemos (solo posible si el script de paridad no se ejecutó).
    static var fallback: Self { get }
}

extension KotlinNamed {
    init(kotlinName: String) {
        if let match = Self(rawValue: kotlinName) {
            self = match
        } else {
            assertionFailure("El enum Kotlin tiene una entrada que el espejo Swift \(Self.self) no conoce: \(kotlinName)")
            self = Self.fallback
        }
    }
}

enum EzTransferDirection: String, KotlinNamed {
    case outgoing = "OUTGOING"
    case incoming = "INCOMING"
    case unknown = "UNKNOWN"
    static var fallback: Self { .unknown }
}

enum EzTransferStatus: String, KotlinNamed {
    case accepted = "ACCEPTED"
    case pending = "PENDING"
    case failed = "FAILED"
    case other = "OTHER"
    static var fallback: Self { .other }
}

enum EzBankBrand: String, KotlinNamed {
    case banmet = "BANMET"
    case bandec = "BANDEC"
    case bpa = "BPA"
    case bicsa = "BICSA"
    case clasica = "CLASICA"
    case ais = "AIS"
    case unknown = "UNKNOWN"
    static var fallback: Self { .unknown }
}

enum EzOperationKind: String, KotlinNamed {
    case transfer = "TRANSFER"
    case cardActivation = "CARD_ACTIVATION"
    case merchantPayment = "MERCHANT_PAYMENT"
    case posPayment = "POS_PAYMENT"
    case withdrawal = "WITHDRAWAL"
    case credit = "CREDIT"
    case payroll = "PAYROLL"
    case reversal = "REVERSAL"
    case etecsaPayment = "ETECSA_PAYMENT"
    case microTopup = "MICRO_TOPUP"
    case mobileTopup = "MOBILE_TOPUP"
    case nautaTopup = "NAUTA_TOPUP"
    case ownTopup = "OWN_TOPUP"
    case unknown = "UNKNOWN"
    static var fallback: Self { .unknown }
}

enum EzCardSkin: String, KotlinNamed {
    case ais = "AIS"
    case bandec = "BANDEC"
    case bandecTropical = "BANDEC_TROPICAL"
    case banmet = "BANMET"
    case bicsa = "BICSA"
    case bpa = "BPA"
    case clasica = "CLASICA"
    case generic = "GENERIC"
    static var fallback: Self { .generic }
}

/// Cómo se clasifica el resultado de una transferencia (`TransferResultKind`). Lo decide el dominio.
enum EzResultKind: String, KotlinNamed {
    case success = "SUCCESS"
    case pending = "PENDING"
    case failed = "FAILED"
    case unknown = "UNKNOWN"
    case notSent = "NOT_SENT"
    static var fallback: Self { .unknown }
}

enum EzRejectionCause: String, KotlinNamed {
    case sessionExpired = "SESSION_EXPIRED"
    case refused = "REFUSED"
    case failedByServer = "FAILED_BY_SERVER"
    static var fallback: Self { .failedByServer }
}

enum EzNotSentReason: String, KotlinNamed {
    case nonPositiveAmount = "NON_POSITIVE_AMOUNT"
    case missingCurrency = "MISSING_CURRENCY"
    case missingSourceCard = "MISSING_SOURCE_CARD"
    case missingDestination = "MISSING_DESTINATION"
    case sameCard = "SAME_CARD"
    case passwordEmpty = "PASSWORD_EMPTY"
    case passwordUnusable = "PASSWORD_UNUSABLE"
    case anotherTransferInProgress = "ANOTHER_TRANSFER_IN_PROGRESS"
    static var fallback: Self { .nonPositiveAmount }
}

enum EzAmountProblem: String, KotlinNamed {
    case empty = "EMPTY"
    case hasWhitespace = "HAS_WHITESPACE"
    case invalid = "INVALID"
    case zero = "ZERO"
    static var fallback: Self { .invalid }
}

enum EzInvalidQuery: String, KotlinNamed {
    case empty = "EMPTY"
    case hasWhitespace = "HAS_WHITESPACE"
    case yourself = "YOURSELF"
    static var fallback: Self { .empty }
}

/// El desenlace de intentar una transferencia, como lo necesita la presentación (espejo de `TransferOutcome`).
enum EzOutcome: Equatable {
    case accepted(status: EzTransferStatus, statusText: String?)
    case rejected(cause: EzRejectionCause, httpStatus: Int?, serverMessage: String?)
    case unknown
    case notSent(EzNotSentReason)
}
