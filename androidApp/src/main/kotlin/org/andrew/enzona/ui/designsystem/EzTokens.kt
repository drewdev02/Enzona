package org.andrew.enzona.ui.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Escala de espaciado sobre una rejilla de 4dp. Usar siempre estos valores, nunca números sueltos. */
@Immutable
data class EzSpacing(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val xxxl: Dp = 48.dp,
)

/** Tamaños de componentes e iconos. Objetivo táctil mínimo: 48dp. */
@Immutable
data class EzSizes(
    val iconSm: Dp = 16.dp,
    val iconMd: Dp = 24.dp,
    val iconLg: Dp = 32.dp,
    val iconXl: Dp = 48.dp,
    val avatarSm: Dp = 32.dp,
    val avatarMd: Dp = 40.dp,
    val avatarLg: Dp = 56.dp,
    val avatarXl: Dp = 88.dp,
    val minTouchTarget: Dp = 48.dp,
    val buttonHeight: Dp = 52.dp,
    val fieldHeight: Dp = 56.dp,
    val listItemMinHeight: Dp = 56.dp,
    val bottomBarHeight: Dp = 56.dp,
    val divider: Dp = 1.dp,
    // Patrón del dashboard
    val brandBadge: Dp = 80.dp,
    val heroGlyph: Dp = 76.dp,
    val heroActionsHeight: Dp = 172.dp,
    /** Icono de una casilla de servicio (el PNG ya trae su marco y sombra). */
    val serviceIcon: Dp = 72.dp,
)

/** Radios de esquina. `pill` = totalmente redondeado (botones, barra inferior). */
@Immutable
data class EzShapes(
    val none: RoundedCornerShape = RoundedCornerShape(0.dp),
    val xs: RoundedCornerShape = RoundedCornerShape(4.dp),
    val sm: RoundedCornerShape = RoundedCornerShape(8.dp),
    val md: RoundedCornerShape = RoundedCornerShape(12.dp),
    val lg: RoundedCornerShape = RoundedCornerShape(16.dp),
    val xl: RoundedCornerShape = RoundedCornerShape(28.dp),
    val pill: RoundedCornerShape = RoundedCornerShape(50),
    /** Hoja blanca del dashboard: solo esquinas superiores. */
    val sheet: RoundedCornerShape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
)

@Immutable
data class EzElevation(
    val none: Dp = 0.dp,
    val low: Dp = 1.dp,
    val medium: Dp = 3.dp,
    val high: Dp = 6.dp,
)

/** Opacidades de estado. */
object EzAlpha {
    const val Disabled = 0.38f
    const val Pressed = 0.85f
}
