package org.andrew.enzona.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.painterResource
import org.andrew.enzona.feature.home.domain.HomeService
import org.andrew.enzona.feature.home.domain.HomeServices
import org.andrew.enzona.ui.designsystem.EnzonaTheme
import org.andrew.enzona.ui.designsystem.WhiteStatusBarIcons
import org.andrew.enzona.ui.designsystem.components.EzServiceGrid
import org.andrew.enzona.ui.designsystem.components.EzServiceTile
import org.andrew.enzona.ui.designsystem.icons.BrandBadge
import org.andrew.enzona.ui.designsystem.icons.ReceiveGlyph
import org.andrew.enzona.ui.designsystem.icons.ScanGlyph

/** La cuadrícula es configuración estática: no depende de la cuenta ni de la red. */
@Composable
fun DashboardRoute(onScan: () -> Unit, onReceive: () -> Unit, onServiceClick: (HomeService) -> Unit) {
    DashboardScreen(onScan, onReceive, onServiceClick)
}

@Composable
fun DashboardScreen(
    onScan: () -> Unit,
    onReceive: () -> Unit,
    onServiceClick: (HomeService) -> Unit,
    modifier: Modifier = Modifier,
    services: List<HomeService> = HomeServices.all,
) {
    WhiteStatusBarIcons()
    val colors = EnzonaTheme.colors
    val spacing = EnzonaTheme.spacing
    val sizes = EnzonaTheme.sizes
    val statusBar = with(LocalDensity.current) { WindowInsets.statusBars.getTop(this).toDp() }
    // La hoja arranca bajo las acciones; la insignia queda centrada en su borde superior.
    val sheetTop = statusBar + spacing.xl + sizes.heroActionsHeight

    Box(
        modifier
            .fillMaxSize()
            .background(Brush.horizontalGradient(listOf(colors.brandBlueLight, colors.brandNavy)))
    ) {
        Row(
            Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = spacing.xl)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            HeaderAction("Escanear", onScan) { ScanGlyph(Modifier.size(sizes.heroGlyph)) }
            HeaderAction("Recibir", onReceive) { ReceiveGlyph(Modifier.size(sizes.heroGlyph)) }
        }

        Surface(
            modifier = Modifier.fillMaxSize().padding(top = sheetTop),
            shape = EnzonaTheme.shapes.sheet,
            color = colors.surface,
        ) {
            EzServiceGrid(
                items = services,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = spacing.lg, end = spacing.lg,
                    top = sizes.brandBadge / 2 + spacing.xl, bottom = spacing.lg,
                ),
            ) { service ->
                EzServiceTile(
                    label = service.label,
                    icon = painterResource(service.iconRes),
                    onClick = { onServiceClick(service) },
                )
            }
        }

        BrandBadge(
            Modifier.align(Alignment.TopCenter).offset(y = sheetTop - sizes.brandBadge / 2),
            diameter = sizes.brandBadge,
        )
    }
}

@Composable
private fun HeaderAction(label: String, onClick: () -> Unit, glyph: @Composable () -> Unit) {
    Column(
        Modifier
            .clickable(onClickLabel = label, role = Role.Button, onClick = onClick)
            .padding(EnzonaTheme.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        glyph()
        Text(label, color = Color.White, style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = EnzonaTheme.spacing.sm))
    }
}

// ---- Previews (no tocan Koin ni la red) ----

@Preview(name = "Inicio", showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun DashboardPreview() = EnzonaTheme(darkTheme = false) { DashboardScreen({}, {}, {}) }

@Preview(name = "Inicio (oscuro)", showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun DashboardDarkPreview() = EnzonaTheme(darkTheme = true) { DashboardScreen({}, {}, {}) }

@Preview(name = "Inicio (fuente x1.5)", showBackground = true, widthDp = 360, heightDp = 780, fontScale = 1.5f)
@Composable
private fun DashboardLargeFontPreview() = EnzonaTheme(darkTheme = false) { DashboardScreen({}, {}, {}) }

@Preview(name = "Inicio (pantalla estrecha)", showBackground = true, widthDp = 320, heightDp = 640)
@Composable
private fun DashboardNarrowPreview() = EnzonaTheme(darkTheme = false) { DashboardScreen({}, {}, {}) }
