package org.andrew.enzona.di

import org.andrew.enzona.Greeting
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
        modules(commonModule)
    }

// Helper for iOS invocation from Swift
fun startKoinIos(): KoinApplication = initKoin {}
fun initKoinIos(): KoinApplication = initKoin {}

// Helper for iOS / Swift to access Koin-managed dependencies directly
object KoinHelper : KoinComponent {
    val greeting: Greeting by inject()
}
