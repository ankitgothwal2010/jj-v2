/*
 * Copyright ©️ 2026 Ankit Gothwal.
 * All rights reserved, subject to applicable third-party license terms.
 *
 * Project: JARVIS AI
 * Developer: Ankit Gothwal
 * Application ID: com.ankit.jarvis
 */
package com.ankit.jarvis

import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.AlarmClock
import android.provider.CallLog
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android. provider.Telephony
import android.telecom.TelecomManager
import android.telephony.SmsManager
import android.view.KeyEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CommandResult(
    val handled: Boolean,
    val spokenResponse: String,
    val badge: String = "DEVICE CMD"
)

/**
 * Complete Master Phone Controller (`CommandExecutor.kt`) for JARVIS AI by Ankit Gothwal.
 *
 * Integrates ALL Android phone features in English & Hindi/Hinglish:
 * 1. Calls & Telephony:
 *    - Direct Call by Number (`"Call +919876543210"`) or Contact Name (`"Call Rahul"`, `"Rahul ko call karo"`)
 *    - Answer Incoming Call (`"Answer call"`, `"Pick up call"`, `"Call uthao"`)
 *    - End / Reject Call (`"End call"`, `"Hang up"`, `"Call kato"`)
 *    - Speakerphone On / Off (`"Speaker on"`, `"Speaker off"`, `"Speaker chalu karo"`)
 *    - Redial Last Number (`"Redial"`, `"Call last number"`)
 *    - Read Missed / Recent Calls (`"Missed calls"`, `"Recent calls"`, `"Kiska call aaya tha"`)
 * 2. SMS & WhatsApp Messaging:
 *    - Direct SMS Send (`"Send message to +919876543210 I am coming"`, `"Send SMS to Rahul hello"`)
 *    - Read Latest SMS Inbox (`"Read my messages"`, `"Read last SMS"`, `"Mere message padho"`)
 *    - WhatsApp Direct Message (`"WhatsApp +919876543210 Hello"`, `"WhatsApp Rahul namaste"`)
 * 3. Connectivity & Wireless Controls:
 *    - Wi-Fi On / Off (`"Turn Wi-Fi on"`, `"Turn Wi-Fi off"`, `"Wifi chalu karo"`, `"Wifi band karo"`)
 *    - Bluetooth On / Off (`"Turn Bluetooth on"`, `"Turn Bluetooth off"`, `"Bluetooth chalu karo"`)
 *    - Mobile Data / Internet Panel (`"Turn on mobile data"`, `"Data chalu karo"`, `"Internet settings"`)
 *    - Wi-Fi Hotspot / Tethering (`"Turn on hotspot"`, `"Hotspot kholo"`)
 *    - Location / GPS Settings (`"Turn on location"`, `"GPS kholo"`)
 *    - Airplane Mode Settings (`"Airplane mode"`, `"Flight mode"`)
 *    - NFC Settings (`"Open NFC"`)
 * 4. Display, Brightness, Auto-Rotate & Flashlight:
 *    - Flashlight / Torch On & Off (`"Flashlight on"`, `"Torch off"`, `"Torch jala do"`, `"Torch band karo"`)
 *    - Screen Brightness 0%–100% (`"Set brightness to 80%"`, `"Brightness full karo"`, `"Brightness kam karo"`)
 *    - Auto-Rotate Screen On / Off (`"Turn on auto rotate"`, `"Auto rotate off"`)
 * 5. Sound Profiles, DND & Volume (Media / Ring / Alarm):
 *    - Silent Mode (`"Silent mode"`, `"Phone silent karo"`), Vibrate Mode (`"Vibrate mode"`), Normal Ring Mode (`"Normal mode"`, `"Ringer on"`)
 *    - Do Not Disturb (`"Turn on DND"`, `"Do not disturb off"`)
 *    - Volume Up / Down / Mute / Unmute / Percentage (`"Set volume to 70%"`, `"Volume up"`, `"Awaz badhao"`)
 * 6. Screen Lock & System Navigation (Accessibility + DeviceAdmin):
 *    - Lock Phone (`"Lock my phone"`, `"Phone lock karo"`)
 *    - Take Screenshot (`"Take a screenshot"`, `"Screenshot lo"`)
 *    - Go Home (`"Go home"`), Go Back (`"Go back"`), Recent Apps (`"Recent apps"`), Split Screen (`"Split screen"`)
 *    - Notifications (`"Open notifications"`), Quick Settings (`"Open quick settings"`), Power Menu (`"Power menu"`, `"Reboot menu"`)
 * 7. Camera (Selfie, Rear Photo, Video) & Media Playback:
 *    - `"Take a selfie"`, `"Open front camera"`, `"Record a video"`, `"Open camera"`
 *    - `"Play music"`, `"Pause music"`, `"Next song"`, `"Previous song"`
 * 8. Alarms, Timers, Calendar, Notes, Clipboard & Device Telemetry:
 *    - `"Set alarm for 6 30 am"`, `"Set timer for 10 minutes"`, `"Create calendar event"`
 *    - `"Copy to clipboard <text>"`, `"Read clipboard"`, `"Storage status"`, `"Battery status"`, `"RAM status"`
 *    - `"Open <Any Installed App>"` or `"<App Name> kholo"`
 */
class CommandExecutor(
    private val context: Context,
    private val memoryManager: LocalMemoryManager
) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun processCommand(rawInput: String): CommandResult {
        val input = rawInput.trim()
        val lower = input.lowercase(Locale.ROOT)

        if (lower.isEmpty()) {
            return CommandResult(false, "")
        }

        // 1. Creator & Ownership Queries
        if (lower.contains("who created you") ||
            lower.contains("who made you") ||
            lower.contains("your creator") ||
            lower.contains("your developer") ||
            lower.contains("tumhe kisne banaya") ||
            lower.contains("aapko kisne banaya") ||
            lower.contains("who owns you")
        ) {
            return CommandResult(
                handled = true,
                spokenResponse = "I am JARVIS, an independent personal AI assistant created and developed by Ankit Gothwal.",
                badge = "IDENTITY"
            )
        }

        // 2. Lock Phone Screen ("lock my phone", "lock screen", "phone lock karo")
        if (lower.contains("lock my phone") ||
            lower.contains("lock the phone") ||
            lower.contains("lock phone") ||
            lower.contains("lock screen") ||
            lower.contains("phone lock karo") ||
            lower.contains("screen lock karo")
        ) {
            return lockPhoneScreen()
        }

        // 3. Power Menu / Shutdown Dialog ("power menu", "power off menu", "reboot phone")
        if (lower.contains("power menu") || lower.contains("power dialog") || lower.contains("reboot menu") || lower.contains("switch off phone")) {
            val svc = JarvisAccessibilityService.instance
            return if (svc != null && svc.showPowerDialog()) {
                CommandResult(true, "Opening the system power menu.", "POWER MENU")
            } else {
                openAccessibilitySettingsPrompt("Enable 'JARVIS Full Phone Control Service' in Accessibility Settings to open the power menu by voice.")
            }
        }

        // 4. Flashlight / Torch Control
        if (lower.contains("flashlight on") ||
            lower.contains("turn on flashlight") ||
            lower.contains("turn flashlight on") ||
            lower.contains("torch on") ||
            lower.contains("turn on torch") ||
            lower.contains("torch chalu") ||
            lower.contains("torch jala") ||
            lower.contains("light jala")
        ) {
            return setFlashlight(true)
        }
        if (lower.contains("flashlight off") ||
            lower.contains("turn off flashlight") ||
            lower.contains("turn flashlight off") ||
            lower.contains("torch off") ||
            lower.contains("turn off torch") ||
            lower.contains("torch band") ||
            lower.contains("light band")
        ) {
            return setFlashlight(false)
        }

        // 5. Wi-Fi On / Off
        if (lower.contains("turn wifi on") ||
            lower.contains("turn on wifi") ||
            lower.contains("turn wi-fi on") ||
            lower.contains("turn on wi-fi") ||
            lower.contains("enable wifi") ||
            lower.contains("wifi on") ||
            lower.contains("wifi chalu")
        ) {
            return setWifiState(true)
        }
        if (lower.contains("turn wifi off") ||
            lower.contains("turn off wifi") ||
            lower.contains("turn wi-fi off") ||
            lower.contains("turn off wi-fi") ||
            lower.contains("disable wifi") ||
            lower.contains("wifi off") ||
            lower.contains("wifi band")
        ) {
            return setWifiState(false)
        }

        // 6. Bluetooth On / Off
        if (lower.contains("turn bluetooth on") ||
            lower.contains("turn on bluetooth") ||
            lower.contains("enable bluetooth") ||
            lower.contains("bluetooth on") ||
            lower.contains("bluetooth chalu")
        ) {
            return setBluetoothState(true)
        }
        if (lower.contains("turn bluetooth off") ||
            lower.contains("turn off bluetooth") ||
            lower.contains("disable bluetooth") ||
            lower.contains("bluetooth off") ||
            lower.contains("bluetooth band")
        ) {
            return setBluetoothState(false)
        }

        // 7. Mobile Data / Internet / Hotspot / Location / Airplane Mode / NFC
        if (lower.contains("mobile data") || lower.contains("internet on") || lower.contains("internet off") || lower.contains("data chalu") || lower.contains("data band")) {
            return openConnectivityPanelOrSetting(
                panelAction = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Settings.Panel.ACTION_INTERNET_CONNECTIVITY else null,
                fallbackAction = Settings.ACTION_DATA_ROAMING_SETTINGS,
                message = "Opening Mobile Data & Internet controls."
            )
        }
        if (lower.contains("hotspot") || lower.contains("tethering")) {
            return openSystemSettings(Settings.ACTION_WIRELESS_SETTINGS, "Opening Hotspot & Tethering controls.")
        }
        if (lower.contains("location on") || lower.contains("location off") || lower.contains("turn on location") || lower.contains("turn on gps") || lower.contains("gps kholo") || lower.contains("location settings")) {
            return openSystemSettings(Settings.ACTION_LOCATION_SOURCE_SETTINGS, "Opening Location / GPS controls.")
        }
        if (lower.contains("airplane mode") || lower.contains("flight mode")) {
            return openSystemSettings(Settings.ACTION_AIRPLANE_MODE_SETTINGS, "Opening Airplane Mode settings.")
        }
        if (lower.contains("nfc")) {
            return openSystemSettings(Settings.ACTION_NFC_SETTINGS, "Opening NFC settings.")
        }

        // 8. Screen Brightness Control ("set brightness to 80%", "brightness full karo", "brightness low")
        if (lower.contains("brightness")) {
            return handleBrightnessCommand(lower)
        }

        // 9. Auto-Rotate Screen Control ("auto rotate on", "auto rotate off")
        if (lower.contains("auto rotate") || lower.contains("screen rotation")) {
            val enable = !lower.contains("off") && !lower.contains("disable") && !lower.contains("band")
            return setAutoRotate(enable)
        }

        // 10. Ringer Modes: Silent / Vibrate / Normal / Do Not Disturb (DND)
        if (lower.contains("silent mode") || lower.contains("phone silent") || lower.contains("silent karo")) {
            return setRingerProfile(AudioManager.RINGER_MODE_SILENT, "Silent mode")
        }
        if (lower.contains("vibrate mode") || lower.contains("vibration mode") || lower.contains("vibrate par")) {
            return setRingerProfile(AudioManager.RINGER_MODE_VIBRATE, "Vibrate mode")
        }
        if (lower.contains("normal mode") || lower.contains("ringer on") || lower.contains("general mode") || lower.contains("sound on")) {
            return setRingerProfile(AudioManager.RINGER_MODE_NORMAL, "Normal ringing mode")
        }
        if (lower.contains("do not disturb") || lower.contains("dnd on") || lower.contains("dnd off")) {
            val turnOn = !lower.contains("off") && !lower.contains("disable") && !lower.contains("band")
            return setDoNotDisturb(turnOn)
        }

        // 11. Call Controls: Answer Call, End Call, Speakerphone, Redial, Missed Calls
        if (lower.contains("answer call") || lower.contains("pick up call") || lower.contains("accept call") || lower.contains("call uthao")) {
            return answerIncomingCall()
        }
        if (lower.contains("end call") || lower.contains("hang up") || lower.contains("reject call") || lower.contains("call kato")) {
            return endActiveCall()
        }
        if (lower.contains("speaker on") || lower.contains("turn on speaker") || lower.contains("speakerphone on") || lower.contains("speaker chalu")) {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = true
            return CommandResult(true, "Speakerphone turned ON.", "CALL AUDIO")
        }
        if (lower.contains("speaker off") || lower.contains("turn off speaker") || lower.contains("speakerphone off") || lower.contains("speaker band")) {
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = false
            return CommandResult(true, "Speakerphone turned OFF.", "CALL AUDIO")
        }
        if (lower == "redial" || lower.contains("call last number") || lower.contains("redial last")) {
            return redialLastCall()
        }
        if (lower.contains("missed call") || lower.contains("recent call") || lower.contains("call log") || lower.contains("kiska call aaya")) {
            return readRecentCallLog()
        }

        // 12. Read Latest SMS Messages ("read my messages", "read last sms", "mere message padho")
        if (lower.contains("read my messages") ||
            lower.contains("read last message") ||
            lower.contains("read last sms") ||
            lower.contains("check messages") ||
            lower.contains("mere message padho") ||
            lower.contains("inbox dikhao")
        ) {
            return readRecentSmsInbox()
        }

        // 13. Send WhatsApp or SMS Message
        if (lower.startsWith("whatsapp ")) {
            val rest = input.substring("whatsapp ".length).trim()
            return handleWhatsAppCommand(rest)
        }
        val smsPrefixes = listOf(
            "send a message to ", "send message to ", "send an sms to ",
            "send sms to ", "text message to ", "sms to ", "message to "
        )
        for (prefix in smsPrefixes) {
            if (lower.startsWith(prefix)) {
                val remainder = input.substring(prefix.length).trim()
                return handleSendSmsCommand(remainder)
            }
        }

        // 14. Make a Phone Call ("call +919876543210", "Call Rahul", "Rahul ko call karo")
        if (lower.startsWith("call ") || lower.startsWith("dial ") || lower.startsWith("phone call ")) {
            val target = input.substringAfter(" ").removePrefix("call ").trim()
            if (target.isNotEmpty()) {
                return handlePhoneCallCommand(target)
            }
        }
        if (lower.endsWith(" ko call karo") || lower.endsWith(" ko phone lagao")) {
            val target = input.substringBeforeLast(" ko ").trim()
            if (target.isNotEmpty()) {
                return handlePhoneCallCommand(target)
            }
        }

        // 15. System Navigation & Global Actions (Screenshot, Home, Back, Recents, Split Screen, Notifications, Quick Settings)
        if (lower.contains("take a screenshot") || lower.contains("take screenshot") || lower.contains("screenshot lo")) {
            val svc = JarvisAccessibilityService.instance
            return if (svc != null && svc.takeScreenshot()) {
                CommandResult(true, "Taking a screenshot now.", "SCREENSHOT")
            } else {
                openAccessibilitySettingsPrompt("Enable 'JARVIS Full Phone Control Service' in Accessibility Settings to take screenshots by voice.")
            }
        }
        if (lower.contains("go to home screen") || lower == "go home" || lower == "home screen" || lower == "home chalo") {
            val svc = JarvisAccessibilityService.instance
            if (svc != null && svc.goHome()) {
                return CommandResult(true, "Going to the home screen.", "SYSTEM NAV")
            }
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(homeIntent)
            return CommandResult(true, "Going to the home screen.", "SYSTEM NAV")
        }
        if (lower == "go back" || lower == "back" || lower == "peeche jao") {
            val svc = JarvisAccessibilityService.instance
            return if (svc != null && svc.goBack()) {
                CommandResult(true, "Navigating back.", "SYSTEM NAV")
            } else {
                openAccessibilitySettingsPrompt("Enable 'JARVIS Full Phone Control Service' in Accessibility Settings for global Back navigation.")
            }
        }
        if (lower.contains("recent apps") || lower.contains("open recents")) {
            val svc = JarvisAccessibilityService.instance
            return if (svc != null && svc.openRecents()) {
                CommandResult(true, "Opening recent applications.", "SYSTEM NAV")
            } else {
                openAccessibilitySettingsPrompt("Enable 'JARVIS Full Phone Control Service' in Accessibility Settings to open Recent Apps.")
            }
        }
        if (lower.contains("split screen")) {
            val svc = JarvisAccessibilityService.instance
            return if (svc != null && svc.toggleSplitScreen()) {
                CommandResult(true, "Toggling split screen mode.", "SYSTEM NAV")
            } else {
                openAccessibilitySettingsPrompt("Enable 'JARVIS Full Phone Control Service' in Accessibility Settings to toggle Split Screen.")
            }
        }
        if (lower.contains("open notifications") || lower.contains("show notifications") || lower.contains("notification dikhao")) {
            val svc = JarvisAccessibilityService.instance
            return if (svc != null && svc.openNotifications()) {
                CommandResult(true, "Expanding notifications shade.", "SYSTEM NAV")
            } else {
                openAccessibilitySettingsPrompt("Enable 'JARVIS Full Phone Control Service' in Accessibility Settings to expand notifications.")
            }
        }
        if (lower.contains("open quick settings") || lower.contains("quick settings")) {
            val svc = JarvisAccessibilityService.instance
            return if (svc != null && svc.openQuickSettings()) {
                CommandResult(true, "Opening Quick Settings panel.", "SYSTEM NAV")
            } else {
                openAccessibilitySettingsPrompt("Enable 'JARVIS Full Phone Control Service' in Accessibility Settings to open Quick Settings.")
            }
        }

        // 16. Camera Modes: Selfie / Front Camera, Video Recording, Rear Camera
        if (lower.contains("selfie") || lower.contains("front camera")) {
            return launchCameraMode(frontCamera = true, videoMode = false)
        }
        if (lower.contains("record video") || lower.contains("record a video") || lower.contains("video camera") || lower.contains("video banao")) {
            return launchCameraMode(frontCamera = false, videoMode = true)
        }
        if (lower.contains("open camera") ||
            lower.contains("launch camera") ||
            lower.contains("take a photo") ||
            lower.contains("take a picture") ||
            lower.contains("camera kholo") ||
            lower.contains("photo khicho")
        ) {
            return launchCameraMode(frontCamera = false, videoMode = false)
        }

        // 17. Volume Controls (Percentage, Up, Down, Max, Mute, Unmute)
        if (lower.contains("volume")) {
            val pctMatch = Regex("(\\d{1,3})\\s*%?").find(lower)
            if (pctMatch != null && (lower.contains("set") || lower.contains("to") || lower.contains("%"))) {
                val pct = pctMatch.groupValues[1].toIntOrNull()?.coerceIn(0, 100) ?: 50
                val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val targetVol = ((pct / 100.0) * max).toInt().coerceIn(0, max)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, AudioManager.FLAG_SHOW_UI)
                return CommandResult(true, "Media volume set to $pct% ($targetVol out of $max).", "AUDIO CTRL")
            }
        }
        if (lower.contains("volume up") ||
            lower.contains("increase volume") ||
            lower.contains("raise volume") ||
            lower.contains("louder") ||
            lower.contains("awaz badhao") ||
            lower.contains("volume badhao")
        ) {
            return adjustMediaVolume(AudioManager.ADJUST_RAISE, "Increasing media volume.")
        }
        if (lower.contains("volume down") ||
            lower.contains("decrease volume") ||
            lower.contains("lower volume") ||
            lower.contains("quieter") ||
            lower.contains("awaz kam karo") ||
            lower.contains("volume kam karo")
        ) {
            return adjustMediaVolume(AudioManager.ADJUST_LOWER, "Decreasing media volume.")
        }
        if (lower.contains("full volume") || lower.contains("max volume") || lower.contains("awaz full karo")) {
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, max, AudioManager.FLAG_SHOW_UI)
            return CommandResult(true, "Media volume set to maximum ($max).", "AUDIO CTRL")
        }
        if (lower.contains("mute media") || lower.contains("mute volume") || lower.contains("awaz band karo")) {
            return adjustMediaVolume(AudioManager.ADJUST_MUTE, "Media audio muted.")
        }
        if (lower.contains("unmute media") || lower.contains("unmute volume") || lower.contains("awaz chalu karo")) {
            return adjustMediaVolume(AudioManager.ADJUST_UNMUTE, "Media audio unmuted.")
        }

        // 18. Media Playback Controls
        if (lower == "play music" || lower == "resume music" || lower == "play media" || lower == "gaana chalao") {
            return dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY, "Sending media play command.")
        }
        if (lower == "pause music" || lower == "stop music" || lower == "pause media" || lower == "gaana roko") {
            return dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PAUSE, "Sending media pause command.")
        }
        if (lower.contains("next song") || lower.contains("next track") || lower.contains("agla gaana")) {
            return dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT, "Skipping to the next track.")
        }
        if (lower.contains("previous song") || lower.contains("previous track") || lower.contains("pichla gaana")) {
            return dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS, "Returning to the previous track.")
        }

        // 19. Alarms, Countdown Timers & Calendar Events
        if (lower.startsWith("set timer") || lower.contains("timer for ") || lower.contains("timer lagao")) {
            return handleTimerCommand(lower)
        }
        if (lower.startsWith("set alarm") || lower.startsWith("open alarm") || lower.contains("alarm lagao")) {
            return handleAlarmCommand(lower)
        }
        if (lower.startsWith("create event") || lower.startsWith("add calendar") || lower.contains("calendar event")) {
            val title = input.substringAfter("event").trim().ifEmpty { "JARVIS Reminder" }
            val calIntent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, title)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(calIntent)
            return CommandResult(true, "Opening Calendar to save event: \"$title\".", "CALENDAR")
        }

        // 20. Clipboard Control ("copy to clipboard <text>", "read clipboard")
        if (lower.startsWith("copy to clipboard ") || lower.startsWith("copy text ")) {
            val textToCopy = input.substringAfter("clipboard ").substringAfter("text ").trim()
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("JARVIS Copy", textToCopy))
            vibratePulse()
            return CommandResult(true, "Copied to your clipboard: \"$textToCopy\"", "CLIPBOARD")
        }
        if (lower.contains("read clipboard") || lower.contains("what is in my clipboard")) {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipText = cm.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()?.trim()
            return if (!clipText.isNullOrEmpty()) {
                CommandResult(true, "Your clipboard contains: \"$clipText\"", "CLIPBOARD")
            } else {
                CommandResult(true, "Your clipboard is currently empty or inaccessible.", "CLIPBOARD")
            }
        }

        // 21. Device Telemetry: Time, Date, Battery, Storage Space
        if (lower.contains("what time") || lower.contains("current time") || lower.contains("tell me the time") ||
            lower == "time" || lower.contains("kya time") || lower.contains("samay kya") || lower.contains("kitne baje")
        ) {
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            return CommandResult(true, "The current time is ${sdf.format(Date())}.", "SYSTEM CLOCK")
        }
        if (lower.contains("what date") || lower.contains("today's date") || lower.contains("what day is it") ||
            lower == "date" || lower.contains("aaj kya tareekh") || lower.contains("aaj kaun sa din")
        ) {
            val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
            return CommandResult(true, "Today is ${sdf.format(Date())}.", "SYSTEM CLOCK")
        }
        if (lower.contains("battery") || lower.contains("charge level") || lower.contains("battery kitni hai")) {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val pct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            return CommandResult(true, "Your device battery is currently at $pct percent.", "TELEMETRY")
        }
        if (lower.contains("storage") || lower.contains("disk space") || lower.contains("free space") || lower.contains("storage kitni hai")) {
            return getStorageReport()
        }

        // 22. Local Notes
        val notePrefixes = listOf(
            "create a note ", "take a note ", "save a note ", "add note ",
            "note down ", "remember that ", "note likho "
        )
        for (prefix in notePrefixes) {
            if (lower.startsWith(prefix)) {
                val noteBody = input.substring(prefix.length).trim()
                if (noteBody.isNotEmpty()) {
                    val saved = memoryManager.addNote(noteBody)
                    return CommandResult(true, "Saved to your local notes: \"${saved.content}\"", "LOCAL NOTES")
                }
            }
        }
        if (lower.contains("show my notes") || lower.contains("read my notes") || lower.contains("view notes") ||
            lower == "my notes" || lower.contains("mere notes dikhao")
        ) {
            val notes = memoryManager.getAllNotes()
            if (notes.isEmpty()) {
                return CommandResult(true, "You do not have any saved local notes yet. Say 'Note down' followed by your message.", "LOCAL NOTES")
            }
            val formatted = notes.take(5).mapIndexed { index, note ->
                "${index + 1}. ${note.content} (${note.formattedDate()})"
            }.joinToString("\n")
            return CommandResult(true, "Here are your recent local notes:\n$formatted", "LOCAL NOTES")
        }
        if (lower.contains("clear my notes") || lower.contains("delete all notes")) {
            memoryManager.clearAllNotes()
            return CommandResult(true, "All local notes have been permanently deleted.", "LOCAL NOTES")
        }

        // 23. System Settings Screens
        if (lower.contains("wifi settings") || lower.contains("wi-fi settings") || lower.contains("open wifi")) {
            return openSystemSettings(Settings.ACTION_WIFI_SETTINGS, "Opening Wi-Fi settings.")
        }
        if (lower.contains("bluetooth settings") || lower.contains("open bluetooth")) {
            return openSystemSettings(Settings.ACTION_BLUETOOTH_SETTINGS, "Opening Bluetooth settings.")
        }
        if (lower.contains("display settings") || lower.contains("brightness settings")) {
            return openSystemSettings(Settings.ACTION_DISPLAY_SETTINGS, "Opening Display settings.")
        }
        if (lower.contains("sound settings") || lower.contains("audio settings")) {
            return openSystemSettings(Settings.ACTION_SOUND_SETTINGS, "Opening Sound settings.")
        }
        if (lower.contains("developer options")) {
            return openSystemSettings(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS, "Opening Developer Options.")
        }
        if (lower.contains("open settings") || lower.contains("system settings") || lower.contains("settings kholo")) {
            return openSystemSettings(Settings.ACTION_SETTINGS, "Opening Android system settings.")
        }

        // 24. Explicit Website Launch
        if (lower.startsWith("open website ") || lower.startsWith("go to website ") || lower.startsWith("browse ")) {
            val rawTarget = input.substringAfter(" ").substringAfter("website ").trim()
            return launchWebsite(rawTarget)
        }

        // 25. Open Any Installed Application (English + Hinglish)
        if (lower.startsWith("open ") || lower.startsWith("launch ")) {
            val targetApp = input.substringAfter(" ").trim()
            if (targetApp.isNotEmpty()) {
                return launchInstalledAppOrSite(targetApp)
            }
        }
        if (lower.endsWith(" kholo")) {
            val targetApp = input.substringBeforeLast(" kholo").trim()
            if (targetApp.isNotEmpty()) {
                return launchInstalledAppOrSite(targetApp)
            }
        }

        return CommandResult(handled = false, spokenResponse = "")
    }

    // --- Detailed Implementations for All Phone Controls ---

    private fun vibratePulse() {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(60)
            }
        } catch (_: Exception) {
        }
    }

    private fun lockPhoneScreen(): CommandResult {
        val a11y = JarvisAccessibilityService.instance
        if (a11y != null && a11y.lockPhoneScreen()) {
            return CommandResult(true, "Locking your phone screen now.", "SCREEN LOCK")
        }

        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(context, JarvisDeviceAdminReceiver::class.java)
        if (dpm.isAdminActive(adminComponent)) {
            dpm.lockNow()
            return CommandResult(true, "Locking your phone screen now.", "SCREEN LOCK")
        }

        return try {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Allow JARVIS AI (by Ankit Gothwal) to lock your phone screen when you say 'Lock my phone'."
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            CommandResult(
                true,
                "Please tap 'Activate' on the screen once to allow JARVIS to lock your phone anytime.",
                "SETUP LOCK"
            )
        } catch (e: Exception) {
            openAccessibilitySettingsPrompt("Enable 'JARVIS Full Phone Control Service' in Accessibility Settings to lock your phone.")
        }
    }

    private fun openAccessibilitySettingsPrompt(message: String): CommandResult {
        return try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            CommandResult(true, message, "ACCESSIBILITY")
        } catch (e: Exception) {
            CommandResult(true, message, "ACCESSIBILITY")
        }
    }

    private fun setFlashlight(turnOn: Boolean): CommandResult {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull()
                ?: return CommandResult(true, "No camera flash hardware found on this device.", "TORCH")
            cameraManager.setTorchMode(cameraId, turnOn)
            vibratePulse()
            val stateWord = if (turnOn) "ON" else "OFF"
            CommandResult(true, "Flashlight turned $stateWord.", "TORCH CTRL")
        } catch (e: Exception) {
            CommandResult(true, "Unable to toggle flashlight: ${e.localizedMessage}", "ERROR")
        }
    }

    @Suppress("DEPRECATION")
    private fun setWifiState(turnOn: Boolean): CommandResult {
        return try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                wifiManager.isWifiEnabled = turnOn
                val word = if (turnOn) "enabled" else "disabled"
                CommandResult(true, "Wi-Fi has been $word.", "WIFI CTRL")
            } else {
                val panelIntent = Intent(Settings.Panel.ACTION_WIFI).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(panelIntent)
                val word = if (turnOn) "ON" else "OFF"
                CommandResult(true, "Opened the quick Wi-Fi toggle panel to switch Wi-Fi $word.", "WIFI PANEL")
            }
        } catch (e: Exception) {
            openSystemSettings(Settings.ACTION_WIFI_SETTINGS, "Opening Wi-Fi settings.")
        }
    }

    @Suppress("DEPRECATION", "MissingPermission")
    private fun setBluetoothState(turnOn: Boolean): CommandResult {
        return try {
            val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = btManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
            if (adapter == null) {
                return CommandResult(true, "Bluetooth is not supported on this device.", "BLUETOOTH")
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                val ok = if (turnOn) adapter.enable() else adapter.disable()
                if (ok) {
                    val word = if (turnOn) "ON" else "OFF"
                    return CommandResult(true, "Turning Bluetooth $word.", "BLUETOOTH")
                }
            }
            val intentAction = if (turnOn) BluetoothAdapter.ACTION_REQUEST_ENABLE else Settings.ACTION_BLUETOOTH_SETTINGS
            val intent = Intent(intentAction).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            CommandResult(true, "Opening Bluetooth control prompt.", "BLUETOOTH")
        } catch (e: Exception) {
            openSystemSettings(Settings.ACTION_BLUETOOTH_SETTINGS, "Opening Bluetooth settings.")
        }
    }

    private fun openConnectivityPanelOrSetting(panelAction: String?, fallbackAction: String, message: String): CommandResult {
        return try {
            if (panelAction != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val intent = Intent(panelAction).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                CommandResult(true, message, "QUICK PANEL")
            } else {
                openSystemSettings(fallbackAction, message)
            }
        } catch (_: Exception) {
            openSystemSettings(fallbackAction, message)
        }
    }

    private fun handleBrightnessCommand(lower: String): CommandResult {
        if (!PermissionManager.canWriteSystemSettings(context)) {
            return try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                CommandResult(
                    true,
                    "Please enable 'Allow modify system settings' on this screen once so JARVIS can control your screen brightness.",
                    "BRIGHTNESS PERM"
                )
            } catch (_: Exception) {
                openSystemSettings(Settings.ACTION_DISPLAY_SETTINGS, "Opening Display settings.")
            }
        }

        val numMatch = Regex("(\\d{1,3})").find(lower)
        val pct = when {
            numMatch != null -> numMatch.groupValues[1].toIntOrNull()?.coerceIn(1, 100) ?: 50
            lower.contains("full") || lower.contains("max") || lower.contains("100") || lower.contains("badhao") || lower.contains("high") -> 100
            lower.contains("low") || lower.contains("dim") || lower.contains("min") || lower.contains("kam") -> 20
            lower.contains("medium") || lower.contains("half") -> 50
            else -> 70
        }

        return try {
            val sysVal = ((pct / 100.0) * 255).toInt().coerceIn(5, 255)
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                sysVal
            )
            CommandResult(true, "Screen brightness set to $pct%.", "BRIGHTNESS")
        } catch (e: Exception) {
            CommandResult(true, "Could not change brightness: ${e.localizedMessage}", "ERROR")
        }
    }

    private fun setAutoRotate(enable: Boolean): CommandResult {
        if (!PermissionManager.canWriteSystemSettings(context)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return CommandResult(true, "Please allow 'Modify system settings' once so JARVIS can toggle Auto-Rotate.", "ROTATION")
        }
        return try {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                if (enable) 1 else 0
            )
            val word = if (enable) "ON" else "OFF"
            CommandResult(true, "Screen auto-rotation turned $word.", "ROTATION")
        } catch (e: Exception) {
            CommandResult(true, "Unable to toggle auto-rotate: ${e.localizedMessage}", "ERROR")
        }
    }

    private fun setRingerProfile(mode: Int, label: String): CommandResult {
        return try {
            if (mode == AudioManager.RINGER_MODE_SILENT && !PermissionManager.hasDndPermission(context)) {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return CommandResult(
                    true,
                    "Please grant 'Do Not Disturb access' to JARVIS once so it can switch your phone to Silent mode.",
                    "DND PERM"
                )
            }
            audioManager.ringerMode = mode
            vibratePulse()
            CommandResult(true, "Phone sound profile switched to $label.", "RINGER CTRL")
        } catch (e: Exception) {
            CommandResult(true, "Could not change ringer mode: ${e.localizedMessage}", "ERROR")
        }
    }

    private fun setDoNotDisturb(turnOn: Boolean): CommandResult {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!nm.isNotificationPolicyAccessGranted) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return CommandResult(
                true,
                "Please grant Do Not Disturb permission to JARVIS on this screen.",
                "DND PERM"
            )
        }
        return try {
            nm.setInterruptionFilter(
                if (turnOn) NotificationManager.INTERRUPTION_FILTER_NONE
                else NotificationManager.INTERRUPTION_FILTER_ALL
            )
            val word = if (turnOn) "ENABLED" else "DISABLED"
            CommandResult(true, "Do Not Disturb mode $word.", "DND CTRL")
        } catch (e: Exception) {
            CommandResult(true, "Could not change Do Not Disturb: ${e.localizedMessage}", "ERROR")
        }
    }

    @Suppress("DEPRECATION", "MissingPermission")
    private fun answerIncomingCall(): CommandResult {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && PermissionManager.hasAnswerCallPermission(context)) {
                val tm = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                tm.acceptRingingCall()
                CommandResult(true, "Answered incoming phone call.", "CALL CTRL")
            } else {
                dispatchMediaKey(KeyEvent.KEYCODE_HEADSETHOOK, "Sending answer call hook key.")
            }
        } catch (e: Exception) {
            CommandResult(true, "Unable to answer call: ${e.localizedMessage}", "ERROR")
        }
    }

    @Suppress("DEPRECATION", "MissingPermission")
    private fun endActiveCall(): CommandResult {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && PermissionManager.hasAnswerCallPermission(context)) {
                val tm = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                val ended = tm.endCall()
                if (ended) {
                    return CommandResult(true, "Call ended.", "CALL CTRL")
                }
            }
            dispatchMediaKey(KeyEvent.KEYCODE_ENDCALL, "Sent end call key.")
        } catch (e: Exception) {
            CommandResult(true, "Unable to end call: ${e.localizedMessage}", "ERROR")
        }
    }

    private fun redialLastCall(): CommandResult {
        if (!PermissionManager.hasCallLogPermission(context)) {
            return CommandResult(true, "Please grant Call Log & Phone permissions in JARVIS Settings to use Redial.", "CALL LOG")
        }
        return try {
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME),
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val number = it.getString(0)
                    if (!number.isNullOrBlank()) {
                        return handlePhoneCallCommand(number)
                    }
                }
            }
            CommandResult(true, "No recent call found in your call log.", "CALL LOG")
        } catch (e: Exception) {
            CommandResult(true, "Could not read call log: ${e.localizedMessage}", "ERROR")
        }
    }

    private fun readRecentCallLog(): CommandResult {
        if (!PermissionManager.hasCallLogPermission(context)) {
            return CommandResult(true, "Please tap 'Grant All Phone Permissions' in JARVIS Settings to read missed and recent calls.", "CALL LOG")
        }
        return try {
            val entries = mutableListOf<String>()
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.TYPE, CallLog.Calls.DATE),
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            )
            val sdf = SimpleDateFormat("dd MMM hh:mm a", Locale.getDefault())
            cursor?.use {
                var count = 0
                while (it.moveToNext() && count < 4) {
                    val num = it.getString(0) ?: "Unknown"
                    val name = it.getString(1)?.takeIf { n -> n.isNotBlank() } ?: num
                    val typeInt = it.getInt(2)
                    val dateMs = it.getLong(3)
                    val typeLabel = when (typeInt) {
                        CallLog.Calls.MISSED_TYPE -> "Missed"
                        CallLog.Calls.INCOMING_TYPE -> "Incoming"
                        CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                        else -> "Call"
                    }
                    entries.add("• $typeLabel: $name (${sdf.format(Date(dateMs))})")
                    count++
                }
            }
            if (entries.isEmpty()) {
                CommandResult(true, "Your recent call log is empty.", "CALL LOG")
            } else {
                CommandResult(true, "Here are your recent calls:\n${entries.joinToString("\n")}", "CALL LOG")
            }
        } catch (e: Exception) {
            CommandResult(true, "Unable to read call log: ${e.localizedMessage}", "ERROR")
        }
    }

    private fun readRecentSmsInbox(): CommandResult {
        if (!PermissionManager.hasReadSmsPermission(context)) {
            return CommandResult(true, "Please tap 'Grant All Phone Permissions' in JARVIS Settings to read your SMS inbox aloud.", "SMS INBOX")
        }
        return try {
            val messages = mutableListOf<String>()
            val cursor = context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
                null,
                null,
                "${Telephony.Sms.DATE} DESC"
            )
            cursor?.use {
                var count = 0
                while (it.moveToNext() && count < 3) {
                    val sender = it.getString(0) ?: "Unknown"
                    val body = it.getString(1)?.replace("\n", " ")?.take(90) ?: ""
                    messages.add("${count + 1}. From $sender: \"$body\"")
                    count++
                }
            }
            if (messages.isEmpty()) {
                CommandResult(true, "No SMS messages found in your inbox.", "SMS INBOX")
            } else {
                CommandResult(true, "Your latest SMS messages:\n${messages.joinToString("\n")}", "SMS INBOX")
            }
        } catch (e: Exception) {
            CommandResult(true, "Unable to read SMS inbox: ${e.localizedMessage}", "ERROR")
        }
    }

    private fun handlePhoneCallCommand(rawTarget: String): CommandResult {
        val cleanedTarget = rawTarget.trim()
        val digitsOnly = cleanedTarget.filter { it.isDigit() || it == '+' }
        val resolvedNumber = if (digitsOnly.length >= 6) {
            digitsOnly
        } else {
            lookupContactNumberByName(cleanedTarget)
        }

        if (resolvedNumber.isNullOrBlank()) {
            return CommandResult(
                true,
                "Could not find a phone number for \"$cleanedTarget\". Say 'Call +919876543210' or grant Contacts permission in Settings.",
                "CALL"
            )
        }

        return try {
            val hasCallPerm = PermissionManager.hasCallPermission(context)
            val action = if (hasCallPerm) Intent.ACTION_CALL else Intent.ACTION_DIAL
            val callIntent = Intent(action, Uri.parse("tel:${Uri.encode(resolvedNumber)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(callIntent)
            if (hasCallPerm) {
                CommandResult(true, "Calling $cleanedTarget ($resolvedNumber) now.", "DIRECT CALL")
            } else {
                CommandResult(
                    true,
                    "Opened dialer with $resolvedNumber. Tap 'Grant All Phone Permissions' in Settings for direct calling.",
                    "DIALER"
                )
            }
        } catch (e: Exception) {
            CommandResult(true, "Unable to place call: ${e.localizedMessage}", "ERROR")
        }
    }

    @Suppress("DEPRECATION")
    private fun handleSendSmsCommand(remainder: String): CommandResult {
        if (remainder.isBlank()) {
            return CommandResult(
                true,
                "Please specify the recipient and message, e.g., 'Send message to +919876543210 I am reaching soon'.",
                "SMS"
            )
        }

        val phoneRegex = Regex("^(\\+?\\d[\\d\\s\\-]{6,15}\\d)\\s+(?:saying\\s+|that\\s+|message\\s+)?(.+)$", RegexOption.IGNORE_CASE)
        val match = phoneRegex.find(remainder)

        val recipientLabel: String
        val resolvedNumber: String?
        val messageBody: String

        if (match != null) {
            recipientLabel = match.groupValues[1].trim()
            resolvedNumber = recipientLabel.filter { it.isDigit() || it == '+' }
            messageBody = match.groupValues[2].trim()
        } else {
            val parts = if (remainder.lowercase().contains(" saying ")) {
                val idx = remainder.lowercase().indexOf(" saying ")
                Pair(remainder.substring(0, idx).trim(), remainder.substring(idx + 8).trim())
            } else {
                val firstSpace = remainder.indexOf(' ')
                if (firstSpace > 0) {
                    Pair(remainder.substring(0, firstSpace).trim(), remainder.substring(firstSpace + 1).trim())
                } else {
                    Pair(remainder, "")
                }
            }
            recipientLabel = parts.first
            messageBody = parts.second
            val digits = recipientLabel.filter { it.isDigit() || it == '+' }
            resolvedNumber = if (digits.length >= 6) digits else lookupContactNumberByName(recipientLabel)
        }

        if (resolvedNumber.isNullOrBlank()) {
            return CommandResult(
                true,
                "Could not find a phone number for \"$recipientLabel\". Try saying 'Send message to +919876543210 Hello'.",
                "SMS"
            )
        }

        if (messageBody.isBlank()) {
            val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$resolvedNumber")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(smsIntent)
            return CommandResult(true, "Opened SMS app for $resolvedNumber.", "SMS APP")
        }

        return try {
            if (PermissionManager.hasSmsPermission(context)) {
                val smsManager = SmsManager.getDefault()
                val parts = smsManager.divideMessage(messageBody)
                smsManager.sendMultipartTextMessage(resolvedNumber, null, parts, null, null)
                CommandResult(true, "SMS sent directly to $resolvedNumber: \"$messageBody\"", "DIRECT SMS")
            } else {
                val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$resolvedNumber")).apply {
                    putExtra("sms_body", messageBody)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(smsIntent)
                CommandResult(
                    true,
                    "Opened SMS app with your message for $resolvedNumber. Grant SMS permission in JARVIS Settings for direct sending.",
                    "SMS PREFILL"
                )
            }
        } catch (e: Exception) {
            CommandResult(true, "Failed to send SMS: ${e.localizedMessage}", "ERROR")
        }
    }

    private fun handleWhatsAppCommand(rest: String): CommandResult {
        val phoneRegex = Regex("^(\\+?\\d[\\d\\s\\-]{6,15}\\d)\\s*(.*)$")
        val match = phoneRegex.find(rest)
        val rawNumber: String
        val text: String

        if (match != null) {
            rawNumber = match.groupValues[1].filter { it.isDigit() }
            text = match.groupValues[2].trim()
        } else {
            val firstWord = rest.substringBefore(" ").trim()
            val lookedUp = lookupContactNumberByName(firstWord)?.filter { it.isDigit() } ?: ""
            rawNumber = lookedUp
            text = rest.substringAfter(" ", "").trim()
        }

        if (rawNumber.length < 8) {
            return launchInstalledAppOrSite("whatsapp")
        }
        val cleanPhone = if (rawNumber.length == 10) "91$rawNumber" else rawNumber
        return try {
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(text)}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            CommandResult(true, "Opening WhatsApp chat for +$cleanPhone.", "WHATSAPP")
        } catch (e: Exception) {
            CommandResult(true, "WhatsApp is not installed or could not be opened.", "WHATSAPP")
        }
    }

    private fun lookupContactNumberByName(nameQuery: String): String? {
        if (!PermissionManager.hasContactsPermission(context)) return null
        val cleanQuery = nameQuery.trim()
        if (cleanQuery.isEmpty()) return null

        return try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$cleanQuery%"),
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val numberIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    if (numberIdx >= 0) {
                        return it.getString(numberIdx)?.trim()
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun handleAlarmCommand(lowerInput: String): CommandResult {
        val timeRegex = Regex("(\\d{1,2})(?::|\\s+)(\\d{2})?\\s*(am|pm)?")
        val match = timeRegex.find(lowerInput)
        return try {
            if (match != null) {
                var hour = match.groupValues[1].toIntOrNull() ?: 7
                val minute = match.groupValues[2].toIntOrNull() ?: 0
                val ampm = match.groupValues[3]
                if (ampm == "pm" && hour < 12) hour += 12
                if (ampm == "am" && hour == 12) hour = 0
                val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_HOUR, hour)
                    putExtra(AlarmClock.EXTRA_MINUTES, minute)
                    putExtra(AlarmClock.EXTRA_MESSAGE, "JARVIS Alarm")
                    putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                CommandResult(true, "Alarm set for ${String.format(Locale.US, "%02d:%02d", hour, minute)}.", "ALARM")
            } else {
                val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                CommandResult(true, "Opening your clock and alarms.", "CLOCK INTENT")
            }
        } catch (e: Exception) {
            CommandResult(true, "Unable to set alarm: ${e.localizedMessage}", "ERROR")
        }
    }

    private fun handleTimerCommand(lowerInput: String): CommandResult {
        val numMatch = Regex("(\\d+)").find(lowerInput)
        val amount = numMatch?.groupValues?.get(1)?.toIntOrNull() ?: 5
        val seconds = when {
            lowerInput.contains("hour") || lowerInput.contains("ghante") -> amount * 3600
            lowerInput.contains("sec") -> amount
            else -> amount * 60
        }
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, "JARVIS Timer")
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            CommandResult(true, "Countdown timer started for $seconds seconds.", "TIMER")
        } catch (e: Exception) {
            CommandResult(true, "Unable to start countdown timer: ${e.localizedMessage}", "ERROR")
        }
    }

    private fun getStorageReport(): CommandResult {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val freeGb = (stat.availableBytes / (1024.0 * 1024.0 * 1024.0))
            val totalGb = (stat.totalBytes / (1024.0 * 1024.0 * 1024.0))
            val formatted = String.format(Locale.US, "%.2f GB free out of %.2f GB total internal storage.", freeGb, totalGb)
            CommandResult(true, "Device Storage Report: $formatted", "STORAGE")
        } catch (e: Exception) {
            CommandResult(true, "Unable to read storage stats.", "STORAGE")
        }
    }

    private fun launchCameraMode(frontCamera: Boolean, videoMode: Boolean): CommandResult {
        return try {
            val action = if (videoMode) MediaStore.INTENT_ACTION_VIDEO_CAMERA else MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA
            val intent = Intent(action).apply {
                if (frontCamera) {
                    putExtra("android.intent.extras.CAMERA_FACING", 1)
                    putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
                    putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val label = when {
                frontCamera -> "Launching front selfie camera."
                videoMode -> "Launching video camera."
                else -> "Launching camera."
            }
            CommandResult(true, label, "CAMERA INTENT")
        } catch (e: Exception) {
            CommandResult(true, "Unable to launch camera: ${e.localizedMessage}", "ERROR")
        }
    }

    private fun adjustMediaVolume(direction: Int, message: String): CommandResult {
        return try {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                direction,
                AudioManager.FLAG_SHOW_UI
            )
            val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            CommandResult(true, "$message Current media volume is $current out of $max.", "AUDIO CTRL")
        } catch (e: Exception) {
            CommandResult(true, "Could not adjust media volume: ${e.localizedMessage}", "ERROR")
        }
    }

    private fun dispatchMediaKey(keyCode: Int, message: String): CommandResult {
        return try {
            val eventTime = SystemClock.uptimeMillis()
            val downEvent = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0)
            val upEvent = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0)
            audioManager.dispatchMediaKeyEvent(downEvent)
            audioManager.dispatchMediaKeyEvent(upEvent)
            CommandResult(true, message, "MEDIA CTRL")
        } catch (e: Exception) {
            CommandResult(true, "Could not send media playback key: ${e.localizedMessage}", "ERROR")
        }
    }

    private fun openSystemSettings(action: String, message: String): CommandResult {
        return try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            CommandResult(true, message, "SETTINGS INTENT")
        } catch (e: Exception) {
            CommandResult(true, "Unable to open the requested settings screen.", "ERROR")
        }
    }

    private fun launchWebsite(rawUrl: String): CommandResult {
        val cleaned = rawUrl.trim().lowercase(Locale.ROOT)
        if (cleaned.isEmpty()) {
            return CommandResult(true, "Please specify a valid website address to open.", "BROWSER")
        }
        val fullUrl = if (cleaned.startsWith("http://") || cleaned.startsWith("https://")) {
            cleaned
        } else if (cleaned.contains(".")) {
            "https://$cleaned"
        } else {
            "https://www.$cleaned.com"
        }

        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            CommandResult(true, "Opening $fullUrl in your default browser.", "BROWSER")
        } catch (e: Exception) {
            CommandResult(true, "Could not open website $fullUrl.", "ERROR")
        }
    }

    private fun launchInstalledAppOrSite(targetName: String): CommandResult {
        val lowerTarget = targetName.lowercase(Locale.ROOT).trim()

        if (lowerTarget.contains(".com") || lowerTarget.contains(".org") || lowerTarget.contains(".in")) {
            return launchWebsite(lowerTarget)
        }

        val pm: PackageManager = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolvedList = pm.queryIntentActivities(mainIntent, 0)
        for (info in resolvedList) {
            val label = info.loadLabel(pm).toString().lowercase(Locale.ROOT)
            val pkg = info.activityInfo.packageName.lowercase(Locale.ROOT)
            if (label == lowerTarget || label.contains(lowerTarget) || pkg.contains(lowerTarget)) {
                val launchIntent = pm.getLaunchIntentForPackage(info.activityInfo.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    val displayLabel = info.loadLabel(pm).toString()
                    return CommandResult(true, "Opening $displayLabel.", "APP LAUNCHER")
                }
            }
        }

        return CommandResult(
            handled = true,
            spokenResponse = "I could not find an installed application matching \"$targetName\" on your phone.",
            badge = "APP LAUNCHER"
        )
    }
}
