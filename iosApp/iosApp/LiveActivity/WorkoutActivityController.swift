import ActivityKit
import Foundation
import Shared

/// La Live Activity vista desde Kotlin (WorkoutActivityBridge). Kotlin decide cuándo mostrarla y
/// la actualiza solo en los cambios de fase; aquí solo se pide, se actualiza y se termina.
final class WorkoutActivityController: NSObject, WorkoutActivityBridge {
    static let shared = WorkoutActivityController()

    override private init() {
        super.init()
    }

    private var activity: Activity<WorkoutActivityAttributes>?

    /// Inicio del descanso en curso: se conserva entre actualizaciones (+15 s, -15 s) para que el
    /// anillo no vuelva a empezar; cambia solo al empezar otro descanso o al pasar a Listo.
    private var restStartedAt = Date()
    private var lastEndsAtMs: Int64 = 0

    func show(state: WorkoutActivityState) {
        guard ActivityAuthorizationInfo().areActivitiesEnabled else { return }
        let newRest = state.ready || lastEndsAtMs == 0 || activity?.attributes.sessionId != state.sessionId
        if newRest && !state.ready {
            restStartedAt = Date()
        }
        lastEndsAtMs = state.ready ? 0 : state.endsAtEpochMs
        let content = ActivityContent(state: Self.contentState(from: state, startedAt: restStartedAt), staleDate: nil)
        if let activity, activity.attributes.sessionId == state.sessionId, activity.activityState == .active {
            Task { await activity.update(content) }
            return
        }
        endCurrent()
        activity = try? Activity.request(
            attributes: WorkoutActivityAttributes(sessionId: state.sessionId),
            content: content,
            pushType: nil
        )
    }

    func end() {
        lastEndsAtMs = 0
        endCurrent()
    }

    private func endCurrent() {
        guard let current = activity else { return }
        activity = nil
        Task { await current.end(nil, dismissalPolicy: .immediate) }
    }

    private static func contentState(
        from state: WorkoutActivityState,
        startedAt: Date
    ) -> WorkoutActivityAttributes.ContentState {
        let endsAt: Date? = state.endsAtEpochMs > 0
            ? Date(timeIntervalSince1970: Double(state.endsAtEpochMs) / 1000)
            : nil
        return WorkoutActivityAttributes.ContentState(
            title: state.title,
            detail: state.detail,
            endsAt: endsAt,
            startedAt: startedAt,
            ready: state.ready,
            readyChip: state.readyChip
        )
    }
}
