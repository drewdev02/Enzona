import SwiftUI

extension Ez {
    /// Escala tipográfica sobre los estilos de texto del sistema: respeta Dynamic Type.
    /// Los nombres coinciden con los roles de Android (Material 3).
    enum Typography {
        static let displayMedium = Font.largeTitle.weight(.bold)
        static let headlineLarge = Font.title.weight(.bold)
        static let headlineMedium = Font.title2.weight(.semibold)
        static let headlineSmall = Font.title3.weight(.semibold)
        static let titleLarge = Font.headline
        static let titleMedium = Font.headline.weight(.medium)
        static let titleSmall = Font.subheadline.weight(.medium)
        static let bodyLarge = Font.body
        static let bodyMedium = Font.subheadline
        static let bodySmall = Font.footnote
        static let labelLarge = Font.subheadline.weight(.medium)
        static let labelMedium = Font.caption.weight(.medium)
        static let labelSmall = Font.caption2.weight(.medium)

        /// Importes: cifras tabulares para que los de una lista se alineen.
        static let amount = Font.headline.weight(.semibold).monospacedDigit()
        static let amountLarge = Font.largeTitle.weight(.bold).monospacedDigit()
    }
}
