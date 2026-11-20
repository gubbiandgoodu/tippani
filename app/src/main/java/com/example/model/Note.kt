package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val categoryId: String = "personal",
    val noteType: String = NOTE_FORMAT_NOTES, // Notes or Checklist
    val tags: String = "", // Comma-separated tags
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val wordCount: Int = 0,
    val readingTimeMinutes: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
