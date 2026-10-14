package org.andrew.enzona.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import org.andrew.enzona.ui.designsystem.EnzonaTheme
import org.andrew.enzona.ui.designsystem.icons.GroupGlyph

/** Destinos de primer nivel de la zona autenticada. */
enum class TopLevel(val label: String, val route: Any) {
    HOME("Inicio", Home),
    NOTIFICATIONS("Avisos", Notifications),
    FRIENDS("Amigos", Friends),
    PROFILE("Perfil", Profile),
}

/**
 * Barra inferior en píldora. Es una Row propia y no `NavigationBar`: esa tiene 80dp fijos
 * y no da la píldora delgada del diseño. Cada ítem expone semántica de pestaña.
 */
@Composable
fun EnzonaBottomBar(selected: TopLevel, onSelect: (TopLevel) -> Unit, modifier: Modifier = Modifier) {
    val spacing = EnzonaTheme.spacing
    val sizes = EnzonaTheme.sizes
    val pill = EnzonaTheme.shapes.pill
    Row(
        modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = spacing.md, vertical = spacing.sm)
            .shadow(EnzonaTheme.elevation.high, pill)
            .background(EnzonaTheme.colors.brandBlue, pill)
            .fillMaxWidth()
            .height(sizes.bottomBarHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TopLevel.entries.forEach { item ->
            val isSelected = item == selected
            val tint = if (isSelected) EnzonaTheme.colors.brandNavy else Color.White
            Box(
                Modifier
                    .weight(1f)
                    .height(sizes.bottomBarHeight)
                    .semantics { this.selected = isSelected; contentDescription = item.label }
                    .clickable(role = Role.Tab, onClickLabel = item.label) { onSelect(item) },
                contentAlignment = Alignment.Center,
            ) {
                when (item) {
                    TopLevel.HOME -> Icon(Icons.Filled.Home, null, Modifier.size(sizes.iconMd + spacing.xs), tint)
                    TopLevel.NOTIFICATIONS -> Icon(Icons.Filled.Notifications, null, Modifier.size(sizes.iconMd + spacing.xs), tint)
                    TopLevel.FRIENDS -> GroupGlyph(Modifier.size(sizes.iconLg - spacing.xxs), tint)
                    TopLevel.PROFILE -> Icon(Icons.Filled.Person, null, Modifier.size(sizes.iconMd + spacing.xs), tint)
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun BottomBarPreview() = EnzonaTheme(darkTheme = false) {
    EnzonaBottomBar(selected = TopLevel.HOME, onSelect = {})
}
