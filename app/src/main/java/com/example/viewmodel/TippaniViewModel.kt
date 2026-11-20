package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppAppearanceMode
import com.example.data.AppPreferences
import com.example.data.AppThemeMode
import com.example.data.BackupUtils
import com.example.data.LocalBackupHelper
import com.example.data.NoteRepository
import com.example.data.TippaniDatabase
import com.example.model.ChecklistUtils
import com.example.model.DEFAULT_CATEGORIES
import com.example.model.NOTE_FORMAT_CHECKLIST
import com.example.model.NOTE_FORMAT_NOTES
import com.example.model.Note
import com.example.model.NotebookCategory
import com.example.model.SampleNotesGenerator
import com.example.model.TagUtils
import com.example.model.normalizeNoteFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TippaniViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NoteRepository
    val preferences: AppPreferences = AppPreferences(application)

    // Reactive SAF Local Backup states backed by LocalBackupHelper
    private val _hasBackupFolder = MutableStateFlow(LocalBackupHelper.hasBackupFolder(application))
    val hasBackupFolder: StateFlow<Boolean> = _hasBackupFolder.asStateFlow()

    private val _backupFolderName = MutableStateFlow(LocalBackupHelper.getBackupFolderName(application))
    val backupFolderName: StateFlow<String?> = _backupFolderName.asStateFlow()

    private val _lastBackupTime = MutableStateFlow(LocalBackupHelper.getLastBackupTime(application))
    val lastBackupTime: StateFlow<String?> = _lastBackupTime.asStateFlow()

    private val _promptBackupOnOpen = MutableStateFlow(LocalBackupHelper.isPromptBackupOnOpen(application))
    val promptBackupOnOpen: StateFlow<Boolean> = _promptBackupOnOpen.asStateFlow()

    init {
        val db = TippaniDatabase.getDatabase(application)
        repository = NoteRepository(db.noteDao())

        // Preload sample notes if first run
        if (!preferences.hasLoadedInitialData) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.insertAll(SampleNotesGenerator.generateSamples())
                preferences.hasLoadedInitialData = true
            }
        }
    }

    val activeNotes: StateFlow<List<Note>> = repository.activeNotes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val archivedNotes: StateFlow<List<Note>> = repository.archivedNotes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Current navigation tab: 0 = Notes, 1 = Books, 2 = Tags
    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    // Filters for Notes list
    private val _selectedCategoryFilter = MutableStateFlow<String?>(null) // null = All Books
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    private val _selectedTypeFilter = MutableStateFlow<String?>(null)
    val selectedTypeFilter: StateFlow<String?> = _selectedTypeFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Statistics Time Range: "All Time", "This Month", "This Week"
    private val _statsTimeFilter = MutableStateFlow("All Time")
    val statsTimeFilter: StateFlow<String> = _statsTimeFilter.asStateFlow()

    // Status snackbar/message state
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // Persisted Books list
    private val _categories = MutableStateFlow(preferences.loadBooks())
    val categories: StateFlow<List<NotebookCategory>> = _categories.asStateFlow()

    // Persisted Locked Note IDs & Pinned Note Timestamps & Lock PIN availability
    private val _lockedNoteIds = MutableStateFlow(preferences.loadLockedNoteIds())
    val lockedNoteIds: StateFlow<Set<Long>> = _lockedNoteIds.asStateFlow()

    private val _pinnedNoteTimestamps = MutableStateFlow(preferences.loadPinnedNoteTimestamps())
    val pinnedNoteTimestamps: StateFlow<Map<Long, Long>> = _pinnedNoteTimestamps.asStateFlow()

    private val _hasLockPin = MutableStateFlow(preferences.hasLockPin())
    val hasLockPin: StateFlow<Boolean> = _hasLockPin.asStateFlow()

    // Selected appearance (Light, Dark, Black, System)
    private val _currentAppearanceMode = MutableStateFlow(
        AppAppearanceMode.entries.getOrElse(preferences.selectedAppearanceIndex) { AppAppearanceMode.LIGHT }
    )
    val currentAppearanceMode: StateFlow<AppAppearanceMode> = _currentAppearanceMode.asStateFlow()

    // Selected accent theme
    private val _currentThemeMode = MutableStateFlow(
        AppThemeMode.entries.getOrElse(preferences.selectedThemeIndex) { AppThemeMode.SLATE }
    )
    val currentThemeMode: StateFlow<AppThemeMode> = _currentThemeMode.asStateFlow()

    // Note editing / creation state
    private val _editingNote = MutableStateFlow<Note?>(null)
    val editingNote: StateFlow<Note?> = _editingNote.asStateFlow()

    private val _isEditorOpen = MutableStateFlow(false)
    val isEditorOpen: StateFlow<Boolean> = _isEditorOpen.asStateFlow()

    // Combined filtered notes
    val filteredNotes: StateFlow<List<Note>> = combine(
        activeNotes,
        _selectedCategoryFilter,
        _selectedTypeFilter,
        _searchQuery
    ) { notes, catFilter, typeFilter, query ->
        notes.filter { note ->
            val matchesCategory = catFilter == null || note.categoryId == catFilter
            val matchesType = typeFilter == null || normalizeNoteFormat(note.noteType).equals(typeFilter, ignoreCase = true)
            val matchesQuery = query.isBlank() ||
                    note.title.contains(query, ignoreCase = true) ||
                    note.content.contains(query, ignoreCase = true) ||
                    note.tags.contains(query, ignoreCase = true)
            matchesCategory && matchesType && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setTab(index: Int) {
        _currentTab.value = index
    }

    fun setCategoryFilter(categoryId: String?) {
        _selectedCategoryFilter.value = categoryId
    }

    fun setTypeFilter(type: String?) {
        _selectedTypeFilter.value = type
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatsTimeFilter(filter: String) {
        _statsTimeFilter.value = filter
    }

    fun setAppearanceMode(mode: AppAppearanceMode) {
        _currentAppearanceMode.value = mode
        preferences.selectedAppearanceIndex = mode.ordinal
    }

    fun setThemeMode(mode: AppThemeMode) {
        _currentThemeMode.value = mode
        preferences.selectedThemeIndex = mode.ordinal
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun openNewNote(categoryId: String? = null) {
        val currentBooks = _categories.value
        val resolvedBookId = categoryId
            ?: currentBooks.find { it.id == preferences.defaultNotebook }?.id
            ?: currentBooks.firstOrNull()?.id
            ?: "personal"
        _editingNote.value = Note(
            title = "",
            content = "",
            categoryId = resolvedBookId,
            noteType = NOTE_FORMAT_NOTES,
            tags = ""
        )
        _isEditorOpen.value = true
    }

    fun createBook(name: String, colorHex: Long = 0xFF6366F1L, iconName: String = "menu_book") {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val current = _categories.value
        val baseSlug = trimmed.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifEmpty { "book" }
        val uniqueId = if (current.none { it.id == baseSlug }) {
            baseSlug
        } else {
            "${baseSlug}_${System.currentTimeMillis()}"
        }
        val newBook = NotebookCategory(
            id = uniqueId,
            name = trimmed,
            iconName = iconName,
            colorHex = colorHex
        )
        val updated = current + newBook
        _categories.value = updated
        preferences.saveBooks(updated)
        _statusMessage.value = "Created book \"$trimmed\""
    }

    fun renameBook(bookId: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        val current = _categories.value
        if (current.none { it.id == bookId }) return
        val updated = current.map { book ->
            if (book.id == bookId) book.copy(name = trimmed) else book
        }
        _categories.value = updated
        preferences.saveBooks(updated)
        _statusMessage.value = "Renamed book to \"$trimmed\""
    }

    fun deleteBook(bookId: String, deleteContainedNotes: Boolean = true) {
        deleteBooks(setOf(bookId), deleteContainedNotes)
    }

    fun deleteBooks(bookIds: Set<String>, deleteContainedNotes: Boolean = true) {
        if (bookIds.isEmpty()) return
        val current = _categories.value
        val removed = current.filter { it.id in bookIds }
        if (removed.isEmpty()) return

        val remaining = current.filterNot { it.id in bookIds }
        _categories.value = remaining
        preferences.saveBooks(remaining)

        if (_selectedCategoryFilter.value in bookIds) {
            _selectedCategoryFilter.value = null
        }
        if (preferences.defaultNotebook in bookIds) {
            preferences.defaultNotebook = remaining.firstOrNull()?.id ?: "personal"
        }

        if (deleteContainedNotes) {
            viewModelScope.launch(Dispatchers.IO) {
                val notesToDelete = repository.getAllDirect().filter { it.categoryId in bookIds }
                notesToDelete.forEach { repository.deleteById(it.id) }
            }
        }

        _statusMessage.value = if (removed.size == 1) {
            "Deleted book \"${removed.first().name}\""
        } else {
            "Deleted ${removed.size} books"
        }
    }

    fun togglePinBook(bookId: String) {
        val current = _categories.value
        val target = current.find { it.id == bookId } ?: return
        val newPinned = !target.isPinned
        val now = System.currentTimeMillis()
        val updated = current.map { book ->
            if (book.id == bookId) {
                book.copy(isPinned = newPinned, pinnedAt = if (newPinned) now else 0L)
            } else {
                book
            }
        }
        _categories.value = updated
        preferences.saveBooks(updated)
        _statusMessage.value = if (newPinned) {
            "Pinned \"${target.name}\" to top"
        } else {
            "Unpinned \"${target.name}\""
        }
    }

    fun setBookLocked(bookId: String, locked: Boolean) {
        val current = _categories.value
        val target = current.find { it.id == bookId } ?: return
        val updated = current.map { book ->
            if (book.id == bookId) book.copy(isLocked = locked) else book
        }
        _categories.value = updated
        preferences.saveBooks(updated)
        _statusMessage.value = if (locked) {
            "Locked book \"${target.name}\""
        } else {
            "Unlocked book \"${target.name}\""
        }
    }

    fun togglePinNote(note: Note) {
        val newPinned = !note.isPinned
        val now = System.currentTimeMillis()
        viewModelScope.launch(Dispatchers.IO) {
            val updatedNote = note.copy(isPinned = newPinned)
            repository.update(updatedNote)
            withContext(Dispatchers.Main) {
                val updatedMap = _pinnedNoteTimestamps.value.toMutableMap()
                if (newPinned) {
                    updatedMap[note.id] = now
                } else {
                    updatedMap.remove(note.id)
                }
                _pinnedNoteTimestamps.value = updatedMap
                preferences.savePinnedNoteTimestamps(updatedMap)
                if (_editingNote.value?.id == note.id) {
                    _editingNote.value = updatedNote
                }
                _statusMessage.value = if (newPinned) "Pinned note to top" else "Unpinned note"
            }
        }
    }

    fun setNotesPinned(noteIds: Set<Long>, pinned: Boolean) {
        if (noteIds.isEmpty()) return
        val now = System.currentTimeMillis()
        viewModelScope.launch(Dispatchers.IO) {
            val targets = repository.getAllDirect().filter { it.id in noteIds }
            targets.forEach { note ->
                if (note.isPinned != pinned) {
                    repository.update(note.copy(isPinned = pinned))
                }
            }
            withContext(Dispatchers.Main) {
                val updatedMap = _pinnedNoteTimestamps.value.toMutableMap()
                targets.forEachIndexed { idx, note ->
                    if (pinned) {
                        updatedMap[note.id] = now + idx
                    } else {
                        updatedMap.remove(note.id)
                    }
                }
                _pinnedNoteTimestamps.value = updatedMap
                preferences.savePinnedNoteTimestamps(updatedMap)
                _statusMessage.value = if (pinned) {
                    if (targets.size == 1) "Pinned 1 note to top" else "Pinned ${targets.size} notes to top"
                } else {
                    if (targets.size == 1) "Unpinned 1 note" else "Unpinned ${targets.size} notes"
                }
            }
        }
    }

    fun setNoteLocked(noteId: Long, locked: Boolean) {
        setNotesLocked(setOf(noteId), locked)
    }

    fun setNotesLocked(noteIds: Set<Long>, locked: Boolean) {
        if (noteIds.isEmpty()) return
        val updated = if (locked) {
            _lockedNoteIds.value + noteIds
        } else {
            _lockedNoteIds.value - noteIds
        }
        _lockedNoteIds.value = updated
        preferences.saveLockedNoteIds(updated)
        _statusMessage.value = if (locked) {
            if (noteIds.size == 1) "Note locked" else "Locked ${noteIds.size} notes"
        } else {
            if (noteIds.size == 1) "Note unlocked" else "Unlocked ${noteIds.size} notes"
        }
    }

    fun isNoteProtected(note: Note): Boolean {
        if (note.id in _lockedNoteIds.value) return true
        return _categories.value.any { it.id == note.categoryId && it.isLocked }
    }

    fun setLockPin(pin: String) {
        preferences.setLockPin(pin)
        _hasLockPin.value = true
    }

    fun verifyLockPin(pin: String): Boolean {
        return preferences.verifyLockPin(pin)
    }

    fun openEditNote(note: Note) {
        _editingNote.value = note
        _isEditorOpen.value = true
    }

    fun closeEditor() {
        _editingNote.value = null
        _isEditorOpen.value = false
    }

    fun saveNote(
        title: String,
        content: String,
        categoryId: String,
        noteType: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val words = if (content.isBlank()) 0 else content.trim().split("\\s+".toRegex()).size
            val readingTime = maxOf(1, (words / 150) + if (words % 150 > 0) 1 else 0)
            val current = _editingNote.value

            val normalizedType = normalizeNoteFormat(noteType)
            val resolvedTitle = title.ifBlank { "Untitled Note" }
            val autoDetectedTags = TagUtils.extractTagsString(resolvedTitle, content, normalizedType)

            val noteToSave = if (current != null && current.id > 0) {
                current.copy(
                    title = resolvedTitle,
                    content = content,
                    categoryId = categoryId,
                    noteType = normalizedType,
                    tags = autoDetectedTags,
                    wordCount = words,
                    readingTimeMinutes = readingTime,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                Note(
                    title = resolvedTitle,
                    content = content,
                    categoryId = categoryId,
                    noteType = normalizedType,
                    tags = autoDetectedTags,
                    wordCount = words,
                    readingTimeMinutes = readingTime,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            }

            if (noteToSave.id > 0) {
                repository.update(noteToSave)
            } else {
                repository.insert(noteToSave)
            }

            withContext(Dispatchers.Main) {
                closeEditor()
                _statusMessage.value = "Note saved"
            }
        }
    }

    /**
     * Persists checklist item check/uncheck changes directly from read-only view
     * without closing the open note viewer.
     */
    fun updateChecklistContent(note: Note, updatedContent: String) {
        if (note.id <= 0L) return
        viewModelScope.launch(Dispatchers.IO) {
            val words = if (updatedContent.isBlank()) 0 else updatedContent.trim().split("\\s+".toRegex()).size
            val readingTime = maxOf(1, (words / 150) + if (words % 150 > 0) 1 else 0)
            val updatedNote = note.copy(
                content = updatedContent,
                wordCount = words,
                readingTimeMinutes = readingTime,
                updatedAt = System.currentTimeMillis()
            )
            repository.update(updatedNote)
            withContext(Dispatchers.Main) {
                if (_editingNote.value?.id == note.id) {
                    _editingNote.value = updatedNote
                }
            }
        }
    }

    fun toggleArchive(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.update(note.copy(isArchived = !note.isArchived))
            withContext(Dispatchers.Main) {
                _statusMessage.value = if (!note.isArchived) "Note archived" else "Note unarchived"
            }
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(note)
            withContext(Dispatchers.Main) {
                _statusMessage.value = "Note deleted"
            }
        }
    }

    fun deleteNotes(noteIds: Set<Long>) {
        if (noteIds.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            noteIds.forEach { id ->
                repository.deleteById(id)
            }
            val count = noteIds.size
            withContext(Dispatchers.Main) {
                _statusMessage.value = if (count == 1) "Deleted 1 note" else "Deleted $count notes"
            }
        }
    }

    fun moveNotesToNotebook(noteIds: Set<Long>, targetCategoryId: String, targetCategoryName: String) {
        if (noteIds.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val allNotes = repository.getAllDirect()
            val targets = allNotes.filter { it.id in noteIds }
            targets.forEach { note ->
                repository.update(
                    note.copy(
                        categoryId = targetCategoryId,
                        updatedAt = now
                    )
                )
            }
            val count = targets.size
            withContext(Dispatchers.Main) {
                _statusMessage.value = if (count == 1) {
                    "Moved 1 note to $targetCategoryName"
                } else {
                    "Moved $count notes to $targetCategoryName"
                }
            }
        }
    }

    fun addTagToNotes(noteIds: Set<Long>, rawTag: String) {
        val cleanTag = rawTag.trim().removePrefix("#").replace(Regex("\\s+"), "")
        if (noteIds.isEmpty() || cleanTag.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val allNotes = repository.getAllDirect()
            val noteEntries = allNotes.filter {
                it.id in noteIds && normalizeNoteFormat(it.noteType) == NOTE_FORMAT_NOTES
            }

            noteEntries.forEach { note ->
                val existingTags = TagUtils.getTagsForNote(note)
                val alreadyHasTag = existingTags.any { it.equals(cleanTag, ignoreCase = true) }
                val updatedContent = if (alreadyHasTag) {
                    note.content
                } else if (note.content.isBlank()) {
                    "#$cleanTag"
                } else {
                    "${note.content.trimEnd()}\n#$cleanTag"
                }
                val updatedTags = TagUtils.extractTagsString(note.title, updatedContent, NOTE_FORMAT_NOTES)
                val words = if (updatedContent.isBlank()) 0 else updatedContent.trim().split("\\s+".toRegex()).size
                val readingTime = maxOf(1, (words / 150) + if (words % 150 > 0) 1 else 0)
                repository.update(
                    note.copy(
                        content = updatedContent,
                        tags = updatedTags,
                        wordCount = words,
                        readingTimeMinutes = readingTime,
                        updatedAt = now
                    )
                )
            }

            val updatedCount = noteEntries.size
            withContext(Dispatchers.Main) {
                _statusMessage.value = when (updatedCount) {
                    0 -> "Tags only apply to Notes (checklists skipped)"
                    1 -> "Tagged 1 note with #$cleanTag"
                    else -> "Tagged $updatedCount notes with #$cleanTag"
                }
            }
        }
    }

    fun convertNotesFormat(noteIds: Set<Long>, targetFormat: String) {
        if (noteIds.isEmpty()) return
        val normalizedTarget = normalizeNoteFormat(targetFormat)
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val allNotes = repository.getAllDirect()
            val targets = allNotes.filter { it.id in noteIds }

            targets.forEach { note ->
                val currentFormat = normalizeNoteFormat(note.noteType)
                if (currentFormat != normalizedTarget) {
                    val newContent = if (normalizedTarget == NOTE_FORMAT_CHECKLIST) {
                        ChecklistUtils.serialize(ChecklistUtils.parse(note.content))
                    } else {
                        ChecklistUtils.toPlainText(ChecklistUtils.parse(note.content))
                    }
                    val newTags = TagUtils.extractTagsString(note.title, newContent, normalizedTarget)
                    val words = if (newContent.isBlank()) 0 else newContent.trim().split("\\s+".toRegex()).size
                    val readingTime = maxOf(1, (words / 150) + if (words % 150 > 0) 1 else 0)
                    repository.update(
                        note.copy(
                            noteType = normalizedTarget,
                            content = newContent,
                            tags = newTags,
                            wordCount = words,
                            readingTimeMinutes = readingTime,
                            updatedAt = now
                        )
                    )
                }
            }

            val count = targets.size
            withContext(Dispatchers.Main) {
                _statusMessage.value = if (count == 1) {
                    "Updated 1 item to $normalizedTarget"
                } else {
                    "Updated $count items to $normalizedTarget"
                }
            }
        }
    }

    fun moveAllNotesFromNotebooks(
        sourceCategoryIds: Set<String>,
        targetCategoryId: String,
        targetCategoryName: String
    ) {
        if (sourceCategoryIds.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val targets = repository.getAllDirect().filter {
                it.categoryId in sourceCategoryIds && it.categoryId != targetCategoryId
            }
            targets.forEach { note ->
                repository.update(note.copy(categoryId = targetCategoryId, updatedAt = now))
            }
            val count = targets.size
            withContext(Dispatchers.Main) {
                _statusMessage.value = if (count == 0) {
                    "No notes to move"
                } else if (count == 1) {
                    "Moved 1 note to $targetCategoryName"
                } else {
                    "Moved $count notes to $targetCategoryName"
                }
            }
        }
    }

    fun deleteAllNotesInNotebooks(categoryIds: Set<String>) {
        if (categoryIds.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val targets = repository.getAllDirect().filter { it.categoryId in categoryIds }
            targets.forEach { note ->
                repository.deleteById(note.id)
            }
            val count = targets.size
            withContext(Dispatchers.Main) {
                _statusMessage.value = if (count == 0) {
                    "Selected books are already empty"
                } else if (count == 1) {
                    "Deleted 1 note"
                } else {
                    "Deleted $count notes"
                }
            }
        }
    }

    fun removeTagsFromAllNotes(tagNames: Set<String>) {
        if (tagNames.isEmpty()) return
        val lowerTargets = tagNames.map { it.lowercase() }.toSet()
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val noteEntries = repository.getAllDirect().filter {
                normalizeNoteFormat(it.noteType) == NOTE_FORMAT_NOTES
            }
            var affected = 0
            noteEntries.forEach { note ->
                val currentTags = TagUtils.getTagsForNote(note)
                if (currentTags.any { it.lowercase() in lowerTargets }) {
                    var updatedTitle = note.title
                    var updatedContent = note.content
                    lowerTargets.forEach { tag ->
                        val regex = Regex("""(?i)(^|[\s(\["'])#${Regex.escape(tag)}(?![\p{L}\p{N}_-])""")
                        updatedTitle = updatedTitle.replace(regex, "$1").replace(Regex(" {2,}"), " ").trim()
                        updatedContent = updatedContent.replace(regex, "$1").replace(Regex("[ \\t]{2,}"), " ").trim()
                    }
                    val recomputedTags = TagUtils.extractTagsString(updatedTitle, updatedContent, NOTE_FORMAT_NOTES)
                    repository.update(
                        note.copy(
                            title = updatedTitle.ifBlank { "Untitled Note" },
                            content = updatedContent,
                            tags = recomputedTags,
                            updatedAt = now
                        )
                    )
                    affected++
                }
            }
            withContext(Dispatchers.Main) {
                _statusMessage.value = if (affected == 1) {
                    "Removed tags from 1 note"
                } else {
                    "Removed tags from $affected notes"
                }
            }
        }
    }

    fun renameOrMergeTags(sourceTags: Set<String>, rawTargetTag: String) {
        val cleanTarget = rawTargetTag.trim().removePrefix("#").replace(Regex("\\s+"), "")
        if (sourceTags.isEmpty() || cleanTarget.isEmpty()) return
        val lowerTargets = sourceTags.map { it.lowercase() }.toSet()

        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val noteEntries = repository.getAllDirect().filter {
                normalizeNoteFormat(it.noteType) == NOTE_FORMAT_NOTES
            }
            var affected = 0
            noteEntries.forEach { note ->
                val currentTags = TagUtils.getTagsForNote(note)
                if (currentTags.any { it.lowercase() in lowerTargets }) {
                    var updatedTitle = note.title
                    var updatedContent = note.content
                    var replacedInText = false
                    lowerTargets.forEach { tag ->
                        val regex = Regex("""(?i)(^|[\s(\["'])#${Regex.escape(tag)}(?![\p{L}\p{N}_-])""")
                        if (regex.containsMatchIn(updatedTitle)) {
                            updatedTitle = updatedTitle.replace(regex, "$1#$cleanTarget")
                            replacedInText = true
                        }
                        if (regex.containsMatchIn(updatedContent)) {
                            updatedContent = updatedContent.replace(regex, "$1#$cleanTarget")
                            replacedInText = true
                        }
                    }
                    if (!replacedInText) {
                        updatedContent = if (updatedContent.isBlank()) "#$cleanTarget" else "${updatedContent.trimEnd()}\n#$cleanTarget"
                    }
                    val recomputedTags = TagUtils.extractTagsString(updatedTitle, updatedContent, NOTE_FORMAT_NOTES)
                    repository.update(
                        note.copy(
                            title = updatedTitle,
                            content = updatedContent,
                            tags = recomputedTags,
                            updatedAt = now
                        )
                    )
                    affected++
                }
            }
            withContext(Dispatchers.Main) {
                _statusMessage.value = if (affected == 1) {
                    "Updated tag to #$cleanTarget on 1 note"
                } else {
                    "Updated tag to #$cleanTarget on $affected notes"
                }
            }
        }
    }

    fun loadSampleData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertAll(SampleNotesGenerator.generateSamples())
            withContext(Dispatchers.Main) {
                _statusMessage.value = "Sample notes loaded successfully"
            }
        }
    }

    fun clearAllNotes() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAll()
            withContext(Dispatchers.Main) {
                _statusMessage.value = "All notes removed"
            }
        }
    }

    // ==========================================
    // SAF LOCAL BACKUP & RESTORE
    // ==========================================

    private fun refreshBackupState(context: Context) {
        _hasBackupFolder.value = LocalBackupHelper.hasBackupFolder(context)
        _backupFolderName.value = LocalBackupHelper.getBackupFolderName(context)
        _lastBackupTime.value = LocalBackupHelper.getLastBackupTime(context)
        _promptBackupOnOpen.value = LocalBackupHelper.isPromptBackupOnOpen(context)
    }

    fun setPromptBackupOnOpen(enabled: Boolean) {
        val appContext = getApplication<Application>()
        LocalBackupHelper.setPromptBackupOnOpen(appContext, enabled)
        _promptBackupOnOpen.value = enabled
    }

    /**
     * Persists the SAF folder URI permission, stores its display name,
     * and immediately performs an initial date-tagged backup into that folder.
     */
    fun onBackupFolderSelected(
        context: Context,
        treeUri: Uri,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val appContext = context.applicationContext
        val folderName = LocalBackupHelper.saveBackupFolder(appContext, treeUri)
        refreshBackupState(appContext)

        performLocalBackup(appContext) { success, detail ->
            if (success) {
                onComplete(true, "Folder \"$folderName\" set. Saved $detail")
            } else {
                onComplete(false, "Folder saved, but initial backup failed: $detail")
            }
        }
    }

    /**
     * Writes a new, non-overwriting date-tagged JSON backup file into the
     * configured SAF folder on Dispatchers.IO.
     */
    fun performLocalBackup(
        context: Context,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val appContext = context.applicationContext
        viewModelScope.launch(Dispatchers.IO) {
            val result = try {
                val allNotes = repository.getAllDirect()
                val jsonContent = BackupUtils.serializeNotesToJson(allNotes)
                LocalBackupHelper.writeBackupToConfiguredFolder(appContext, jsonContent)
            } catch (e: Exception) {
                Result.failure(e)
            }

            withContext(Dispatchers.Main) {
                refreshBackupState(appContext)
                result.fold(
                    onSuccess = { fileName ->
                        onComplete(true, fileName)
                    },
                    onFailure = { error ->
                        onComplete(false, error.localizedMessage ?: "Backup failed")
                    }
                )
            }
        }
    }

    /**
     * Automatically runs a background local backup once per calendar day when
     * a backup folder is configured and today's backup hasn't run yet.
     */
    fun checkAndPerformAutoBackup(context: Context) {
        val appContext = context.applicationContext
        if (LocalBackupHelper.shouldRunDailyAutoBackup(appContext)) {
            performLocalBackup(appContext)
        }
    }

    /**
     * Exports all notes as pretty-printed JSON to a user-chosen document URI
     * via SAF CreateDocument.
     */
    fun exportBackupToUri(
        context: Context,
        uri: Uri,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val appContext = context.applicationContext
        viewModelScope.launch(Dispatchers.IO) {
            val result = try {
                val allNotes = repository.getAllDirect()
                val jsonContent = BackupUtils.serializeNotesToJson(allNotes)
                LocalBackupHelper.writeJsonToUri(appContext, uri, jsonContent).map { allNotes.size }
            } catch (e: Exception) {
                Result.failure(e)
            }

            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { count ->
                        onComplete(true, "Exported $count notes to backup JSON file")
                    },
                    onFailure = { error ->
                        onComplete(false, error.localizedMessage ?: "Export failed")
                    }
                )
            }
        }
    }

    /**
     * Reads and parses a user-selected JSON backup file via SAF OpenDocument
     * and inserts the restored notes into the Room database.
     */
    fun restoreBackupFromUri(
        context: Context,
        uri: Uri,
        onComplete: (Boolean, Int, String) -> Unit
    ) {
        val appContext = context.applicationContext
        viewModelScope.launch(Dispatchers.IO) {
            val result = try {
                val jsonText = LocalBackupHelper.readTextFromUri(appContext, uri).getOrThrow()
                val restoredNotes = BackupUtils.parseNotesFromJson(jsonText)
                if (restoredNotes.isNotEmpty()) {
                    repository.insertAll(restoredNotes)
                }
                Result.success(restoredNotes.size)
            } catch (e: Exception) {
                Result.failure(e)
            }

            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { count ->
                        onComplete(true, count, "Restored $count notes successfully")
                    },
                    onFailure = { error ->
                        onComplete(false, 0, error.localizedMessage ?: "Invalid backup file")
                    }
                )
            }
        }
    }
}
