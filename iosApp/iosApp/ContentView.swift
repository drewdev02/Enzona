import SwiftUI
import SharedLogic

struct ContentView: View {
    var body: some View {
        VStack(spacing: 8) {
            Text("EnZona KMP")
                .font(.largeTitle).bold()
            Text("Lógica compartida · UI nativa (M0)")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
            Text(KoinHelper.shared.greeting.greet())
                .font(.footnote)
                .foregroundColor(.secondary)
                .padding(.top, 12)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding()
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
