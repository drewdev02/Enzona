import Foundation

/// Textos del formulario "A una de mis tarjetas". El dominio guarda hechos; las frases son de la UI y siguen las de la
/// app original y las de `OwnCardTransferPresentation.kt` (Android). Sin dependencias del framework Kotlin: se verifica
/// con `tools/check_transfer_presentation.sh`. Las claves son los nombres de los enums de Kotlin.
enum OwnCardPresentation {
    /// `$10.50 CUP`: símbolo, dos decimales sin separador de miles y la moneda en mayúsculas.
    static func moneyShown(minorUnits: Int64, currency: String) -> String {
        "$" + OperationPresentation.formatMinorUnits(minorUnits) + " " + OperationPresentation.displayCurrency(currency)
    }

    /// `10.50 CUP`: sin símbolo, como el comprobante original.
    static func amountPlain(minorUnits: Int64, currency: String) -> String {
        OperationPresentation.formatMinorUnits(minorUnits) + " " + OperationPresentation.displayCurrency(currency)
    }

    /// `(6856) CUP`: la línea de la tarjeta de origen del comprobante.
    static func sourceSuffix(last4: String, currency: String) -> String {
        "(\(last4)) \(OperationPresentation.displayCurrency(currency))"
    }

    /// `AmountProblem`.
    static func amountProblem(_ name: String) -> String {
        switch name {
        case "HAS_WHITESPACE": return "El monto no puede contener espacios en blanco"
        case "ZERO": return "El monto no puede ser $0.00"
        default: return "Introduzca un monto válido"
        }
    }

    /// La pregunta de confirmación. Con monedas distintas el servidor aplica su tasa de cambio.
    static func confirmation(amount: String, source: String, destination: String, destinationCurrency: String,
                             exchangeApplies: Bool) -> String {
        if exchangeApplies {
            return "¿Desea transferir \(amount) de la tarjeta \(source) a una tarjeta en "
                + "\(OperationPresentation.displayCurrency(destinationCurrency)) aplicando la tasa de cambio vigente?"
        }
        return "¿Está seguro que desea transferir \(amount) de la tarjeta \(source) a la tarjeta \(destination)?"
    }

    /// `NotSentReason`: por qué no salió el intento (nada se envió: se puede corregir y volver a probar).
    static func notSent(_ name: String) -> String {
        switch name {
        case "PASSWORD_EMPTY": return "Escribe tu contraseña de pago."
        case "PASSWORD_UNUSABLE": return "No se pudo proteger tu contraseña de pago. Inténtalo de nuevo."
        case "ANOTHER_TRANSFER_IN_PROGRESS": return "Hay otra transferencia en curso. Espera a que termine."
        default: return "No se pudo enviar la transferencia. Revisa los datos."
        }
    }

    enum ResultKind { case success, pending, failed, unknown }

    struct Result: Equatable {
        let kind: ResultKind
        let title: String
        let detail: String?
    }

    /// Cómo se cuenta el desenlace al usuario. `outcome` es `ACCEPTED`, `REJECTED`, `UNKNOWN` o `NOT_SENT`;
    /// `receiptStatus` es el `TransferStatus` de un recibo; `cause` es el `RejectionCause` o el `NotSentReason`.
    static func result(outcome: String, receiptStatus: String?, statusText: String?, cause: String?, httpStatus: Int?,
                       serverMessage: String?) -> Result {
        switch outcome {
        case "ACCEPTED":
            switch receiptStatus {
            case "ACCEPTED":
                return Result(kind: .success, title: "Transferencia confirmada", detail: nil)
            case "PENDING":
                return Result(kind: .pending, title: "Transferencia pendiente",
                              detail: "El servicio la recibió y la está procesando. Revisa su estado en el historial.")
            default:
                let state = statusText.map { " (estado: \($0))" } ?? ""
                return Result(kind: .pending, title: "Transferencia recibida",
                              detail: "El servicio la aceptó\(state). Confírmala en el historial.")
            }
        case "REJECTED":
            let detail: String
            if cause == "SESSION_EXPIRED" {
                detail = "Tu sesión expiró. Inicia sesión de nuevo. No se movió dinero."
            } else if let serverMessage {
                detail = "\(serverMessage)\nNo se movió dinero."
            } else if cause == "REFUSED", let httpStatus {
                detail = "El servicio no aceptó la transferencia (código \(httpStatus)). No se movió dinero."
            } else {
                detail = "El servicio no pudo completar la transferencia. No se movió dinero."
            }
            return Result(kind: .failed, title: "Transferencia fallida", detail: detail)
        case "UNKNOWN":
            return Result(kind: .unknown, title: "No sabemos si se realizó",
                          detail: "No repitas la transferencia hasta revisar el historial: puede que ya se haya hecho.")
        default:
            return Result(kind: .failed, title: "No se envió", detail: notSent(cause ?? ""))
        }
    }

    struct Row: Equatable {
        let label: String
        let value: String
    }

    /// Las filas del comprobante: el número de operación (si el servidor lo dio) y la descripción (si hay).
    static func rows(operationCode: String?, description: String) -> [Row] {
        var rows: [Row] = []
        if let operationCode, !operationCode.isEmpty { rows.append(Row(label: "No.Operación", value: operationCode)) }
        if !description.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            rows.append(Row(label: "Descripción", value: description))
        }
        return rows
    }

    /// La fecha del comprobante: la hora de reloj del servidor formateada; si no se pudo leer, el texto recibido;
    /// y sin ninguno, nada.
    static func dateText(wallClock: (year: Int, month: Int, day: Int, hour: Int, minute: Int)?, receivedText: String?) -> String? {
        if let c = wallClock {
            return TransferPresentation.wallClock(year: c.year, month: c.month, day: c.day, hour: c.hour, minute: c.minute)
        }
        if let receivedText, !receivedText.trimmingCharacters(in: .whitespaces).isEmpty { return receivedText }
        return nil
    }
}
