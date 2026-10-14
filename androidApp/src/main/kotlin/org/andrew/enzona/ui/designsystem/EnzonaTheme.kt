package org.andrew.enzona.ui.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalEzColors = staticCompositionLocalOf { LightEzColors }
private val LocalEzSpacing = staticCompositionLocalOf { EzSpacing() }
private val LocalEzSizes = staticCompositionLocalOf { EzSizes() }
private val LocalEzShapes = staticCompositionLocalOf { EzShapes() }
private val LocalEzElevation = staticCompositionLocalOf { EzElevation() }
private val LocalEzExtraTypography = staticCompositionLocalOf { EzExtraTypography() }

/** Acceso a los tokens: `EnzonaTheme.spacing.lg`, `EnzonaTheme.colors.primary`, etc. */
object EnzonaTheme {
    val colors: EzColors @Composable @ReadOnlyComposable get() = LocalEzColors.current
    val spacing: EzSpacing @Composable @ReadOnlyComposable get() = LocalEzSpacing.current
    val sizes: EzSizes @Composable @ReadOnlyComposable get() = LocalEzSizes.current
    val shapes: EzShapes @Composable @ReadOnlyComposable get() = LocalEzShapes.current
    val elevation: EzElevation @Composable @ReadOnlyComposable get() = LocalEzElevation.current
    val extraTypography: EzExtraTypography @Composable @ReadOnlyComposable get() = LocalEzExtraTypography.current
}

/** Tema raíz. También configura MaterialTheme para que los componentes M3 hereden la paleta. */
@Composable
fun EnzonaTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val c = if (darkTheme) DarkEzColors else LightEzColors
    val shapes = EzShapes()
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = c.primary, onPrimary = c.onPrimary,
            primaryContainer = c.primaryContainer, onPrimaryContainer = c.onPrimaryContainer,
            // Sin esto Material usa su lavanda por defecto en los botones secundarios.
            secondary = c.primary, onSecondary = c.onPrimary,
            secondaryContainer = c.primaryContainer, onSecondaryContainer = c.onPrimaryContainer,
            background = c.background, onBackground = c.onSurface,
            surface = c.surface, onSurface = c.onSurface,
            surfaceVariant = c.surfaceVariant, onSurfaceVariant = c.onSurfaceVariant,
            outline = c.outline, outlineVariant = c.divider, scrim = c.scrim,
            error = c.error, onError = c.onError,
            errorContainer = c.errorContainer, onErrorContainer = c.onErrorContainer,
        )
    } else {
        lightColorScheme(
            primary = c.primary, onPrimary = c.onPrimary,
            primaryContainer = c.primaryContainer, onPrimaryContainer = c.onPrimaryContainer,
            // Sin esto Material usa su lavanda por defecto en los botones secundarios.
            secondary = c.primary, onSecondary = c.onPrimary,
            secondaryContainer = c.primaryContainer, onSecondaryContainer = c.onPrimaryContainer,
            background = c.background, onBackground = c.onSurface,
            surface = c.surface, onSurface = c.onSurface,
            surfaceVariant = c.surfaceVariant, onSurfaceVariant = c.onSurfaceVariant,
            outline = c.outline, outlineVariant = c.divider, scrim = c.scrim,
            error = c.error, onError = c.onError,
            errorContainer = c.errorContainer, onErrorContainer = c.onErrorContainer,
        )
    }
    CompositionLocalProvider(
        LocalEzColors provides c,
        LocalEzShapes provides shapes,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = EzMaterialTypography,
            shapes = Shapes(
                extraSmall = shapes.xs, small = shapes.sm, medium = shapes.md,
                large = shapes.lg, extraLarge = shapes.xl,
            ),
            content = content,
        )
    }
}
