package org.andrew.enzona.di

import org.andrew.enzona.Greeting
import org.andrew.enzona.core.di.coreModule
import org.andrew.enzona.core.di.platformModule
import org.andrew.enzona.feature.account.accountModule
import org.andrew.enzona.feature.auth.authModule
import org.andrew.enzona.feature.cards.cardsModule
import org.koin.core.KoinApplication
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val commonModule: Module = module {
    single { Greeting() }
}

fun initKoin(appDeclaration: KoinAppDeclaration = {}): KoinApplication =
    startKoin {
        appDeclaration()
        modules(commonModule, coreModule, platformModule(), authModule, accountModule, cardsModule)
    }

// Helper for iOS invocation from Swift
fun startKoinIos(): KoinApplication = initKoin {}
fun initKoinIos(): KoinApplication = initKoin {}

// Helper for iOS / Swift to access Koin-managed dependencies directly
object KoinHelper : KoinComponent {
    val greeting: Greeting by inject()
    val authViewModel: org.andrew.enzona.feature.auth.presentation.AuthViewModel by inject()
    val accountViewModel: org.andrew.enzona.feature.account.presentation.AccountViewModel by inject()
    val cardsViewModel: org.andrew.enzona.feature.cards.presentation.CardsViewModel by inject()
}
