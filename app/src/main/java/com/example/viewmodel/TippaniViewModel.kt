package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppPreferences
import com.example.data.AppThemeMode
import com.example.data.BackupUtils
import com.example.data.LocalBackupHelper
import com.example.data.NoteRepository
import com.example.data.TippaniDatabase
import com.example.model.DEFAULT_CATEGORIES
import com.example.model.Note
import com.example.model.NotebookCategory
import com.example.model.SampleNotesGenerator
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

    // Current navigation tab: 0 = Notes, 1 = Notebooks, 2 = Tags
    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    // Filters for Notes list
    private val _selectedCategoryFilter = MutableStateFlow<String?>(null) // null = All Notebooks
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

    // Categories list
    private val _categories = MutableStateFlow(DEFAULT_CATEGORIES)
    val categories: StateFlow<List<NotebookCategory>> = _categories.asStateFlow()

    // Selected theme
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
            val matchesType = typeFilter == null || note.noteType.equals(typeFilter, ignoreCase = true)
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

    fun setThemeMode(mode: AppThemeMode) {
        _currentThemeMode.value = mode
        preferences.selectedThemeIndex = mode.ordinal
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun openNewNote() {
        _editingNote.value = Note(
            title = "",
            content = "",
            categoryId = preferences.defaultNotebook,
            noteType = "Quick Note",
            tags = ""
        )
        _isEditorOpen.value = true
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
        noteType: String,
        tags: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val words = if (content.isBlank()) 0 else content.trim().split("\\s+".toRegex()).size
            val readingTime = maxOf(1, (words / 150) + if (words % 150 > 0) 1 else 0)
            val current = _editingNote.value

            val noteToSave = if (current != null && current.id > 0) {
                current.copy(
                    title = title.ifBlank { "Untitled Note" },
                    content = content,
                    categoryId = categoryId,
                    noteType = noteType,
                    tags = tags,
                    wordCount = words,
                    readingTimeMinutes = readingTime,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                Note(
                    title = title.ifBlank { "Untitled Note" },
                    content = content,
                    categoryId = categoryId,
                    noteType = noteType,
                    tags = tags,
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
