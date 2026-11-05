import SwiftUI
import Shared

@main
struct iOSApp: App {
    @Environment(\.scenePhase) private var scenePhase

    init() {
        // Koin y la base arrancan antes de la interfaz (F1-13), con la Live Activity (F1-16).
        IosStartupKt.startIronProgress(bridge: WorkoutActivityController.shared)
        WorkoutActivityActions.handler = { action in
            await withCheckedContinuation { continuation in
                IosStartupKt.runWorkoutAction(action: action) {
                    continuation.resume()
                }
            }
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
        .onChange(of: scenePhase) { _, phase in
            // La Live Activity solo sale con la app fuera de la vista.
            IosStartupKt.setAppInForeground(active: phase == .active)
        }
    }
}
