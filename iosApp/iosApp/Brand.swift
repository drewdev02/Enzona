import SwiftUI

/// Paleta aproximada del mockup. Ajustar aquí cuando haya guía de marca propia.
enum Brand {
    static let blueLight = Color(red: 0x1E / 255, green: 0x96 / 255, blue: 0xD6 / 255)
    static let blue = Color(red: 0x21 / 255, green: 0x96 / 255, blue: 0xD8 / 255)
    static let navy = Color(red: 0x0F / 255, green: 0x2A / 255, blue: 0x5E / 255)
}

/// Tres barras: PLACEHOLDER neutro de la marca. Reemplazar por el logotipo propio.
private struct BrandBars: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        for (y, end) in [(0.25, 0.90), (0.50, 1.00), (0.75, 0.70)] {
            path.move(to: CGPoint(x: 0, y: rect.height * y))
            path.addLine(to: CGPoint(x: rect.width * end, y: rect.height * y))
        }
        return path
    }
}

/// Insignia circular de la marca. Es el único sitio que la dibuja.
struct BrandBadge: View {
    var diameter: CGFloat = 80

    var body: some View {
        ZStack {
            Circle().fill(.white)
            Circle().fill(Brand.blue).padding(4)
            BrandBars()
                .stroke(.white, style: StrokeStyle(lineWidth: diameter * 0.065, lineCap: .round))
                .frame(width: diameter * 0.5, height: diameter * 0.5)
        }
        .frame(width: diameter, height: diameter)
        .accessibilityHidden(true)
    }
}
