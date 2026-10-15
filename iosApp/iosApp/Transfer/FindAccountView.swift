import SwiftUI
import SharedLogic

/// "Buscar una cuenta" conectado al ViewModel. Solo una cuenta que el servidor devolvió permite seguir.
struct FindAccountView: View {
    @StateObject private var model = FindAccountModel()
    let onRecipient: (PersonModel) -> Void

    var body: some View {
        FindAccountContent(
            phase: model.phase, onSearch: { model.search($0) }, onEdit: { model.reset() }, onRecipient: onRecipient)
    }
}

struct FindAccountContent: View {
    let phase: FindAccountPhase
    var onSearch: (String) -> Void = { _ in }
    var onEdit: () -> Void = {}
    var onRecipient: (PersonModel) -> Void = { _ in }
    @State private var query = ""

    private var isSearching: Bool {
        if case .searching = phase { return true }
        return false
    }

    private var invalidMessage: String? {
        if case .invalid(let message) = phase { return message }
        return nil
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Ez.Space.md) {
                EzTextField(label: "Nombre de usuario", text: $query, error: invalidMessage)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .onSubmit { onSearch(query) }
                    .onChange(of: query) { _, _ in
                        if case .idle = phase {} else { onEdit() }
                    }
                EzButton(title: "Buscar", isLoading: isSearching) { onSearch(query) }
                    .disabled(isSearching)
                result
            }
            .padding(Ez.Space.lg)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Ez.Palette.background)
        .brandNavigationChrome(title: "Buscar una cuenta")
    }

    @ViewBuilder
    private var result: some View {
        switch phase {
        case .found(let person):
            Button { onRecipient(person) } label: {
                EzListItem(title: person.displayName, subtitle: person.username, leading: {
                    EzAvatar(initials: TransferPresentation.initials(person.displayName))
                })
            }
            .buttonStyle(.plain)
        case .notFound:
            notice("No encontramos una cuenta con ese nombre de usuario.")
        case .error(let message):
            notice(message)
        default:
            EmptyView()
        }
    }

    private func notice(_ text: String) -> some View {
        Text(text).font(Ez.Typography.bodyMedium).foregroundStyle(Ez.Palette.onSurfaceVariant)
    }
}

#Preview("Buscar: encontrada") {
    NavigationStack { FindAccountContent(phase: .found(PersonModel.samples[0])) }
}
#Preview("Buscar: no existe") {
    NavigationStack { FindAccountContent(phase: .notFound) }
}
