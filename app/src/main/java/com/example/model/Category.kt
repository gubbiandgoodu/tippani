package com.example.model

data class NotebookCategory(
    val id: String,
    val name: String,
    val iconName: String,
    val colorHex: Long,
    val isPinned: Boolean = false,
    val isLocked: Boolean = false,
    val pinnedAt: Long = 0L
)

val FALLBACK_BOOK = NotebookCategory("personal", "General", "menu_book", 0xFF6366F1)

val BOOK_COLOR_OPTIONS = listOf(
    0xFF6366F1L, // Indigo
    0xFF0EA5E9L, // Sky Blue
    0xFFF59E0BL, // Amber
    0xFF10B981L, // Emerald
    0xFFEC4899L, // Pink
    0xFF8B5CF6L, // Violet
    0xFFF97316L, // Orange
    0xFF14B8A6L  // Teal
)

val DEFAULT_CATEGORIES = listOf(
    NotebookCategory("personal", "Personal", "person", 0xFF6366F1),
    NotebookCategory("work", "Work & Projects", "work", 0xFF0EA5E9),
    NotebookCategory("ideas", "Quick Ideas", "lightbulb", 0xFFF59E0B),
    NotebookCategory("study", "Study & Reads", "menu_book", 0xFF10B981),
    NotebookCategory("journal", "Daily Journal", "auto_stories", 0xFFEC4899),
    NotebookCategory("tasks", "Tasks & Checklist", "check_circle", 0xFF8B5CF6)
)
