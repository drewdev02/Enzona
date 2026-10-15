package org.andrew.enzona.feature.transfer.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import kotlinx.serialization.json.JsonObject
import org.andrew.enzona.core.json.asArrayOrEmpty
import org.andrew.enzona.core.json.hasFault
import org.andrew.enzona.core.logging.logger
import org.andrew.enzona.core.network.apiCall
import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.result.Failure
import org.andrew.enzona.feature.transfer.domain.TransferFilter
import org.andrew.enzona.feature.transfer.domain.TransferPage
import org.andrew.enzona.feature.transfer.domain.TransferRepository

/**
 * `GET /transfer/v1.0.0/transfer?limit=&offset=` (+ `status_filter`, `start_date`, `end_date`). Solo lectura.
 * Decodificado de la app original; sin validar contra el backend real.
 */
class TransferRepositoryImpl(private val client: HttpClient) : TransferRepository {
    private val log = logger<TransferRepositoryImpl>()

    override suspend fun list(offset: Int, limit: Int, filter: TransferFilter): EzResult<TransferPage> = apiCall {
        val response = client.get("/transfer/v1.0.0/transfer") {
            parameter("limit", limit)
            parameter("offset", offset)
            filter.status?.code?.let { parameter("status_filter", it) }
            filter.startDate?.let { parameter("start_date", it.toString()) }
            filter.endDate?.let { parameter("end_date", it.toString()) }
        }
        if (!response.status.isSuccess()) {
            // Sin el cuerpo: puede traer datos personales.
            return@apiCall EzResult.Error(Failure.Http(response.status.value))
        }
        val root = response.body<JsonObject>()
        if (root["fault"].hasFault()) {
            return@apiCall EzResult.Error(Failure.Unknown("El servicio respondió con un error."))
        }
        var skipped = 0
        val items = root["transfer"].asArrayOrEmpty().mapIndexedNotNull { index, element ->
            (element as? JsonObject)?.toTransferSummary(fallbackId = "row-${offset + index}")
                .also { if (it == null) skipped++ }
        }
        // Solo cuántas; nunca qué transferencia ni su contenido.
        if (skipped > 0) log.warn("Transferencias ilegibles omitidas: {Omitidas}", skipped)
        EzResult.Success(TransferPage(items, skipped, offset, limit))
    }
}
