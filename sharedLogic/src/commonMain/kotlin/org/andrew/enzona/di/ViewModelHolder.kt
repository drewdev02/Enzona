package org.andrew.enzona.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import org.koin.core.Koin
import org.koin.core.parameter.ParametersDefinition

/**
 * Dueño del ciclo de vida de un ViewModel para plataformas sin `ViewModelStoreOwner`
 * (iOS). El modelo Swift de la pantalla crea el holder al nacer y llama a [clear] al
 * destruirse, lo que cancela `viewModelScope` igual que haría Android.
 */
class ViewModelHolder<T : ViewModel> internal constructor(
    private val store: ViewModelStore,
    val viewModel: T,
) {
    fun clear() = store.clear()
}

internal inline fun <reified T : ViewModel> createViewModelHolder(noinline create: () -> T): ViewModelHolder<T> {
    val store = ViewModelStore()
    val factory = viewModelFactory { initializer { create() } }
    return ViewModelHolder(store, ViewModelProvider.create(store, factory)[T::class])
}

internal inline fun <reified T : ViewModel> Koin.viewModelHolder(
    noinline parameters: ParametersDefinition? = null,
): ViewModelHolder<T> =
    createViewModelHolder { get<T>(parameters = parameters) }

/** Versión sin generics para Swift Export (no exporta generics): mismo ciclo de vida, tipo concreto. */
class LoginViewModelHolder internal constructor(
    private val holder: ViewModelHolder<org.andrew.enzona.feature.auth.presentation.LoginViewModel>,
) {
    val viewModel: org.andrew.enzona.feature.auth.presentation.LoginViewModel get() = holder.viewModel
    fun clear() = holder.clear()
}

class AccountViewModelHolder internal constructor(private val holder: ViewModelHolder<org.andrew.enzona.feature.account.presentation.AccountViewModel>) {
    val viewModel: org.andrew.enzona.feature.account.presentation.AccountViewModel get() = holder.viewModel
    fun clear() = holder.clear()
}

class CardsViewModelHolder internal constructor(private val holder: ViewModelHolder<org.andrew.enzona.feature.cards.presentation.CardsViewModel>) {
    val viewModel: org.andrew.enzona.feature.cards.presentation.CardsViewModel get() = holder.viewModel
    fun clear() = holder.clear()
}

class CardDetailViewModelHolder internal constructor(private val holder: ViewModelHolder<org.andrew.enzona.feature.cards.presentation.CardDetailViewModel>) {
    val viewModel: org.andrew.enzona.feature.cards.presentation.CardDetailViewModel get() = holder.viewModel
    fun clear() = holder.clear()
}

class TransferMenuViewModelHolder internal constructor(private val holder: ViewModelHolder<org.andrew.enzona.feature.transfer.presentation.TransferMenuViewModel>) {
    val viewModel: org.andrew.enzona.feature.transfer.presentation.TransferMenuViewModel get() = holder.viewModel
    fun clear() = holder.clear()
}

class TransferHistoryViewModelHolder internal constructor(private val holder: ViewModelHolder<org.andrew.enzona.feature.transfer.presentation.TransferHistoryViewModel>) {
    val viewModel: org.andrew.enzona.feature.transfer.presentation.TransferHistoryViewModel get() = holder.viewModel
    fun clear() = holder.clear()
}

class FindRecipientViewModelHolder internal constructor(private val holder: ViewModelHolder<org.andrew.enzona.feature.transfer.presentation.FindRecipientViewModel>) {
    val viewModel: org.andrew.enzona.feature.transfer.presentation.FindRecipientViewModel get() = holder.viewModel
    fun clear() = holder.clear()
}

class FriendsViewModelHolder internal constructor(private val holder: ViewModelHolder<org.andrew.enzona.feature.friends.presentation.FriendsViewModel>) {
    val viewModel: org.andrew.enzona.feature.friends.presentation.FriendsViewModel get() = holder.viewModel
    fun clear() = holder.clear()
}

class OwnCardTransferViewModelHolder internal constructor(private val holder: ViewModelHolder<org.andrew.enzona.feature.transfer.presentation.OwnCardTransferViewModel>) {
    val viewModel: org.andrew.enzona.feature.transfer.presentation.OwnCardTransferViewModel get() = holder.viewModel
    fun clear() = holder.clear()
}

class RecipientTransferViewModelHolder internal constructor(private val holder: ViewModelHolder<org.andrew.enzona.feature.transfer.presentation.RecipientTransferViewModel>) {
    val viewModel: org.andrew.enzona.feature.transfer.presentation.RecipientTransferViewModel get() = holder.viewModel
    fun clear() = holder.clear()
}
