package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
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
import com.example.ui.components.BackupPromptDialog
import com.example.ui.components.NoteViewerDialog
import com.example.ui.components.TippaniBottomNavigation
import com.example.ui.screens.NotebooksScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TagsScreen
import com.example.ui.theme.CleanBackground
import com.example.ui.theme.TippaniTheme
import com.example.viewmodel.TippaniViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: TippaniViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TippaniTheme {
                TippaniApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun TippaniApp(viewModel: TippaniViewModel) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsState()
    val isEditorOpen by viewModel.isEditorOpen.collectAsState()
    val editingNote by viewModel.editingNote.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    var isSettingsOpen by rememberSaveable { mutableStateOf(false) }

    // Show backup popup once per app open whenever promptBackupOnOpen is true (survives config changes)
    var showBackupDialog by rememberSaveable {
        mutableStateOf(LocalBackupHelper.isPromptBackupOnOpen(context))
    }

    val snackbarHostState = remember { SnackbarHostState() }

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
            containerColor = CleanBackground,
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
                        onNoteClick = { note -> viewModel.openEditNote(note) },
                        onOpenSettings = { isSettingsOpen = true },
                        onAddClick = { viewModel.openNewNote() }
                    )
                    1 -> NotebooksScreen(
                        viewModel = viewModel,
                        onNotebookSelected = { selectedCategory ->
                            viewModel.setCategoryFilter(selectedCategory.id)
                            viewModel.setTab(0) // Switch to Notes tab filtered by this notebook
                        },
                        onOpenSettings = { isSettingsOpen = true },
                        onAddClick = { viewModel.openNewNote() }
                    )
                    2 -> TagsScreen(
                        viewModel = viewModel,
                        onTagSelected = { tagName ->
                            viewModel.setSearchQuery(tagName)
                            viewModel.setTab(0)
                        },
                        onNoteClick = { note -> viewModel.openEditNote(note) },
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
        NoteViewerDialog(
            initialNote = editingNote,
            categories = categories,
            onDismiss = { viewModel.closeEditor() },
            onSave = { title, content, categoryId, noteType, tags ->
                viewModel.saveNote(
                    title = title,
                    content = content,
                    categoryId = categoryId,
                    noteType = noteType,
                    tags = tags
                )
            },
            onDelete = { note ->
                viewModel.deleteNote(note)
            }
        )
    }
}
