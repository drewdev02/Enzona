package org.andrew.enzona.feature.cards.data

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import org.andrew.enzona.core.json.asCleanText
import org.andrew.enzona.core.money.Money
import org.andrew.enzona.core.money.parseMinorUnits
import org.andrew.enzona.core.network.enzonaJson
import org.andrew.enzona.feature.cards.domain.CardActivity
import org.andrew.enzona.feature.cards.domain.CardOperation
import org.andrew.enzona.feature.cards.domain.OperationAction
import org.andrew.enzona.feature.cards.domain.OperationChannel
import org.andrew.enzona.feature.cards.domain.classifyOperation
import org.andrew.enzona.feature.cards.domain.direction

/** `DB` / `CR`, sensibles a mayúsculas como en el original; cualquier otro valor conserva su texto. */
internal fun parseOperationAction(raw: String): OperationAction = when (raw) {
    "DB" -> OperationAction.Debit
    "CR" -> OperationAction.Credit
    else -> OperationAction.Other(raw)
}

/** `REV` / `EV` / `POS` / `ENZ`, sensibles a mayúsculas como en el original; cualquier otro valor conserva su texto. */
internal fun parseOperationChannel(raw: String): OperationChannel = when (raw) {
    "REV" -> OperationChannel.Reversal
    "EV" -> OperationChannel.Event
    "POS" -> OperationChannel.Pos
    "ENZ" -> OperationChannel.Enzona
    else -> OperationChannel.Other(raw)
}

/**
 * Transforma [CardActivityDto] al modelo de dominio [CardActivity].
 *
 * Si `operation` es un array JSON, decodifica cada elemento individualmente.
 * Los elementos que no sean objetos o que fallen al decodificarse cuentan como
 * operaciones omitidas. Las operaciones con importe ilegible también se omiten.
 * Si `operation` no es un array (es null, booleano, objeto, etc.), la lista de
 * operaciones es vacía y las operaciones omitidas son 0.
 *
 * Los saldos ilegibles se mapean a null sin invalidar la actividad completa.
 */
internal fun CardActivityDto.toDomain(): CardActivity {
    val rawCurrency = currency.asCleanText().orEmpty()

    val balanceMoney = parseMinorUnits(balance.asCleanText())?.let {
        Money(minorUnits = it, currency = rawCurrency)
    }
    val availableMoney = parseMinorUnits(balanceAvailable.asCleanText())?.let {
        Money(minorUnits = it, currency = rawCurrency)
    }

    val validOperations = mutableListOf<CardOperation>()
    var skippedCount = 0

    val operationElement = operation
    if (operationElement is JsonArray) {
        for (item in operationElement) {
            val op = if (item is JsonObject) {
                try {
                    enzonaJson.decodeFromJsonElement<OperationDto>(item)
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }

            if (op == null) {
                skippedCount++
                continue
            }

            val amountText = op.amount.asCleanText()
            val minorUnits = parseMinorUnits(amountText)
            if (minorUnits == null) {
                skippedCount++
                continue
            }

            val rawAlias = op.alias.asCleanText()
            val merchantAlias = if (rawAlias == null || rawAlias.isBlank() || rawAlias.equals("null", ignoreCase = true)) {
                null
            } else {
                rawAlias
            }

            val dateText = op.operationDate.asCleanText().orEmpty()
            // El texto del backend se interpreta aquí, una sola vez: el dominio recibe valores tipados.
            val action = parseOperationAction(op.action.asCleanText().orEmpty())
            val channel = parseOperationChannel(op.channel.asCleanText().orEmpty())
            val description = op.description.asCleanText().orEmpty()

            val direction = action.direction
            val kind = classifyOperation(description = description, channel = channel, action = action)
            val amountMoney = Money(minorUnits = minorUnits, currency = rawCurrency)

            validOperations.add(
                CardOperation(
                    kind = kind,
                    direction = direction,
                    amount = amountMoney,
                    merchantAlias = merchantAlias,
                    dateText = dateText,
                )
            )
        }
    }

    return CardActivity(
        balance = balanceMoney,
        available = availableMoney,
        currency = rawCurrency,
        operations = validOperations,
        skippedOperations = skippedCount,
    )
}
