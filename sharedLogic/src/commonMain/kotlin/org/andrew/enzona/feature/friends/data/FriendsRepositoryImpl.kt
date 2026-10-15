package org.andrew.enzona.feature.friends.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.serialization.json.JsonObject
import org.andrew.enzona.core.json.asArrayOrEmpty
import org.andrew.enzona.core.json.flag
import org.andrew.enzona.core.json.hasFault
import org.andrew.enzona.core.json.text
import org.andrew.enzona.core.logging.logger
import org.andrew.enzona.core.network.apiCall
import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.result.Failure
import org.andrew.enzona.feature.friends.domain.Friend
import org.andrew.enzona.feature.friends.domain.FriendsRepository

/**
 * `GET /contact/1.0/api/account/contacts/{accountUuid}` → `{"contacts":[…]}`. Solo lectura.
 * Decodificado de la app original; la ruta exacta (el uuid de la cuenta al final) y la forma de la lista
 * no se han validado contra el backend real.
 */
class FriendsRepositoryImpl(private val client: HttpClient) : FriendsRepository {
    private val log = logger<FriendsRepositoryImpl>()

    override suspend fun list(accountUuid: String): EzResult<List<Friend>> = apiCall {
        val response = client.get("/contact/1.0/api/account/contacts/$accountUuid")
        if (!response.status.isSuccess()) {
            // Sin el cuerpo: puede traer datos personales.
            return@apiCall EzResult.Error(Failure.Http(response.status.value))
        }
        val root = response.body<JsonObject>()
        if (root["fault"].hasFault()) {
            return@apiCall EzResult.Error(Failure.Unknown("El servicio respondió con un error."))
        }
        var skipped = 0
        val friends = root["contacts"].asArrayOrEmpty().mapNotNull { element ->
            val obj = element as? JsonObject
            // Un contacto eliminado no debe ofrecerse para transferir.
            if (obj.flag("deleted")) return@mapNotNull null
            val uuid = obj.text("uuid")
            val username = obj.text("username")
            if (obj == null || uuid == null || username == null) {
                skipped++
                return@mapNotNull null
            }
            Friend(
                uuid = uuid,
                username = username,
                name = obj.text("name"),
                avatar = obj.text("avatar"),
                phone = obj.text("phone"),
                primaryCurrency = obj.text("primaryCurrency"),
            )
        }
        if (skipped > 0) log.warn("Contactos ilegibles omitidos: {Omitidos}", skipped)
        EzResult.Success(friends)
    }
}
