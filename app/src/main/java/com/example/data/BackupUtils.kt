package com.example.data

import com.example.model.Note
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupUtils {

    private const val APP_NAME = "Tippani"
    private const val BACKUP_VERSION = "1.0"

    /**
     * Serializes all app notes into structured, pretty-printed JSON containing
     * metadata, summary counts, and the records array.
     */
    fun serializeNotesToJson(notes: List<Note>): String {
        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val totalWords = notes.sumOf { it.wordCount }
        val activeCount = notes.count { !it.isArchived }
        val archivedCount = notes.count { it.isArchived }

        val notesArray = JSONArray()
        notes.forEach { note ->
            val obj = JSONObject().apply {
                put("id", note.id)
                put("title", note.title)
                put("content", note.content)
                put("categoryId", note.categoryId)
                put("noteType", note.noteType)
                put("tags", note.tags)
                put("isPinned", note.isPinned)
                put("isArchived", note.isArchived)
                put("wordCount", note.wordCount)
                put("readingTimeMinutes", note.readingTimeMinutes)
                put("createdAt", note.createdAt)
                put("updatedAt", note.updatedAt)
            }
            notesArray.put(obj)
        }

        val summary = JSONObject().apply {
            put("totalNotes", notes.size)
            put("activeNotes", activeCount)
            put("archivedNotes", archivedCount)
            put("totalWords", totalWords)
        }

        val root = JSONObject().apply {
            put("appName", APP_NAME)
            put("version", BACKUP_VERSION)
            put("backupDate", dateFormat.format(Date(now)))
            put("timestamp", now)
            put("notesCount", notes.size)
            put("summary", summary)
            put("notes", notesArray)
        }

        return root.toString(2)
    }

    /**
     * Safely parses a backup JSON string back into a list of [Note] entities
     * with sensible defaults for missing fields and fresh auto-generated IDs (id = 0).
     */
    fun parseNotesFromJson(jsonString: String): List<Note> {
        val trimmed = jsonString.trim()
        if (trimmed.isEmpty()) {
            throw IllegalArgumentException("Backup file is empty")
        }

        val notesArray: JSONArray = when {
            trimmed.startsWith("{") -> {
                val root = JSONObject(trimmed)
                when {
                    root.has("notes") && !root.isNull("notes") -> root.getJSONArray("notes")
                    root.has("records") && !root.isNull("records") -> root.getJSONArray("records")
                    else -> throw IllegalArgumentException("Backup file does not contain a notes array")
                }
            }
            trimmed.startsWith("[") -> JSONArray(trimmed)
            else -> throw IllegalArgumentException("Invalid JSON backup format")
        }

        val now = System.currentTimeMillis()
        val parsedNotes = mutableListOf<Note>()

        for (i in 0 until notesArray.length()) {
            val obj = notesArray.optJSONObject(i) ?: continue
            val content = obj.optString("content", "")
            val title = obj.optString("title", "").ifBlank { "Untitled Note" }
            val computedWords = if (content.isBlank()) 0 else content.trim().split("\\s+".toRegex()).size
            val wordCount = if (obj.has("wordCount")) obj.optInt("wordCount", computedWords) else computedWords
            val computedReadingTime = maxOf(1, (wordCount / 150) + if (wordCount % 150 > 0) 1 else 0)
            val readingTime = if (obj.has("readingTimeMinutes")) {
                obj.optInt("readingTimeMinutes", computedReadingTime).coerceAtLeast(1)
            } else {
                computedReadingTime
            }

            val categoryId = obj.optString("categoryId", "")
                .ifBlank { obj.optString("category", "personal") }
                .ifBlank { "personal" }

            val noteType = obj.optString("noteType", "")
                .ifBlank { obj.optString("type", "Quick Note") }
                .ifBlank { "Quick Note" }

            val createdAt = obj.optLong("createdAt", now).let { if (it > 0L) it else now }
            val updatedAt = obj.optLong("updatedAt", createdAt).let { if (it > 0L) it else createdAt }

            parsedNotes.add(
                Note(
                    id = 0, // Auto-generate ID on Room insert to avoid collisions
                    title = title,
                    content = content,
                    categoryId = categoryId,
                    noteType = noteType,
                    tags = obj.optString("tags", ""),
                    isPinned = false,
                    isArchived = obj.optBoolean("isArchived", false),
                    wordCount = wordCount,
                    readingTimeMinutes = readingTime,
                    createdAt = createdAt,
                    updatedAt = updatedAt
                )
            )
        }

        return parsedNotes
    }
}
