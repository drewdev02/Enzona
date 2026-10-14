package org.andrew.enzona.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import org.koin.core.Koin

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

internal inline fun <reified T : ViewModel> viewModelHolder(noinline create: () -> T): ViewModelHolder<T> {
    val store = ViewModelStore()
    val factory = viewModelFactory { initializer { create() } }
    return ViewModelHolder(store, ViewModelProvider.create(store, factory)[T::class])
}

internal inline fun <reified T : ViewModel> Koin.viewModelHolder(): ViewModelHolder<T> =
    viewModelHolder { get<T>() }
