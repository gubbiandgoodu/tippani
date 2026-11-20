package com.example.model

import java.util.Locale

object TagUtils {

    // Matches #tag tokens (letters, digits, underscores, hyphens) at start of text
    // or preceded by whitespace / opening punctuation.
    private val HASHTAG_REGEX = Regex("""(?:^|(?<=[\s(\["']))#([\p{L}\p{N}_-]+)""")

    /**
     * Auto-detects unique tag names from `#word` hashtags in [title] and [content].
     * For example, `#imp` is extracted as `imp`.
     */
    fun extractHashtags(title: String, content: String): List<String> {
        val combined = "$title\n$content"
        val seen = LinkedHashSet<String>()
        HASHTAG_REGEX.findAll(combined).forEach { match ->
            val raw = match.groupValues[1].trim()
            if (raw.isNotEmpty()) {
                seen.add(raw.lowercase(Locale.getDefault()))
            }
        }
        return seen.toList()
    }

    /**
     * Returns comma-separated auto-detected tags for Notes format, or empty string for Checklists.
     */
    fun extractTagsString(title: String, content: String, noteType: String): String {
        if (normalizeNoteFormat(noteType) != NOTE_FORMAT_NOTES) return ""
        return extractHashtags(title, content).joinToString(", ")
    }

    /**
     * Returns all normalized tags associated with a [Note].
     * Always returns an empty list for Checklist notes.
     */
    fun getTagsForNote(note: Note): List<String> {
        if (normalizeNoteFormat(note.noteType) != NOTE_FORMAT_NOTES) return emptyList()
        val seen = LinkedHashSet<String>()
        extractHashtags(note.title, note.content).forEach { seen.add(it) }
        if (note.tags.isNotBlank()) {
            note.tags.split(",")
                .map { it.trim().removePrefix("#").lowercase(Locale.getDefault()) }
                .filter { it.isNotEmpty() }
                .forEach { seen.add(it) }
        }
        return seen.toList()
    }
}
