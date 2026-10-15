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
 * transfer-menu, transfer-menu-empty, transfer-history, transfer-history-empty, transfer-history-error, find-account, find-account-notfound, friends.
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
                    "balance" -> BalanceScreen(CardsUiState.Content(CardPreviewData.all), {}, {}, {})
                    else -> CardDetailScreen(CardDetailUiState.Content(CardDetailPreviewData.activity), label, {}, {})
                }
            }
        }
    }
}
