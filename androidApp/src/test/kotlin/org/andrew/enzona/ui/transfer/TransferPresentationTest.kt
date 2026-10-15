package org.andrew.enzona.ui.transfer

import kotlinx.datetime.LocalDateTime
import org.andrew.enzona.core.money.Money
import org.andrew.enzona.feature.transfer.domain.TransferDirection
import org.andrew.enzona.feature.transfer.domain.TransferParty
import org.andrew.enzona.feature.transfer.domain.TransferStatus
import org.andrew.enzona.feature.transfer.domain.TransferSummary
import org.andrew.enzona.ui.designsystem.components.EzTone
import kotlin.test.Test
import kotlin.test.assertEquals

class TransferPresentationTest {
    private val none = TransferParty(null, null, null, null, null)
    private fun summary(
        direction: TransferDirection = TransferDirection.OUTGOING,
        minor: Long = 140_000,
        status: TransferStatus = TransferStatus.ACCEPTED,
        statusText: String? = null,
        createdAt: LocalDateTime? = null,
        createdAtText: String? = null,
        source: TransferParty = none,
        recipient: TransferParty = none,
    ) = TransferSummary("1", direction, Money(minor, "cup"), status, statusText, source, recipient, null, createdAt, createdAtText)

    @Test
    fun partyLabelIsBankWithLastDigits() {
        assertEquals("BANMET(6856)", TransferParty(null, null, "BANMET", "6856", null).label())
    }

    @Test
    fun partyLabelFallsBackWithoutInventingData() {
        assertEquals("Tarjeta (6856)", TransferParty(null, null, null, "6856", null).label())
        assertEquals("ana", TransferParty(null, "ana", "BPA", null, null).label())
        assertEquals("Ana Pérez", TransferParty("Ana Pérez", "ana", null, null, null).label())
        assertEquals("BPA", TransferParty(null, null, "BPA", null, null).label())
        assertEquals("—", none.label())
    }

    @Test
    fun amountSignComesFromDirection() {
        assertEquals("-1400.00", summary(TransferDirection.OUTGOING).amountText())
        assertEquals("+1400.00", summary(TransferDirection.INCOMING).amountText())
        assertEquals("1400.00", summary(TransferDirection.UNKNOWN).amountText())
        assertEquals("CUP", summary().currencyText())
    }

    @Test
    fun wallClockUsesTwelveHourFormat() {
        assertEquals("05/03/2026 12:07 AM", formatWallClock(LocalDateTime(2026, 3, 5, 0, 7)))
        assertEquals("05/03/2026 11:59 AM", formatWallClock(LocalDateTime(2026, 3, 5, 11, 59)))
        assertEquals("05/03/2026 12:00 PM", formatWallClock(LocalDateTime(2026, 3, 5, 12, 0)))
        assertEquals("14/10/2026 04:30 PM", formatWallClock(LocalDateTime(2026, 10, 14, 16, 30)))
    }

    @Test
    fun unreadableDateKeepsTheReceivedText() {
        assertEquals("ayer", summary(createdAtText = "ayer").dateText())
        assertEquals("", summary().dateText())
    }

    @Test
    fun statusLabelsAndTones() {
        assertEquals("Aceptada" to EzTone.Success, summary(status = TransferStatus.ACCEPTED).let { it.statusLabel() to it.statusTone() })
        assertEquals("Pendiente" to EzTone.Warning, summary(status = TransferStatus.PENDING).let { it.statusLabel() to it.statusTone() })
        assertEquals("Fallida" to EzTone.Error, summary(status = TransferStatus.FAILED).let { it.statusLabel() to it.statusTone() })
        assertEquals("Reversada" to EzTone.Neutral, summary(status = TransferStatus.OTHER, statusText = "Reversada").let { it.statusLabel() to it.statusTone() })
        assertEquals("Sin estado", summary(status = TransferStatus.OTHER).statusLabel())
    }

    @Test
    fun initialsTakeTwoWords() {
        assertEquals("AP", initialsOf("ana pérez gómez"))
        assertEquals("J", initialsOf("juan"))
        assertEquals("?", initialsOf("  "))
    }
}
