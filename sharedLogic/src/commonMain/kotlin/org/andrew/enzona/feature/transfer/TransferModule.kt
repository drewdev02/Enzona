package org.andrew.enzona.feature.transfer

import org.andrew.enzona.feature.transfer.data.TransferRepositoryImpl
import org.andrew.enzona.feature.transfer.domain.FindRecipientUseCase
import org.andrew.enzona.feature.transfer.domain.TransferRepository
import org.andrew.enzona.feature.transfer.presentation.FindRecipientViewModel
import org.andrew.enzona.feature.transfer.presentation.TransferHistoryViewModel
import org.andrew.enzona.feature.transfer.presentation.TransferMenuViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val transferModule: Module = module {
    singleOf(::TransferRepositoryImpl) bind TransferRepository::class
    singleOf(::FindRecipientUseCase)
    viewModelOf(::TransferMenuViewModel)
    viewModelOf(::TransferHistoryViewModel)
    viewModelOf(::FindRecipientViewModel)
}
