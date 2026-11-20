package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.DEFAULT_CATEGORIES
import com.example.model.NotebookCategory
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

enum class AppAppearanceMode(val title: String, val subtitle: String) {
    LIGHT("Light", "Crisp daylight surfaces"),
    DARK("Dark", "Deep slate night surfaces"),
    BLACK("Black", "Pure #000000 AMOLED canvas"),
    SYSTEM("System Default", "Follow device appearance")
}

enum class AppThemeMode(
    val title: String,
    val colorHex: Long,
    val darkColorHex: Long,
    val onLightAccentHex: Long = 0xFFFFFFFF,
    val onDarkAccentHex: Long = 0xFF0B1320
) {
    SLATE("Classic Slate", 0xFF0B1320, 0xFFF1F5F9, 0xFFFFFFFF, 0xFF0B1320),
    INDIGO("Nordic Indigo", 0xFF4F46E5, 0xFF818CF8, 0xFFFFFFFF, 0xFF0B1120),
    EMERALD("Forest Emerald", 0xFF059669, 0xFF34D399, 0xFFFFFFFF, 0xFF052E16),
    CRIMSON("Royal Crimson", 0xFFE11D48, 0xFFFB7185, 0xFFFFFFFF, 0xFF2A0510),
    AMBER("Warm Amber", 0xFFD97706, 0xFFFBBF24, 0xFFFFFFFF, 0xFF271702),
    OCEAN("Ocean Blue", 0xFF0284C7, 0xFF38BDF8, 0xFFFFFFFF, 0xFF082F49),
    VIOLET("Vivid Violet", 0xFF7C3AED, 0xFFA78BFA, 0xFFFFFFFF, 0xFF1E103C)
}

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("tippani_prefs", Context.MODE_PRIVATE)

    var selectedAppearanceIndex: Int
        get() = prefs.getInt("selected_appearance_index", 0)
        set(value) = prefs.edit().putInt("selected_appearance_index", value).apply()

    var selectedThemeIndex: Int
        get() = prefs.getInt("selected_theme_index", 0)
        set(value) = prefs.edit().putInt("selected_theme_index", value).apply()

    var defaultNotebook: String
        get() = prefs.getString("default_notebook", "personal") ?: "personal"
        set(value) = prefs.edit().putString("default_notebook", value).apply()

    var hasLoadedInitialData: Boolean
        get() = prefs.getBoolean("has_loaded_initial_data", false)
        set(value) = prefs.edit().putBoolean("has_loaded_initial_data", value).apply()

    fun loadBooks(): List<NotebookCategory> {
        val raw = prefs.getString("saved_books_json", null) ?: return DEFAULT_CATEGORIES
        return try {
            val arr = JSONArray(raw)
            val result = mutableListOf<NotebookCategory>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.optString("id", "").trim()
                val name = obj.optString("name", "").trim()
                if (id.isNotEmpty() && name.isNotEmpty()) {
                    result.add(
                        NotebookCategory(
                            id = id,
                            name = name,
                            iconName = obj.optString("iconName", "menu_book"),
                            colorHex = obj.optLong("colorHex", 0xFF6366F1L),
                            isPinned = obj.optBoolean("isPinned", false),
                            isLocked = obj.optBoolean("isLocked", false),
                            pinnedAt = obj.optLong("pinnedAt", 0L)
                        )
                    )
                }
            }
            result
        } catch (_: Exception) {
            DEFAULT_CATEGORIES
        }
    }

    fun saveBooks(books: List<NotebookCategory>) {
        val arr = JSONArray()
        books.forEach { book ->
            val obj = JSONObject().apply {
                put("id", book.id)
                put("name", book.name)
                put("iconName", book.iconName)
                put("colorHex", book.colorHex)
                put("isPinned", book.isPinned)
                put("isLocked", book.isLocked)
                put("pinnedAt", book.pinnedAt)
            }
            arr.put(obj)
        }
        prefs.edit().putString("saved_books_json", arr.toString()).apply()
    }

    // Locked Note IDs
    fun loadLockedNoteIds(): Set<Long> {
        val rawSet = prefs.getStringSet("locked_note_ids", emptySet()) ?: emptySet()
        return rawSet.mapNotNull { it.toLongOrNull() }.toSet()
    }

    fun saveLockedNoteIds(ids: Set<Long>) {
        prefs.edit().putStringSet("locked_note_ids", ids.map { it.toString() }.toSet()).apply()
    }

    // Pinned Note timestamps (for stable pinned order unaffected by sorting)
    fun loadPinnedNoteTimestamps(): Map<Long, Long> {
        val raw = prefs.getString("pinned_note_timestamps_json", null) ?: return emptyMap()
        return try {
            val obj = JSONObject(raw)
            val result = mutableMapOf<Long, Long>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val id = key.toLongOrNull()
                if (id != null) {
                    result[id] = obj.optLong(key, 0L)
                }
            }
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun savePinnedNoteTimestamps(map: Map<Long, Long>) {
        val obj = JSONObject()
        map.forEach { (id, ts) ->
            obj.put(id.toString(), ts)
        }
        prefs.edit().putString("pinned_note_timestamps_json", obj.toString()).apply()
    }

    // Lock PIN (SHA-256 salted hash)
    fun hasLockPin(): Boolean {
        return !prefs.getString("lock_pin_hash", null).isNullOrBlank()
    }

    fun setLockPin(pin: String) {
        val hash = hashPin(pin)
        prefs.edit().putString("lock_pin_hash", hash).apply()
    }

    fun verifyLockPin(pin: String): Boolean {
        val stored = prefs.getString("lock_pin_hash", null) ?: return false
        return stored == hashPin(pin)
    }

    private fun hashPin(pin: String): String {
        val salted = "tippani_lock_v1:$pin"
        val digest = MessageDigest.getInstance("SHA-256").digest(salted.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
