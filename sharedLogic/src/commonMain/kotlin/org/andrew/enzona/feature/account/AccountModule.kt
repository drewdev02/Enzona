package org.andrew.enzona.feature.account

import org.andrew.enzona.feature.account.data.AccountRepositoryImpl
import org.andrew.enzona.feature.account.domain.AccountRepository
import org.andrew.enzona.feature.account.domain.GetAccountUseCase
import org.andrew.enzona.feature.account.presentation.AccountViewModel
import org.koin.core.module.Module
import org.koin.dsl.module

val accountModule: Module = module {
    single<AccountRepository> { AccountRepositoryImpl(get()) }
    single { GetAccountUseCase(get(), get()) }
    single { AccountViewModel(get()) }
}
