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
                    RingWithTime(state: context.state, size: 64)
                        .padding(.leading, 4)
                }
                DynamicIslandExpandedRegion(.center) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(context.state.title).font(.headline).foregroundStyle(ForjaTokens.text1).lineLimit(1)
                        Text(context.state.detail).font(.caption).foregroundStyle(ForjaTokens.text2).lineLimit(2)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                DynamicIslandExpandedRegion(.bottom) {
                    Actions(state: context.state)
                }
            } compactLeading: {
                RestRing(state: context.state, size: 22)
            } compactTrailing: {
                Countdown(state: context.state, size: 15)
                    .frame(maxWidth: 52)
            } minimal: {
                RestRing(state: context.state, size: 22)
            }
            .keylineTint(context.state.ready ? ForjaTokens.ready : ForjaTokens.ice)
        }
    }
}

/// Anillo azul que se va vaciando mientras descansa; en Listo, un círculo verde con el check.
/// Lo anima el sistema con ProgressView(timerInterval:), sin actualizar la actividad cada segundo.
/// ActivityKit no deja dibujar alrededor de la isla: el anillo va dentro, a la izquierda.
private struct RestRing: View {
    let state: WorkoutActivityAttributes.ContentState
    let size: CGFloat

    var body: some View {
        Group {
            if !state.ready, let endsAt = state.endsAt, endsAt > state.startedAt {
                ProgressView(timerInterval: state.startedAt...endsAt, countsDown: true) {
                    EmptyView()
                } currentValueLabel: {
                    EmptyView()
                }
                .progressViewStyle(.circular)
                .tint(ForjaTokens.ice)
            } else {
                Image(systemName: "checkmark.circle.fill")
                    .resizable()
                    .scaledToFit()
                    .foregroundStyle(ForjaTokens.ready)
            }
        }
        .frame(width: size, height: size)
    }
}

/// El anillo grande con la cuenta atrás en el centro: expandida, pantalla bloqueada y StandBy.
private struct RingWithTime: View {
    let state: WorkoutActivityAttributes.ContentState
    let size: CGFloat

    var body: some View {
        ZStack {
            RestRing(state: state, size: size)
            if !state.ready {
                Countdown(state: state, size: size * 0.28)
                    .frame(width: size * 0.72)
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
                .multilineTextAlignment(.center)
                .lineLimit(1)
                .minimumScaleFactor(0.6)
        } else {
            Text(state.readyChip)
                .font(ForjaTokens.figure(size))
                .foregroundStyle(ForjaTokens.ready)
                .lineLimit(1)
                .minimumScaleFactor(0.6)
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
                Button(intent: AdjustRestIntent(seconds: -15)) { Text("-15 s").frame(maxWidth: .infinity) }
                    .tint(ForjaTokens.surface3)
                Button(intent: SkipRestIntent()) { Text("Saltar").frame(maxWidth: .infinity) }
                    .tint(ForjaTokens.ice)
                Button(intent: AdjustRestIntent(seconds: 15)) { Text("+15 s").frame(maxWidth: .infinity) }
                    .tint(ForjaTokens.surface3)
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
            HStack(alignment: .center, spacing: 14) {
                RingWithTime(state: state, size: 72)
                VStack(alignment: .leading, spacing: 2) {
                    Text(state.title).font(.headline).foregroundStyle(ForjaTokens.text1).lineLimit(1)
                    Text(state.detail).font(.subheadline).foregroundStyle(ForjaTokens.text2).lineLimit(2)
                }
                Spacer(minLength: 0)
            }
            Actions(state: state)
        }
        .padding(16)
    }
}
