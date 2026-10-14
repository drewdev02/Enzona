package org.andrew.enzona.feature.cards

import org.andrew.enzona.feature.cards.data.CardsRepositoryImpl
import org.andrew.enzona.feature.cards.domain.CardsRepository
import org.andrew.enzona.feature.cards.domain.GetCardsUseCase
import org.andrew.enzona.feature.cards.presentation.CardsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val cardsModule: Module = module {
    single<CardsRepository> { CardsRepositoryImpl(get()) }
    single { GetCardsUseCase(get(), get()) }
    viewModel { CardsViewModel(get()) }
}
