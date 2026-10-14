package org.andrew.enzona.core.result

/** Error de dominio, atraviesa todas las capas (análogo al Either/Failure del original). */
sealed interface Failure {
    val message: String?

    data class Network(override val message: String? = null) : Failure
    data class Http(val status: Int, override val message: String? = null) : Failure
    data class Parse(override val message: String? = null) : Failure
    data class Auth(override val message: String? = null) : Failure
    data class Unknown(override val message: String? = null) : Failure
}

/** Resultado de una operación: éxito con valor o fallo tipado. */
sealed interface EzResult<out T> {
    data class Success<T>(val value: T) : EzResult<T>
    data class Error(val failure: Failure) : EzResult<Nothing>
}

inline fun <T, R> EzResult<T>.map(transform: (T) -> R): EzResult<R> = when (this) {
    is EzResult.Success -> EzResult.Success(transform(value))
    is EzResult.Error -> this
}

inline fun <T> EzResult<T>.onSuccess(action: (T) -> Unit): EzResult<T> {
    if (this is EzResult.Success) action(value)
    return this
}

inline fun <T> EzResult<T>.onError(action: (Failure) -> Unit): EzResult<T> {
    if (this is EzResult.Error) action(failure)
    return this
}
