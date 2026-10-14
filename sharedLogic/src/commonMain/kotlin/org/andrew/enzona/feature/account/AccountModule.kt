package org.andrew.enzona.feature.account

import org.andrew.enzona.feature.account.data.AccountRepositoryImpl
import org.andrew.enzona.feature.account.domain.AccountRepository
import org.andrew.enzona.feature.account.domain.GetAccountUseCase
import org.andrew.enzona.feature.account.presentation.AccountViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val accountModule: Module = module {
    singleOf(::AccountRepositoryImpl) bind AccountRepository::class
    singleOf(::GetAccountUseCase)
    viewModelOf(::AccountViewModel)
}
