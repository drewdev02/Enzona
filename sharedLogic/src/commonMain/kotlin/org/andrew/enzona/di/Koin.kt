package org.andrew.enzona.di

import org.andrew.enzona.Greeting
import org.andrew.enzona.core.di.coreModule
import org.andrew.enzona.core.di.platformModule
import org.andrew.enzona.core.util.Cancellable
import org.andrew.enzona.core.util.watch
import org.andrew.enzona.feature.auth.domain.SessionRepository
import org.andrew.enzona.feature.auth.domain.SessionState
import org.andrew.enzona.feature.account.accountModule
import org.andrew.enzona.feature.auth.authModule
import org.andrew.enzona.feature.cards.cardsModule
import org.koin.core.KoinApplication
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
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
    val session: SessionRepository by inject()

    /** Observa la sesión desde Swift (puente manual en lugar de SKIE). */
    fun watchSession(onEach: (SessionState) -> Unit): Cancellable = session.state.watch(onEach)

    /** Un ViewModel nuevo por pantalla; quien lo pide llama a `clear()` al destruirla. */
    fun loginViewModelHolder(): ViewModelHolder<org.andrew.enzona.feature.auth.presentation.LoginViewModel> =
        getKoin().viewModelHolder()

    fun accountViewModelHolder(): ViewModelHolder<org.andrew.enzona.feature.account.presentation.AccountViewModel> =
        getKoin().viewModelHolder()

    fun cardsViewModelHolder(): ViewModelHolder<org.andrew.enzona.feature.cards.presentation.CardsViewModel> =
        getKoin().viewModelHolder()

    fun cardDetailViewModelHolder(cardId: String): ViewModelHolder<org.andrew.enzona.feature.cards.presentation.CardDetailViewModel> =
        getKoin().viewModelHolder { parametersOf(cardId) }
}

