package org.andrew.enzona.di

import org.andrew.enzona.core.crypto.PaymentPasswordEncryptor
import org.andrew.enzona.core.crypto.RsaPaymentPasswordEncryptor
import org.andrew.enzona.core.network.createPlainHttpClient
import org.andrew.enzona.core.json.enzonaJson
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** Cliente sin auth, para identity.enzona.net (token/refresh). */
val PlainHttpClient = named("plainHttpClient")

/** Dependencias comunes. El cliente de la API (con Bearer + refresh) vive en authModule. */
val coreModule: Module = module {
    single { enzonaJson }
    single(PlainHttpClient) { createPlainHttpClient() }
    single<PaymentPasswordEncryptor> { RsaPaymentPasswordEncryptor() }
}

/** Dependencias específicas de plataforma (SecureStorage, etc.). */
expect fun platformModule(): Module
