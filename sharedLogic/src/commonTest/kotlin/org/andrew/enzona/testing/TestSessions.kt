package org.andrew.enzona.testing

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import okio.ByteString.Companion.encodeUtf8
import org.andrew.enzona.core.storage.SecureStorage
import org.andrew.enzona.feature.account.domain.Account
import org.andrew.enzona.feature.auth.data.OAuthService
import org.andrew.enzona.feature.auth.data.TokenStore
import org.andrew.enzona.feature.auth.data.dto.TokenDto
import org.andrew.enzona.feature.auth.domain.SessionRepository

class MemoryStorage : SecureStorage {
    private val map = mutableMapOf<String, String>()
    override fun put(key: String, value: String) { map[key] = value }
    override fun get(key: String) = map[key]
    override fun remove(key: String) { map.remove(key) }
    override fun clear() = map.clear()
}

/** Sesión abierta del usuario [username] (el `sub` del id_token; la firma no se verifica en el cliente). */
fun sessionOf(username: String): SessionRepository {
    val idToken = "x." + """{"sub":"$username"}""".encodeUtf8().base64Url().trimEnd('=') + ".y"
    val store = TokenStore(MemoryStorage()).also { it.save(TokenDto(accessToken = "at", idToken = idToken)) }
    return SessionRepository(OAuthService(HttpClient(MockEngine { respond("{}", HttpStatusCode.OK) })), store)
}

/** Una cuenta inventada con los valores mínimos. */
fun testAccount(
    uuid: String = "acc-uuid-1",
    username: String = "alice",
    first: String = "Alice",
    last: String = "Perez",
    avatar: String? = "images/a.png",
) = Account(
    uuid = uuid, username = username, firstName = first, lastName = last, email = null, phone = null, avatarUrl = avatar,
    primaryCurrency = null, profileLevel = null, isVerified = false, isPhoneVerified = false,
    hasPaymentPassword = false, biometricEnabled = false, isMerchant = false, createdAt = null,
)
