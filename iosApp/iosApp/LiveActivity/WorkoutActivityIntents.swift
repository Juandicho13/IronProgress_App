import AppIntents
import Foundation

/// Lo que hacen los botones. La app lo llena al arrancar (iOSApp.init) con la llamada a Kotlin;
/// en la extensión queda vacío, porque un LiveActivityIntent corre en el proceso de la app.
enum WorkoutActivityActions {
    nonisolated(unsafe) static var handler: ((String) async -> Void)?

    static func run(_ action: String) async {
        await handler?(action)
    }
}

/// +15 s o -15 s al descanso.
struct AdjustRestIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "Ajustar el descanso"

    @Parameter(title: "Segundos")
    var seconds: Int

    init() {
        seconds = 15
    }

    init(seconds: Int) {
        self.seconds = seconds
    }

    func perform() async throws -> some IntentResult {
        await WorkoutActivityActions.run(seconds > 0 ? "plus" : "minus")
        return .result()
    }
}

struct SkipRestIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "Saltar el descanso"

    func perform() async throws -> some IntentResult {
        await WorkoutActivityActions.run("skip")
        return .result()
    }
}

struct CompleteSetIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "Completar serie"

    func perform() async throws -> some IntentResult {
        await WorkoutActivityActions.run("complete")
        return .result()
    }
}
