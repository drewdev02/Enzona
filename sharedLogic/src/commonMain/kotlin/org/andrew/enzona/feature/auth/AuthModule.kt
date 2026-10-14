package org.andrew.enzona.feature.auth

import org.andrew.enzona.feature.auth.data.OAuthService
import org.andrew.enzona.feature.auth.data.TokenStore
import org.andrew.enzona.feature.auth.domain.AuthRepository
import org.andrew.enzona.feature.auth.presentation.AuthViewModel
import org.koin.core.module.Module
import org.koin.dsl.module

val authModule: Module = module {
    single { OAuthService(get()) }
    single { TokenStore(get()) }
    single { AuthRepository(get(), get()) }
    single { AuthViewModel(get()) }
}
