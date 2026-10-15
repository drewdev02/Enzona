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
import org.andrew.enzona.feature.transfer.domain.TransferRecipient
import org.andrew.enzona.feature.transfer.domain.TransferRequest
import org.andrew.enzona.feature.transfer.domain.TransferResultKind
import org.andrew.enzona.feature.transfer.domain.TransferStatus
import org.andrew.enzona.feature.transfer.domain.UnknownCause
import org.andrew.enzona.feature.transfer.presentation.RecipientTransferUiState
import org.andrew.enzona.feature.transfer.presentation.RecipientTransferViewModel
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

private fun card(id: String, currency: String = "CUP", primary: Boolean = false) = BankCard(
    id = id, last4 = id.takeLast(4).padStart(4, '0'), holder = "TITULAR", expiry = "12/29", currency = currency,
    bankCode = "05", bankName = "BANMET", isPrimary = primary, createdAt = null,
)

private class Cards(var answer: EzResult<List<BankCard>>) : CardsRepository {
    var calls = 0
    override suspend fun getCards(username: String): EzResult<List<BankCard>> { calls++; return answer }
    override suspend fun getActivity(cardId: String): EzResult<CardActivity> = error("no se usa")
}

private class Gateway(var answer: suspend () -> TransferOutcome) : TransferGateway {
    val sent = mutableListOf<Pair<TransferRequest, String>>()
    override suspend fun submit(request: TransferRequest, encryptedPassword: String): TransferOutcome {
        sent += request to encryptedPassword
        return answer()
    }
}

private object LengthOnlyEncryptor : PaymentPasswordEncryptor {
    override fun encrypt(password: String): EzResult<String> = EzResult.Success("ENC<${password.length}>")
}

private val accepted = TransferOutcome.Accepted(TransferReceipt("EZ-9", TransferStatus.ACCEPTED, "Aceptada", null, null, null))

@OptIn(ExperimentalCoroutinesApi::class)
class RecipientTransferViewModelTest {
    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private val ana = TransferRecipient("acc-uuid-ana", "ana.perez", "Ana Pérez", "images/ana.png", "cup")
    private val cards = listOf(card("card-aaaa1111", "CUP", primary = true), card("card-bbbb2222", "USD"))

    private class Rig(recipient: TransferRecipient, loaded: EzResult<List<BankCard>>, answer: suspend () -> TransferOutcome) {
        val cards = Cards(loaded)
        val gateway = Gateway(answer)
        val vm = RecipientTransferViewModel(
            recipient, GetCardsUseCase(cards, sessionOf("me")), SubmitTransferUseCase(gateway, LengthOnlyEncryptor),
        )
        val state get() = vm.state.value
    }

    private fun rig(
        recipient: TransferRecipient = ana,
        loaded: List<BankCard> = cards,
        answer: suspend () -> TransferOutcome = { accepted },
    ) = Rig(recipient, EzResult.Success(loaded), answer).also { it.vm.load() }

    private fun Rig.review(amount: String = "10.50", description: String = "") {
        vm.onAmountChange(amount)
        vm.onDescriptionChange(description)
        vm.review()
    }

    // ---------- Carga ----------

    @Test
    fun startsOnThePrimaryCardWithTheGivenRecipientUntouched() = runTest {
        val s = assertIs<RecipientTransferUiState.Editing>(rig().state)
        assertEquals("card-aaaa1111", s.sourceId)
        assertSame(ana, s.recipient)
        assertEquals("", s.amountText)
    }

    @Test
    fun oneCardIsEnoughBecauseTheDestinationIsAnotherPerson() = runTest {
        assertIs<RecipientTransferUiState.Editing>(rig(loaded = listOf(card("card-aaaa1111"))).state)
    }

    @Test
    fun withoutCardsThereIsNothingToSendFrom() = runTest {
        assertIs<RecipientTransferUiState.NoCards>(rig(loaded = emptyList()).state)
    }

    @Test
    fun aLoadErrorCanBeRetriedAndReloadingNeverWipesWhatWasTyped() = runTest {
        val r = Rig(ana, EzResult.Error(Failure.Network()), { accepted })
        r.vm.load()
        assertIs<RecipientTransferUiState.LoadError>(r.state)
        r.cards.answer = EzResult.Success(cards)
        r.vm.load()
        r.vm.onAmountChange("7")
        r.vm.load()
        assertEquals("7", assertIs<RecipientTransferUiState.Editing>(r.state).amountText)
        assertEquals(2, r.cards.calls)
    }

    // ---------- Edición y revisión ----------

    @Test
    fun theSourceCanBeChangedOnlyToACardThatExists() = runTest {
        val r = rig()
        r.vm.selectSource("card-bbbb2222")
        assertEquals("card-bbbb2222", assertIs<RecipientTransferUiState.Editing>(r.state).sourceId)
        r.vm.selectSource("no-existe")
        assertEquals("card-bbbb2222", assertIs<RecipientTransferUiState.Editing>(r.state).sourceId)
    }

    @Test
    fun badAmountsStayOnTheFormWithTheirReasonAndEditingClearsIt() = runTest {
        val r = rig()
        for ((text, problem) in listOf(
            "" to AmountProblem.EMPTY, "0" to AmountProblem.ZERO, "1 0" to AmountProblem.HAS_WHITESPACE, "1,5" to AmountProblem.INVALID,
        )) {
            r.vm.onAmountChange(text)
            r.vm.review()
            assertEquals(problem, assertIs<RecipientTransferUiState.Editing>(r.state).amountProblem, text)
        }
        r.vm.onAmountChange("5")
        assertNull(assertIs<RecipientTransferUiState.Editing>(r.state).amountProblem)
    }

    @Test
    fun theDescriptionIsOptionalInThisForm() = runTest {
        val r = rig()
        r.review(description = "   ")
        val c = assertIs<RecipientTransferUiState.Confirming>(r.state)
        assertEquals("", c.request.description)
    }

    @Test
    fun theRequestCarriesTheRecipientVerbatimTheSourceCurrencyAndATrimmedDescription() = runTest {
        val r = rig()
        r.review(amount = "10.50", description = "  Cena  ")
        val c = assertIs<RecipientTransferUiState.Confirming>(r.state)
        assertEquals(TransferRequest.ToAccount("card-aaaa1111", ana, Money(1_050, "CUP"), "Cena"), c.request)
        assertSame(ana, c.request.recipient, "el destinatario enviado es el mismo objeto verificado")
    }

    @Test
    fun theExchangeWarningNeedsAKnownDifferentCurrency() = runTest {
        // Moneda del destinatario distinta de la de la tarjeta de origen: se avisa.
        val usd = ana.copy(currency = "usd")
        assertTrue(assertIs<RecipientTransferUiState.Confirming>(rig(usd).also { it.review() }.state).exchangeApplies)
        // Misma moneda (sin distinguir mayúsculas): no.
        assertFalse(assertIs<RecipientTransferUiState.Confirming>(rig(ana).also { it.review() }.state).exchangeApplies)
        // Desconocida o vacía: no se afirma nada.
        assertFalse(assertIs<RecipientTransferUiState.Confirming>(rig(ana.copy(currency = null)).also { it.review() }.state).exchangeApplies)
        assertFalse(assertIs<RecipientTransferUiState.Confirming>(rig(ana.copy(currency = " ")).also { it.review() }.state).exchangeApplies)
    }

    @Test
    fun goingBackKeepsEverythingTyped() = runTest {
        val r = rig()
        r.review(amount = "7.25", description = "Cena")
        r.vm.back()
        val s = assertIs<RecipientTransferUiState.Editing>(r.state)
        assertEquals("7.25", s.amountText)
        assertEquals("Cena", s.description)
    }

    // ---------- Envío ----------

    @Test
    fun submittingSendsTheEncryptedPasswordAndFinishesWithoutKeepingIt() = runTest {
        val r = rig()
        r.review()
        r.vm.submit("123456")
        val f = assertIs<RecipientTransferUiState.Finished>(r.state)
        assertEquals(accepted, f.outcome)
        assertEquals(TransferResultKind.SUCCESS, f.kind)
        assertEquals(1, r.gateway.sent.size)
        assertEquals("ENC<6>", r.gateway.sent[0].second)
        assertEquals(f.request, r.gateway.sent[0].first)
        assertFalse("123456" in f.toString())
    }

    @Test
    fun aSecondTapWhileSendingDoesNothingAndTheFormIsFrozen() = runTest {
        val release = CompletableDeferred<Unit>()
        val r = rig { release.await(); accepted }
        r.review()
        r.vm.submit("1234")
        assertIs<RecipientTransferUiState.Submitting>(r.state)
        r.vm.submit("1234")
        r.vm.onAmountChange("999")
        r.vm.review()
        r.vm.back()
        r.vm.selectSource("card-bbbb2222")
        assertIs<RecipientTransferUiState.Submitting>(r.state)
        assertEquals(1, r.gateway.sent.size)
        release.complete(Unit)
        assertIs<RecipientTransferUiState.Finished>(r.state)
        assertEquals(1, r.gateway.sent.size)
    }

    @Test
    fun anEmptyPasswordGoesBackToConfirmingWithoutSending() = runTest {
        val r = rig()
        r.review()
        r.vm.submit("")
        assertEquals(NotSentReason.PASSWORD_EMPTY, assertIs<RecipientTransferUiState.Confirming>(r.state).notSent)
        assertTrue(r.gateway.sent.isEmpty())
        r.vm.submit("1234")
        assertIs<RecipientTransferUiState.Finished>(r.state)
    }

    @Test
    fun aRejectionAllowsEditingAgainButAnUnknownResultNeverOffersToRetry() = runTest {
        val rejected = rig(answer = { TransferOutcome.Rejected(RejectionCause.REFUSED, 400, "Saldo insuficiente") })
        rejected.review(amount = "99999")
        rejected.vm.submit("1234")
        assertEquals(TransferResultKind.FAILED, assertIs<RecipientTransferUiState.Finished>(rejected.state).kind)
        rejected.vm.editAgain()
        assertEquals("99999", assertIs<RecipientTransferUiState.Editing>(rejected.state).amountText)

        val unknown = rig(answer = { TransferOutcome.Unknown(UnknownCause.NO_RESPONSE) })
        unknown.review()
        unknown.vm.submit("1234")
        val finished = assertIs<RecipientTransferUiState.Finished>(unknown.state)
        assertEquals(TransferResultKind.UNKNOWN, finished.kind)
        unknown.vm.editAgain()
        assertSame(finished, unknown.state, "tras un desconocido no se vuelve a editar con los mismos datos")
        unknown.vm.submit("1234")
        assertEquals(1, unknown.gateway.sent.size, "ni se reenvía")
    }

    @Test
    fun logsNeverContainThePasswordTheAmountTheRecipientOrTheCards() = runTest {
        withLogging(minimumLevel = LogLevel.TRACE) { sink ->
            val r = rig()
            r.review(amount = "10.50", description = "Nota privada")
            r.vm.submit("clave-secreta-777")
            val written = sink.everythingWritten()
            assertTrue("Transferencia" in written, "el test debe observar logs reales")
            for (secret in listOf(
                "clave-secreta-777", "10.50", "1050", "card-aaaa1111", "acc-uuid-ana", "ana.perez", "Ana Pérez", "Nota privada",
            )) {
                assertFalse(secret in written, "$secret apareció en los logs")
            }
        }
    }
}
