package org.andrew.enzona.feature.transfer

import kotlinx.datetime.LocalDate
import org.andrew.enzona.feature.transfer.domain.TransferFilter
import org.andrew.enzona.feature.transfer.domain.TransferStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TransferFilterTest {
    @Test
    fun buildsAFilterFromIsoDaysAndStatus() {
        val filter = TransferFilter.fromIso("2026-10-01", "2026-10-31", TransferStatus.ACCEPTED)
        assertEquals(TransferFilter(LocalDate(2026, 10, 1), LocalDate(2026, 10, 31), TransferStatus.ACCEPTED), filter)
    }

    @Test
    fun noValuesIsNoFilter() {
        assertEquals(TransferFilter.None, TransferFilter.fromIso(null, null, null))
    }

    @Test
    fun anUnreadableDayIsDroppedNotReinterpreted() {
        assertEquals(TransferFilter.None, TransferFilter.fromIso("01/10/2026", "2026-13-40", null))
    }

    @Test
    fun aStatusWithoutACodeCannotBeFilteredByAndIsDroppedInsteadOfCrashing() {
        // OTHER no tiene código: antes ningún número podía resolverse a él; ahora que se pasa el enum, se descarta.
        assertNull(TransferFilter.fromIso(null, null, TransferStatus.OTHER).status)
    }

    @Test
    fun everyFilterableStatusHasACodeAndOnlyThoseDo() {
        assertEquals(listOf(TransferStatus.ACCEPTED, TransferStatus.PENDING, TransferStatus.FAILED), TransferStatus.filterable)
        for (status in TransferStatus.entries) {
            assertEquals(status in TransferStatus.filterable, status.code != null, status.name)
        }
        for (status in TransferStatus.filterable) {
            assertEquals(status, TransferFilter.fromIso(null, null, status).status)
        }
    }
}
