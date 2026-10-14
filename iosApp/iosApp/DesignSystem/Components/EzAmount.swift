import SwiftUI

enum EzAmountTone { case neutral, positive, negative }

/// Importe con moneda. `value` llega ya formateado como String: el backend devuelve los montos
/// como texto y el dinero nunca se pasa por Double. El color refuerza el signo, no lo sustituye.
struct EzAmount: View {
    let value: String
    var currency: String? = nil
    var tone: EzAmountTone = .neutral
    var large = false

    private var color: Color {
        switch tone {
        case .neutral: Ez.Palette.onSurface
        case .positive: Ez.Palette.success
        case .negative: Ez.Palette.error
        }
    }

    var body: some View {
        HStack(alignment: .lastTextBaseline, spacing: Ez.Space.xs) {
            Text(value).font(large ? Ez.Typography.amountLarge : Ez.Typography.amount).foregroundStyle(color)
            if let currency {
                Text(currency).font(Ez.Typography.labelMedium).foregroundStyle(Ez.Palette.onSurfaceVariant)
            }
        }
        .accessibilityElement(children: .combine)
    }
}
