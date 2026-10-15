package org.andrew.enzona.feature.transfer

import org.andrew.enzona.feature.transfer.data.TransferGatewayImpl
import org.andrew.enzona.feature.transfer.data.TransferRepositoryImpl
import org.andrew.enzona.feature.transfer.domain.FindRecipientUseCase
import org.andrew.enzona.feature.transfer.domain.SubmitTransferUseCase
import org.andrew.enzona.feature.transfer.domain.TransferGateway
import org.andrew.enzona.feature.transfer.domain.TransferRecipient
import org.andrew.enzona.feature.transfer.domain.TransferRepository
import org.andrew.enzona.feature.transfer.presentation.FindRecipientViewModel
import org.andrew.enzona.feature.transfer.presentation.OwnCardTransferViewModel
import org.andrew.enzona.feature.transfer.presentation.RecipientTransferViewModel
import org.andrew.enzona.feature.transfer.presentation.TransferHistoryViewModel
import org.andrew.enzona.feature.transfer.presentation.TransferMenuViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val transferModule: Module = module {
    singleOf(::TransferRepositoryImpl) bind TransferRepository::class
    singleOf(::TransferGatewayImpl) bind TransferGateway::class
    singleOf(::FindRecipientUseCase)
    // Una sola instancia: su Mutex es lo que impide dos transferencias a la vez en toda la app.
    singleOf(::SubmitTransferUseCase)
    viewModelOf(::TransferMenuViewModel)
    viewModelOf(::TransferHistoryViewModel)
    viewModelOf(::FindRecipientViewModel)
    viewModelOf(::OwnCardTransferViewModel)
    // El destinatario (ya verificado) lo pone quien abre la pantalla (parametersOf).
    viewModel { (recipient: TransferRecipient) -> RecipientTransferViewModel(recipient, get(), get()) }
}
