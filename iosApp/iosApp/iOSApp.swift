import SwiftUI
import SharedLogic

@main
struct iOSApp: App {
    init() {
        // Debug: desde Debug y mensajes legibles. Release: desde Info y os_log los oculta.
        #if DEBUG
        LoggingSetupKt.installLogging(debug: true)
        #else
        LoggingSetupKt.installLogging(debug: false)
        #endif
        KoinKt.startKoinIos()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}