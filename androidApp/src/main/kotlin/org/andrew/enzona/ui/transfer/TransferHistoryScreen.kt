package org.andrew.enzona.ui.transfer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.andrew.enzona.feature.transfer.domain.TransferFilter
import org.andrew.enzona.feature.transfer.domain.TransferStatus
import org.andrew.enzona.feature.transfer.presentation.TransferHistoryUiState
import org.andrew.enzona.feature.transfer.presentation.TransferHistoryViewModel
import org.andrew.enzona.ui.designsystem.EnzonaTheme
import org.andrew.enzona.ui.designsystem.components.EzButton
import org.andrew.enzona.ui.designsystem.components.EzButtonStyle
import org.andrew.enzona.ui.designsystem.components.EzDivider
import org.andrew.enzona.ui.designsystem.components.EzEmptyState
import org.andrew.enzona.ui.designsystem.components.EzErrorState
import org.andrew.enzona.ui.designsystem.components.EzGradientTopAppBar
import org.andrew.enzona.ui.designsystem.components.EzLoadingState
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TransferHistoryRoute(onBack: () -> Unit, vm: TransferHistoryViewModel = koinViewModel()) {
    LaunchedEffect(Unit) { vm.load() }
    val state by vm.state.collectAsStateWithLifecycle()
    TransferHistoryScreen(state, onBack, onRetry = vm::load, onFilter = vm::applyFilter, onLoadMore = vm::loadMore)
}

/** "Todas" (sin filtro) y luego los estados por los que el dominio permite filtrar, con su etiqueta en plural. */
private val STATUS_CHOICES: List<Pair<TransferStatus?, String>> =
    listOf<TransferStatus?>(null).plus(TransferStatus.filterable).map { it to it.filterLabel() }

private fun TransferStatus?.filterLabel(): String = when (this) {
    null -> "Todas"
    TransferStatus.ACCEPTED -> "Aceptadas"
    TransferStatus.PENDING -> "Pendientes"
    TransferStatus.FAILED -> "Fallidas"
    TransferStatus.OTHER -> "Otras"
}

/** Fecha como la muestra el filtro: `dd/MM/yyyy`. */
internal fun LocalDate.display(): String =
    "${dayOfMonth.toString().padStart(2, '0')}/${monthNumber.toString().padStart(2, '0')}/${year.toString().padStart(4, '0')}"

/** Historial con filtros (estado y rango de fechas) y paginación al llegar al final. */
@Composable
fun TransferHistoryScreen(
    state: TransferHistoryUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onFilter: (TransferFilter) -> Unit,
    onLoadMore: () -> Unit,
) {
    // Mientras carga no hay filtro en el estado: la pantalla recuerda el último que pidió.
    var requested by remember { mutableStateOf(TransferFilter.None) }
    val filter = when (state) {
        is TransferHistoryUiState.Content -> state.filter
        is TransferHistoryUiState.Error -> state.filter
        is TransferHistoryUiState.Loading -> requested
    }
    fun apply(next: TransferFilter) { requested = next; onFilter(next) }

    Scaffold(
        topBar = { EzGradientTopAppBar("Historial", onBack) },
        containerColor = EnzonaTheme.colors.background,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Filters(filter, ::apply)
            EzDivider()
            Box(Modifier.fillMaxSize()) {
                when (state) {
                    is TransferHistoryUiState.Loading -> EzLoadingState()
                    is TransferHistoryUiState.Error -> EzErrorState(state.message, onRetry)
                    is TransferHistoryUiState.Content -> HistoryList(state, onLoadMore)
                }
            }
        }
    }
}

@Composable
private fun HistoryList(state: TransferHistoryUiState.Content, onLoadMore: () -> Unit) {
    if (state.items.isEmpty() && !state.hasMore) {
        EzEmptyState(
            title = "Sin transferencias",
            message = if (state.filter == TransferFilter.None) "Aún no has hecho transferencias." else "No hay transferencias con estos filtros.",
        )
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(state.items, key = { it.id }) {
            TransferRow(it)
            EzDivider()
        }
        if (state.skipped > 0) item { Message("No se pudieron mostrar ${state.skipped} transferencias.") }
        if (state.hasMore) {
            item(key = "more") {
                val error = state.moreError
                when {
                    error != null -> Column(Modifier.fillMaxWidth().padding(EnzonaTheme.spacing.lg), verticalArrangement = Arrangement.spacedBy(EnzonaTheme.spacing.sm)) {
                        Message(error)
                        EzButton("Reintentar", onLoadMore, style = EzButtonStyle.Secondary)
                    }
                    else -> {
                        // Al aparecer el final de la lista se pide la siguiente página (el ViewModel ignora repeticiones).
                        LaunchedEffect(state.items.size) { onLoadMore() }
                        Box(Modifier.fillMaxWidth().padding(EnzonaTheme.spacing.lg)) { EzLoadingState() }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Filters(filter: TransferFilter, onChange: (TransferFilter) -> Unit) {
    val spacing = EnzonaTheme.spacing
    var picking by remember { mutableStateOf<DateField?>(null) }
    Column(Modifier.fillMaxWidth().padding(spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            STATUS_CHOICES.forEach { (status, label) ->
                FilterChip(selected = filter.status == status, onClick = { onChange(filter.copy(status = status)) }, label = { Text(label) })
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            FilterChip(selected = filter.startDate != null, onClick = { picking = DateField.START },
                label = { Text(filter.startDate?.let { "Desde ${it.display()}" } ?: "Desde") })
            FilterChip(selected = filter.endDate != null, onClick = { picking = DateField.END },
                label = { Text(filter.endDate?.let { "Hasta ${it.display()}" } ?: "Hasta") })
            if (filter != TransferFilter.None) TextButton(onClick = { onChange(TransferFilter.None) }) { Text("Limpiar") }
        }
    }
    picking?.let { field ->
        val current = if (field == DateField.START) filter.startDate else filter.endDate
        // El selector trabaja en UTC a medianoche: se convierte de y a fecha de calendario sin desfase de zona.
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = current?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds())
        DatePickerDialog(
            onDismissRequest = { picking = null },
            confirmButton = {
                TextButton(onClick = {
                    val picked = pickerState.selectedDateMillis?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date }
                    onChange(if (field == DateField.START) filter.copy(startDate = picked) else filter.copy(endDate = picked))
                    picking = null
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { picking = null }) { Text("Cancelar") } },
        ) { DatePicker(pickerState) }
    }
}

private enum class DateField { START, END }

@Preview(name = "Historial", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun TransferHistoryPreview() = EnzonaTheme(darkTheme = false) {
    TransferHistoryScreen(
        TransferHistoryUiState.Content(TransferPreviewData.transfers, 0, TransferFilter.None, hasMore = false), {}, {}, {}, {},
    )
}

@Preview(name = "Historial (fuente grande)", showBackground = true, widthDp = 360, heightDp = 900, fontScale = 1.6f)
@Composable
private fun TransferHistoryLargeFontPreview() = EnzonaTheme(darkTheme = false) {
    TransferHistoryScreen(
        TransferHistoryUiState.Content(TransferPreviewData.transfers, 0, TransferFilter.None, hasMore = false), {}, {}, {}, {},
    )
}
