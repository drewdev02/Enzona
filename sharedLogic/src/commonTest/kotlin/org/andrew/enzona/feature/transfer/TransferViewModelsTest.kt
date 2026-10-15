package org.andrew.enzona.feature.transfer

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import org.andrew.enzona.core.logging.LogLevel
import org.andrew.enzona.core.logging.withLogging
import org.andrew.enzona.core.money.Money
import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.result.Failure
import org.andrew.enzona.di.createViewModelHolder
import org.andrew.enzona.feature.transfer.domain.FindRecipientUseCase
import org.andrew.enzona.feature.transfer.domain.InvalidQuery
import org.andrew.enzona.feature.transfer.domain.TransferDirection
import org.andrew.enzona.feature.transfer.domain.TransferFilter
import org.andrew.enzona.feature.transfer.domain.TransferPage
import org.andrew.enzona.feature.transfer.domain.TransferParty
import org.andrew.enzona.feature.transfer.domain.TransferRecipient
import org.andrew.enzona.feature.transfer.domain.TransferRepository
import org.andrew.enzona.feature.transfer.domain.TransferStatus
import org.andrew.enzona.feature.transfer.domain.TransferSummary
import org.andrew.enzona.feature.transfer.presentation.FindRecipientUiState
import org.andrew.enzona.feature.transfer.presentation.FindRecipientViewModel
import org.andrew.enzona.feature.transfer.presentation.TransferHistoryUiState
import org.andrew.enzona.feature.transfer.presentation.TransferHistoryViewModel
import org.andrew.enzona.feature.transfer.presentation.TransferMenuUiState
import org.andrew.enzona.feature.transfer.presentation.TransferMenuViewModel
import org.andrew.enzona.testing.FakeAccounts
import org.andrew.enzona.testing.sessionOf
import org.andrew.enzona.testing.testAccount
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val nobody = TransferParty(null, null, null, null, null)

private fun summary(id: String) = TransferSummary(
    id = id, direction = TransferDirection.OUTGOING, amount = Money(100, "CUP"), status = TransferStatus.ACCEPTED,
    statusText = "Aceptada", source = nobody, recipient = nobody, description = null, createdAt = null, createdAtText = null,
)

private fun pageOf(offset: Int, limit: Int, ids: List<String>, skipped: Int = 0) =
    TransferPage(ids.map(::summary), skipped, offset, limit)

/** Repositorio que apunta cada petición y responde con [answer] (que puede suspenderse). */
private class FakeTransfers(
    private val answer: suspend (offset: Int, limit: Int, filter: TransferFilter) -> EzResult<TransferPage>,
) : TransferRepository {
    class Call(val offset: Int, val limit: Int, val filter: TransferFilter)
    val calls = mutableListOf<Call>()
    var cancelled = 0
    override suspend fun list(offset: Int, limit: Int, filter: TransferFilter): EzResult<TransferPage> {
        calls += Call(offset, limit, filter)
        try {
            return answer(offset, limit, filter)
        } catch (e: kotlinx.coroutines.CancellationException) {
            cancelled++
            throw e
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TransferViewModelsTest {
    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    // ---------- Menú: Recientes ----------

    @Test
    fun theMenuLoadsTheTenMostRecent() = runTest {
        val repo = FakeTransfers { o, l, _ -> EzResult.Success(pageOf(o, l, listOf("a", "b"))) }
        val vm = TransferMenuViewModel(repo)
        assertIs<TransferMenuUiState.Loading>(vm.state.value)
        vm.load()
        val content = vm.state.value as TransferMenuUiState.Content
        assertEquals(listOf("a", "b"), content.recent.map { it.id })
        assertEquals(0, repo.calls.single().offset)
        assertEquals(10, repo.calls.single().limit)
        assertEquals(TransferFilter.None, repo.calls.single().filter)
    }

    @Test
    fun theMenuShowsAUserMessageOnFailureAndKeepsContentWhileReloading() = runTest {
        var fail = false
        val gate = CompletableDeferred<Unit>()
        val repo = FakeTransfers { o, l, _ ->
            if (fail) { gate.await(); EzResult.Error(Failure.Network("x")) } else EzResult.Success(pageOf(o, l, listOf("a")))
        }
        val vm = TransferMenuViewModel(repo)
        vm.load()
        fail = true
        vm.load()
        assertIs<TransferMenuUiState.Content>(vm.state.value, "mientras recarga se sigue viendo lo anterior")
        gate.complete(Unit)
        val error = vm.state.value as TransferMenuUiState.Error
        assertFalse("x" == error.message, "no se filtran detalles del fallo")
    }

    @Test
    fun destroyingTheMenuCancelsTheInFlightLoad() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repo = FakeTransfers { o, l, _ -> gate.await(); EzResult.Success(pageOf(o, l, listOf("a"))) }
        val holder = createViewModelHolder { TransferMenuViewModel(repo) }
        holder.viewModel.load()
        holder.clear()
        gate.complete(Unit)
        assertEquals(1, repo.cancelled)
        assertIs<TransferMenuUiState.Loading>(holder.viewModel.state.value, "una respuesta tardía no escribe estado")
    }

    // ---------- Historial ----------

    private fun history(repo: TransferRepository) = TransferHistoryViewModel(repo)

    @Test
    fun theHistoryStartsWithTheFirstPage() = runTest {
        val repo = FakeTransfers { o, l, _ -> EzResult.Success(pageOf(o, l, (1..10).map { "t$it" })) }
        val vm = history(repo)
        vm.load()
        val c = vm.state.value as TransferHistoryUiState.Content
        assertEquals(10, c.items.size)
        assertTrue(c.hasMore, "una página llena puede tener más")
        assertEquals(0, repo.calls.single().offset)
    }

    @Test
    fun aShortPageMeansThereIsNothingMoreToLoad() = runTest {
        val repo = FakeTransfers { o, l, _ -> EzResult.Success(pageOf(o, l, listOf("a", "b", "c"))) }
        val vm = history(repo)
        vm.load()
        assertFalse((vm.state.value as TransferHistoryUiState.Content).hasMore)
        vm.loadMore()
        assertEquals(1, repo.calls.size, "sin más páginas no se pide nada")
    }

    @Test
    fun loadMoreAppendsTheNextPageAtTheNextOffset() = runTest {
        val repo = FakeTransfers { o, l, _ ->
            EzResult.Success(if (o == 0) pageOf(o, l, (1..10).map { "t$it" }) else pageOf(o, l, listOf("t11", "t12"), skipped = 1))
        }
        val vm = history(repo)
        vm.load()
        vm.loadMore()
        val c = vm.state.value as TransferHistoryUiState.Content
        assertEquals((1..12).map { "t$it" }, c.items.map { it.id })
        assertEquals(1, c.skipped)
        assertEquals(10, repo.calls[1].offset)
        assertFalse(c.hasMore)
        assertFalse(c.isLoadingMore)
    }

    @Test
    fun aRowTheServerShiftedIntoTheNextPageIsNotRepeated() = runTest {
        val repo = FakeTransfers { o, l, _ ->
            EzResult.Success(if (o == 0) pageOf(o, l, (1..10).map { "t$it" }) else pageOf(o, l, listOf("t10", "t11")))
        }
        val vm = history(repo)
        vm.load()
        vm.loadMore()
        assertEquals((1..11).map { "t$it" }, (vm.state.value as TransferHistoryUiState.Content).items.map { it.id })
    }

    @Test
    fun aSecondLoadMoreWhileOneIsInFlightIsIgnored() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repo = FakeTransfers { o, l, _ ->
            if (o == 0) EzResult.Success(pageOf(o, l, (1..10).map { "t$it" })) else { gate.await(); EzResult.Success(pageOf(o, l, listOf("t11"))) }
        }
        val vm = history(repo)
        vm.load()
        vm.loadMore()
        assertTrue((vm.state.value as TransferHistoryUiState.Content).isLoadingMore)
        vm.loadMore()
        vm.loadMore()
        assertEquals(2, repo.calls.size, "un doble toque no duplica la petición")
        gate.complete(Unit)
        assertEquals(11, (vm.state.value as TransferHistoryUiState.Content).items.size)
    }

    @Test
    fun aFailedNextPageKeepsWhatWasLoadedAndCanBeRetried() = runTest {
        var failNext = true
        val repo = FakeTransfers { o, l, _ ->
            when {
                o == 0 -> EzResult.Success(pageOf(o, l, (1..10).map { "t$it" }))
                failNext -> EzResult.Error(Failure.Network("sin red"))
                else -> EzResult.Success(pageOf(o, l, listOf("t11")))
            }
        }
        val vm = history(repo)
        vm.load()
        vm.loadMore()
        val failed = vm.state.value as TransferHistoryUiState.Content
        assertEquals(10, failed.items.size, "lo ya cargado se conserva")
        assertTrue(failed.moreError != null)
        assertFalse(failed.isLoadingMore)
        failNext = false
        vm.loadMore()
        val ok = vm.state.value as TransferHistoryUiState.Content
        assertEquals(11, ok.items.size)
        assertNull(ok.moreError)
        assertEquals(listOf(10, 10), repo.calls.drop(1).map { it.offset }, "el reintento pide la MISMA página")
    }

    @Test
    fun aNewFilterGoesBackToTheFirstPageWithThatFilter() = runTest {
        val repo = FakeTransfers { o, l, _ -> EzResult.Success(pageOf(o, l, (1..10).map { "t$it" })) }
        val vm = history(repo)
        vm.load()
        vm.loadMore()
        val filter = TransferFilter(LocalDate(2026, 10, 1), null, TransferStatus.PENDING)
        vm.applyFilter(filter)
        val last = repo.calls.last()
        assertEquals(0, last.offset)
        assertEquals(filter, last.filter)
        assertEquals(filter, (vm.state.value as TransferHistoryUiState.Content).filter)
        vm.loadMore()
        assertEquals(filter, repo.calls.last().filter, "las páginas siguientes conservan el filtro")
    }

    @Test
    fun changingTheFilterWhileLoadingMoreDropsTheLateResponseOfTheOldFilter() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repo = FakeTransfers { o, l, f ->
            when {
                o == 0 && f == TransferFilter.None -> EzResult.Success(pageOf(o, l, (1..10).map { "old$it" }))
                f == TransferFilter.None -> { gate.await(); EzResult.Success(pageOf(o, l, listOf("old11"))) }
                else -> EzResult.Success(pageOf(o, l, listOf("new1")))
            }
        }
        val vm = history(repo)
        vm.load()
        vm.loadMore()
        vm.applyFilter(TransferFilter(status = TransferStatus.ACCEPTED))
        gate.complete(Unit)
        val c = vm.state.value as TransferHistoryUiState.Content
        assertEquals(listOf("new1"), c.items.map { it.id }, "la respuesta tardía del filtro anterior no pisa la lista")
        assertEquals(1, repo.cancelled)
    }

    @Test
    fun aFailedFirstLoadShowsAnErrorAndLoadRetries() = runTest {
        var fail = true
        val repo = FakeTransfers { o, l, _ -> if (fail) EzResult.Error(Failure.Http(500)) else EzResult.Success(pageOf(o, l, listOf("a"))) }
        val vm = history(repo)
        vm.load()
        assertIs<TransferHistoryUiState.Error>(vm.state.value)
        fail = false
        vm.load()
        assertIs<TransferHistoryUiState.Content>(vm.state.value)
    }

    // ---------- Buscar una cuenta ----------

    private fun finder(accounts: FakeAccounts, me: String = "me") =
        FindRecipientViewModel(FindRecipientUseCase(accounts, sessionOf(me)))

    @Test
    fun theSearchMapsEveryOutcomeToAState() = runTest {
        val vm = finder(FakeAccounts { name ->
            when (name) {
                "alice" -> EzResult.Success(testAccount())
                "nadie" -> EzResult.Error(Failure.Http(404))
                else -> EzResult.Error(Failure.Network("x"))
            }
        })
        assertIs<FindRecipientUiState.Idle>(vm.state.value)
        vm.search("alice")
        assertEquals(
            TransferRecipient("acc-uuid-1", "alice", "Alice Perez", "images/a.png"),
            (vm.state.value as FindRecipientUiState.Found).recipient,
        )
        vm.search("nadie")
        assertIs<FindRecipientUiState.NotFound>(vm.state.value)
        vm.search("")
        assertEquals(InvalidQuery.EMPTY, (vm.state.value as FindRecipientUiState.Invalid).reason)
        vm.search("otro")
        assertIs<FindRecipientUiState.Error>(vm.state.value)
    }

    @Test
    fun aNewSearchSupersedesAnInFlightOneSoALateResponseCannotReplaceIt() = runTest {
        val first = CompletableDeferred<Unit>()
        val accounts = FakeAccounts { name ->
            if (name == "alice") { first.await(); EzResult.Success(testAccount(uuid = "uuid-alice", username = "alice")) }
            else EzResult.Success(testAccount(uuid = "uuid-bob", username = "bob", first = "Bob", last = "Lopez"))
        }
        val vm = finder(accounts)
        vm.search("alice")
        assertIs<FindRecipientUiState.Searching>(vm.state.value)
        vm.search("bob")
        assertEquals("uuid-bob", (vm.state.value as FindRecipientUiState.Found).recipient.accountUuid)
        first.complete(Unit)
        assertEquals("uuid-bob", (vm.state.value as FindRecipientUiState.Found).recipient.accountUuid, "alice llegó tarde")
    }

    @Test
    fun resetReturnsToIdleAndCancelsTheSearchInFlight() = runTest {
        val gate = CompletableDeferred<Unit>()
        val vm = finder(FakeAccounts { gate.await(); EzResult.Success(testAccount()) })
        vm.search("alice")
        vm.reset()
        gate.complete(Unit)
        assertIs<FindRecipientUiState.Idle>(vm.state.value)
    }

    @Test
    fun theSearchedNameAndTheFoundPersonNeverReachTheLogs() = runTest {
        withLogging(minimumLevel = LogLevel.TRACE) { sink ->
            val vm = finder(FakeAccounts { EzResult.Success(testAccount(uuid = "uuid-secreto", username = "usuario-secreto", first = "Nombre", last = "Secreto")) })
            vm.search("usuario-secreto")
            val written = sink.everythingWritten()
            assertTrue("Búsqueda de cuenta" in written, "el test debe observar logs reales")
            for (secret in listOf("usuario-secreto", "uuid-secreto", "Nombre", "Secreto")) {
                assertFalse(secret in written, "$secret apareció en los logs")
            }
        }
    }
}
