package com.olarte.ironprogress.ui.rest

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.rest_notification_permission_continue
import com.olarte.ironprogress.ui.resources.rest_notification_permission_not_now
import com.olarte.ironprogress.ui.resources.rest_notification_permission_rationale
import com.olarte.ironprogress.ui.theme.ForjaTheme
import org.jetbrains.compose.resources.stringResource

private const val REQUEST_POST_NOTIFICATIONS = 1401

@Composable
actual fun RestNotificationBridge(restController: RestController, isWorkoutScreenOpen: Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val restState by restController.state.collectAsStateWithLifecycle()

    LaunchedEffect(isWorkoutScreenOpen) {
        restController.setWorkoutScreenOpen(isWorkoutScreenOpen)
    }

    DisposableEffect(lifecycleOwner, restController) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> restController.setAppInForeground(true, System.currentTimeMillis())
                Lifecycle.Event.ON_STOP -> restController.setAppInForeground(false, System.currentTimeMillis())
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val pendingSessionId = restState.pendingPermissionSessionId
    if (pendingSessionId != null) {
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

        if (!needsPermission) {
            LaunchedEffect(pendingSessionId) {
                restController.markNotificationPermissionHandled(pendingSessionId)
            }
        } else {
            AlertDialog(
                onDismissRequest = { restController.markNotificationPermissionHandled(pendingSessionId) },
                text = {
                    Text(
                        text = stringResource(Res.string.rest_notification_permission_rationale),
                        style = ForjaTheme.typography.body,
                        color = ForjaTheme.colors.text1,
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            restController.markNotificationPermissionHandled(pendingSessionId)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                context.findActivity()?.requestPermissions(
                                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                                    REQUEST_POST_NOTIFICATIONS,
                                )
                            }
                        },
                    ) {
                        Text(
                            text = stringResource(Res.string.rest_notification_permission_continue),
                            style = ForjaTheme.typography.label,
                            color = ForjaTheme.colors.ember,
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { restController.markNotificationPermissionHandled(pendingSessionId) },
                    ) {
                        Text(
                            text = stringResource(Res.string.rest_notification_permission_not_now),
                            style = ForjaTheme.typography.label,
                            color = ForjaTheme.colors.text2,
                        )
                    }
                },
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
