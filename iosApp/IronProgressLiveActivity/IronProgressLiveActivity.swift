import ActivityKit
import AppIntents
import SwiftUI
import WidgetKit

/// Live Activity del entreno (F1-16, PANTALLAS 5.9): compacta, mínima, expandida, pantalla
/// bloqueada y StandBy. El contador lo lleva el sistema con Text(timerInterval:).
struct IronProgressLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: WorkoutActivityAttributes.self) { context in
            LockScreenView(state: context.state)
                .activityBackgroundTint(ForjaTokens.background)
                .activitySystemActionForegroundColor(ForjaTokens.text1)
        } dynamicIsland: { context in
            DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    RestRing(state: context.state, size: 44)
                }
                DynamicIslandExpandedRegion(.trailing) {
                    Countdown(state: context.state, size: 28)
                }
                DynamicIslandExpandedRegion(.center) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(context.state.title).font(.headline).foregroundStyle(ForjaTokens.text1).lineLimit(1)
                        Text(context.state.detail).font(.caption).foregroundStyle(ForjaTokens.text2).lineLimit(1)
                    }
                }
                DynamicIslandExpandedRegion(.bottom) {
                    Actions(state: context.state)
                }
            } compactLeading: {
                RestRing(state: context.state, size: 20)
            } compactTrailing: {
                Countdown(state: context.state, size: 14)
                    .frame(maxWidth: 48)
            } minimal: {
                RestRing(state: context.state, size: 20)
            }
            .keylineTint(context.state.ready ? ForjaTokens.ember : ForjaTokens.ice)
        }
    }
}

/// Anillo: hielo mientras descansa y brasa en Listo.
private struct RestRing: View {
    let state: WorkoutActivityAttributes.ContentState
    let size: CGFloat

    var body: some View {
        Group {
            if let endsAt = state.endsAt, endsAt > state.startedAt {
                ProgressView(timerInterval: state.startedAt...endsAt, countsDown: true) {
                    EmptyView()
                } currentValueLabel: {
                    EmptyView()
                }
                .progressViewStyle(.circular)
                .tint(ForjaTokens.ice)
            } else {
                Circle()
                    .stroke(ForjaTokens.ember, lineWidth: max(2, size / 8))
            }
        }
        .frame(width: size, height: size)
    }
}

/// "1:12" contando hacia atrás, o "Listo".
private struct Countdown: View {
    let state: WorkoutActivityAttributes.ContentState
    let size: CGFloat

    var body: some View {
        if let endsAt = state.endsAt, !state.ready {
            Text(timerInterval: Date()...max(endsAt, Date()), countsDown: true)
                .font(ForjaTokens.figure(size))
                .foregroundStyle(ForjaTokens.ice)
                .multilineTextAlignment(.trailing)
        } else {
            Text(state.readyChip)
                .font(ForjaTokens.figure(size))
                .foregroundStyle(ForjaTokens.ember)
        }
    }
}

/// Completar serie en Listo; -15 s, +15 s y Saltar mientras descansa.
private struct Actions: View {
    let state: WorkoutActivityAttributes.ContentState

    var body: some View {
        HStack(spacing: 8) {
            if state.ready {
                Button(intent: CompleteSetIntent()) {
                    Text("Completar serie").frame(maxWidth: .infinity)
                }
                .tint(ForjaTokens.ember)
            } else {
                Button(intent: AdjustRestIntent(seconds: -15)) { Text("-15 s") }
                Button(intent: AdjustRestIntent(seconds: 15)) { Text("+15 s") }
                Button(intent: SkipRestIntent()) { Text("Saltar") }
                    .tint(ForjaTokens.ice)
            }
        }
        .buttonStyle(.borderedProminent)
        .font(.subheadline.weight(.semibold))
    }
}

/// Pantalla bloqueada y StandBy (en StandBy el sistema la muestra al 200 %).
private struct LockScreenView: View {
    let state: WorkoutActivityAttributes.ContentState

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .center, spacing: 12) {
                RestRing(state: state, size: 40)
                VStack(alignment: .leading, spacing: 2) {
                    Text(state.title).font(.headline).foregroundStyle(ForjaTokens.text1).lineLimit(1)
                    Text(state.detail).font(.subheadline).foregroundStyle(ForjaTokens.text2).lineLimit(1)
                }
                Spacer(minLength: 8)
                Countdown(state: state, size: 34)
            }
            Actions(state: state)
        }
        .padding(16)
    }
}
