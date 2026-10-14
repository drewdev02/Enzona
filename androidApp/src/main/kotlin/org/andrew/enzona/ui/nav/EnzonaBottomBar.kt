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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import org.andrew.enzona.ui.components.GroupGlyph
import org.andrew.enzona.ui.theme.EnzonaColors
import org.andrew.enzona.ui.theme.EnzonaTheme

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
    Row(
        modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .shadow(6.dp, RoundedCornerShape(50))
            .background(EnzonaColors.Blue, RoundedCornerShape(50))
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TopLevel.entries.forEach { item ->
            val isSelected = item == selected
            val tint = if (isSelected) EnzonaColors.Navy else Color.White
            Box(
                Modifier
                    .weight(1f)
                    .height(56.dp)
                    .semantics { this.selected = isSelected; contentDescription = item.label }
                    .clickable(role = Role.Tab, onClickLabel = item.label) { onSelect(item) },
                contentAlignment = Alignment.Center,
            ) {
                when (item) {
                    TopLevel.HOME -> Icon(Icons.Filled.Home, null, Modifier.size(28.dp), tint)
                    TopLevel.NOTIFICATIONS -> Icon(Icons.Filled.Notifications, null, Modifier.size(28.dp), tint)
                    TopLevel.FRIENDS -> GroupGlyph(Modifier.size(30.dp), tint)
                    TopLevel.PROFILE -> Icon(Icons.Filled.Person, null, Modifier.size(28.dp), tint)
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun BottomBarPreview() = EnzonaTheme {
    EnzonaBottomBar(selected = TopLevel.HOME, onSelect = {})
}
