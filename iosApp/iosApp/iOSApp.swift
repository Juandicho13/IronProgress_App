import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        // Koin y la base arrancan antes de la interfaz (F1-13).
        IosStartupKt.startIronProgress()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
