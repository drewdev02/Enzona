import SwiftUI
import SharedLogic

@main
struct iOSApp: App {
    init() {
        KoinKt.startKoinIos()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}