import ActivityKit
import Foundation

/// Datos de la Live Activity del entreno (F1-16, PANTALLAS 5.9).
/// Vive en LiveActivityShared, carpeta que compilan la app y la extensión IronProgressLiveActivity.
struct WorkoutActivityAttributes: ActivityAttributes {
    public struct ContentState: Codable, Hashable {
        /// "Descanso · Press banca" o "A la serie 3 de 4".
        var title: String
        /// "Sigue: serie 3 de 4 · 62,5 kg × 10".
        var detail: String
        /// Fin del descanso. Nil en Listo: el sistema cuenta solo con Text(timerInterval:).
        var endsAt: Date?
        var startedAt: Date
        var ready: Bool
        var readyChip: String
    }

    var sessionId: String
}
