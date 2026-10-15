package org.andrew.enzona.feature.friends

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.andrew.enzona.core.logging.LogLevel
import org.andrew.enzona.core.logging.withLogging
import org.andrew.enzona.core.network.TokenProvider
import org.andrew.enzona.core.network.createApiHttpClient
import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.result.Failure
import org.andrew.enzona.feature.friends.data.FriendsRepositoryImpl
import org.andrew.enzona.feature.friends.domain.Friend
import org.andrew.enzona.feature.transfer.domain.TransferRecipient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private object NoRefresh : TokenProvider {
    override fun accessToken() = "tok"
    override suspend fun refreshAfterUnauthorized(failedToken: String?) = false
}

class FriendsRepositoryTest {

    // Forma decodificada de `FriendModel` (uuid, username, name, avatar, phone, email, primaryCurrency,
    // account_uuid, contact_uuid, deleted...). Datos inventados.
    private val realShape = """
        {"contacts":[
          {"uuid":"f-1","username":"alice","name":"Alice Perez","avatar":"images/user/avatar/alice.png","phone":"53000001",
           "email":"a@example.com","primaryCurrency":"cup","account_uuid":"mine","contact_uuid":"c-1","deleted":false},
          {"uuid":"f-2","username":"bob","name":false,"avatar":false,"phone":false,"primaryCurrency":"usd","deleted":"0"}
        ]}
    """.trimIndent()

    private fun repo(
        status: HttpStatusCode = HttpStatusCode.OK,
        body: String = realShape,
        onRequest: (io.ktor.client.request.HttpRequestData) -> Unit = {},
    ) = FriendsRepositoryImpl(createApiHttpClient(NoRefresh, MockEngine { req ->
        onRequest(req)
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
    }))

    private suspend fun friends(r: FriendsRepositoryImpl): List<Friend> = (r.list("mine") as EzResult.Success).value

    @Test
    fun mapsTheDecodedShape() = runTest {
        val list = friends(repo())
        assertEquals(2, list.size)
        assertEquals(Friend("f-1", "alice", "Alice Perez", "images/user/avatar/alice.png", "53000001", "cup"), list[0])
        assertEquals(Friend("f-2", "bob", null, null, null, "usd"), list[1])
        assertEquals("bob", list[1].displayName, "sin nombre se muestra el usuario")
    }

    @Test
    fun requestsTheContactsOfTheGivenAccount() = runTest {
        var url: String? = null
        repo(onRequest = { url = it.url.toString() }).list("acc-123")
        assertEquals("https://api.enzona.net/contact/1.0/api/account/contacts/acc-123", url)
    }

    @Test
    fun deletedContactsAreNeverOfferedToTransferTo() = runTest {
        val body = """{"contacts":[
            {"uuid":"a","username":"a1","deleted":"1"},
            {"uuid":"b","username":"b1","deleted":true},
            {"uuid":"c","username":"c1","deleted":"true"},
            {"uuid":"d","username":"d1","deleted":false},
            {"uuid":"e","username":"e1"}]}""".trimIndent()
        assertEquals(listOf("d1", "e1"), friends(repo(body = body)).map { it.username })
    }

    @Test
    fun aContactWithoutUuidOrUsernameIsSkippedBecauseItCannotBeTheTarget() = runTest {
        val body = """{"contacts":[{"username":"sin-uuid"},{"uuid":"sin-usuario"},"x",5,{"uuid":"ok","username":"ok"}]}"""
        assertEquals(listOf("ok"), friends(repo(body = body)).map { it.uuid })
    }

    @Test
    fun anAbsentOrNonArrayListIsEmptyNotAFailure() = runTest {
        for (body in listOf("{}", """{"contacts":false}""", """{"contacts":null}""", """{"contacts":{}}""")) {
            assertTrue(friends(repo(body = body)).isEmpty(), body)
        }
    }

    @Test
    fun errorsCarryNoResponseBody() = runTest {
        assertEquals(Failure.Http(404), (repo(HttpStatusCode.NotFound, """{"x":"privado"}""").list("m") as EzResult.Error).failure)
        assertIs<Failure.Unknown>((repo(body = """{"fault":{"code":"1"}}""").list("m") as EzResult.Error).failure)
        assertIs<Failure.Parse>((repo(body = "[1,2]").list("m") as EzResult.Error).failure)
    }

    @Test
    fun aFriendBecomesTheRecipientBuiltFromItsOwnFields() {
        val friend = Friend("f-1", "alice", "Alice Perez", "images/a.png", null, null)
        assertEquals(TransferRecipient("f-1", "alice", "Alice Perez", "images/a.png"), friend.asRecipient())
        assertNull(Friend("f-2", "bob", null, null, null, null).asRecipient().avatar)
        assertNull(friend.asRecipient().currency, "sin moneda no se afirma ninguna")
        assertEquals("usd", Friend("f-3", "carol", null, null, null, "usd").asRecipient().currency)
    }

    @Test
    fun noPersonalDataReachesTheLogs() = runTest {
        val body = """{"contacts":[
            {"uuid":"u-SECRETO","username":"usuario-secreto","name":"Nombre Secreto","phone":"53999999"},
            {"username":"sin-uuid-secreto"}]}""".trimIndent()
        // Nivel Info = lo que escribe una build de release. A nivel Debug el logging de red incluye la RUTA (que
        // lleva el uuid de la cuenta) a propósito; en Info nunca (ver RequestLogging).
        withLogging(minimumLevel = LogLevel.INFO) { sink ->
            repo(body = body).list("cuenta-secreta")
            val written = sink.everythingWritten()
            assertTrue("omitidos" in written, "el test debe observar logs reales")
            for (secret in listOf("u-SECRETO", "usuario-secreto", "Nombre Secreto", "53999999", "sin-uuid-secreto", "cuenta-secreta")) {
                assertFalse(secret in written, "$secret apareció en los logs")
            }
        }
    }
}
