import SwiftUI

/// Una transferencia en una lista: logo del banco del destinatario, destinatario y origen, importe, estado y fecha.
/// Con texto de accesibilidad la fila se apila para que el importe nunca se corte ni se parta.
struct TransferRowView: View {
    let row: TransferRowModel
    @Environment(\.dynamicTypeSize) private var typeSize

    var body: some View {
        Group {
            if typeSize.isAccessibilitySize {
                VStack(alignment: .leading, spacing: Ez.Space.sm) {
                    HStack(spacing: Ez.Space.lg) {
                        logo
                        titles
                    }
                    EzAmount(value: row.amountText, currency: row.currency, tone: row.amountTone)
                    Text(row.dateText).font(Ez.Typography.bodySmall).foregroundStyle(Ez.Palette.onSurfaceVariant)
                    EzStatusPill(text: row.statusText, tone: row.statusTone)
                }
            } else {
                HStack(spacing: Ez.Space.lg) {
                    logo
                    titles
                    Spacer(minLength: Ez.Space.sm)
                    VStack(alignment: .trailing, spacing: Ez.Space.xxs) {
                        EzAmount(value: row.amountText, currency: row.currency, tone: row.amountTone)
                        Text(row.dateText).font(Ez.Typography.bodySmall).foregroundStyle(Ez.Palette.onSurfaceVariant)
                        EzStatusPill(text: row.statusText, tone: row.statusTone)
                    }
                    .fixedSize(horizontal: true, vertical: false)
                }
            }
        }
        .padding(.horizontal, Ez.Space.lg)
        .padding(.vertical, Ez.Space.md)
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(row.spoken)
    }

    private var logo: some View {
        Image(row.logoName)
            .resizable()
            .scaledToFit()
            .frame(width: Ez.Size.avatarLg, height: Ez.Size.avatarLg)
            .clipShape(Circle())
            .accessibilityHidden(true)
    }

    private var titles: some View {
        VStack(alignment: .leading, spacing: Ez.Space.xxs) {
            Text(row.title).font(Ez.Typography.bodyLarge).foregroundStyle(Ez.Palette.onSurface)
            Text(row.subtitle).font(Ez.Typography.bodyMedium).foregroundStyle(Ez.Palette.onSurfaceVariant)
        }
    }
}

/// Texto centrado para avisos dentro de una lista ("Aún no has hecho transferencias").
struct TransferNotice: View {
    let text: String

    var body: some View {
        Text(text)
            .font(Ez.Typography.bodyMedium)
            .foregroundStyle(Ez.Palette.onSurfaceVariant)
            .multilineTextAlignment(.center)
            .frame(maxWidth: .infinity)
            .padding(Ez.Space.xl)
    }
}
