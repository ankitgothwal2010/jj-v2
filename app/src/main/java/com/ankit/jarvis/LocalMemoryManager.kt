/*
 * Copyright ©️ 2026 Ankit Gothwal.
 * All rights reserved, subject to applicable third-party license terms.
 *
 * Project: JARVIS AI
 * Developer: Ankit Gothwal
 * Application ID: com.ankit.jarvis
 */
package com.ankit.jarvis

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Represents a single chat message stored locally on the device.
 */
data class ChatMessage(
    val id: Long = 0,
    val sender: Sender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sourceBadge: String = "LOCAL"
) {
    enum class Sender { USER, JARVIS, SYSTEM }

    fun formattedTime(): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}

/**
 * Represents a local user note stored on-device.
 */
data class LocalNote(
    val id: Long = 0,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun formattedDate(): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return sdf.format(Date(createdAt))
    }
}

/**
 * Manages 100% on-device SQLite storage for conversations, local notes, and user preferences.
 * No data ever leaves the device. Includes a complete one-tap wipe function.
 */
class LocalMemoryManager(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val DB_NAME = "jarvis_local_memory.db"
        private const val DB_VERSION = 1
        private const val PREFS_NAME = "jarvis_preferences"

        private const val TABLE_MESSAGES = "messages"
        private const val COL_MSG_ID = "id"
        private const val COL_MSG_SENDER = "sender"
        private const val COL_MSG_TEXT = "text"
        private const val COL_MSG_TIME = "timestamp"
        private const val COL_MSG_BADGE = "badge"

        private const val TABLE_NOTES = "notes"
        private const val COL_NOTE_ID = "id"
        private const val COL_NOTE_CONTENT = "content"
        private const val COL_NOTE_TIME = "created_at"

        const val KEY_SPEECH_RATE = "pref_speech_rate"
        const val KEY_VOICE_MUTED = "pref_voice_muted"
        const val KEY_LANGUAGE = "pref_language" // "en-IN" or "hi-IN"
        const val KEY_THEME_ACCENT = "pref_theme_accent" // "CYAN", "BLUE", "HIGH_CONTRAST"
        const val KEY_SHOW_FOOTER_CREDIT = "pref_show_footer_credit"
        const val KEY_MODEL_PATH = "pref_gguf_model_path"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_MESSAGES (
                $COL_MSG_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_MSG_SENDER TEXT NOT NULL,
                $COL_MSG_TEXT TEXT NOT NULL,
                $COL_MSG_TIME INTEGER NOT NULL,
                $COL_MSG_BADGE TEXT NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_NOTES (
                $COL_NOTE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NOTE_CONTENT TEXT NOT NULL,
                $COL_NOTE_TIME INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MESSAGES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NOTES")
        onCreate(db)
    }

    // --- Conversation History Methods ---

    fun saveMessage(sender: ChatMessage.Sender, text: String, badge: String = "LOCAL"): ChatMessage {
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put(COL_MSG_SENDER, sender.name)
            put(COL_MSG_TEXT, text)
            put(COL_MSG_TIME, now)
            put(COL_MSG_BADGE, badge)
        }
        val id = writableDatabase.insert(TABLE_MESSAGES, null, values)
        return ChatMessage(id = id, sender = sender, text = text, timestamp = now, sourceBadge = badge)
    }

    fun getRecentMessages(limit: Int = 100): List<ChatMessage> {
        val list = mutableListOf<ChatMessage>()
        val cursor = readableDatabase.query(
            TABLE_MESSAGES,
            null,
            null,
            null,
            null,
            null,
            "$COL_MSG_TIME ASC",
            limit.toString()
        )
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(COL_MSG_ID))
                val senderStr = it.getString(it.getColumnIndexOrThrow(COL_MSG_SENDER))
                val text = it.getString(it.getColumnIndexOrThrow(COL_MSG_TEXT))
                val time = it.getLong(it.getColumnIndexOrThrow(COL_MSG_TIME))
                val badge = it.getString(it.getColumnIndexOrThrow(COL_MSG_BADGE))
                val sender = runCatching { ChatMessage.Sender.valueOf(senderStr) }
                    .getOrDefault(ChatMessage.Sender.SYSTEM)
                list.add(ChatMessage(id, sender, text, time, badge))
            }
        }
        return list
    }

    fun clearAllConversations() {
        writableDatabase.delete(TABLE_MESSAGES, null, null)
    }

    // --- Local Notes Methods ---

    fun addNote(content: String): LocalNote {
        val cleanContent = content.trim()
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put(COL_NOTE_CONTENT, cleanContent)
            put(COL_NOTE_TIME, now)
        }
        val id = writableDatabase.insert(TABLE_NOTES, null, values)
        return LocalNote(id = id, content = cleanContent, createdAt = now)
    }

    fun getAllNotes(): List<LocalNote> {
        val notes = mutableListOf<LocalNote>()
        val cursor = readableDatabase.query(
            TABLE_NOTES,
            null,
            null,
            null,
            null,
            null,
            "$COL_NOTE_TIME DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(COL_NOTE_ID))
                val content = it.getString(it.getColumnIndexOrThrow(COL_NOTE_CONTENT))
                val time = it.getLong(it.getColumnIndexOrThrow(COL_NOTE_TIME))
                notes.add(LocalNote(id, content, time))
            }
        }
        return notes
    }

    fun clearAllNotes() {
        writableDatabase.delete(TABLE_NOTES, null, null)
    }

    fun wipeAllMemory() {
        clearAllConversations()
        clearAllNotes()
    }

    // --- User Preferences ---

    var speechRate: Float
        get() = prefs.getFloat(KEY_SPEECH_RATE, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_SPEECH_RATE, value.coerceIn(0.5f, 2.0f)).apply()

    var isVoiceMuted: Boolean
        get() = prefs.getBoolean(KEY_VOICE_MUTED, false)
        set(value) = prefs.edit().putBoolean(KEY_VOICE_MUTED, value).apply()

    var preferredLanguage: String
        get() = prefs.getString(KEY_LANGUAGE, "en-IN") ?: "en-IN"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    var themeAccent: String
        get() = prefs.getString(KEY_THEME_ACCENT, "CYAN") ?: "CYAN"
        set(value) = prefs.edit().putString(KEY_THEME_ACCENT, value).apply()

    var showFooterCredit: Boolean
        get() = prefs.getBoolean(KEY_SHOW_FOOTER_CREDIT, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_FOOTER_CREDIT, value).apply()

    var customModelPath: String
        get() = prefs.getString(KEY_MODEL_PATH, "") ?: ""
        set(value) = prefs.edit().putString(KEY_MODEL_PATH, value.trim()).apply()
}
