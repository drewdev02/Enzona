package org.andrew.enzona.feature.friends

import org.andrew.enzona.feature.friends.data.FriendsRepositoryImpl
import org.andrew.enzona.feature.friends.domain.FriendsRepository
import org.andrew.enzona.feature.friends.presentation.FriendsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val friendsModule: Module = module {
    singleOf(::FriendsRepositoryImpl) bind FriendsRepository::class
    viewModelOf(::FriendsViewModel)
}
