package org.andrew.enzona.ui.designsystem.components

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.andrew.enzona.ui.designsystem.EnzonaTheme

@Composable
fun EzDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier, thickness = EnzonaTheme.sizes.divider, color = EnzonaTheme.colors.divider)
}
