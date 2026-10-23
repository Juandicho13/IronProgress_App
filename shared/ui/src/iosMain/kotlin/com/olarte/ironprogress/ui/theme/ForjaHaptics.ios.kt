package com.olarte.ironprogress.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType
import platform.UIKit.UISelectionFeedbackGenerator

@Composable
actual fun rememberForjaHaptics(enabled: Boolean): ForjaHaptics =
    remember(enabled) {
        if (!enabled) {
            NoOpForjaHaptics
        } else {
            IosForjaHaptics()
        }
    }

private class IosForjaHaptics : ForjaHaptics {
    private val mediumImpact = UIImpactFeedbackGenerator(
        style = UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium,
    )
    private val lightImpact = UIImpactFeedbackGenerator(
        style = UIImpactFeedbackStyle.UIImpactFeedbackStyleLight,
    )
    private val rigidImpact = UIImpactFeedbackGenerator(
        style = UIImpactFeedbackStyle.UIImpactFeedbackStyleRigid,
    )
    private val selectionFeedback = UISelectionFeedbackGenerator()
    private val notificationFeedback = UINotificationFeedbackGenerator()

    override fun setCompleted() {
        mediumImpact.prepare()
        mediumImpact.impactOccurred()
    }

    override fun segmentTick() {
        selectionFeedback.prepare()
        selectionFeedback.selectionChanged()
    }

    override fun keyboardTap() {
        lightImpact.prepare()
        lightImpact.impactOccurred()
    }

    override fun restCountdownTick() {
        selectionFeedback.prepare()
        selectionFeedback.selectionChanged()
    }

    override fun restFinished() {
        notificationFeedback.prepare()
        notificationFeedback.notificationOccurred(
            UINotificationFeedbackType.UINotificationFeedbackTypeSuccess,
        )
    }

    override fun personalRecord() {
        notificationFeedback.prepare()
        notificationFeedback.notificationOccurred(
            UINotificationFeedbackType.UINotificationFeedbackTypeSuccess,
        )
    }

    override fun slideThresholdReached() {
        rigidImpact.prepare()
        rigidImpact.impactOccurred()
    }

    override fun actionRejected() {
        notificationFeedback.prepare()
        notificationFeedback.notificationOccurred(
            UINotificationFeedbackType.UINotificationFeedbackTypeError,
        )
    }
}