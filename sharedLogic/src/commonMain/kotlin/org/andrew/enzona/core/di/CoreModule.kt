package org.andrew.enzona.core.di

import org.andrew.enzona.core.network.createHttpClient
import org.andrew.enzona.core.network.enzonaJson
import org.koin.core.module.Module
import org.koin.dsl.module

/** Dependencias comunes: cliente HTTP y JSON. */
val coreModule: Module = module {
    single { enzonaJson }
    single { createHttpClient() }
}

/** Dependencias específicas de plataforma (SecureStorage, etc.). */
expect fun platformModule(): Module
