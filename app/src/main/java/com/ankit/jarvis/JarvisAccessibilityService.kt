/*
 * Copyright ©️ 2026 Ankit Gothwal.
 * All rights reserved, subject to applicable third-party license terms.
 *
 * Project: JARVIS AI
 * Developer: Ankit Gothwal
 * Application ID: com.ankit.jarvis
 */
package com.ankit.jarvis

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.view.accessibility.AccessibilityEvent

/**
 * Global Phone Control Service for JARVIS AI (`com.ankit.jarvis`) by Ankit Gothwal.
 * Supports:
 * - Locking the screen (`GLOBAL_ACTION_LOCK_SCREEN`)
 * - Taking a screenshot (`GLOBAL_ACTION_TAKE_SCREENSHOT`)
 * - Opening the Power Menu (`GLOBAL_ACTION_POWER_DIALOG`)
 * - Navigating Home (`GLOBAL_ACTION_HOME`), Back (`GLOBAL_ACTION_BACK`), Recent Apps (`GLOBAL_ACTION_RECENTS`)
 * - Toggling Split Screen (`GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN`)
 * - Expanding Notifications (`GLOBAL_ACTION_NOTIFICATIONS`) & Quick Settings (`GLOBAL_ACTION_QUICK_SETTINGS`)
 */
class JarvisAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: JarvisAccessibilityService? = null
            private set

        fun isServiceActive(): Boolean = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onDestroy() {
        if (instance === this) {
            instance = null
        }
        super.onDestroy()
    }

    fun lockPhoneScreen(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) else false

    fun takeScreenshot(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT) else false

    fun showPowerDialog(): Boolean = performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
    fun goHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)
    fun goBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)
    fun openRecents(): Boolean = performGlobalAction(GLOBAL_ACTION_RECENTS)
    fun toggleSplitScreen(): Boolean = performGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)
    fun openNotifications(): Boolean = performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    fun openQuickSettings(): Boolean = performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
}
