package org.andrew.enzona.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.andrew.enzona.feature.cards.presentation.CardDetailUiState
import org.andrew.enzona.feature.cards.presentation.CardsUiState
import org.andrew.enzona.ui.cards.BalanceScreen
import org.andrew.enzona.ui.cards.CardDetailPreviewData
import org.andrew.enzona.ui.cards.CardDetailScreen
import org.andrew.enzona.ui.cards.CardPreviewData
import org.andrew.enzona.ui.designsystem.EnzonaTheme
import org.andrew.enzona.feature.friends.presentation.FriendsUiState
import org.andrew.enzona.feature.transfer.domain.TransferFilter
import org.andrew.enzona.feature.transfer.presentation.FindRecipientUiState
import org.andrew.enzona.feature.transfer.presentation.TransferHistoryUiState
import org.andrew.enzona.feature.transfer.presentation.TransferMenuUiState
import org.andrew.enzona.ui.transfer.FindAccountScreen
import org.andrew.enzona.ui.transfer.OwnCardTransferActions
import org.andrew.enzona.ui.transfer.OwnCardTransferPreviewData
import org.andrew.enzona.ui.transfer.OwnCardTransferScreen
import org.andrew.enzona.ui.transfer.RecipientKind
import org.andrew.enzona.ui.transfer.RecipientTransferActions
import org.andrew.enzona.ui.transfer.RecipientTransferPreviewData
import org.andrew.enzona.ui.transfer.RecipientTransferScreen
import org.andrew.enzona.ui.transfer.TransferFriendsScreen
import org.andrew.enzona.ui.transfer.TransferHistoryScreen
import org.andrew.enzona.ui.transfer.TransferMenuScreen
import org.andrew.enzona.ui.transfer.TransferPreviewData

/**
 * Solo builds debug. Muestra una pantalla con datos INVENTADOS, sin Koin ni red, para revisarla en el
 * emulador sin iniciar sesión. El modo oscuro y el tamaño de letra se cambian con el sistema:
 *   adb shell cmd uimode night yes        adb shell settings put system font_scale 1.6
 *
 *   adb shell am start -n org.andrew.enzona/.debug.PreviewActivity --es screen <nombre>
 * Nombres: detail (por defecto), detail-empty, detail-partial, detail-loading, detail-error, balance,
 * transfer-menu, transfer-menu-empty, transfer-history, transfer-history-empty, transfer-history-error, find-account, find-account-notfound, friends,
 * own-form, own-errors, own-confirm, own-sending, own-accepted, own-rejected, own-unknown,
 * acct-form, acct-problem, acct-confirm, friend-exchange, acct-sending, acct-receipt, acct-rejected, acct-unknown.
 */
class PreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val screen = intent.getStringExtra("screen") ?: "detail"
        val label = "BANMET 6856"
        setContent {
            EnzonaTheme {
                when (screen) {
                    "detail-empty" -> CardDetailScreen(CardDetailUiState.Content(CardDetailPreviewData.noOperations), label, {}, {})
                    "detail-partial" -> CardDetailScreen(CardDetailUiState.Content(CardDetailPreviewData.partial), label, {}, {})
                    "detail-loading" -> CardDetailScreen(CardDetailUiState.Loading, label, {}, {})
                    "detail-error" -> CardDetailScreen(
                        CardDetailUiState.Error("Sin conexión. Revisa tu red e inténtalo de nuevo."), label, {}, {},
                    )
                    "transfer-menu" -> TransferMenuScreen(TransferMenuUiState.Content(TransferPreviewData.transfers.take(5), 0), {}, {}, {}, {})
                    "transfer-menu-empty" -> TransferMenuScreen(TransferMenuUiState.Content(emptyList(), 0), {}, {}, {}, {})
                    "transfer-history" -> TransferHistoryScreen(
                        TransferHistoryUiState.Content(TransferPreviewData.transfers, 1, TransferFilter.None, hasMore = false), {}, {}, {}, {},
                    )
                    "transfer-history-empty" -> TransferHistoryScreen(
                        TransferHistoryUiState.Content(emptyList(), 0, TransferFilter.None, hasMore = false), {}, {}, {}, {},
                    )
                    "transfer-history-error" -> TransferHistoryScreen(
                        TransferHistoryUiState.Error("Sin conexión. Revisa tu red e inténtalo de nuevo.", TransferFilter.None), {}, {}, {}, {},
                    )
                    "find-account" -> FindAccountScreen(FindRecipientUiState.Found(TransferPreviewData.recipient), {}, {}, {}, {})
                    "find-account-notfound" -> FindAccountScreen(FindRecipientUiState.NotFound, {}, {}, {}, {})
                    "friends" -> TransferFriendsScreen(FriendsUiState.Content(TransferPreviewData.friends), {}, {}, {})
                    "own-form" -> OwnCardTransferScreen(OwnCardTransferPreviewData.editing, OwnCardTransferActions())
                    "own-errors" -> OwnCardTransferScreen(OwnCardTransferPreviewData.withProblems, OwnCardTransferActions())
                    "own-confirm" -> OwnCardTransferScreen(OwnCardTransferPreviewData.confirming, OwnCardTransferActions())
                    "own-sending" -> OwnCardTransferScreen(OwnCardTransferPreviewData.submitting, OwnCardTransferActions())
                    "own-accepted" -> OwnCardTransferScreen(OwnCardTransferPreviewData.accepted, OwnCardTransferActions())
                    "acct-form" -> RecipientTransferScreen(RecipientTransferPreviewData.editing, RecipientKind.ACCOUNT, RecipientTransferActions())
                    "acct-problem" -> RecipientTransferScreen(RecipientTransferPreviewData.withProblem, RecipientKind.ACCOUNT, RecipientTransferActions())
                    "acct-confirm" -> RecipientTransferScreen(RecipientTransferPreviewData.confirming, RecipientKind.ACCOUNT, RecipientTransferActions())
                    "friend-exchange" -> RecipientTransferScreen(RecipientTransferPreviewData.confirmingWithExchange, RecipientKind.FRIEND, RecipientTransferActions())
                    "acct-sending" -> RecipientTransferScreen(RecipientTransferPreviewData.submitting, RecipientKind.ACCOUNT, RecipientTransferActions())
                    "acct-receipt" -> RecipientTransferScreen(RecipientTransferPreviewData.accepted, RecipientKind.ACCOUNT, RecipientTransferActions())
                    "acct-rejected" -> RecipientTransferScreen(RecipientTransferPreviewData.rejected, RecipientKind.FRIEND, RecipientTransferActions())
                    "acct-unknown" -> RecipientTransferScreen(RecipientTransferPreviewData.unknown, RecipientKind.ACCOUNT, RecipientTransferActions())
                    "own-pending" -> OwnCardTransferScreen(OwnCardTransferPreviewData.pending, OwnCardTransferActions())
                    "own-rejected" -> OwnCardTransferScreen(OwnCardTransferPreviewData.rejected, OwnCardTransferActions())
                    "own-unknown" -> OwnCardTransferScreen(OwnCardTransferPreviewData.unknown, OwnCardTransferActions())
                    "balance" -> BalanceScreen(CardsUiState.Content(CardPreviewData.all), {}, {}, {})
                    else -> CardDetailScreen(CardDetailUiState.Content(CardDetailPreviewData.activity), label, {}, {})
                }
            }
        }
    }
}
