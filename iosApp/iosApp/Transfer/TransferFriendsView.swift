import SwiftUI
import SharedLogicSwift

/// "Mis amigos" conectado al ViewModel: elegir a quién transferir.
struct TransferFriendsView: View {
    @StateObject private var model = FriendsModel()
    let onFriend: (PersonModel) -> Void

    var body: some View {
        TransferFriendsContent(phase: model.phase, onRetry: { model.load() }, onFriend: onFriend)
            .onAppear { model.load() }
    }
}

struct TransferFriendsContent: View {
    let phase: FriendsPhase
    var onRetry: () -> Void = {}
    var onFriend: (PersonModel) -> Void = { _ in }

    var body: some View {
        Group {
            switch phase {
            case .loading: EzLoadingState()
            case .error(let message): EzErrorState(message: message, onRetry: onRetry)
            case .empty:
                EzEmptyState(title: "Aún no tienes amigos", message: "Cuando agregues amigos aparecerán aquí.",
                             systemImage: "person.2")
            case .content(let friends):
                ScrollView {
                    LazyVStack(spacing: 0) {
                        ForEach(friends) { friend in
                            Button { onFriend(friend) } label: {
                                EzListItem(title: friend.displayName, subtitle: friend.subtitle, leading: {
                                    EzAvatar(initials: TransferPresentation.initials(friend.displayName))
                                })
                            }
                            .buttonStyle(.plain)
                            Rectangle().fill(Ez.Palette.divider).frame(height: Ez.Size.divider)
                        }
                    }
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Ez.Palette.background)
        .brandNavigationChrome(title: "Mis amigos")
    }
}

#Preview("Amigos") {
    NavigationStack { TransferFriendsContent(phase: .content(PersonModel.samples)) }
}
