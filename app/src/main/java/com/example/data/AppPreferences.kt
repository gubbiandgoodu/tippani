package com.example.data

import android.content.Context
import android.content.SharedPreferences

enum class AppThemeMode(val title: String, val colorHex: Long) {
    SLATE("Classic Slate", 0xFF0F172A),
    CRIMSON("Royal Crimson", 0xFFE11D48),
    EMERALD("Forest Emerald", 0xFF059669),
    INDIGO("Nordic Indigo", 0xFF4F46E5),
    AMBER("Warm Amber", 0xFFD97706)
}

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("tippani_prefs", Context.MODE_PRIVATE)

    var selectedThemeIndex: Int
        get() = prefs.getInt("selected_theme_index", 0)
        set(value) = prefs.edit().putInt("selected_theme_index", value).apply()

    var defaultNotebook: String
        get() = prefs.getString("default_notebook", "personal") ?: "personal"
        set(value) = prefs.edit().putString("default_notebook", value).apply()

    var hasLoadedInitialData: Boolean
        get() = prefs.getBoolean("has_loaded_initial_data", false)
        set(value) = prefs.edit().putBoolean("has_loaded_initial_data", value).apply()
}
