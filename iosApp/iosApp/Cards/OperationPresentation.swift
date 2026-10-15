import Foundation

/// Presentación de una operación. Los textos y los iconos son de la UI (el dominio solo guarda hechos);
/// el rediseño de iOS puede cambiarlo sin tocar la lógica compartida. Mismos textos que la app original.
///
/// Sin dependencias del framework Kotlin: se verifica con un script (tools/check_assets.py comprueba que la
/// tabla de iconos cubre todos los `OperationKind` y coincide con la de Android) y se puede previsualizar.
/// Los tipos son los espejos de `EzKinds.swift`: ningún `switch` compara nombres en texto.
enum OperationPresentation {
    /// Tipo -> imageset. Debe coincidir con `OperationIcons.kt` (Android).
    static let icons: [EzOperationKind: String] = [
        .transfer: "op_transfer",
        .reversal: "op_transfer",
        .cardActivation: "op_ez",
        .merchantPayment: "op_merchant",
        .unknown: "op_merchant",
        .posPayment: "op_pos",
        .withdrawal: "op_withdrawal",
        .credit: "op_payroll",
        .payroll: "op_payroll",
        .etecsaPayment: "op_mobile",
        .microTopup: "op_mobile",
        .mobileTopup: "op_mobile",
        .nautaTopup: "op_mobile",
        .ownTopup: "op_mobile",
    ]

    /// El verificador garantiza que la tabla cubre todos los tipos; el valor por defecto solo evita romper la pantalla.
    static func icon(kind: EzOperationKind) -> String { icons[kind] ?? "op_merchant" }

    /// Título de la fila. Un pago a comercio muestra el nombre del comercio si el backend lo trae.
    static func title(kind: EzOperationKind, merchantAlias: String?) -> String {
        switch kind {
        case .transfer: return "Transferencia"
        case .cardActivation: return "Activación de Tarjeta"
        case .merchantPayment: return merchantAlias ?? "Pago a comercio"
        case .posPayment: return "Pago en POS"
        case .withdrawal: return "Extracción"
        case .credit: return "Crédito"
        case .payroll: return "Nómina"
        case .reversal: return "Reverso"
        case .etecsaPayment: return "Pago de servicio a ETECSA"
        case .microTopup: return "Micro Recarga Móvil"
        case .mobileTopup: return "Recarga Móvil"
        case .nautaTopup: return "Recarga Nauta"
        case .ownTopup: return "Recarga Propia"
        case .unknown: return "Operación"
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
    /// nombre real (el original mostraba "BANMET" para cualquier banco desconocido).
    static func cardDetailLabel(skin: EzCardSkin, bankName: String, last4: String) -> String {
        let bank: String
        switch skin {
        case .ais: bank = "AIS"
        case .bandec, .bandecTropical: bank = "BANDEC"
        case .banmet: bank = "BANMET"
        case .bicsa: bank = "BICSA"
        case .bpa: bank = "BPA"
        case .clasica: bank = "Clásica"
        case .generic:
            let name = bankName.trimmingCharacters(in: .whitespaces)
            bank = name.isEmpty ? "Tarjeta" : name
        }
        return "\(bank) \(last4)"
    }
}
