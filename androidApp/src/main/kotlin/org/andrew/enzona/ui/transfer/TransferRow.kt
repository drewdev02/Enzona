package org.andrew.enzona.ui.transfer

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import org.andrew.enzona.R
import org.andrew.enzona.feature.cards.domain.bankBrandOf
import org.andrew.enzona.feature.transfer.domain.TransferDirection
import org.andrew.enzona.feature.transfer.domain.TransferSummary
import org.andrew.enzona.ui.designsystem.EnzonaTheme
import org.andrew.enzona.ui.designsystem.components.EzAmount
import org.andrew.enzona.ui.designsystem.components.EzAmountTone
import org.andrew.enzona.ui.designsystem.components.EzStatusPill
import org.andrew.enzona.ui.designsystem.isLargeFont

/**
 * Una transferencia en una lista: logo del banco del destinatario, destinatario y origen, importe, estado y fecha.
 * Con fuente grande las filas se apilan para que el importe nunca se corte ni se parta.
 */
@Composable
fun TransferRow(transfer: TransferSummary, modifier: Modifier = Modifier) {
    val spacing = EnzonaTheme.spacing
    val spoken = transfer.spoken()
    val rowModifier = modifier.fillMaxWidth().padding(horizontal = spacing.lg, vertical = spacing.md)
        .semantics(mergeDescendants = true) { contentDescription = spoken }
    val logo = bankBrandOf(transfer.recipient.bank).logoRes ?: R.drawable.avatar_placeholder
    val icon: @Composable () -> Unit = {
        Image(painterResource(logo), contentDescription = null, Modifier.size(EnzonaTheme.sizes.avatarLg))
    }
    val titles: @Composable (Modifier) -> Unit = { m ->
        Column(m) {
            Text(transfer.title(), style = MaterialTheme.typography.bodyLarge, color = EnzonaTheme.colors.onSurface)
            Text(transfer.subtitle(), style = MaterialTheme.typography.bodyMedium, color = EnzonaTheme.colors.onSurfaceVariant)
        }
    }
    val tone = when (transfer.direction) {
        TransferDirection.OUTGOING -> EzAmountTone.Negative
        TransferDirection.INCOMING -> EzAmountTone.Positive
        TransferDirection.UNKNOWN -> EzAmountTone.Neutral
    }
    val amount: @Composable () -> Unit = { EzAmount(transfer.amountText(), currency = transfer.currencyText(), tone = tone) }
    val details: @Composable () -> Unit = {
        Text(transfer.dateText(), style = MaterialTheme.typography.bodySmall, color = EnzonaTheme.colors.onSurfaceVariant)
        EzStatusPill(transfer.statusLabel(), transfer.statusTone())
    }
    if (isLargeFont()) {
        Column(rowModifier, verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
                icon()
                titles(Modifier.weight(1f))
            }
            amount()
            details()
        }
    } else {
        Row(rowModifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
            icon()
            titles(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                amount()
                details()
            }
        }
    }
}
