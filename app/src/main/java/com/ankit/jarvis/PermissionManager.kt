/*
 * Copyright ©️ 2026 Ankit Gothwal.
 * All rights reserved, subject to applicable third-party license terms.
 *
 * Project: JARVIS AI
 * Developer: Ankit Gothwal
 * Application ID: com.ankit.jarvis
 */
package com.ankit.jarvis

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Governs all runtime and special system permissions required for Full Phone Control:
 * - Microphone (`RECORD_AUDIO`)
 * - Calls & Call Log (`CALL_PHONE`, `ANSWER_PHONE_CALLS`, `READ_PHONE_STATE`, `READ_CALL_LOG`)
 * - Contacts (`READ_CONTACTS`)
 * - SMS Send & Read (`SEND_SMS`, `READ_SMS`)
 * - Bluetooth (`BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`)
 * - System Settings Modification (`Settings.System.canWrite` for Screen Brightness & Auto-Rotate)
 * - Do Not Disturb / Ringer Policy (`NotificationManager.isNotificationPolicyAccessGranted`)
 */
class PermissionManager(private val activity: Activity) {

    companion object {
        const val REQUEST_RECORD_AUDIO = 1001
        const val REQUEST_ALL_PHONE_PERMS = 1002

        fun hasPermission(context: Context, perm: String): Boolean =
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED

        fun hasAudioPermission(context: Context): Boolean =
            hasPermission(context, Manifest.permission.RECORD_AUDIO)

        fun hasCallPermission(context: Context): Boolean =
            hasPermission(context, Manifest.permission.CALL_PHONE)

        fun hasAnswerCallPermission(context: Context): Boolean =
            hasPermission(context, Manifest.permission.ANSWER_PHONE_CALLS)

        fun hasCallLogPermission(context: Context): Boolean =
            hasPermission(context, Manifest.permission.READ_CALL_LOG)

        fun hasSmsPermission(context: Context): Boolean =
            hasPermission(context, Manifest.permission.SEND_SMS)

        fun hasReadSmsPermission(context: Context): Boolean =
            hasPermission(context, Manifest.permission.READ_SMS)

        fun hasContactsPermission(context: Context): Boolean =
            hasPermission(context, Manifest.permission.READ_CONTACTS)

        fun canWriteSystemSettings(context: Context): Boolean =
            Settings.System.canWrite(context)

        fun hasDndPermission(context: Context): Boolean {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            return nm.isNotificationPolicyAccessGranted
        }
    }

    fun ensureMicrophonePermission(onGranted: () -> Unit, onDenied: () -> Unit) {
        if (hasAudioPermission(activity)) {
            onGranted()
            return
        }

        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO)) {
            AlertDialog.Builder(activity, R.style.Theme_JarvisAI_Dialog)
                .setTitle(R.string.perm_mic_title)
                .setMessage(R.string.perm_mic_rationale)
                .setPositiveButton(R.string.btn_grant) { _, _ ->
                    requestAudioPermission()
                }
                .setNegativeButton(R.string.btn_use_text_only) { dialog, _ ->
                    dialog.dismiss()
                    onDenied()
                }
                .show()
        } else {
            requestAudioPermission()
        }
    }

    /**
     * Requests all runtime permissions for Full Phone Control in a single batch.
     */
    fun requestAllPhoneControlPermissions() {
        val needed = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.ANSWER_PHONE_CALLS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_SMS
        )
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            needed.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            needed.add(Manifest.permission.BLUETOOTH_CONNECT)
            needed.add(Manifest.permission.BLUETOOTH_SCAN)
        }
        val missing = needed.filter { !hasPermission(activity, it) }.toTypedArray()
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, missing, REQUEST_ALL_PHONE_PERMS)
        }
    }

    fun openWriteSettingsPermissionPage() {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:${activity.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    fun openDndPolicyPermissionPage() {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    private fun requestAudioPermission() {
        val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2 &&
            !hasPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)
        ) {
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        ActivityCompat.requestPermissions(
            activity,
            perms.toTypedArray(),
            REQUEST_RECORD_AUDIO
        )
    }

    fun handlePermissionsResult(
        requestCode: Int,
        grantResults: IntArray,
        onGranted: () -> Unit,
        onPermanentlyDenied: () -> Unit
    ) {
        if (requestCode == REQUEST_RECORD_AUDIO) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onGranted()
            } else {
                val showRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.RECORD_AUDIO
                )
                if (!showRationale) {
                    showSettingsRedirectDialog()
                }
                onPermanentlyDenied()
            }
        }
    }

    private fun showSettingsRedirectDialog() {
        AlertDialog.Builder(activity, R.style.Theme_JarvisAI_Dialog)
            .setTitle(R.string.perm_mic_denied_title)
            .setMessage(R.string.perm_mic_settings_guide)
            .setPositiveButton(R.string.btn_open_settings) { _, _ ->
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", activity.packageName, null)
                )
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                activity.startActivity(intent)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
