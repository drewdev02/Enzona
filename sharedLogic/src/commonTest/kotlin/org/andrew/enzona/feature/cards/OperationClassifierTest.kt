package org.andrew.enzona.feature.cards

import org.andrew.enzona.feature.cards.data.parseOperationAction
import org.andrew.enzona.feature.cards.data.parseOperationChannel
import org.andrew.enzona.feature.cards.domain.OperationAction
import org.andrew.enzona.feature.cards.domain.OperationChannel
import org.andrew.enzona.feature.cards.domain.OperationDirection
import org.andrew.enzona.feature.cards.domain.OperationKind
import org.andrew.enzona.feature.cards.domain.direction
import kotlin.test.Test
import kotlin.test.assertEquals

/** Los casos se escriben con los textos del backend; pasan por los mismos parsers que usa el mapper. */
private fun classifyOperation(description: String, channel: String, action: String): OperationKind =
    org.andrew.enzona.feature.cards.domain.classifyOperation(
        description = description, channel = parseOperationChannel(channel), action = parseOperationAction(action),
    )

class OperationCodesTest {
    @Test
    fun parsesTheKnownActionsAndChannelsExactly() {
        assertEquals(OperationAction.Debit, parseOperationAction("DB"))
        assertEquals(OperationAction.Credit, parseOperationAction("CR"))
        assertEquals(OperationChannel.Reversal, parseOperationChannel("REV"))
        assertEquals(OperationChannel.Event, parseOperationChannel("EV"))
        assertEquals(OperationChannel.Pos, parseOperationChannel("POS"))
        assertEquals(OperationChannel.Enzona, parseOperationChannel("ENZ"))
    }

    @Test
    fun anythingElseKeepsItsRawTextInsteadOfBeingGuessed() {
        assertEquals(OperationAction.Other("db"), parseOperationAction("db"), "sensible a mayúsculas, como el original")
        assertEquals(OperationAction.Other(""), parseOperationAction(""))
        assertEquals(OperationAction.Other("XX"), parseOperationAction("XX"))
        assertEquals(OperationChannel.Other("ATM"), parseOperationChannel("ATM"))
        assertEquals(OperationChannel.Other("pos"), parseOperationChannel("pos"))
        assertEquals(OperationChannel.Other(""), parseOperationChannel(""))
    }

    @Test
    fun onlyAnExplicitDebitIsADebit() {
        assertEquals(OperationDirection.DEBIT, OperationAction.Debit.direction)
        assertEquals(OperationDirection.CREDIT, OperationAction.Credit.direction)
        assertEquals(OperationDirection.CREDIT, OperationAction.Other("").direction, "ausente: se muestra como crédito")
        assertEquals(OperationDirection.CREDIT, OperationAction.Other("db").direction)
    }
}

class OperationClassifierTest {

    @Test
    fun classifiesEmptyDescriptionByChannelAndAction() {
        // channel == "REV" -> REVERSAL
        assertEquals(OperationKind.REVERSAL, classifyOperation(description = "", channel = "REV", action = "DB"))
        assertEquals(OperationKind.REVERSAL, classifyOperation(description = "", channel = "REV", action = "CR"))

        // channel == "EV" && action == "CR" -> PAYROLL
        assertEquals(OperationKind.PAYROLL, classifyOperation(description = "", channel = "EV", action = "CR"))

        // action == "DB" && channel == "POS" -> POS_PAYMENT
        assertEquals(OperationKind.POS_PAYMENT, classifyOperation(description = "", channel = "POS", action = "DB"))

        // action == "DB" -> WITHDRAWAL
        assertEquals(OperationKind.WITHDRAWAL, classifyOperation(description = "", channel = "ATM", action = "DB"))
        assertEquals(OperationKind.WITHDRAWAL, classifyOperation(description = "", channel = "EV", action = "DB"))

        // Resto -> CREDIT
        assertEquals(OperationKind.CREDIT, classifyOperation(description = "", channel = "ATM", action = "CR"))
        assertEquals(OperationKind.CREDIT, classifyOperation(description = "", channel = "", action = "CR"))
        assertEquals(OperationKind.CREDIT, classifyOperation(description = "", channel = "POS", action = "CR"))
    }

    @Test
    fun classifiesTransferAndPrecedenceOverActivation() {
        assertEquals(OperationKind.TRANSFER, classifyOperation(description = "TRF", channel = "ENZ", action = "DB"))
        assertEquals(OperationKind.TRANSFER, classifyOperation(description = "TRAN", channel = "ATM", action = "CR"))

        // "TRF" gana independientemente del canal (incluso si channel == "ENZ")
        assertEquals(OperationKind.TRANSFER, classifyOperation(description = "TRF", channel = "ENZ", action = "DB"))
    }

    @Test
    fun classifiesCardActivation() {
        assertEquals(OperationKind.CARD_ACTIVATION, classifyOperation(description = "TR EZ", channel = "ENZ", action = "DB"))
        assertEquals(OperationKind.CARD_ACTIVATION, classifyOperation(description = "PAGO TR EZ NUEVA", channel = "ENZ", action = "CR"))

        // Si el canal no es ENZ no es activación; no tiene códigos de telefonía -> MERCHANT_PAYMENT
        assertEquals(OperationKind.MERCHANT_PAYMENT, classifyOperation(description = "TR EZ", channel = "POS", action = "DB"))
    }

    @Test
    fun classifiesServicesWithoutTelephonyCodesAsMerchantPayment() {
        // Códigos de servicio sin telefonía (GAS, Correos, CIMX, CAD, etc.) caen en MERCHANT_PAYMENT
        assertEquals(OperationKind.MERCHANT_PAYMENT, classifyOperation(description = "GAS", channel = "ENZ", action = "DB"))
        assertEquals(OperationKind.MERCHANT_PAYMENT, classifyOperation(description = "Correos de Cuba", channel = "ENZ", action = "DB"))
        assertEquals(OperationKind.MERCHANT_PAYMENT, classifyOperation(description = "CIMX", channel = "POS", action = "DB"))
        assertEquals(OperationKind.MERCHANT_PAYMENT, classifyOperation(description = "LUZ", channel = "ENZ", action = "DB"))
        assertEquals(OperationKind.MERCHANT_PAYMENT, classifyOperation(description = "ONAT", channel = "ENZ", action = "DB"))
        assertEquals(OperationKind.MERCHANT_PAYMENT, classifyOperation(description = "CAD", channel = "ENZ", action = "DB"))
        assertEquals(OperationKind.MERCHANT_PAYMENT, classifyOperation(description = "TIENDA_CENTRAL", channel = "ENZ", action = "DB"))
    }

    @Test
    fun cadIsClassifiedAsMerchantPayment() {
        assertEquals(OperationKind.MERCHANT_PAYMENT, classifyOperation(description = "CAD", channel = "ENZ", action = "DB"))
        assertEquals(OperationKind.MERCHANT_PAYMENT, classifyOperation(description = "CAD", channel = "POS", action = "CR"))
    }

    @Test
    fun classifiesTelephonyOperations() {
        // TEL -> ETECSA_PAYMENT
        assertEquals(OperationKind.ETECSA_PAYMENT, classifyOperation(description = "TEL", channel = "ENZ", action = "DB"))
        assertEquals(OperationKind.ETECSA_PAYMENT, classifyOperation(description = "FACTURA TEL", channel = "ENZ", action = "DB"))

        // MREC / MRG -> MICRO_TOPUP
        assertEquals(OperationKind.MICRO_TOPUP, classifyOperation(description = "MREC", channel = "ENZ", action = "DB"))
        assertEquals(OperationKind.MICRO_TOPUP, classifyOperation(description = "MRG", channel = "ENZ", action = "DB"))
        assertEquals(OperationKind.MICRO_TOPUP, classifyOperation(description = "RECARGA MREC 50", channel = "ENZ", action = "DB"))

        // RECA / RMV / MOVI exacto -> MOBILE_TOPUP
        assertEquals(OperationKind.MOBILE_TOPUP, classifyOperation(description = "RECA", channel = "ENZ", action = "DB"))
        assertEquals(OperationKind.MOBILE_TOPUP, classifyOperation(description = "PAGO RECARGA", channel = "ENZ", action = "DB"))
        assertEquals(OperationKind.MOBILE_TOPUP, classifyOperation(description = "RMV", channel = "ENZ", action = "DB"))
        assertEquals(OperationKind.MOBILE_TOPUP, classifyOperation(description = "RMV_CUBA", channel = "ENZ", action = "DB"))
        assertEquals(OperationKind.MOBILE_TOPUP, classifyOperation(description = "MOVI", channel = "ENZ", action = "DB"))

        // MOVI como substring pasa los 11 pero no cumple d == "MOVI" -> UNKNOWN
        assertEquals(OperationKind.UNKNOWN, classifyOperation(description = "xMOVIx", channel = "ENZ", action = "DB"))

        // NAU / RNT -> NAUTA_TOPUP
        assertEquals(OperationKind.NAUTA_TOPUP, classifyOperation(description = "NAU", channel = "ENZ", action = "DB"))
        assertEquals(OperationKind.NAUTA_TOPUP, classifyOperation(description = "NAUTA_HOGAR", channel = "ENZ", action = "DB"))
        assertEquals(OperationKind.NAUTA_TOPUP, classifyOperation(description = "RNT", channel = "ENZ", action = "DB"))

        // PCU / RPP / PROP -> OWN_TOPUP
        assertEquals(OperationKind.OWN_TOPUP, classifyOperation(description = "PCU", channel = "ENZ", action = "DB"))
        assertEquals(OperationKind.OWN_TOPUP, classifyOperation(description = "RPP", channel = "ENZ", action = "DB"))
        assertEquals(OperationKind.OWN_TOPUP, classifyOperation(description = "PROP", channel = "ENZ", action = "DB"))
        assertEquals(OperationKind.OWN_TOPUP, classifyOperation(description = "PROPIA_TARJETA", channel = "ENZ", action = "DB"))
    }

    @Test
    fun checksCaseSensitivity() {
        // En minúsculas no coincide con las constantes mayúsculas; al no tener códigos telefónicos cae en MERCHANT_PAYMENT
        assertEquals(OperationKind.MERCHANT_PAYMENT, classifyOperation(description = "trf", channel = "ENZ", action = "DB"))
        assertEquals(OperationKind.MERCHANT_PAYMENT, classifyOperation(description = "tel", channel = "ENZ", action = "DB"))
        assertEquals(OperationKind.MERCHANT_PAYMENT, classifyOperation(description = "rmv", channel = "ENZ", action = "DB"))
        assertEquals(OperationKind.MERCHANT_PAYMENT, classifyOperation(description = "movi", channel = "ENZ", action = "DB"))
    }
}
