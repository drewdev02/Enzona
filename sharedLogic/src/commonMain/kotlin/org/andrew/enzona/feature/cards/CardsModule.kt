package org.andrew.enzona.feature.cards

import org.andrew.enzona.feature.cards.data.CardsRepositoryImpl
import org.andrew.enzona.feature.cards.domain.CardsRepository
import org.andrew.enzona.feature.cards.domain.GetCardsUseCase
import org.andrew.enzona.feature.cards.presentation.CardsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val cardsModule: Module = module {
    singleOf(::CardsRepositoryImpl) bind CardsRepository::class
    singleOf(::GetCardsUseCase)
    viewModelOf(::CardsViewModel)
}
