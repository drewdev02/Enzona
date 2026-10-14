package org.andrew.enzona.core.di

import org.andrew.enzona.core.network.createPlainHttpClient
import org.andrew.enzona.core.network.enzonaJson
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** Cliente sin auth, para identity.enzona.net (token/refresh). */
val PlainHttpClient = named("plainHttpClient")

/** Dependencias comunes. El cliente de la API (con Bearer + refresh) vive en authModule. */
val coreModule: Module = module {
    single { enzonaJson }
    single(PlainHttpClient) { createPlainHttpClient() }
}

/** Dependencias específicas de plataforma (SecureStorage, etc.). */
expect fun platformModule(): Module
