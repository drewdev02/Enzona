import SwiftUI
import SharedLogicSwift

@main
struct iOSApp: App {
    init() {
        // Debug: desde Debug y mensajes legibles. Release: desde Info y os_log los oculta.
        #if DEBUG
        LoggingPackage.installLogging(debug: true)
        #else
        LoggingPackage.installLogging(debug: false)
        #endif
        _ = DiPackage.startKoinIos()
    }

    var body: some Scene {
        WindowGroup {
            #if DEBUG
            // `-ezPreview <nombre>` muestra una pantalla con datos inventados (ver DebugPreview.swift).
            if let name = DebugPreview.requested {
                DebugPreview.view(name)
            } else {
                ContentView()
            }
            #else
            ContentView()
            #endif
        }
    }
}