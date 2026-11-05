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

    func show(state: WorkoutActivityState) {
        guard ActivityAuthorizationInfo().areActivitiesEnabled else { return }
        let content = ActivityContent(state: Self.contentState(from: state), staleDate: nil)
        if let activity, activity.attributes.sessionId == state.sessionId, activity.activityState == .active {
            Task { await activity.update(content) }
            return
        }
        end()
        activity = try? Activity.request(
            attributes: WorkoutActivityAttributes(sessionId: state.sessionId),
            content: content,
            pushType: nil
        )
    }

    func end() {
        guard let current = activity else { return }
        activity = nil
        Task { await current.end(nil, dismissalPolicy: .immediate) }
    }

    private static func contentState(from state: WorkoutActivityState) -> WorkoutActivityAttributes.ContentState {
        let endsAt: Date? = state.endsAtEpochMs > 0
            ? Date(timeIntervalSince1970: Double(state.endsAtEpochMs) / 1000)
            : nil
        return WorkoutActivityAttributes.ContentState(
            title: state.title,
            detail: state.detail,
            endsAt: endsAt,
            startedAt: Date(),
            ready: state.ready,
            readyChip: state.readyChip
        )
    }
}
