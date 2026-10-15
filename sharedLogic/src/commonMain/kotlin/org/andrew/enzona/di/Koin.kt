package org.andrew.enzona.di

import org.andrew.enzona.core.di.coreModule
import org.andrew.enzona.core.di.platformModule
import org.andrew.enzona.core.util.Cancellable
import org.andrew.enzona.core.util.watch
import org.andrew.enzona.feature.auth.domain.SessionRepository
import org.andrew.enzona.feature.auth.domain.SessionState
import org.andrew.enzona.feature.account.accountModule
import org.andrew.enzona.feature.auth.authModule
import org.andrew.enzona.feature.cards.cardsModule
import org.andrew.enzona.feature.friends.friendsModule
import org.andrew.enzona.feature.friends.presentation.FriendsViewModel
import org.andrew.enzona.feature.transfer.presentation.FindRecipientViewModel
import org.andrew.enzona.feature.transfer.domain.TransferRecipient
import org.andrew.enzona.feature.transfer.presentation.OwnCardTransferViewModel
import org.andrew.enzona.feature.transfer.presentation.RecipientTransferViewModel
import org.andrew.enzona.feature.transfer.presentation.TransferHistoryViewModel
import org.andrew.enzona.feature.transfer.presentation.TransferMenuViewModel
import org.andrew.enzona.feature.transfer.transferModule
import org.koin.core.KoinApplication
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.parameter.parametersOf
import org.koin.dsl.KoinAppDeclaration

fun initKoin(appDeclaration: KoinAppDeclaration = {}): KoinApplication =
    startKoin {
        appDeclaration()
        modules(
            coreModule, platformModule(),
            authModule, accountModule, cardsModule, transferModule, friendsModule,
        )
    }

// Helper for iOS invocation from Swift
fun startKoinIos(): KoinApplication = initKoin {}
fun initKoinIos(): KoinApplication = initKoin {}

// Helper for iOS / Swift to access Koin-managed dependencies directly
object KoinHelper : KoinComponent {
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

    fun transferMenuViewModelHolder(): ViewModelHolder<TransferMenuViewModel> = getKoin().viewModelHolder()

    fun transferHistoryViewModelHolder(): ViewModelHolder<TransferHistoryViewModel> = getKoin().viewModelHolder()

    fun findRecipientViewModelHolder(): ViewModelHolder<FindRecipientViewModel> = getKoin().viewModelHolder()

    fun friendsViewModelHolder(): ViewModelHolder<FriendsViewModel> = getKoin().viewModelHolder()

    fun ownCardTransferViewModelHolder(): ViewModelHolder<OwnCardTransferViewModel> = getKoin().viewModelHolder()

    /** "A una cuenta" y "A un amigo": el destinatario ya verificado lo pone quien abre la pantalla. */
    fun recipientTransferViewModelHolder(recipient: TransferRecipient): ViewModelHolder<RecipientTransferViewModel> =
        getKoin().viewModelHolder { parametersOf(recipient) }
}

