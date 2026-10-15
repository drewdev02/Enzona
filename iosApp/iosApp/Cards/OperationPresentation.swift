import Foundation

/// Presentación de una operación. Los textos y los iconos son de la UI (el dominio solo guarda hechos);
/// el rediseño de iOS puede cambiarlos sin tocar la lógica compartida. Mismos textos que la app original.
///
/// Sin dependencias del framework Kotlin: se verifica con un script (tools/check_assets.py comprueba que la
/// tabla de iconos cubre todos los `OperationKind` y coincide con la de Android) y se puede previsualizar.
/// Las claves son los nombres de `OperationKind` (Kotlin).
enum OperationPresentation {
    /// Tipo -> imageset. Debe coincidir con `OperationIcons.kt` (Android).
    static let icons: [String: String] = [
        "TRANSFER": "op_transfer",
        "REVERSAL": "op_transfer",
        "CARD_ACTIVATION": "op_ez",
        "MERCHANT_PAYMENT": "op_merchant",
        "UNKNOWN": "op_merchant",
        "POS_PAYMENT": "op_pos",
        "WITHDRAWAL": "op_withdrawal",
        "CREDIT": "op_payroll",
        "PAYROLL": "op_payroll",
        "ETECSA_PAYMENT": "op_mobile",
        "MICRO_TOPUP": "op_mobile",
        "MOBILE_TOPUP": "op_mobile",
        "NAUTA_TOPUP": "op_mobile",
        "OWN_TOPUP": "op_mobile",
    ]

    /// Un tipo nuevo del dominio que aún no tenga icono no rompe la pantalla (el verificador lo detecta).
    static func icon(kind: String) -> String { icons[kind] ?? "op_merchant" }

    /// Título de la fila. Un pago a comercio muestra el nombre del comercio si el backend lo trae.
    static func title(kind: String, merchantAlias: String?) -> String {
        switch kind {
        case "TRANSFER": return "Transferencia"
        case "CARD_ACTIVATION": return "Activación de Tarjeta"
        case "MERCHANT_PAYMENT": return merchantAlias ?? "Pago a comercio"
        case "POS_PAYMENT": return "Pago en POS"
        case "WITHDRAWAL": return "Extracción"
        case "CREDIT": return "Crédito"
        case "PAYROLL": return "Nómina"
        case "REVERSAL": return "Reverso"
        case "ETECSA_PAYMENT": return "Pago de servicio a ETECSA"
        case "MICRO_TOPUP": return "Micro Recarga Móvil"
        case "MOBILE_TOPUP": return "Recarga Móvil"
        case "NAUTA_TOPUP": return "Recarga Nauta"
        case "OWN_TOPUP": return "Recarga Propia"
        default: return "Operación"
        }
    }

    static func label(isDebit: Bool) -> String { isDebit ? "Débito" : "Crédito" }

    /// Signo que antepone el original al importe (que llega como magnitud).
    static func sign(isDebit: Bool) -> String { isDebit ? "-" : "+" }

    /// Unidades menores (centavos) a texto con dos decimales y sin separador de miles, como la app original
    /// (`$3395.15`). Trabaja con el texto de los dígitos: el dinero nunca pasa por `Double` y `abs(Int64.min)`
    /// desbordaría.
    static func formatMinorUnits(_ minorUnits: Int64) -> String {
        let negative = minorUnits < 0
        var digits = String(minorUnits)
        if negative { digits.removeFirst() }
        while digits.count < 3 { digits = "0" + digits }
        let text = String(digits.dropLast(2)) + "." + String(digits.suffix(2))
        return negative ? "-" + text : text
    }

    /// La moneda se muestra en mayúsculas aunque el backend la mande en minúsculas (`cup`).
    static func displayCurrency(_ currency: String) -> String {
        currency.trimmingCharacters(in: .whitespaces).uppercased()
    }

    /// Texto de la fila "Tarjeta": banco + últimos 4 dígitos ("BANMET 6856"). Un banco desconocido muestra su
    /// nombre real (el original mostraba "BANMET" para cualquier banco desconocido). `skinName` es `CardSkin.name`.
    static func cardDetailLabel(skinName: String, bankName: String, last4: String) -> String {
        let bank: String
        switch skinName {
        case "AIS": bank = "AIS"
        case "BANDEC", "BANDEC_USD": bank = "BANDEC"
        case "BANMET": bank = "BANMET"
        case "BICSA": bank = "BICSA"
        case "BPA": bank = "BPA"
        case "CLASICA": bank = "Clásica"
        default:
            let name = bankName.trimmingCharacters(in: .whitespaces)
            bank = name.isEmpty ? "Tarjeta" : name
        }
        return "\(bank) \(last4)"
    }
}
