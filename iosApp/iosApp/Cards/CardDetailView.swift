import SwiftUI
import SharedLogicSwift

/// Detalle de una tarjeta conectado al ViewModel (uno por pantalla, con el id de la tarjeta). El cromo de
/// navegación vive aquí; el contenido (`CardDetailContent`) no depende del framework Kotlin y se previsualiza.
struct CardDetailView: View {
    let cardLabel: String
    @StateObject private var model: CardDetailModel

    init(cardId: String, cardLabel: String) {
        self.cardLabel = cardLabel
        // `wrappedValue` es un autoclosure: el modelo (y su ViewModel) se crea una vez por pantalla.
        _model = StateObject(wrappedValue: CardDetailModel(cardId: cardId))
    }

    var body: some View {
        CardDetailContent(phase: model.phase, cardLabel: cardLabel, onRetry: { model.load() })
            .balanceNavigationChrome()
            .onAppear { model.load() }
    }
}

/// Sin dependencias del framework Kotlin: se puede previsualizar.
struct CardDetailContent: View {
    let phase: CardDetailPhase
    let cardLabel: String
    var onRetry: () -> Void = {}
    @Environment(\.dynamicTypeSize) private var typeSize

    var body: some View {
        Group {
            switch phase {
            case .loading:
                EzLoadingState()
            case .error(let message):
                EzErrorState(message: message, onRetry: onRetry)
            case .content(let data):
                activity(data)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Ez.Palette.background)
    }

    private func activity(_ data: CardActivityData) -> some View {
        ScrollView {
            LazyVStack(spacing: 0) {
                availableBalance(data)
                detailRow("Saldo contable", data.bookText ?? "—")
                Rectangle().fill(Ez.Palette.divider).frame(height: Ez.Size.divider)
                detailRow("Tarjeta", cardLabel)
                EzLabeledDivider(title: "Últimas operaciones")
                if data.operations.isEmpty {
                    Text("Sin operaciones recientes")
                        .font(Ez.Typography.bodyLarge)
                        .foregroundStyle(Ez.Palette.onSurfaceVariant)
                        .frame(maxWidth: .infinity)
                        .padding(Ez.Space.xl)
                } else {
                    ForEach(data.operations) { operationRow($0) }
                }
                if data.skipped > 0 {
                    // No ocultamos que faltan datos: la lista puede estar incompleta.
                    Text("No se pudieron mostrar \(data.skipped) operaciones.")
                        .font(Ez.Typography.bodyMedium)
                        .foregroundStyle(Ez.Palette.onSurfaceVariant)
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: .infinity)
                        .padding(Ez.Space.lg)
                }
            }
        }
    }

    private func availableBalance(_ data: CardActivityData) -> some View {
        VStack(spacing: Ez.Space.xs) {
            Text("Saldo disponible")
                .font(.title2)
                .foregroundStyle(Ez.Palette.onSurfaceVariant)
            if let text = data.availableText {
                EzAmount(value: text, currency: data.availableCurrency, large: true)
            } else {
                Text("—").font(Ez.Typography.amountLarge).foregroundStyle(Ez.Palette.onSurfaceVariant)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.top, Ez.Space.lg)
        .padding(.bottom, Ez.Space.md)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Saldo disponible \(data.availableText.map { "\($0) \(data.availableCurrency)" } ?? "no disponible")")
    }

    private func detailRow(_ label: String, _ value: String) -> some View {
        HStack(alignment: .firstTextBaseline) {
            Text(label).font(.title3).foregroundStyle(Ez.Palette.onSurface)
            Spacer(minLength: Ez.Space.md)
            Text(value).font(.title3).foregroundStyle(Ez.Palette.onSurfaceVariant)
                .multilineTextAlignment(.trailing)
        }
        .padding(.horizontal, Ez.Space.lg)
        .padding(.vertical, Ez.Space.md)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(label): \(value)")
    }

    private func operationRow(_ row: OperationRowModel) -> some View {
        Group {
            if typeSize.isAccessibilitySize {
                // Con texto de accesibilidad no caben dos columnas: el importe nunca debe partirse, así que
                // la fila se apila (icono y título arriba, importe y fecha debajo).
                VStack(alignment: .leading, spacing: Ez.Space.sm) {
                    HStack(spacing: Ez.Space.lg) {
                        icon(row)
                        titles(row)
                    }
                    HStack(alignment: .firstTextBaseline) {
                        amount(row)
                        Spacer(minLength: Ez.Space.md)
                        date(row)
                    }
                }
            } else {
                HStack(spacing: Ez.Space.lg) {
                    icon(row)
                    titles(row)
                    Spacer(minLength: Ez.Space.sm)
                    VStack(alignment: .trailing, spacing: Ez.Space.xxs) {
                        amount(row)
                        date(row)
                    }
                    .fixedSize(horizontal: true, vertical: false)
                }
            }
        }
        .padding(.horizontal, Ez.Space.lg)
        .padding(.vertical, Ez.Space.md)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(row.spoken)
    }

    private func icon(_ row: OperationRowModel) -> some View {
        Image(row.iconName)
            .resizable()
            .scaledToFit()
            .frame(width: Ez.Size.avatarLg, height: Ez.Size.avatarLg)
            .accessibilityHidden(true)
    }

    private func titles(_ row: OperationRowModel) -> some View {
        VStack(alignment: .leading, spacing: Ez.Space.xxs) {
            Text(row.title).font(Ez.Typography.bodyLarge).foregroundStyle(Ez.Palette.onSurface)
            Text(row.subtitle).font(Ez.Typography.bodyMedium).foregroundStyle(Ez.Palette.onSurfaceVariant)
        }
    }

    private func amount(_ row: OperationRowModel) -> some View {
        EzAmount(value: row.amountText, currency: row.currency)
    }

    @ViewBuilder
    private func date(_ row: OperationRowModel) -> some View {
        if !row.dateText.isEmpty {
            Text(row.dateText).font(Ez.Typography.bodyMedium).foregroundStyle(Ez.Palette.onSurfaceVariant)
        }
    }
}

private let label = "BANMET 6856"

#Preview("Detalle") {
    NavigationStack { CardDetailContent(phase: .content(.sample), cardLabel: label).balanceNavigationChrome() }
}
#Preview("Detalle (oscuro)") {
    NavigationStack { CardDetailContent(phase: .content(.sample), cardLabel: label).balanceNavigationChrome() }
        .preferredColorScheme(.dark)
}
#Preview("Detalle (texto grande)") {
    NavigationStack { CardDetailContent(phase: .content(.sample), cardLabel: label).balanceNavigationChrome() }
        .dynamicTypeSize(.accessibility3)
}
#Preview("Detalle: sin operaciones") {
    NavigationStack { CardDetailContent(phase: .content(.noOperations), cardLabel: label).balanceNavigationChrome() }
}
#Preview("Detalle: datos parciales") {
    NavigationStack { CardDetailContent(phase: .content(.partial), cardLabel: label).balanceNavigationChrome() }
}
#Preview("Detalle: cargando") {
    NavigationStack { CardDetailContent(phase: .loading, cardLabel: label).balanceNavigationChrome() }
}
#Preview("Detalle: error") {
    NavigationStack {
        CardDetailContent(phase: .error("Sin conexión. Revisa tu red e inténtalo de nuevo."), cardLabel: label)
            .balanceNavigationChrome()
    }
}
