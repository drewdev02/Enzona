import SwiftUI

/// Namespace del sistema de diseño. Los nombres de roles coinciden con los de Android
/// (`EnzonaTheme.spacing.lg` ↔ `Ez.Space.lg`) para que ambos lados se correspondan.
enum Ez {

    /// Escala de espaciado sobre una rejilla de 4pt. Usar siempre estos valores, nunca números sueltos.
    enum Space {
        static let xxs: CGFloat = 2
        static let xs: CGFloat = 4
        static let sm: CGFloat = 8
        static let md: CGFloat = 12
        static let lg: CGFloat = 16
        static let xl: CGFloat = 24
        static let xxl: CGFloat = 32
        static let xxxl: CGFloat = 48
    }

    /// Radios de esquina. `pill` = totalmente redondeado (usar `Capsule()`).
    enum Radius {
        static let xs: CGFloat = 4
        static let sm: CGFloat = 8
        static let md: CGFloat = 12
        static let lg: CGFloat = 16
        static let xl: CGFloat = 28
        /// Hoja blanca del dashboard (solo esquinas superiores).
        static let sheet: CGFloat = 36
    }

    /// Tamaños de componentes e iconos. Objetivo táctil mínimo en iOS: 44pt.
    enum Size {
        static let iconSm: CGFloat = 16
        static let iconMd: CGFloat = 24
        static let iconLg: CGFloat = 32
        static let iconXl: CGFloat = 48
        static let avatarSm: CGFloat = 32
        static let avatarMd: CGFloat = 40
        static let avatarLg: CGFloat = 56
        static let avatarXl: CGFloat = 88
        static let minTouchTarget: CGFloat = 44
        static let buttonHeight: CGFloat = 52
        static let fieldHeight: CGFloat = 56
        static let listItemMinHeight: CGFloat = 56
        static let divider: CGFloat = 1
        // Patrón del dashboard
        static let brandBadge: CGFloat = 80
        static let heroGlyph: CGFloat = 64
        static let heroActionsHeight: CGFloat = 172
        /// Icono de una casilla de servicio (el PNG ya trae su marco y sombra).
        static let serviceIcon: CGFloat = 72
    }

    enum Elevation {
        static let low: CGFloat = 1
        static let medium: CGFloat = 3
        static let high: CGFloat = 6
    }

    enum Opacity {
        static let disabled: Double = 0.38
        static let pressed: Double = 0.85
    }

    /// Geometría de la tarjeta bancaria, medida sobre el original. Si cambias estos valores, revisa
    /// también `tools/check_card_skins.py` (las zonas de texto y el recorte salen de aquí).
    enum Card {
        /// Alto / ancho: el original usa 0.55 y recorta la imagen (BoxFit.cover).
        static let heightRatio: CGFloat = 0.55
        /// Margen izquierdo del texto, en fracción del ancho.
        static let startInset: CGFloat = 0.044
        /// Radio de esquina, en fracción del ancho (el de los PNG: ~20px de 630).
        static let cornerRadius: CGFloat = 0.032
        /// Centro vertical de cada fila de texto, en fracción del alto.
        static let numberCenter: CGFloat = 0.51
        static let nameCenter: CGFloat = 0.776
        static let infoCenter: CGFloat = 0.88
        /// Posición de los iconos de arriba a la derecha.
        static let iconCenterY: CGFloat = 0.175
        static let menuCenterX: CGFloat = 0.94
        static let checkCenterX: CGFloat = 0.855
        /// Opacidad del velo negro tras el icono cuando el fondo es una foto. Igual a SCRIM en tools/check_card_skins.py.
        static let menuScrimAlpha: Double = 0.35
    }
}
