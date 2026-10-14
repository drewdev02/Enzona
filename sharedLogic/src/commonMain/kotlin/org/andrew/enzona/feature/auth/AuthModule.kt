package org.andrew.enzona.feature.auth

import io.ktor.client.HttpClient
import org.andrew.enzona.core.di.PlainHttpClient
import org.andrew.enzona.core.network.TokenProvider
import org.andrew.enzona.core.network.createApiHttpClient
import org.andrew.enzona.feature.auth.data.OAuthService
import org.andrew.enzona.feature.auth.data.SessionTokenProvider
import org.andrew.enzona.feature.auth.data.TokenStore
import org.andrew.enzona.feature.auth.domain.SessionRepository
import org.andrew.enzona.feature.auth.presentation.LoginViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val authModule: Module = module {
    single { OAuthService(get(PlainHttpClient)) }
    single { TokenStore(get()) }
    single { SessionRepository(get(), get()) }
    viewModel { LoginViewModel(get()) }

    single<TokenProvider> { SessionTokenProvider(get(), get()) }
    // Cliente que usarán las features: `get<HttpClient>()`.
    single<HttpClient> { createApiHttpClient(get()) }
}
