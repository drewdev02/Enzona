package org.andrew.enzona.core.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Suscripción cancelable, consumible desde Swift (reemplaza a SKIE mientras no haya soporte). */
class Cancellable(private val job: Job) {
    fun cancel() { job.cancel() }
}

/** Observa un StateFlow desde Swift: entrega cada valor en el hilo principal. */
fun <T> StateFlow<T>.watch(onEach: (T) -> Unit): Cancellable {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    val job = scope.launch { collect { onEach(it) } }
    return Cancellable(job)
}
