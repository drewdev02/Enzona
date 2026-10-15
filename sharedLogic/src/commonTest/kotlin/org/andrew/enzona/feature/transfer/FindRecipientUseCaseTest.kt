package org.andrew.enzona.feature.transfer

import kotlinx.coroutines.test.runTest
import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.result.Failure
import org.andrew.enzona.feature.account.domain.AccountRepository
import org.andrew.enzona.feature.transfer.domain.FindRecipientResult
import org.andrew.enzona.feature.transfer.domain.FindRecipientUseCase
import org.andrew.enzona.feature.transfer.domain.InvalidQuery
import org.andrew.enzona.feature.transfer.domain.TransferRecipient
import org.andrew.enzona.testing.FakeAccounts
import org.andrew.enzona.testing.sessionOf
import org.andrew.enzona.testing.testAccount as account
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FindRecipientUseCaseTest {
    private fun useCase(accounts: AccountRepository, me: String = "me") = FindRecipientUseCase(accounts, sessionOf(me))

    @Test
    fun aFoundAccountBecomesARecipientBuiltFromTheServersObject() = runTest {
        val r = useCase(FakeAccounts { EzResult.Success(account()) })("alice")
        assertEquals(
            FindRecipientResult.Found(TransferRecipient("acc-uuid-1", "alice", "Alice Perez", "images/a.png")),
            r,
        )
    }

    @Test
    fun theAccountsCurrencyTravelsWithTheRecipientAndAbsentStaysUnknown() = runTest {
        val withCurrency = useCase(FakeAccounts { EzResult.Success(account(currency = "usd")) })("alice")
        assertEquals("usd", (withCurrency as FindRecipientResult.Found).recipient.currency)
        val without = useCase(FakeAccounts { EzResult.Success(account()) })("alice")
        assertEquals(null, (without as FindRecipientResult.Found).recipient.currency)
    }

    @Test
    fun theQueryIsTrimmedAndTheServerIsAskedForExactlyThat() = runTest {
        val accounts = FakeAccounts { EzResult.Success(account()) }
        useCase(accounts)("  alice \n")
        assertEquals(listOf("alice"), accounts.asked)
    }

    @Test
    fun anEmptyQueryIsInvalidAndNeverReachesTheServer() = runTest {
        val accounts = FakeAccounts { error("no debe llamarse") }
        assertEquals(FindRecipientResult.Invalid(InvalidQuery.EMPTY), useCase(accounts)(""))
        assertEquals(FindRecipientResult.Invalid(InvalidQuery.EMPTY), useCase(accounts)("   "))
        assertTrue(accounts.asked.isEmpty())
    }

    @Test
    fun whitespaceInsideTheQueryIsInvalid() = runTest {
        val accounts = FakeAccounts { error("no debe llamarse") }
        assertEquals(FindRecipientResult.Invalid(InvalidQuery.HAS_WHITESPACE), useCase(accounts)("ali ce"))
        assertEquals(FindRecipientResult.Invalid(InvalidQuery.HAS_WHITESPACE), useCase(accounts)("ali\tce"))
        assertTrue(accounts.asked.isEmpty())
    }

    @Test
    fun youCannotSearchYourselfEvenWithDifferentCase() = runTest {
        val accounts = FakeAccounts { error("no debe llamarse") }
        assertEquals(FindRecipientResult.Invalid(InvalidQuery.YOURSELF), useCase(accounts, me = "Andrew")("andrew"))
        assertEquals(FindRecipientResult.Invalid(InvalidQuery.YOURSELF), useCase(accounts, me = "andrew")(" ANDREW "))
        assertTrue(accounts.asked.isEmpty())
    }

    @Test
    fun a404MeansNotFoundButOtherFailuresAreReportedAsFailures() = runTest {
        assertEquals(
            FindRecipientResult.NotFound,
            useCase(FakeAccounts { EzResult.Error(Failure.Http(404)) })("nadie"),
        )
        val network = Failure.Network("sin red")
        assertEquals(FindRecipientResult.Failed(network), useCase(FakeAccounts { EzResult.Error(network) })("alice"))
        val server = Failure.Http(500)
        assertEquals(FindRecipientResult.Failed(server), useCase(FakeAccounts { EzResult.Error(server) })("alice"))
    }

    @Test
    fun anAccountThatIsNotTheOneAskedForIsNeverUsed() = runTest {
        // Un servidor que devolviera otra cuenta (búsqueda aproximada...) no debe convertirse en destinatario.
        val r = useCase(FakeAccounts { EzResult.Success(account(username = "alicia")) })("alice")
        assertEquals(FindRecipientResult.NotFound, r)
    }

    @Test
    fun theUsernameComparisonIgnoresCaseSoTheServersSpellingIsKept() = runTest {
        val r = useCase(FakeAccounts { EzResult.Success(account(username = "Alice")) })("alice")
        assertEquals("Alice", (r as FindRecipientResult.Found).recipient.username)
    }

    @Test
    fun anAccountWithoutUuidCannotReceiveTransfers() = runTest {
        val r = useCase(FakeAccounts { EzResult.Success(account(uuid = " ")) })("alice")
        assertIs<Failure.Parse>((r as FindRecipientResult.Failed).failure)
    }

    @Test
    fun theDisplayNameFallsBackToTheUsernameAndTheAvatarMayBeAbsent() = runTest {
        val r = useCase(FakeAccounts { EzResult.Success(account(first = "", last = "", avatar = null)) })("alice")
        val recipient = (r as FindRecipientResult.Found).recipient
        assertEquals("alice", recipient.displayName)
        assertEquals(null, recipient.avatar)
    }
}
