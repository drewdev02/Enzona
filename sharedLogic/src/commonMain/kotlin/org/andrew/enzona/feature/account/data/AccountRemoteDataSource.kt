package org.andrew.enzona.feature.account.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import org.andrew.enzona.core.network.apiCall
import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.result.Failure
import org.andrew.enzona.feature.account.domain.Account
import org.andrew.enzona.feature.account.domain.AccountRepository

/** GET /account/v1.0.0/account?username=<sub> (verificado en vivo, HTTP 200). */
class AccountRepositoryImpl(private val client: HttpClient) : AccountRepository {

    override suspend fun getAccount(username: String): EzResult<Account> = apiCall {
        val response = client.get("/account/v1.0.0/account") { parameter("username", username) }
        if (!response.status.isSuccess()) {
            // No se incluye el cuerpo en el error: puede contener datos personales.
            return@apiCall EzResult.Error(Failure.Http(response.status.value))
        }
        val dto = response.body<AccountEnvelopeDto>().account
            ?: return@apiCall EzResult.Error(Failure.Parse("Respuesta sin objeto `account`."))
        dto.toDomain()
    }
}
