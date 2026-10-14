package org.andrew.enzona.di

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import io.ktor.client.HttpClient
import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.storage.SecureKeys
import org.andrew.enzona.core.storage.SecureStorage
import org.andrew.enzona.feature.auth.data.OAuthService
import org.andrew.enzona.feature.auth.data.TokenStore
import org.andrew.enzona.feature.auth.data.dto.TokenDto
import org.andrew.enzona.feature.auth.domain.SessionRepository
import org.andrew.enzona.feature.cards.domain.BankCard
import org.andrew.enzona.feature.cards.domain.CardsRepository
import org.andrew.enzona.feature.cards.domain.GetCardsUseCase
import org.andrew.enzona.feature.cards.presentation.CardsUiState
import org.andrew.enzona.feature.cards.presentation.CardsViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private class MemoryStore : SecureStorage {
    private val map = mutableMapOf<String, String>()
    override fun put(key: String, value: String) { map[key] = value }
    override fun get(key: String) = map[key]
    override fun remove(key: String) { map.remove(key) }
    override fun clear() = map.clear()
}

/** Respuesta de red que el test completa a mano, para controlar cuándo "llega". */
private class PendingCards(private val gate: CompletableDeferred<List<BankCard>>) : CardsRepository {
    var cancelled = false
    override suspend fun getCards(username: String): EzResult<List<BankCard>> =
        try {
            EzResult.Success(gate.await())
        } catch (e: kotlinx.coroutines.CancellationException) {
            cancelled = true
            throw e
        }
}

/** id_token con payload `{"sub":"user1"}`; la firma no se verifica en cliente. */
private const val ID_TOKEN = "x.eyJzdWIiOiJ1c2VyMSJ9.y"

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelLifecycleTest {
    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun auth(): SessionRepository {
        val store = TokenStore(MemoryStore())
        store.save(TokenDto(accessToken = "tok", idToken = ID_TOKEN))
        val idp = HttpClient(MockEngine { respond("{}", HttpStatusCode.OK) })
        return SessionRepository(OAuthService(idp), store)
    }

    @Test
    fun clearingTheHolderCancelsAnInFlightLoad() = runTest {
        val gate = CompletableDeferred<List<BankCard>>()
        val repo = PendingCards(gate)
        val holder = viewModelHolder { CardsViewModel(GetCardsUseCase(repo, auth())) }
        val vm = holder.viewModel

        vm.load()
        assertIs<CardsUiState.Loading>(vm.state.value)

        holder.clear()
        gate.complete(emptyList())

        assertTrue(repo.cancelled, "la carga en vuelo debe cancelarse al destruir la pantalla")
        assertIs<CardsUiState.Loading>(vm.state.value, "una respuesta tardía no debe escribir estado")
    }

    @Test
    fun aNewScreenNeverSeesTheStateOfThePreviousOne() = runTest {
        val repo = object : CardsRepository {
            override suspend fun getCards(username: String): EzResult<List<BankCard>> = EzResult.Success(emptyList())
        }
        val first = CardsViewModel(GetCardsUseCase(repo, auth()))
        first.load()
        assertEquals(CardsUiState.Empty, first.state.value)

        // Tras un logout la pantalla se recrea: debe empezar de cero, no con lo del usuario anterior.
        val second = CardsViewModel(GetCardsUseCase(repo, auth()))
        assertEquals(CardsUiState.Loading, second.state.value)
    }
}
