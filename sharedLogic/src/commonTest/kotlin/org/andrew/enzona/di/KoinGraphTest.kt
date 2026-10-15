package org.andrew.enzona.di

import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.andrew.enzona.core.di.PlainHttpClient
import org.andrew.enzona.core.di.coreModule
import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.storage.SecureStorage
import org.andrew.enzona.feature.account.accountModule
import org.andrew.enzona.feature.account.presentation.AccountViewModel
import org.andrew.enzona.feature.auth.authModule
import org.andrew.enzona.feature.auth.data.OAuthService
import org.andrew.enzona.feature.auth.domain.SessionRepository
import org.andrew.enzona.feature.auth.presentation.LoginViewModel
import org.andrew.enzona.feature.cards.cardsModule
import org.andrew.enzona.feature.cards.domain.BankCard
import org.andrew.enzona.feature.cards.domain.CardActivity
import org.andrew.enzona.feature.cards.domain.CardsRepository
import org.andrew.enzona.feature.cards.presentation.CardDetailViewModel
import org.andrew.enzona.feature.cards.presentation.CardsViewModel
import org.andrew.enzona.feature.friends.domain.FriendsRepository
import org.andrew.enzona.feature.friends.friendsModule
import org.andrew.enzona.feature.friends.presentation.FriendsViewModel
import org.andrew.enzona.feature.transfer.domain.TransferRepository
import org.andrew.enzona.feature.transfer.presentation.FindRecipientViewModel
import org.andrew.enzona.feature.transfer.presentation.TransferHistoryViewModel
import org.andrew.enzona.feature.transfer.presentation.TransferMenuViewModel
import org.andrew.enzona.feature.transfer.transferModule
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertSame

private class MemoryStorage : SecureStorage {
    private val map = mutableMapOf<String, String>()
    override fun put(key: String, value: String) { map[key] = value }
    override fun get(key: String) = map[key]
    override fun remove(key: String) { map.remove(key) }
    override fun clear() = map.clear()
}

/**
 * Compilar no basta: el grafo de Koin falla en TIEMPO DE EJECUCIÓN (definición ausente,
 * cliente mal calificado). Este test resuelve cada pieza que usan las apps.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class KoinGraphTest {
    @Test
    fun resolvesTheWholeGraph() = runTest {
        // Los ViewModels lanzan en Dispatchers.Main desde su init.
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val koin = koinApplication {
                modules(
                    coreModule, authModule, accountModule, cardsModule, transferModule, friendsModule,
                    module { single<SecureStorage> { MemoryStorage() } },
                )
            }.koin

            // Transferir: cada pantalla, su ViewModel; los repositorios, uno para toda la app.
            assertNotSame(koin.get<TransferMenuViewModel>(), koin.get<TransferMenuViewModel>())
            assertNotSame(koin.get<TransferHistoryViewModel>(), koin.get<TransferHistoryViewModel>())
            assertNotSame(koin.get<FindRecipientViewModel>(), koin.get<FindRecipientViewModel>())
            assertNotSame(koin.get<FriendsViewModel>(), koin.get<FriendsViewModel>())
            assertSame(koin.get<TransferRepository>(), koin.get<TransferRepository>())
            assertSame(koin.get<FriendsRepository>(), koin.get<FriendsRepository>())

            // La sesión es de la app (una sola); la pantalla de login, no.
            assertSame(koin.get<SessionRepository>(), koin.get<SessionRepository>())
            assertNotSame(koin.get<LoginViewModel>(), koin.get<LoginViewModel>())
            assertNotNull(koin.get<AccountViewModel>())
            assertNotNull(koin.get<CardsViewModel>())
            // Estado por pantalla: cada pantalla recibe su propia instancia, así nada del
            // usuario anterior sobrevive a un logout.
            assertNotSame(koin.get<AccountViewModel>(), koin.get<AccountViewModel>())
            assertNotSame(koin.get<CardsViewModel>(), koin.get<CardsViewModel>())
            assertNotNull(koin.get<OAuthService>())
            // Dos clientes distintos: el del IdP (sin auth) y el de la API (con Bearer + refresh).
            assertNotSame(koin.get<HttpClient>(), koin.get<HttpClient>(PlainHttpClient))

            // CardDetailViewModel con parámetro: resoluciones distintas son instancias distintas
            val detailVm1 = koin.get<CardDetailViewModel> { parametersOf("id-1") }
            val detailVm2 = koin.get<CardDetailViewModel> { parametersOf("id-2") }
            assertNotNull(detailVm1)
            assertNotNull(detailVm2)
            assertNotSame(detailVm1, detailVm2)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun resolvesCardDetailViewModelWithParametersAndForwardsIdToRepository() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            var receivedCardId: String? = null
            val fakeRepo = object : CardsRepository {
                override suspend fun getCards(username: String): EzResult<List<BankCard>> = EzResult.Success(emptyList())
                override suspend fun getActivity(cardId: String): EzResult<CardActivity> {
                    receivedCardId = cardId
                    return EzResult.Success(
                        CardActivity(
                            balance = null,
                            available = null,
                            currency = "CUP",
                            operations = emptyList(),
                            skippedOperations = 0,
                        ),
                    )
                }
            }
            val koin = koinApplication {
                allowOverride(true)
                modules(
                    cardsModule,
                    module {
                        single<CardsRepository> { fakeRepo }
                    },
                )
            }.koin

            val vm1 = koin.get<CardDetailViewModel> { parametersOf("card-alpha") }
            val vm2 = koin.get<CardDetailViewModel> { parametersOf("card-beta") }
            assertNotNull(vm1)
            assertNotNull(vm2)
            assertNotSame(vm1, vm2)

            vm1.load()
            assertEquals("card-alpha", receivedCardId)

            vm2.load()
            assertEquals("card-beta", receivedCardId)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
