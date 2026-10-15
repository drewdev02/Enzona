package org.andrew.enzona.feature.transfer

import kotlinx.datetime.LocalDate
import org.andrew.enzona.feature.transfer.domain.TransferFilter
import org.andrew.enzona.feature.transfer.domain.TransferStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class TransferFilterTest {
    @Test
    fun buildsAFilterFromIsoDaysAndStatusCode() {
        val filter = TransferFilter.fromIso("2026-10-01", "2026-10-31", 2222)
        assertEquals(TransferFilter(LocalDate(2026, 10, 1), LocalDate(2026, 10, 31), TransferStatus.ACCEPTED), filter)
    }

    @Test
    fun noValuesIsNoFilter() {
        assertEquals(TransferFilter.None, TransferFilter.fromIso(null, null, null))
    }

    @Test
    fun anUnreadableDayOrUnknownCodeIsDroppedNotReinterpreted() {
        assertEquals(TransferFilter.None, TransferFilter.fromIso("01/10/2026", "2026-13-40", 9999))
        // OTHER no tiene código: ningún número puede resolverse a él.
        assertEquals(null, TransferFilter.fromIso(null, null, 0).status)
    }
}
