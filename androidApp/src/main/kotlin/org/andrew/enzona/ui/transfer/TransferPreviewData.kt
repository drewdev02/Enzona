package org.andrew.enzona.ui.transfer

import kotlinx.datetime.LocalDateTime
import org.andrew.enzona.core.money.Money
import org.andrew.enzona.feature.friends.domain.Friend
import org.andrew.enzona.feature.transfer.domain.TransferDirection
import org.andrew.enzona.feature.transfer.domain.TransferParty
import org.andrew.enzona.feature.transfer.domain.TransferRecipient
import org.andrew.enzona.feature.transfer.domain.TransferStatus
import org.andrew.enzona.feature.transfer.domain.TransferSummary

/** Datos INVENTADOS para previews: no tocan Koin ni la red. */
object TransferPreviewData {
    private fun card(bank: String, last4: String) = TransferParty(null, null, bank, last4, null)

    private fun transfer(
        id: String, direction: TransferDirection, minor: Long, status: TransferStatus,
        source: TransferParty, recipient: TransferParty, at: LocalDateTime, statusText: String? = null,
    ) = TransferSummary(id, direction, Money(minor, "CUP"), status, statusText, source, recipient, null, at, null)

    val transfers = listOf(
        transfer("1", TransferDirection.OUTGOING, 140_000, TransferStatus.ACCEPTED, card("BANMET", "6856"), card("BPA", "1234"), LocalDateTime(2026, 10, 14, 16, 30)),
        transfer("2", TransferDirection.INCOMING, 50_000, TransferStatus.ACCEPTED, card("BANDEC", "9021"), card("BANMET", "6856"), LocalDateTime(2026, 10, 12, 9, 5)),
        transfer("3", TransferDirection.OUTGOING, 25_000, TransferStatus.PENDING, card("BANMET", "6856"), card("BICSA", "4410"), LocalDateTime(2026, 10, 10, 0, 15)),
        transfer("4", TransferDirection.OUTGOING, 1_200_000, TransferStatus.FAILED, card("BANMET", "6856"), card("CLASICA", "7733"), LocalDateTime(2026, 10, 9, 12, 0)),
        transfer("5", TransferDirection.OUTGOING, 9_950, TransferStatus.OTHER, card("BANMET", "6856"), card("AIS", "5502"), LocalDateTime(2026, 10, 8, 18, 45), statusText = "Reversada"),
        transfer("6", TransferDirection.OUTGOING, 300_000, TransferStatus.ACCEPTED, card("BANMET", "6856"), TransferParty("Ana Pérez", "ana.perez", null, null, null), LocalDateTime(2026, 10, 7, 11, 20)),
    )

    val recipient = TransferRecipient("uuid-ana", "ana.perez", "Ana Pérez Gómez", null)

    val friends = listOf(
        Friend("u1", "ana.perez", "Ana Pérez Gómez", null, null, "CUP"),
        Friend("u2", "carlos87", null, null, null, "CUP"),
        Friend("u3", "maria.lopez", "María López", null, null, null),
    )
}
