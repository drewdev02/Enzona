import SwiftUI

struct EzSectionHeader: View {
    let title: String
    var actionText: String? = nil
    var onAction: (() -> Void)? = nil

    var body: some View {
        HStack {
            Text(title).font(Ez.Typography.titleMedium).foregroundStyle(Ez.Palette.onSurface)
            Spacer()
            if let actionText, let onAction {
                Button(actionText, action: onAction)
                    .font(Ez.Typography.labelLarge)
                    .foregroundStyle(Ez.Palette.primary)
            }
        }
        .padding(.horizontal, Ez.Space.lg)
        .padding(.vertical, Ez.Space.sm)
        .accessibilityAddTraits(.isHeader)
    }
}

struct EzDivider: View {
    var body: some View {
        Rectangle().fill(Ez.Palette.divider).frame(height: Ez.Size.divider).accessibilityHidden(true)
    }
}
