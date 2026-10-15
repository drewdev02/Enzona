package org.andrew.enzona.ui.transfer

import org.andrew.enzona.core.money.Money
import org.andrew.enzona.feature.cards.domain.BankBrand
import org.andrew.enzona.feature.cards.domain.BankCard
import org.andrew.enzona.feature.cards.domain.bankBrandOf
import org.andrew.enzona.feature.transfer.domain.AmountProblem
import org.andrew.enzona.feature.transfer.domain.NotSentReason
import org.andrew.enzona.feature.transfer.domain.RejectionCause
import org.andrew.enzona.feature.transfer.domain.TransferOutcome
import org.andrew.enzona.feature.transfer.domain.TransferResultKind
import org.andrew.enzona.feature.transfer.domain.TransferStatus
import org.andrew.enzona.feature.transfer.domain.kind
import org.andrew.enzona.feature.transfer.presentation.OwnCardTransferUiState
import org.andrew.enzona.ui.cards.detailLabel
import org.andrew.enzona.ui.cards.displayCurrency
import org.andrew.enzona.ui.cards.formatMinorUnits

// Textos del formulario "A una de mis tarjetas". El dominio guarda hechos; las frases son de la UI y siguen las de la
// app original. Funciones puras: se prueban sin Compose.

/** `$10.50 CUP`: símbolo, dos decimales sin separador de miles y la moneda en mayúsculas. */
fun Money.shown(): String = "$" + formatMinorUnits(minorUnits) + " " + displayCurrency(currency)

fun AmountProblem.message(): String = when (this) {
    AmountProblem.EMPTY -> "Introduzca un monto válido"
    AmountProblem.HAS_WHITESPACE -> "El monto no puede contener espacios en blanco"
    AmountProblem.INVALID -> "Introduzca un monto válido"
    AmountProblem.ZERO -> "El monto no puede ser \$0.00"
}

/** La pregunta del diálogo de confirmación. Con monedas distintas el servidor aplica su tasa de cambio. */
fun confirmationText(amount: Money, source: BankCard, destination: BankCard, exchangeApplies: Boolean): String =
    if (exchangeApplies) {
        "¿Desea transferir ${amount.shown()} de la tarjeta ${source.detailLabel()} a una tarjeta en " +
            "${displayCurrency(destination.currency)} aplicando la tasa de cambio vigente?"
    } else {
        "¿Está seguro que desea transferir ${amount.shown()} de la tarjeta ${source.detailLabel()} " +
            "a la tarjeta ${destination.detailLabel()}?"
    }

/** Por qué no salió el intento de enviar (nada se envió: se puede corregir y volver a probar). */
fun NotSentReason.message(): String = when (this) {
    NotSentReason.PASSWORD_EMPTY -> "Escribe tu contraseña de pago."
    NotSentReason.PASSWORD_UNUSABLE -> "No se pudo proteger tu contraseña de pago. Inténtalo de nuevo."
    NotSentReason.ANOTHER_TRANSFER_IN_PROGRESS -> "Hay otra transferencia en curso. Espera a que termine."
    else -> "No se pudo enviar la transferencia. Revisa los datos."
}

/** Cómo se cuenta el desenlace al usuario. [kind] lo decide el dominio; aquí solo las palabras. [detail] puede traer texto del servidor. */
data class ResultText(val kind: TransferResultKind, val title: String, val detail: String?)

fun TransferOutcome.resultText(): ResultText = when (this) {
    is TransferOutcome.Accepted -> when (receipt.status) {
        TransferStatus.ACCEPTED -> ResultText(kind, "Transferencia confirmada", null)
        TransferStatus.PENDING -> ResultText(
            kind, "Transferencia pendiente",
            "El servicio la recibió y la está procesando. Revisa su estado en el historial.",
        )
        else -> ResultText(
            kind, "Transferencia recibida",
            "El servicio la aceptó" + (receipt.statusText?.let { " (estado: $it)" } ?: "") +
                ". Confírmala en el historial.",
        )
    }
    is TransferOutcome.Rejected -> ResultText(
        kind, "Transferencia fallida",
        when {
            cause == RejectionCause.SESSION_EXPIRED -> "Tu sesión expiró. Inicia sesión de nuevo. No se movió dinero."
            serverMessage != null -> "$serverMessage\nNo se movió dinero."
            httpStatus != null && cause == RejectionCause.REFUSED ->
                "El servicio no aceptó la transferencia (código $httpStatus). No se movió dinero."
            else -> "El servicio no pudo completar la transferencia. No se movió dinero."
        },
    )
    is TransferOutcome.Unknown -> ResultText(
        kind, "No sabemos si se realizó",
        "No repitas la transferencia hasta revisar el historial: puede que ya se haya hecho.",
    )
    // El formulario nunca llega aquí (vuelve a confirmar), pero el `when` es exhaustivo.
    is TransferOutcome.NotSent -> ResultText(kind, "No se envió", reason.message())
}

/**
 * El comprobante "Detalles de la transferencia" de la app original, ya resuelto: logo y texto del destino, estado,
 * importe, fecha, filas, la tarjeta de origen y el QR. Pura y sin Compose: se prueba como cualquier función.
 */
data class TransferDetail(
    val kind: TransferResultKind,
    val statusTitle: String,
    /** Aviso bajo el estado (pendiente, fallida, desconocida). La transferencia confirmada no lleva. */
    val note: String?,
    val mainText: String,
    val destinationBank: BankBrand,
    /** `10.50 CUP`: sin símbolo, como el comprobante original. */
    val amountText: String,
    val dateText: String?,
    val rows: List<Pair<String, String>>,
    val sourceBank: BankBrand,
    val sourceHolder: String,
    /** `(6856) CUP`. */
    val sourceSuffix: String,
    /** Lo que se dibuja como QR (la firma que emite el servidor), o null si no vino. */
    val qrContent: String?,
)

fun OwnCardTransferUiState.Finished.toDetail(): TransferDetail {
    val result = outcome.resultText()
    val receipt = (outcome as? TransferOutcome.Accepted)?.receipt
    val rows = buildList {
        receipt?.operationCode?.let { add("No.Operación" to it) }
        request.description.takeIf { it.isNotBlank() }?.let { add("Descripción" to it) }
    }
    return TransferDetail(
        kind = result.kind,
        statusTitle = result.title,
        note = result.detail,
        mainText = destination.detailLabel(),
        destinationBank = bankBrandOf(destination.bankCode),
        amountText = formatMinorUnits(request.amount.minorUnits) + " " + displayCurrency(request.amount.currency),
        dateText = receipt?.let { it.createdAt?.let(::formatWallClock) ?: it.createdAtText?.takeIf { text -> text.isNotBlank() } },
        rows = rows,
        sourceBank = bankBrandOf(source.bankCode),
        sourceHolder = source.holder,
        sourceSuffix = "(${source.last4}) ${displayCurrency(source.currency)}",
        qrContent = receipt?.signature,
    )
}
