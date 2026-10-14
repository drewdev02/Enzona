import SwiftUI
import UIKit

private extension UIColor {
    convenience init(hex: UInt32) {
        self.init(
            red: CGFloat((hex >> 16) & 0xFF) / 255,
            green: CGFloat((hex >> 8) & 0xFF) / 255,
            blue: CGFloat(hex & 0xFF) / 255,
            alpha: 1
        )
    }
}

private extension Color {
    /// Color dinámico: cambia solo entre modo claro y oscuro.
    init(light: UInt32, dark: UInt32) {
        self.init(uiColor: UIColor { $0.userInterfaceStyle == .dark ? UIColor(hex: dark) : UIColor(hex: light) })
    }
    init(hex: UInt32) { self.init(uiColor: UIColor(hex: hex)) }
}

extension Ez {
    /// Paleta semántica. Los valores cumplen WCAG AA y se validan con `tools/check_contrast.py`:
    /// si cambias un color, cámbialo también allí y en EzColors.kt, y ejecuta el script.
    ///
    /// `brand*` es decorativo y NO cambia con el tema: el azul de marca da solo 3.27:1 con texto
    /// blanco, así que no puede llevar texto. Para botones y enlaces se usa `primary`.
    enum Palette {
        // Marca (decorativa, igual en claro y oscuro)
        static let brandBlue = Color(hex: 0x2196D8)
        static let brandBlueLight = Color(hex: 0x1E96D6)
        static let brandNavy = Color(hex: 0x0F2A5E)

        // Acción
        static let primary = Color(light: 0x0B6FB5, dark: 0x6EC1F2)
        static let onPrimary = Color(light: 0xFFFFFF, dark: 0x06263D)
        static let primaryContainer = Color(light: 0xD6EBF8, dark: 0x0F4568)
        static let onPrimaryContainer = Color(light: 0x0F2A5E, dark: 0xCDE9FA)

        // Superficies
        static let background = Color(light: 0xFFFFFF, dark: 0x0B1218)
        static let surface = Color(light: 0xFFFFFF, dark: 0x111A22)
        static let onSurface = Color(light: 0x12202C, dark: 0xE6EDF3)
        static let surfaceVariant = Color(light: 0xF1F5F9, dark: 0x1C2833)
        static let onSurfaceVariant = Color(light: 0x51606E, dark: 0xA9B7C4)
        /// Borde de controles (>= 3:1).
        static let outline = Color(light: 0x7A8895, dark: 0x7F8EA0)
        /// Separadores decorativos.
        static let divider = Color(light: 0xC9D3DC, dark: 0x2A3744)

        // Estados
        static let success = Color(light: 0x1B7A43, dark: 0x5FD08A)
        static let successContainer = Color(light: 0xD8F3E3, dark: 0x0F3D24)
        static let onSuccessContainer = Color(light: 0x0E4F2B, dark: 0xBDECD0)
        static let warning = Color(light: 0x8A5200, dark: 0xF2B84B)
        static let warningContainer = Color(light: 0xFFEFCC, dark: 0x4A3300)
        static let onWarningContainer = Color(light: 0x6B3F00, dark: 0xFFE2A8)
        static let error = Color(light: 0xC62828, dark: 0xFF8A80)
        static let onError = Color(light: 0xFFFFFF, dark: 0x4A0A0A)
        static let errorContainer = Color(light: 0xFDE0DE, dark: 0x5C1A1A)
        static let onErrorContainer = Color(light: 0x7A1212, dark: 0xFFDAD6)
        static let info = Color(light: 0x0B6FB5, dark: 0x6EC1F2)
        static let infoContainer = Color(light: 0xD6EBF8, dark: 0x0F4568)
        static let onInfoContainer = Color(light: 0x0F2A5E, dark: 0xCDE9FA)
    }

    enum Tone { case success, warning, error, info, neutral }
}

extension Ez.Tone {
    var container: Color {
        switch self {
        case .success: Ez.Palette.successContainer
        case .warning: Ez.Palette.warningContainer
        case .error: Ez.Palette.errorContainer
        case .info: Ez.Palette.infoContainer
        case .neutral: Ez.Palette.surfaceVariant
        }
    }
    var content: Color {
        switch self {
        case .success: Ez.Palette.onSuccessContainer
        case .warning: Ez.Palette.onWarningContainer
        case .error: Ez.Palette.onErrorContainer
        case .info: Ez.Palette.onInfoContainer
        case .neutral: Ez.Palette.onSurfaceVariant
        }
    }
}
