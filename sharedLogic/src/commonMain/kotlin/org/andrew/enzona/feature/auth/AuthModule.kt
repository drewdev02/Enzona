package org.andrew.enzona.feature.auth

import io.ktor.client.HttpClient
import org.andrew.enzona.di.PlainHttpClient
import org.andrew.enzona.core.network.TokenProvider
import org.andrew.enzona.core.network.createApiHttpClient
import org.andrew.enzona.feature.auth.data.OAuthService
import org.andrew.enzona.feature.auth.data.SessionTokenProvider
import org.andrew.enzona.feature.auth.data.TokenStore
import org.andrew.enzona.feature.auth.domain.SessionRepository
import org.andrew.enzona.feature.auth.presentation.LoginViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val authModule: Module = module {
    // Con calificador no hay constructor DSL: el cliente del IdP se pide por nombre.
    single { OAuthService(get(PlainHttpClient)) }
    singleOf(::TokenStore)
    singleOf(::SessionRepository)
    viewModelOf(::LoginViewModel)

    singleOf(::SessionTokenProvider) bind TokenProvider::class
    // Cliente que usarán las features: `get<HttpClient>()`.
    single<HttpClient> { createApiHttpClient(get()) }
}
