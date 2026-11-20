package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.data.LocalBackupHelper
import com.example.model.Note
import com.example.ui.components.BackupPromptDialog
import com.example.ui.components.NoteViewerDialog
import com.example.ui.components.SetLockPinDialog
import com.example.ui.components.TippaniBottomNavigation
import com.example.ui.components.VerifyLockPinDialog
import com.example.ui.screens.NotebooksScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TagsScreen
import com.example.ui.theme.TippaniTheme
import com.example.viewmodel.TippaniViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: TippaniViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val appearanceMode by viewModel.currentAppearanceMode.collectAsState()
            val accentMode by viewModel.currentThemeMode.collectAsState()

            TippaniTheme(
                appearanceMode = appearanceMode,
                accentMode = accentMode
            ) {
                val isDark = TippaniTheme.colors.isDark
                SideEffect {
                    val barStyle = if (isDark) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    }
                    enableEdgeToEdge(
                        statusBarStyle = barStyle,
                        navigationBarStyle = barStyle
                    )
                }

                TippaniApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun TippaniApp(viewModel: TippaniViewModel) {
    val context = LocalContext.current
    val colors = TippaniTheme.colors
    val currentTab by viewModel.currentTab.collectAsState()
    val isEditorOpen by viewModel.isEditorOpen.collectAsState()
    val editingNote by viewModel.editingNote.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val lockedNoteIds by viewModel.lockedNoteIds.collectAsState()
    val hasLockPin by viewModel.hasLockPin.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    var isSettingsOpen by rememberSaveable { mutableStateOf(false) }

    // Pending PIN dialogs for opening protected notes or toggling lock in viewer
    var pendingUnlockOpenNote by remember { mutableStateOf<Note?>(null) }
    var pendingSetPinForNote by remember { mutableStateOf<Note?>(null) }
    var pendingVerifyUnlockNote by remember { mutableStateOf<Note?>(null) }

    // Show backup popup once per app open whenever promptBackupOnOpen is true (survives config changes)
    var showBackupDialog by rememberSaveable {
        mutableStateOf(LocalBackupHelper.isPromptBackupOnOpen(context))
    }

    val snackbarHostState = remember { SnackbarHostState() }

    fun handleNoteClick(note: Note, bookAlreadyUnlocked: Boolean = false) {
        val noteDirectlyLocked = note.id in lockedNoteIds
        val bookLocked = !bookAlreadyUnlocked && categories.any { it.id == note.categoryId && it.isLocked }
        if (noteDirectlyLocked || bookLocked) {
            pendingUnlockOpenNote = note
        } else {
            viewModel.openEditNote(note)
        }
    }

    // Automatic daily backup on app open
    LaunchedEffect(Unit) {
        viewModel.checkAndPerformAutoBackup(context)
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    // Handle back button when on Settings, Editor, or secondary tabs
    BackHandler(enabled = isSettingsOpen || isEditorOpen || currentTab != 0) {
        if (isSettingsOpen) {
            isSettingsOpen = false
        } else if (isEditorOpen) {
            viewModel.closeEditor()
        } else if (currentTab != 0) {
            viewModel.setTab(0)
        }
    }

    if (isSettingsOpen) {
        // Full Settings page opened from top-right icon on every screen
        SettingsScreen(
            viewModel = viewModel,
            onBackClick = { isSettingsOpen = false }
        )
    } else {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .testTag("tippani_main_scaffold"),
            containerColor = colors.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!isEditorOpen) {
                    TippaniBottomNavigation(
                        selectedTab = currentTab,
                        onTabSelected = { viewModel.setTab(it) },
                        modifier = Modifier.navigationBarsPadding()
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    0 -> NotesScreen(
                        viewModel = viewModel,
                        onNoteClick = { note -> handleNoteClick(note, bookAlreadyUnlocked = false) },
                        onOpenSettings = { isSettingsOpen = true },
                        onAddClick = { viewModel.openNewNote() }
                    )
                    1 -> NotebooksScreen(
                        viewModel = viewModel,
                        onNoteClick = { note -> handleNoteClick(note, bookAlreadyUnlocked = true) },
                        onOpenSettings = { isSettingsOpen = true },
                        onAddClick = { categoryId -> viewModel.openNewNote(categoryId) }
                    )
                    2 -> TagsScreen(
                        viewModel = viewModel,
                        onTagSelected = { tagName ->
                            viewModel.setSearchQuery(tagName)
                            viewModel.setTab(0)
                        },
                        onNoteClick = { note -> handleNoteClick(note, bookAlreadyUnlocked = false) },
                        onOpenSettings = { isSettingsOpen = true },
                        onAddClick = { viewModel.openNewNote() }
                    )
                }
            }
        }
    }

    // Startup Backup Dialog (once per app open when promptBackupOnOpen is enabled)
    if (showBackupDialog) {
        BackupPromptDialog(
            viewModel = viewModel,
            onDismiss = { showBackupDialog = false }
        )
    }

    // Note Viewer & Editor Dialog (Opens in read-only by default for existing notes)
    if (isEditorOpen) {
        val currentNote = editingNote
        val isCurrentNoteLocked = currentNote != null && currentNote.id in lockedNoteIds

        NoteViewerDialog(
            initialNote = currentNote,
            categories = categories,
            isLocked = isCurrentNoteLocked,
            onDismiss = { viewModel.closeEditor() },
            onSave = { title, content, categoryId, noteType ->
                viewModel.saveNote(
                    title = title,
                    content = content,
                    categoryId = categoryId,
                    noteType = noteType
                )
            },
            onQuickChecklistChange = { note, updatedContent ->
                viewModel.updateChecklistContent(note, updatedContent)
            },
            onTogglePin = { note ->
                viewModel.togglePinNote(note)
            },
            onToggleLock = { note ->
                if (note.id in lockedNoteIds) {
                    pendingVerifyUnlockNote = note
                } else if (!hasLockPin) {
                    pendingSetPinForNote = note
                } else {
                    viewModel.setNoteLocked(note.id, true)
                }
            },
            onDelete = { note ->
                viewModel.deleteNote(note)
            }
        )
    }

    // PIN Verification to open a locked note or note inside a locked book
    pendingUnlockOpenNote?.let { note ->
        VerifyLockPinDialog(
            title = "Unlock Note",
            subtitle = "Enter your PIN to open \"${note.title.ifBlank { "Untitled Note" }}\".",
            onVerify = { pin -> viewModel.verifyLockPin(pin) },
            onSuccess = {
                pendingUnlockOpenNote = null
                viewModel.openEditNote(note)
            },
            onDismiss = { pendingUnlockOpenNote = null }
        )
    }

    // PIN Setup when locking a note for the first time from NoteViewerDialog
    pendingSetPinForNote?.let { note ->
        SetLockPinDialog(
            onPinSet = { pin ->
                viewModel.setLockPin(pin)
                viewModel.setNoteLocked(note.id, true)
                pendingSetPinForNote = null
            },
            onDismiss = { pendingSetPinForNote = null }
        )
    }

    // PIN Verification before removing lock from a note in NoteViewerDialog
    pendingVerifyUnlockNote?.let { note ->
        VerifyLockPinDialog(
            title = "Remove Lock",
            subtitle = "Enter your PIN to unlock \"${note.title.ifBlank { "Untitled Note" }}\".",
            confirmLabel = "Remove Lock",
            onVerify = { pin -> viewModel.verifyLockPin(pin) },
            onSuccess = {
                viewModel.setNoteLocked(note.id, false)
                pendingVerifyUnlockNote = null
            },
            onDismiss = { pendingVerifyUnlockNote = null }
        )
    }
}
