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

/**
 * Solo builds debug. Muestra una pantalla con datos INVENTADOS, sin Koin ni red, para revisarla en el
 * emulador sin iniciar sesión. El modo oscuro y el tamaño de letra se cambian con el sistema:
 *   adb shell cmd uimode night yes        adb shell settings put system font_scale 1.6
 *
 *   adb shell am start -n org.andrew.enzona/.debug.PreviewActivity --es screen <nombre>
 * Nombres: detail (por defecto), detail-empty, detail-partial, detail-loading, detail-error, balance.
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
                    "balance" -> BalanceScreen(CardsUiState.Content(CardPreviewData.all), {}, {}, {})
                    else -> CardDetailScreen(CardDetailUiState.Content(CardDetailPreviewData.activity), label, {}, {})
                }
            }
        }
    }
}
