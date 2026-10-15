package org.andrew.enzona.feature.transfer

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import okio.ByteString.Companion.encodeUtf8
import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.result.Failure
import org.andrew.enzona.core.storage.SecureStorage
import org.andrew.enzona.feature.account.domain.Account
import org.andrew.enzona.feature.account.domain.AccountRepository
import org.andrew.enzona.feature.auth.data.OAuthService
import org.andrew.enzona.feature.auth.data.TokenStore
import org.andrew.enzona.feature.auth.data.dto.TokenDto
import org.andrew.enzona.feature.auth.domain.SessionRepository
import org.andrew.enzona.feature.transfer.domain.FindRecipientResult
import org.andrew.enzona.feature.transfer.domain.FindRecipientUseCase
import org.andrew.enzona.feature.transfer.domain.InvalidQuery
import org.andrew.enzona.feature.transfer.domain.TransferRecipient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private class Memory : SecureStorage {
    private val map = mutableMapOf<String, String>()
    override fun put(key: String, value: String) { map[key] = value }
    override fun get(key: String) = map[key]
    override fun remove(key: String) { map.remove(key) }
    override fun clear() = map.clear()
}

/** Sesión del usuario [username] (el `sub` del id_token). */
private fun sessionOf(username: String): SessionRepository {
    val idToken = "x." + """{"sub":"$username"}""".encodeUtf8().base64Url().trimEnd('=') + ".y"
    val store = TokenStore(Memory()).also { it.save(TokenDto(accessToken = "at", idToken = idToken)) }
    return SessionRepository(OAuthService(HttpClient(MockEngine { respond("{}", HttpStatusCode.OK) })), store)
}

private fun account(
    uuid: String = "acc-uuid-1", username: String = "alice", first: String = "Alice", last: String = "Perez", avatar: String? = "images/a.png",
) = Account(
    uuid = uuid, username = username, firstName = first, lastName = last, email = null, phone = null, avatarUrl = avatar,
    primaryCurrency = null, profileLevel = null, isVerified = false, isPhoneVerified = false,
    hasPaymentPassword = false, biometricEnabled = false, isMerchant = false, createdAt = null,
)

private class FakeAccounts(private val answer: (String) -> EzResult<Account>) : AccountRepository {
    val asked = mutableListOf<String>()
    override suspend fun getAccount(username: String): EzResult<Account> {
        asked += username
        return answer(username)
    }
}

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
