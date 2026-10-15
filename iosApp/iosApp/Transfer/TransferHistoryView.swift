import SwiftUI
import SharedLogic

/// Historial conectado al ViewModel. El contenido (`TransferHistoryContent`) no depende del framework Kotlin.
struct TransferHistoryView: View {
    @StateObject private var model = TransferHistoryModel()

    var body: some View {
        TransferHistoryContent(
            phase: model.phase, filter: model.filter,
            onFilter: { model.apply(filter: $0) }, onRetry: { model.load() }, onLoadMore: { model.loadMore() }
        )
        .onAppear { model.load() }
    }
}

private enum DateField: Identifiable {
    case start, end
    var id: Self { self }
}

private let statusChoices: [(code: Int?, label: String)] = [
    (nil, "Todas"), (2222, "Aceptadas"), (2242, "Pendientes"), (2224, "Fallidas"),
]

struct TransferHistoryContent: View {
    let phase: TransferHistoryPhase
    let filter: TransferFilterModel
    var onFilter: (TransferFilterModel) -> Void = { _ in }
    var onRetry: () -> Void = {}
    var onLoadMore: () -> Void = {}
    @State private var picking: DateField?

    var body: some View {
        VStack(spacing: 0) {
            filters
            Rectangle().fill(Ez.Palette.divider).frame(height: Ez.Size.divider)
            Group {
                switch phase {
                case .loading: EzLoadingState()
                case .error(let message): EzErrorState(message: message, onRetry: onRetry)
                case .content(let data): list(data)
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Ez.Palette.background)
        .brandNavigationChrome(title: "Historial")
        .sheet(item: $picking) { field in
            DayPickerSheet(
                title: field == .start ? "Desde" : "Hasta",
                day: field == .start ? filter.startDay : filter.endDay
            ) { picked in
                var next = filter
                if field == .start { next.startDay = picked } else { next.endDay = picked }
                onFilter(next)
            }
            .presentationDetents([.medium, .large])
        }
    }

    private var filters: some View {
        VStack(alignment: .leading, spacing: Ez.Space.sm) {
            chipRow {
                ForEach(statusChoices, id: \.label) { choice in
                    chip(choice.label, selected: filter.statusCode == choice.code) {
                        var next = filter
                        next.statusCode = choice.code
                        onFilter(next)
                    }
                }
            }
            chipRow {
                chip(filter.startDay.map { "Desde \(TransferPresentation.displayDay(iso: $0))" } ?? "Desde",
                     selected: filter.startDay != nil) { picking = .start }
                chip(filter.endDay.map { "Hasta \(TransferPresentation.displayDay(iso: $0))" } ?? "Hasta",
                     selected: filter.endDay != nil) { picking = .end }
                if filter != .none {
                    Button("Limpiar") { onFilter(.none) }
                        .font(Ez.Typography.labelLarge).foregroundStyle(Ez.Palette.primary)
                }
            }
        }
        .padding(Ez.Space.md)
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func chipRow<Content: View>(@ViewBuilder _ content: () -> Content) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Ez.Space.sm) { content() }
        }
    }

    private func chip(_ text: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(text)
                .font(Ez.Typography.labelLarge)
                .foregroundStyle(selected ? Ez.Palette.onPrimaryContainer : Ez.Palette.onSurfaceVariant)
                .padding(.horizontal, Ez.Space.md)
                .frame(minHeight: Ez.Size.minTouchTarget - Ez.Space.md)
                .background(Capsule().fill(selected ? Ez.Palette.primaryContainer : Color.clear))
                .overlay(Capsule().stroke(selected ? Color.clear : Ez.Palette.outline, lineWidth: 1))
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(selected ? .isSelected : [])
    }

    @ViewBuilder
    private func list(_ data: TransferHistoryData) -> some View {
        if data.rows.isEmpty && !data.hasMore {
            EzEmptyState(
                title: "Sin transferencias",
                message: filter == .none ? "Aún no has hecho transferencias." : "No hay transferencias con estos filtros.",
                systemImage: "arrow.left.arrow.right")
        } else {
            ScrollView {
                LazyVStack(spacing: 0) {
                    ForEach(data.rows) { row in
                        TransferRowView(row: row)
                        Rectangle().fill(Ez.Palette.divider).frame(height: Ez.Size.divider)
                    }
                    if data.skipped > 0 { TransferNotice(text: "No se pudieron mostrar \(data.skipped) transferencias.") }
                    if data.hasMore {
                        if let error = data.moreError {
                            VStack(spacing: Ez.Space.sm) {
                                TransferNotice(text: error)
                                EzButton(title: "Reintentar", kind: .secondary, fillWidth: false, action: onLoadMore)
                            }
                        } else {
                            // Al aparecer el final de la lista se pide la siguiente página (el ViewModel ignora repeticiones).
                            ProgressView().tint(Ez.Palette.primary).frame(maxWidth: .infinity).padding(Ez.Space.lg)
                                .onAppear { onLoadMore() }
                        }
                    }
                }
            }
        }
    }
}

/// Selector de un día. "Quitar" borra ese límite del filtro.
private struct DayPickerSheet: View {
    let title: String
    let day: String?
    let onPick: (String?) -> Void
    @State private var date: Date
    @Environment(\.dismiss) private var dismiss

    init(title: String, day: String?, onPick: @escaping (String?) -> Void) {
        self.title = title
        self.day = day
        self.onPick = onPick
        _date = State(initialValue: day.flatMap { TransferPresentation.date(iso: $0) } ?? Date())
    }

    var body: some View {
        NavigationStack {
            DatePicker(title, selection: $date, displayedComponents: .date)
                .datePickerStyle(.graphical)
                .padding(Ez.Space.md)
                .navigationTitle(title)
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) { Button("Cancelar") { dismiss() } }
                    ToolbarItem(placement: .confirmationAction) {
                        Button("Aceptar") { onPick(TransferPresentation.isoDay(date)); dismiss() }
                    }
                    if day != nil {
                        ToolbarItem(placement: .bottomBar) { Button("Quitar") { onPick(nil); dismiss() } }
                    }
                }
        }
    }
}

#Preview("Historial") {
    NavigationStack {
        TransferHistoryContent(
            phase: .content(TransferHistoryData(rows: TransferRowModel.samples, skipped: 0, hasMore: false, isLoadingMore: false, moreError: nil)),
            filter: .none)
    }
}
#Preview("Historial (texto grande)") {
    NavigationStack {
        TransferHistoryContent(
            phase: .content(TransferHistoryData(rows: TransferRowModel.samples, skipped: 0, hasMore: false, isLoadingMore: false, moreError: nil)),
            filter: .none)
    }
    .dynamicTypeSize(.accessibility3)
}
#Preview("Historial: filtrado vacío") {
    NavigationStack {
        TransferHistoryContent(
            phase: .content(TransferHistoryData(rows: [], skipped: 0, hasMore: false, isLoadingMore: false, moreError: nil)),
            filter: TransferFilterModel(startDay: "2026-10-01", endDay: nil, statusCode: 2224))
    }
}
