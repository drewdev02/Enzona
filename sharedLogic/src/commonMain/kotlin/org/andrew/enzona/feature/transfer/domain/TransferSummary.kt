package org.andrew.enzona.feature.transfer.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.andrew.enzona.core.money.Money

/**
 * Estado de una transferencia. `code` es el `status_code` del backend (los mismos valores que la app original
 * usa en el filtro del historial: Aceptadas 2222, Pendientes 2242, Fallidas 2224). Un estado que no
 * conocemos es [OTHER], y el texto original se conserva en [TransferSummary.statusText].
 */
enum class TransferStatus(val code: Int?) {
    ACCEPTED(2222),
    PENDING(2242),
    FAILED(2224),
    OTHER(null),
}

/** Sentido según el SIGNO que trae el importe (`-1400.00` al enviar, `+500.00` al recibir). Sin signo es [UNKNOWN]. */
enum class TransferDirection { OUTGOING, INCOMING, UNKNOWN }

/** Una de las dos partes de una transferencia. Son hechos tal como los da el backend: sin formato de pantalla. */
data class TransferParty(
    val name: String?,
    val username: String?,
    val bank: String?,
    val last4: String?,
    val avatar: String?,
)

/**
 * Una fila de "Recientes" o del historial. [amount] es la magnitud (sin signo); el sentido está en [direction].
 * [createdAt] es la hora de reloj TAL CUAL la da el servidor (la app original descarta el desfase horario y
 * muestra esa hora sin convertirla a la zona del teléfono); [createdAtText] conserva el texto recibido.
 */
data class TransferSummary(
    val id: String,
    val direction: TransferDirection,
    val amount: Money,
    val status: TransferStatus,
    val statusText: String?,
    val source: TransferParty,
    val recipient: TransferParty,
    val description: String?,
    val createdAt: LocalDateTime?,
    val createdAtText: String?,
)

/** Una página de resultados. La API no devuelve paginación: puede haber más si llegó una página llena. */
data class TransferPage(
    val items: List<TransferSummary>,
    /** Filas que no se pudieron leer (importe ilegible, no era un objeto...). No se ocultan. */
    val skipped: Int,
    val offset: Int,
    val limit: Int,
) {
    val mayHaveMore: Boolean get() = items.size + skipped >= limit
}

/** Filtros del historial. [status] solo admite ACCEPTED, PENDING o FAILED. Las fechas se envían como `yyyy-MM-dd`. */
data class TransferFilter(
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val status: TransferStatus? = null,
) {
    init {
        require(status == null || status.code != null) { "El filtro de estado necesita un estado con código conocido" }
    }

    companion object {
        val None = TransferFilter()

        /**
         * Para Swift, que no puede construir un `LocalDate`: los días llegan como `yyyy-MM-dd` y [statusCode] es el
         * `status_code` del filtro. Un día ilegible o un código que no es de un filtro se trata como "sin filtro"
         * en ese campo, nunca como otro valor.
         */
        fun fromIso(startDay: String?, endDay: String?, statusCode: Int?): TransferFilter = TransferFilter(
            startDate = startDay?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            endDate = endDay?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            status = statusCode?.let { code -> TransferStatus.entries.firstOrNull { it.code == code } },
        )
    }
}
