package org.andrew.enzona.feature.transfer

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.andrew.enzona.core.crypto.PaymentPasswordEncryptor
import org.andrew.enzona.core.logging.LogLevel
import org.andrew.enzona.core.logging.withLogging
import org.andrew.enzona.core.money.Money
import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.result.Failure
import org.andrew.enzona.feature.cards.domain.BankCard
import org.andrew.enzona.feature.cards.domain.CardActivity
import org.andrew.enzona.feature.cards.domain.CardsRepository
import org.andrew.enzona.feature.cards.domain.GetCardsUseCase
import org.andrew.enzona.feature.transfer.domain.AmountProblem
import org.andrew.enzona.feature.transfer.domain.NotSentReason
import org.andrew.enzona.feature.transfer.domain.RejectionCause
import org.andrew.enzona.feature.transfer.domain.SubmitTransferUseCase
import org.andrew.enzona.feature.transfer.domain.TransferGateway
import org.andrew.enzona.feature.transfer.domain.TransferOutcome
import org.andrew.enzona.feature.transfer.domain.TransferReceipt
import org.andrew.enzona.feature.transfer.domain.TransferRequest
import org.andrew.enzona.feature.transfer.domain.TransferStatus
import org.andrew.enzona.feature.transfer.domain.UnknownCause
import org.andrew.enzona.feature.transfer.presentation.OwnCardTransferUiState
import org.andrew.enzona.feature.transfer.presentation.OwnCardTransferViewModel
import org.andrew.enzona.testing.sessionOf
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

private fun card(id: String, currency: String = "CUP", primary: Boolean = false, last4: String = id.takeLast(4).padStart(4, '0')) =
    BankCard(
        id = id, last4 = last4, holder = "TITULAR", expiry = "12/29", currency = currency, bankCode = "05",
        bankName = "BANMET", isPrimary = primary, createdAt = null,
    )

private class FakeCards(var answer: EzResult<List<BankCard>>) : CardsRepository {
    var calls = 0
    override suspend fun getCards(username: String): EzResult<List<BankCard>> { calls++; return answer }
    override suspend fun getActivity(cardId: String): EzResult<CardActivity> = error("no se usa")
}

private class FakeGateway(var answer: suspend () -> TransferOutcome) : TransferGateway {
    val sent = mutableListOf<Pair<TransferRequest, String>>()
    override suspend fun submit(request: TransferRequest, encryptedPassword: String): TransferOutcome {
        sent += request to encryptedPassword
        return answer()
    }
}

private object ReversibleEncryptor : PaymentPasswordEncryptor {
    override fun encrypt(password: String): EzResult<String> = EzResult.Success("ENC<${password.length}>")
}

private val accepted = TransferOutcome.Accepted(TransferReceipt("EZ-1", TransferStatus.ACCEPTED, "Aceptada", null, null, null))

@OptIn(ExperimentalCoroutinesApi::class)
class OwnCardTransferViewModelTest {
    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private val twoCards = listOf(card("card-aaaa1111", "CUP", primary = true), card("card-bbbb2222", "CUP"))
    private val threeCards = twoCards + card("card-cccc3333", "USD")

    private class Rig(cards: EzResult<List<BankCard>>, gatewayAnswer: suspend () -> TransferOutcome) {
        val cards = FakeCards(cards)
        val gateway = FakeGateway(gatewayAnswer)
        val vm = OwnCardTransferViewModel(
            GetCardsUseCase(this.cards, sessionOf("alice")),
            SubmitTransferUseCase(gateway, ReversibleEncryptor),
        )
        val state get() = vm.state.value
    }

    private fun rig(cards: List<BankCard>, answer: suspend () -> TransferOutcome = { accepted }) =
        Rig(EzResult.Success(cards), answer).also { it.vm.load() }

    private fun Rig.fill(amount: String = "10.50", description: String = "Ahorro", destination: String = "card-bbbb2222") {
        vm.selectDestination(destination)
        vm.onAmountChange(amount)
        vm.onDescriptionChange(description)
    }

    // ---------- Carga ----------

    @Test
    fun startsOnThePrimaryCardWithNoDestination() = runTest {
        val r = rig(threeCards)
        val s = assertIs<OwnCardTransferUiState.Editing>(r.state)
        assertEquals("card-aaaa1111", s.sourceId)
        assertNull(s.destinationId)
        assertEquals("", s.amountText)
    }

    @Test
    fun withoutAPrimaryCardTheFirstIsTheSource() = runTest {
        val r = rig(listOf(card("card-aaaa1111"), card("card-bbbb2222")))
        assertEquals("card-aaaa1111", assertIs<OwnCardTransferUiState.Editing>(r.state).sourceId)
    }

    @Test
    fun fewerThanTwoCardsCannotTransferBetweenThem() = runTest {
        assertIs<OwnCardTransferUiState.NeedsTwoCards>(rig(listOf(card("card-aaaa1111"))).state)
        assertIs<OwnCardTransferUiState.NeedsTwoCards>(rig(emptyList()).state)
    }

    @Test
    fun aLoadErrorCanBeRetried() = runTest {
        val r = Rig(EzResult.Error(Failure.Network()), { accepted })
        r.vm.load()
        assertIs<OwnCardTransferUiState.LoadError>(r.state)
        r.cards.answer = EzResult.Success(twoCards)
        r.vm.load()
        assertIs<OwnCardTransferUiState.Editing>(r.state)
        assertEquals(2, r.cards.calls)
    }

    @Test
    fun reloadingNeverWipesWhatTheUserTyped() = runTest {
        val r = rig(twoCards)
        r.fill()
        r.vm.load()
        assertEquals("10.50", assertIs<OwnCardTransferUiState.Editing>(r.state).amountText)
        assertEquals(1, r.cards.calls)
    }

    // ---------- Edición ----------

    @Test
    fun changingTheSourceToTheDestinationClearsTheDestination() = runTest {
        val r = rig(threeCards)
        r.vm.selectDestination("card-bbbb2222")
        r.vm.selectSource("card-bbbb2222")
        val s = assertIs<OwnCardTransferUiState.Editing>(r.state)
        assertEquals("card-bbbb2222", s.sourceId)
        assertNull(s.destinationId)
    }

    @Test
    fun theDestinationCannotBeTheSourceNorAnUnknownCard() = runTest {
        val r = rig(threeCards)
        r.vm.selectDestination("card-aaaa1111")
        assertNull(assertIs<OwnCardTransferUiState.Editing>(r.state).destinationId)
        r.vm.selectDestination("no-existe")
        assertNull(assertIs<OwnCardTransferUiState.Editing>(r.state).destinationId)
        r.vm.selectSource("no-existe")
        assertEquals("card-aaaa1111", assertIs<OwnCardTransferUiState.Editing>(r.state).sourceId)
    }

    @Test
    fun reviewingAnEmptyFormMarksEverythingAndStaysEditing() = runTest {
        val r = rig(twoCards)
        r.vm.review()
        val s = assertIs<OwnCardTransferUiState.Editing>(r.state)
        assertEquals(AmountProblem.EMPTY, s.problems.amount)
        assertTrue(s.problems.destinationMissing)
        assertTrue(s.problems.descriptionMissing)
    }

    @Test
    fun editingAFieldClearsOnlyItsOwnProblem() = runTest {
        val r = rig(twoCards)
        r.vm.review()
        r.vm.onAmountChange("5")
        var s = assertIs<OwnCardTransferUiState.Editing>(r.state)
        assertNull(s.problems.amount)
        assertTrue(s.problems.destinationMissing)
        assertTrue(s.problems.descriptionMissing)
        r.vm.onDescriptionChange("x")
        r.vm.selectDestination("card-bbbb2222")
        s = assertIs<OwnCardTransferUiState.Editing>(r.state)
        assertFalse(s.problems.any)
    }

    @Test
    fun badAmountsAreReportedWithTheirReason() = runTest {
        val r = rig(twoCards)
        for ((text, problem) in listOf("0" to AmountProblem.ZERO, "1 0" to AmountProblem.HAS_WHITESPACE, "1,5" to AmountProblem.INVALID)) {
            r.fill(amount = text)
            r.vm.review()
            assertEquals(problem, assertIs<OwnCardTransferUiState.Editing>(r.state).problems.amount, text)
        }
    }

    @Test
    fun aBlankDescriptionIsMissing() = runTest {
        val r = rig(twoCards)
        r.fill(description = "   ")
        r.vm.review()
        assertTrue(assertIs<OwnCardTransferUiState.Editing>(r.state).problems.descriptionMissing)
    }

    // ---------- Revisión ----------

    @Test
    fun aValidFormBecomesAConfirmationWithTheSourceCurrency() = runTest {
        val r = rig(twoCards)
        r.fill(amount = "10.50", description = "  Ahorro  ")
        r.vm.review()
        val c = assertIs<OwnCardTransferUiState.Confirming>(r.state)
        assertEquals(
            TransferRequest.ToOwnCard("card-aaaa1111", "card-bbbb2222", Money(1_050, "CUP"), "Ahorro"),
            c.request,
        )
        assertFalse(c.exchangeApplies)
        assertNull(c.notSent)
    }

    @Test
    fun differentCurrenciesFlagTheExchangeAndKeepTheSourceCurrency() = runTest {
        val r = rig(threeCards)
        r.fill(destination = "card-cccc3333")
        r.vm.review()
        val c = assertIs<OwnCardTransferUiState.Confirming>(r.state)
        assertTrue(c.exchangeApplies)
        assertEquals("CUP", c.request.amount.currency, "la moneda es la de la tarjeta de origen")
    }

    @Test
    fun goingBackKeepsEverythingTyped() = runTest {
        val r = rig(twoCards)
        r.fill(amount = "7.25", description = "Cena")
        r.vm.review()
        r.vm.back()
        val s = assertIs<OwnCardTransferUiState.Editing>(r.state)
        assertEquals("7.25", s.amountText)
        assertEquals("Cena", s.description)
        assertEquals("card-bbbb2222", s.destinationId)
    }

    // ---------- Envío ----------

    @Test
    fun submittingSendsTheRequestWithTheEncryptedPasswordAndFinishes() = runTest {
        val r = rig(twoCards)
        r.fill()
        r.vm.review()
        r.vm.submit("123456")
        val f = assertIs<OwnCardTransferUiState.Finished>(r.state)
        assertEquals(accepted, f.outcome)
        assertEquals(1, r.gateway.sent.size)
        assertEquals("ENC<6>", r.gateway.sent[0].second)
        assertEquals(f.request, r.gateway.sent[0].first)
        assertFalse("123456" in f.toString(), "la contraseña no se guarda en el estado")
    }

    @Test
    fun aSecondTapWhileSendingDoesNothing() = runTest {
        val release = CompletableDeferred<Unit>()
        val r = rig(twoCards) { release.await(); accepted }
        r.fill()
        r.vm.review()
        r.vm.submit("1234")
        assertIs<OwnCardTransferUiState.Submitting>(r.state)
        r.vm.submit("1234")
        r.vm.submit("1234")
        assertEquals(1, r.gateway.sent.size)

        // Mientras se envía, nada del formulario responde.
        r.vm.onAmountChange("999")
        r.vm.review()
        r.vm.back()
        r.vm.selectSource("card-bbbb2222")
        assertIs<OwnCardTransferUiState.Submitting>(r.state)

        release.complete(Unit)
        assertIs<OwnCardTransferUiState.Finished>(r.state)
        assertEquals(1, r.gateway.sent.size)
    }

    @Test
    fun anEmptyPasswordGoesBackToConfirmingWithoutSending() = runTest {
        val r = rig(twoCards)
        r.fill()
        r.vm.review()
        r.vm.submit("")
        val c = assertIs<OwnCardTransferUiState.Confirming>(r.state)
        assertEquals(NotSentReason.PASSWORD_EMPTY, c.notSent)
        assertTrue(r.gateway.sent.isEmpty())

        r.vm.submit("1234")
        assertIs<OwnCardTransferUiState.Finished>(r.state)
        assertEquals(1, r.gateway.sent.size)
    }

    @Test
    fun aRejectionAllowsEditingAgainWithTheSameData() = runTest {
        val rejected = TransferOutcome.Rejected(RejectionCause.REFUSED, 400, "Saldo insuficiente")
        val r = rig(twoCards) { rejected }
        r.fill(amount = "99999")
        r.vm.review()
        r.vm.submit("1234")
        assertEquals(rejected, assertIs<OwnCardTransferUiState.Finished>(r.state).outcome)

        r.vm.editAgain()
        val s = assertIs<OwnCardTransferUiState.Editing>(r.state)
        assertEquals("99999", s.amountText)
        assertEquals("card-bbbb2222", s.destinationId)
    }

    @Test
    fun anUnknownOutcomeNeverOffersToRetryTheSameTransfer() = runTest {
        val unknown = TransferOutcome.Unknown(UnknownCause.NO_RESPONSE)
        val r = rig(twoCards) { unknown }
        r.fill()
        r.vm.review()
        r.vm.submit("1234")
        val finished = assertIs<OwnCardTransferUiState.Finished>(r.state)
        assertEquals(unknown, finished.outcome)

        r.vm.editAgain()
        assertSame(finished, r.state, "tras un desconocido no se vuelve a editar con los mismos datos")
        r.vm.submit("1234")
        assertEquals(1, r.gateway.sent.size, "ni se reenvía")

        r.vm.startOver()
        val fresh = assertIs<OwnCardTransferUiState.Editing>(r.state)
        assertEquals("", fresh.amountText)
        assertNull(fresh.destinationId)
    }

    @Test
    fun anAcceptedTransferStartsOverWithAnEmptyFormAndCannotEditAgain() = runTest {
        val r = rig(twoCards)
        r.fill()
        r.vm.review()
        r.vm.submit("1234")
        val finished = assertIs<OwnCardTransferUiState.Finished>(r.state)
        r.vm.editAgain()
        assertSame(finished, r.state)
        r.vm.startOver()
        assertEquals("", assertIs<OwnCardTransferUiState.Editing>(r.state).amountText)
    }

    @Test
    fun logsNeverContainThePasswordTheAmountOrTheCards() = runTest {
        withLogging(minimumLevel = LogLevel.TRACE) { sink ->
            val r = rig(twoCards)
            r.fill(amount = "10.50", description = "Nota privada")
            r.vm.review()
            r.vm.submit("clave-secreta-777")
            val written = sink.everythingWritten()
            assertTrue("Transferencia" in written, "el test debe observar logs reales")
            for (secret in listOf("clave-secreta-777", "10.50", "1050", "card-aaaa1111", "card-bbbb2222", "Nota privada", "TITULAR")) {
                assertFalse(secret in written, "$secret apareció en los logs")
            }
        }
    }
}
