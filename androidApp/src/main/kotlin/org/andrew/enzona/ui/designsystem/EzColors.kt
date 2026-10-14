package org.andrew.enzona.ui.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Paleta semántica. Los valores cumplen WCAG AA y se validan con `tools/check_contrast.py`:
 * si cambias un color aquí, cámbialo también allí (y en EzColors.swift) y ejecuta el script.
 *
 * `brand*` es decorativo y NO cambia con el tema: el azul de marca da solo 3.27:1 con texto
 * blanco, así que no puede llevar texto. Para botones y enlaces se usa `primary`.
 */
@Immutable
data class EzColors(
    val isDark: Boolean,
    // Marca (decorativa, igual en claro y oscuro)
    val brandBlue: Color,
    val brandBlueLight: Color,
    val brandNavy: Color,
    // Acción
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    // Superficies
    val background: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    /** Borde de controles (>= 3:1). */
    val outline: Color,
    /** Separadores decorativos. */
    val divider: Color,
    val scrim: Color,
    // Estados
    val success: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val info: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
)

private val BrandBlue = Color(0xFF2196D8)
private val BrandBlueLight = Color(0xFF1E96D6)
private val BrandNavy = Color(0xFF0F2A5E)

val LightEzColors = EzColors(
    isDark = false,
    brandBlue = BrandBlue, brandBlueLight = BrandBlueLight, brandNavy = BrandNavy,
    primary = Color(0xFF0B6FB5), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6EBF8), onPrimaryContainer = Color(0xFF0F2A5E),
    background = Color(0xFFFFFFFF), surface = Color(0xFFFFFFFF), onSurface = Color(0xFF12202C),
    surfaceVariant = Color(0xFFF1F5F9), onSurfaceVariant = Color(0xFF51606E),
    outline = Color(0xFF7A8895), divider = Color(0xFFC9D3DC), scrim = Color(0x99000000),
    success = Color(0xFF1B7A43), successContainer = Color(0xFFD8F3E3), onSuccessContainer = Color(0xFF0E4F2B),
    warning = Color(0xFF8A5200), warningContainer = Color(0xFFFFEFCC), onWarningContainer = Color(0xFF6B3F00),
    error = Color(0xFFC62828), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFDE0DE), onErrorContainer = Color(0xFF7A1212),
    info = Color(0xFF0B6FB5), infoContainer = Color(0xFFD6EBF8), onInfoContainer = Color(0xFF0F2A5E),
)

val DarkEzColors = EzColors(
    isDark = true,
    brandBlue = BrandBlue, brandBlueLight = BrandBlueLight, brandNavy = BrandNavy,
    primary = Color(0xFF6EC1F2), onPrimary = Color(0xFF06263D),
    primaryContainer = Color(0xFF0F4568), onPrimaryContainer = Color(0xFFCDE9FA),
    background = Color(0xFF0B1218), surface = Color(0xFF111A22), onSurface = Color(0xFFE6EDF3),
    surfaceVariant = Color(0xFF1C2833), onSurfaceVariant = Color(0xFFA9B7C4),
    outline = Color(0xFF7F8EA0), divider = Color(0xFF2A3744), scrim = Color(0xB3000000),
    success = Color(0xFF5FD08A), successContainer = Color(0xFF0F3D24), onSuccessContainer = Color(0xFFBDECD0),
    warning = Color(0xFFF2B84B), warningContainer = Color(0xFF4A3300), onWarningContainer = Color(0xFFFFE2A8),
    error = Color(0xFFFF8A80), onError = Color(0xFF4A0A0A),
    errorContainer = Color(0xFF5C1A1A), onErrorContainer = Color(0xFFFFDAD6),
    info = Color(0xFF6EC1F2), infoContainer = Color(0xFF0F4568), onInfoContainer = Color(0xFFCDE9FA),
)
