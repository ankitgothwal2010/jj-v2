/*
 * Copyright ©️ 2026 Ankit Gothwal.
 * All rights reserved, subject to applicable third-party license terms.
 *
 * Project: JARVIS AI
 * Developer: Ankit Gothwal
 * Application ID: com.ankit.jarvis
 */
package com.ankit.jarvis

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * DeviceAdminReceiver used strictly to support locking the phone screen ("lock my phone")
 * on Android 8.0+ (including ASUS Zenfone Max M1 and Infinix Hot 10) via DevicePolicyManager.lockNow().
 */
class JarvisDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, "JARVIS Screen Lock Admin Enabled.", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Toast.makeText(context, "JARVIS Screen Lock Admin Disabled.", Toast.LENGTH_SHORT).show()
    }
}
