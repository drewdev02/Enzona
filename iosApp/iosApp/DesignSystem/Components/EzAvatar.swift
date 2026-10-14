import SwiftUI

/// Avatar con iniciales. La foto remota llegará con una carga de imágenes.
struct EzAvatar: View {
    let initials: String
    var size: CGFloat = Ez.Size.avatarMd

    var body: some View {
        Text(initials)
            .font(.system(size: size * 0.38, weight: .semibold))
            .foregroundStyle(Ez.Palette.onPrimaryContainer)
            .frame(width: size, height: size)
            .background(Circle().fill(Ez.Palette.primaryContainer))
            .accessibilityHidden(true)
    }
}
