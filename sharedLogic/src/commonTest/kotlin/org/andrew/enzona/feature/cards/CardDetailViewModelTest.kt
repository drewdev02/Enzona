package org.andrew.enzona.feature.cards

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.andrew.enzona.core.logging.LogLevel
import org.andrew.enzona.core.logging.withLogging
import org.andrew.enzona.core.money.Money
import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.result.Failure
import org.andrew.enzona.core.result.userMessage
import org.andrew.enzona.di.createViewModelHolder
import org.andrew.enzona.feature.cards.domain.BankCard
import org.andrew.enzona.feature.cards.domain.CardActivity
import org.andrew.enzona.feature.cards.domain.CardOperation
import org.andrew.enzona.feature.cards.domain.CardsRepository
import org.andrew.enzona.feature.cards.domain.OperationDirection
import org.andrew.enzona.feature.cards.domain.OperationKind
import org.andrew.enzona.feature.cards.presentation.CardDetailUiState
import org.andrew.enzona.feature.cards.presentation.CardDetailViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

private class FakeCardsRepository : CardsRepository {
    var requestedCardId: String? = null
    var activityResult: EzResult<CardActivity>? = null
    var gate: CompletableDeferred<EzResult<CardActivity>>? = null
    var cancelled = false

    override suspend fun getCards(username: String): EzResult<List<BankCard>> =
        EzResult.Success(emptyList())

    override suspend fun getActivity(cardId: String): EzResult<CardActivity> {
        requestedCardId = cardId
        val g = gate
        if (g != null) {
            return try {
                g.await()
            } catch (e: kotlinx.coroutines.CancellationException) {
                cancelled = true
                throw e
            }
        }
        return activityResult ?: EzResult.Error(Failure.Unknown("Sin resultado configurado"))
    }
}

private fun sampleActivity(
    currency: String = "CUP",
    operations: List<CardOperation> = emptyList(),
    skipped: Int = 0,
) = CardActivity(
    balance = Money(150000L, currency),
    available = Money(120000L, currency),
    currency = currency,
    operations = operations,
    skippedOperations = skipped,
)

private const val SECRET_CARD_ID = "card-uuid-secreto-998877"
private const val SECRET_ALIAS = "Comercio Secreto S.A."
private const val SECRET_DESC = "TRF_SECRETA_123"

@OptIn(ExperimentalCoroutinesApi::class)
class CardDetailViewModelTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun startsInLoadingState() {
        val repo = FakeCardsRepository()
        val vm = CardDetailViewModel("card-123", repo)
        assertEquals(CardDetailUiState.Loading, vm.state.value)
    }

    @Test
    fun loadsContentSuccessfully() = runTest {
        val repo = FakeCardsRepository()
        val expected = sampleActivity(
            operations = listOf(
                CardOperation(
                    kind = OperationKind.TRANSFER,
                    direction = OperationDirection.DEBIT,
                    amount = Money(5000L, "CUP"),
                    merchantAlias = null,
                    dateText = "14/10/26",
                ),
            ),
        )
        repo.activityResult = EzResult.Success(expected)

        val vm = CardDetailViewModel("card-123", repo)
        vm.load()

        val content = assertIs<CardDetailUiState.Content>(vm.state.value)
        assertEquals(expected, content.activity)
        assertEquals(1, content.activity.operations.size)
    }

    @Test
    fun emptyOperationsListIsContentState() = runTest {
        val repo = FakeCardsRepository()
        val expected = sampleActivity(operations = emptyList())
        repo.activityResult = EzResult.Success(expected)

        val vm = CardDetailViewModel("card-empty", repo)
        vm.load()

        val content = assertIs<CardDetailUiState.Content>(vm.state.value)
        assertEquals(expected, content.activity)
        assertTrue(content.activity.operations.isEmpty())
    }

    @Test
    fun showsErrorWithUserMessageWhenRepositoryFailsWithoutLeakingDetails() = runTest {
        val repo = FakeCardsRepository()
        val failure = Failure.Http(500, "Error interno con detalle de base de datos secreta")
        repo.activityResult = EzResult.Error(failure)

        val vm = CardDetailViewModel("card-err", repo)
        vm.load()

        val error = assertIs<CardDetailUiState.Error>(vm.state.value)
        assertEquals(failure.userMessage(), error.message)
        assertFalse("base de datos secreta" in error.message)
    }

    @Test
    fun reloadingKeepsPreviousContentUntilNewDataArrives() = runTest {
        val repo = FakeCardsRepository()
        val first = sampleActivity(currency = "CUP")
        repo.activityResult = EzResult.Success(first)

        val vm = CardDetailViewModel("card-reload", repo)
        vm.load()
        assertEquals(CardDetailUiState.Content(first), vm.state.value)

        // Preparamos una recarga retenida por un CompletableDeferred
        val gate = CompletableDeferred<EzResult<CardActivity>>()
        repo.gate = gate

        vm.load()
        // Mientras la carga está en vuelo, se mantiene el contenido previo (no pasa a Loading)
        assertEquals(CardDetailUiState.Content(first), vm.state.value)

        val second = sampleActivity(currency = "USD")
        gate.complete(EzResult.Success(second))

        assertEquals(CardDetailUiState.Content(second), vm.state.value)
    }

    @Test
    fun clearingTheHolderCancelsAnInFlightLoadAndLateResponseDoesNotWriteState() = runTest {
        val repo = FakeCardsRepository()
        val gate = CompletableDeferred<EzResult<CardActivity>>()
        repo.gate = gate

        val holder = createViewModelHolder { CardDetailViewModel("card-cancel", repo) }
        val vm = holder.viewModel

        vm.load()
        assertIs<CardDetailUiState.Loading>(vm.state.value)

        holder.clear()
        gate.complete(EzResult.Success(sampleActivity()))

        assertTrue(repo.cancelled, "la carga en vuelo debe cancelarse al destruir la pantalla")
        assertIs<CardDetailUiState.Loading>(vm.state.value, "una respuesta tardía no debe escribir estado")
    }

    @Test
    fun twoInstancesForDifferentCardsDoNotShareState() = runTest {
        val repo = FakeCardsRepository()
        val act1 = sampleActivity(currency = "CUP")
        val act2 = sampleActivity(currency = "USD")

        val vm1 = CardDetailViewModel("card-1", repo)
        val vm2 = CardDetailViewModel("card-2", repo)

        repo.activityResult = EzResult.Success(act1)
        vm1.load()

        assertEquals(CardDetailUiState.Content(act1), vm1.state.value)
        assertEquals(CardDetailUiState.Loading, vm2.state.value)

        repo.activityResult = EzResult.Success(act2)
        vm2.load()

        assertEquals(CardDetailUiState.Content(act2), vm2.state.value)
    }

    @Test
    fun cardIdReceivedIsTheOneRequestedToTheRepository() = runTest {
        val repo = FakeCardsRepository()
        repo.activityResult = EzResult.Success(sampleActivity())

        val vm = CardDetailViewModel("target-card-777", repo)
        vm.load()

        assertEquals("target-card-777", repo.requestedCardId)
    }

    @Test
    fun loadingRedactsCardIdAmountsAliasesAndSecretsInLogs() = runTest {
        val repo = FakeCardsRepository()
        val activity = CardActivity(
            balance = Money(339515L, "CUP"),
            available = Money(1708800L, "CUP"),
            currency = "CUP",
            operations = listOf(
                CardOperation(
                    kind = OperationKind.MERCHANT_PAYMENT,
                    direction = OperationDirection.DEBIT,
                    amount = Money(95000L, "CUP"),
                    merchantAlias = SECRET_ALIAS,
                    dateText = "14/10/26",
                ),
            ),
            skippedOperations = 1,
        )
        repo.activityResult = EzResult.Success(activity)

        withLogging(minimumLevel = LogLevel.TRACE) { sink ->
            val vm = CardDetailViewModel(SECRET_CARD_ID, repo)
            vm.load()

            val written = sink.everythingWritten()

            // Confirmar que se emitieron logs reales del ViewModel
            assertTrue("Actividad cargada" in written, "debe registrar mensaje de carga exitosa")
            assertTrue("operaciones" in written, "debe registrar el recuento de operaciones")

            // Comprobar que ningún secreto aparece en los logs
            val secrets = mapOf(
                "id de tarjeta" to SECRET_CARD_ID,
                "alias" to SECRET_ALIAS,
                "saldo contable en centavos" to "339515",
                "saldo disponible en centavos" to "1708800",
                "importe de operación en centavos" to "95000",
            )
            for ((label, secret) in secrets) {
                assertFalse(secret in written, "$label ('$secret') no debe aparecer en los logs")
            }
        }
    }

    @Test
    fun failureRedactsCardIdAndOnlyLogsFailureType() = runTest {
        val repo = FakeCardsRepository()
        val failure = Failure.Unknown("detalle_secreto_de_error_interno")
        repo.activityResult = EzResult.Error(failure)

        withLogging(minimumLevel = LogLevel.TRACE) { sink ->
            val vm = CardDetailViewModel(SECRET_CARD_ID, repo)
            vm.load()

            val written = sink.everythingWritten()

            // Confirmar que se emitieron logs reales de advertencia
            assertTrue("No se pudo cargar la actividad de la tarjeta" in written, "debe registrar advertencia")
            assertTrue("Unknown" in written, "debe registrar el tipo de fallo")

            // Comprobar que ningún dato secreto aparece
            assertFalse(SECRET_CARD_ID in written, "el id de tarjeta no debe aparecer en los logs de error")
            assertFalse("detalle_secreto_de_error_interno" in written, "el detalle del fallo no debe aparecer en los logs")
        }
    }
}
