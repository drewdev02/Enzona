package org.andrew.enzona.ui.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import org.andrew.enzona.ui.designsystem.EnzonaTheme

/** Avatar con iniciales. La foto remota llegará con una librería de imágenes. */
@Composable
fun EzAvatar(
    initials: String,
    modifier: Modifier = Modifier,
    size: Dp = EnzonaTheme.sizes.avatarMd,
) {
    Box(
        modifier.size(size).clip(CircleShape).background(EnzonaTheme.colors.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initials,
            color = EnzonaTheme.colors.onPrimaryContainer,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = (size.value * 0.38f).sp),
        )
    }
}
