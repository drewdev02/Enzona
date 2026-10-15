package org.andrew.enzona.feature.transfer

import org.andrew.enzona.feature.transfer.domain.NotSentReason
import org.andrew.enzona.feature.transfer.domain.RejectionCause
import org.andrew.enzona.feature.transfer.domain.TransferOutcome
import org.andrew.enzona.feature.transfer.domain.TransferReceipt
import org.andrew.enzona.feature.transfer.domain.TransferResultKind
import org.andrew.enzona.feature.transfer.domain.TransferStatus
import org.andrew.enzona.feature.transfer.domain.UnknownCause
import org.andrew.enzona.feature.transfer.domain.kind
import kotlin.test.Test
import kotlin.test.assertEquals

class TransferResultKindTest {
    private fun accepted(status: TransferStatus) =
        TransferOutcome.Accepted(TransferReceipt("EZ-1", status, null, null, null, null))

    @Test
    fun onlyAnAcceptedStatusIsASuccess() {
        assertEquals(TransferResultKind.SUCCESS, accepted(TransferStatus.ACCEPTED).kind)
    }

    @Test
    fun anyOtherAcceptedStatusIsPendingNeverASuccess() {
        assertEquals(TransferResultKind.PENDING, accepted(TransferStatus.PENDING).kind)
        assertEquals(TransferResultKind.PENDING, accepted(TransferStatus.OTHER).kind)
        // El servidor respondió "bien" con estado fallida: eso es un rechazo (lo decide el mapper), pero si llegara
        // aquí como recibo, tampoco se dice "confirmada".
        assertEquals(TransferResultKind.PENDING, accepted(TransferStatus.FAILED).kind)
    }

    @Test
    fun rejectionsFailUnknownsStayUnknownAndNotSentIsItsOwnKind() {
        for (cause in RejectionCause.entries) {
            assertEquals(TransferResultKind.FAILED, TransferOutcome.Rejected(cause, 400, null).kind)
        }
        for (cause in UnknownCause.entries) {
            assertEquals(TransferResultKind.UNKNOWN, TransferOutcome.Unknown(cause).kind)
        }
        for (reason in NotSentReason.entries) {
            assertEquals(TransferResultKind.NOT_SENT, TransferOutcome.NotSent(reason).kind)
        }
    }
}
