package com.example.model

const val NOTE_FORMAT_NOTES = "Notes"
const val NOTE_FORMAT_CHECKLIST = "Checklist"

val AVAILABLE_NOTE_FORMATS = listOf(NOTE_FORMAT_NOTES, NOTE_FORMAT_CHECKLIST)

fun normalizeNoteFormat(raw: String?): String {
    return if (raw.equals(NOTE_FORMAT_CHECKLIST, ignoreCase = true)) {
        NOTE_FORMAT_CHECKLIST
    } else {
        NOTE_FORMAT_NOTES
    }
}

data class ChecklistItem(
    val id: Long,
    val text: String,
    val isChecked: Boolean = false
)

object ChecklistUtils {

    private val CHECKED_REGEX = Regex("""^(?:[-*•]\s*)?\[[xX]]\s*(.*)$""")
    private val UNCHECKED_REGEX = Regex("""^(?:[-*•]\s*)?\[\s?]\s*(.*)$""")
    private val BULLET_REGEX = Regex("""^[-*•]\s+(.*)$""")

    /**
     * Parses raw note content into a list of [ChecklistItem]s.
     * Supports `- [x]`, `- [ ]`, plain bullet `- `, and plain lines.
     */
    fun parse(content: String): List<ChecklistItem> {
        if (content.isBlank()) return emptyList()
        var nextId = 1L
        val items = mutableListOf<ChecklistItem>()

        content.lines().forEach { rawLine ->
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) return@forEach

            val checkedMatch = CHECKED_REGEX.matchEntire(trimmed)
            if (checkedMatch != null) {
                items.add(
                    ChecklistItem(
                        id = nextId++,
                        text = checkedMatch.groupValues[1].trim(),
                        isChecked = true
                    )
                )
                return@forEach
            }

            val uncheckedMatch = UNCHECKED_REGEX.matchEntire(trimmed)
            if (uncheckedMatch != null) {
                items.add(
                    ChecklistItem(
                        id = nextId++,
                        text = uncheckedMatch.groupValues[1].trim(),
                        isChecked = false
                    )
                )
                return@forEach
            }

            val bulletMatch = BULLET_REGEX.matchEntire(trimmed)
            val cleanText = bulletMatch?.groupValues?.get(1)?.trim() ?: trimmed
            items.add(
                ChecklistItem(
                    id = nextId++,
                    text = cleanText,
                    isChecked = false
                )
            )
        }

        return items
    }

    /**
     * Serializes [ChecklistItem]s into markdown-style lines with active (unchecked)
     * items first and completed (checked) items at the bottom.
     */
    fun serialize(items: List<ChecklistItem>): String {
        val nonBlank = items.filter { it.text.isNotBlank() }
        val active = nonBlank.filter { !it.isChecked }
        val completed = nonBlank.filter { it.isChecked }
        return (active + completed).joinToString("\n") { item ->
            val prefix = if (item.isChecked) "- [x] " else "- [ ] "
            prefix + item.text.trim()
        }
    }

    /**
     * Converts checklist items into readable plain text when switching from Checklist to Notes.
     */
    fun toPlainText(items: List<ChecklistItem>): String {
        val nonBlank = items.filter { it.text.isNotBlank() }
        val active = nonBlank.filter { !it.isChecked }
        val completed = nonBlank.filter { it.isChecked }
        return (active + completed).joinToString("\n") { it.text.trim() }
    }

    /**
     * Formats checklist items nicely for clipboard copy.
     */
    fun formatForCopy(items: List<ChecklistItem>): String {
        val nonBlank = items.filter { it.text.isNotBlank() }
        val active = nonBlank.filter { !it.isChecked }
        val completed = nonBlank.filter { it.isChecked }
        return (active + completed).joinToString("\n") { item ->
            val mark = if (item.isChecked) "[x]" else "[ ]"
            "$mark ${item.text.trim()}"
        }
    }
}
