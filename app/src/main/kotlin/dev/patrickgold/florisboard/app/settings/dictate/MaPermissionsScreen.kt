/*
 * Copyright (C) 2026 Marko Bosko, Mantra Productions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.patrickgold.florisboard.app.settings.dictate

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.patrickgold.florisboard.dictate.overlay.DictateAccessibilityService
import dev.patrickgold.florisboard.lib.compose.FlorisScreen

/**
 * Every permission this keyboard needs, numbered, in the order they have to be granted.
 *
 * ### Why a screen of its own, at the top of the settings
 *
 * They were spread across a setup wizard he sees once and a scatter of error messages he sees when
 * something has already failed. Reinstalling — which he does several times a day — meant hunting
 * them one at a time, each from a different starting point.
 *
 * **Numbered, because the order is not arbitrary.** Restricted settings must be allowed before
 * accessibility can be switched on at all; the microphone is useless without the keyboard enabled.
 * A list in the order of execution is a list he can work down without thinking, which is the whole
 * point of putting it in one place.
 *
 * ### Each row says whether it is done, and opens the place that fixes it
 *
 * **A permission screen that only names permissions is a worse version of the system settings.** The
 * value is entirely in the two things beside the name: whether it is granted, and one tap to the
 * exact page that grants it.
 *
 * ### It rechecks when he comes back
 *
 * Android grants happen in another app, so this screen is always looking at a stale answer after a
 * trip out. Re-reading on RESUME means the tick appears the moment he returns, rather than the
 * screen insisting a thing is missing that he has just granted.
 */
@Composable
fun MaPermissionsScreen() = FlorisScreen {
    title = "Permissions"

    content {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        // Bumped on every resume; the checks below read it so they re-run.
        var generation by remember { mutableIntStateOf(0) }
        DisposableEffectOnResume(lifecycleOwner) { generation++ }

        Text(
            text = "In this order. Each one opens the page that grants it.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        val steps = remember(generation) { maPermissionSteps(context) }
        steps.forEachIndexed { i, step ->
            MaPermissionRow(
                number = i + 1,
                title = step.title,
                detail = step.detail,
                granted = step.granted,
                onClick = { runCatching { context.startActivity(step.intent()) } },
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

/** One permission: what it is, whether it is granted, and where to go to grant it. */
private class MaPermissionStep(
    val title: String,
    val detail: String,
    val granted: Boolean,
    val intent: () -> Intent,
)

/**
 * The list, in the order they must be done.
 *
 * Built fresh on each check rather than held, because every `granted` here is a fact about the
 * system that this app does not own and cannot be notified about.
 */
private fun maPermissionSteps(context: Context): List<MaPermissionStep> {
    fun appDetails() = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:" + context.packageName)
    }
    val steps = mutableListOf<MaPermissionStep>()

    steps += MaPermissionStep(
        title = "Enable the keyboard",
        detail = "Turn TTT mini on in the list of input methods",
        granted = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_INPUT_METHODS,
        )?.contains(context.packageName) == true,
        intent = { Intent(Settings.ACTION_INPUT_METHOD_SETTINGS) },
    )

    steps += MaPermissionStep(
        title = "Microphone",
        detail = "Without it nothing can be recorded",
        granted = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED,
        // The app-details page rather than a request dialog: an input method cannot show a runtime
        // permission prompt, and a page that is always reachable beats a dialog that sometimes is.
        intent = ::appDetails,
    )

    steps += MaPermissionStep(
        title = "Allow restricted settings",
        detail = "Three dots at the top right of App info. Skip if your phone does not offer it",
        // Nothing can be read here: Android exposes no flag for it. Reported as not granted so it is
        // never ticked off falsely — an unticked step he has already done costs a glance; a ticked
        // one he has not done costs an afternoon.
        granted = false,
        intent = ::appDetails,
    )

    steps += MaPermissionStep(
        title = "Accessibility service",
        detail = "The magic finger, the reader and the recording bar all need it",
        granted = DictateAccessibilityService.isRunning,
        intent = { Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS) },
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        steps += MaPermissionStep(
            title = "All files access",
            detail = "Only for importing keys and audio from storage",
            granted = Environment.isExternalStorageManager(),
            intent = { Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION) },
        )
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        steps += MaPermissionStep(
            title = "Notifications",
            detail = "So a long transcription can say when it is done",
            granted = context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED,
            intent = ::appDetails,
        )
    }

    return steps
}

@Composable
private fun MaPermissionRow(
    number: Int,
    title: String,
    detail: String,
    granted: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The number, because the order is the instruction.
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    color = if (granted) MaGrantedBg else MaPendingBg,
                    shape = RoundedCornerShape(14.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (granted) "\u2713" else "$number",
                color = if (granted) MaGranted else MaPending,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
        }
        Column(modifier = Modifier.padding(start = 14.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Runs [onResume] every time the screen comes back to the front.
 *
 * A permission is granted in another app, so this screen is looking at a stale answer the moment he
 * leaves it. Without this, a granted permission keeps showing as missing until the screen is closed
 * and reopened — which reads as the grant not having worked.
 */
@Composable
private fun DisposableEffectOnResume(
    owner: androidx.lifecycle.LifecycleOwner,
    onResume: () -> Unit,
) {
    LaunchedEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) onResume()
        }
        owner.lifecycle.addObserver(observer)
    }
}

private val MaGranted = Color(0xFF56D364)
private val MaGrantedBg = Color(0x2256D364)
private val MaPending = Color(0xFFE8A64B)
private val MaPendingBg = Color(0x22E8A64B)
