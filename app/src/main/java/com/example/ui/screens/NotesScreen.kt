package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AVAILABLE_NOTE_FORMATS
import com.example.model.FALLBACK_BOOK
import com.example.model.NOTE_FORMAT_CHECKLIST
import com.example.model.NOTE_FORMAT_NOTES
import com.example.model.Note
import com.example.model.normalizeNoteFormat
import com.example.ui.components.BulkActionButton
import com.example.ui.components.NoteItemCard
import com.example.ui.components.NotesSortOption
import com.example.ui.components.SelectionTopBar
import com.example.ui.components.SetLockPinDialog
import com.example.ui.components.SortMenuOption
import com.example.ui.components.TippaniTopHeader
import com.example.ui.components.VerifyLockPinDialog
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SoftIconBgAmber
import com.example.ui.theme.SoftIconBgBlue
import com.example.ui.theme.SoftIconBgGreen
import com.example.ui.theme.SoftIconBgPurple
import com.example.ui.theme.SoftIconBgSlate
import com.example.ui.theme.TippaniTheme
import com.example.viewmodel.TippaniViewModel

@Composable
fun NotesScreen(
    viewModel: TippaniViewModel,
    onNoteClick: (Note) -> Unit,
    onOpenSettings: () -> Unit,
    onAddClick: () -> Unit
) {
    val colors = TippaniTheme.colors
    val filteredNotes by viewModel.filteredNotes.collectAsState()
    val allActiveNotes by viewModel.activeNotes.collectAsState()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsState()
    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val lockedNoteIds by viewModel.lockedNoteIds.collectAsState()
    val pinnedNoteTimestamps by viewModel.pinnedNoteTimestamps.collectAsState()
    val hasLockPin by viewModel.hasLockPin.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }
    var showTypeFilterMenu by remember { mutableStateOf(false) }
    var showCategoryFilterMenu by remember { mutableStateOf(false) }
    var sortOptionName by rememberSaveable { mutableStateOf(NotesSortOption.NEWEST.name) }
    val selectedSort = NotesSortOption.entries.find { it.name == sortOptionName } ?: NotesSortOption.NEWEST

    // Multi-selection state for organizing notes (pin, lock, move book, add tag, format, delete)
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedNoteIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    var showMoveNotebookDialog by remember { mutableStateOf(false) }
    var showAddTagDialog by remember { mutableStateOf(false) }
    var tagInputText by remember { mutableStateOf("") }
    var showChangeFormatDialog by remember { mutableStateOf(false) }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }
    var showSetPinForBulkLock by remember { mutableStateOf(false) }
    var showVerifyPinForBulkUnlock by remember { mutableStateOf(false) }
    var showVerifyPinForBulkDelete by remember { mutableStateOf(false) }

    // Keep selectedNoteIds pruned if notes are deleted externally
    LaunchedEffect(allActiveNotes) {
        val activeIds = allActiveNotes.map { it.id }.toSet()
        val pruned = selectedNoteIds.intersect(activeIds)
        if (pruned != selectedNoteIds) {
            selectedNoteIds = pruned
            if (pruned.isEmpty()) isSelectionMode = false
        }
    }

    fun exitSelectionMode() {
        isSelectionMode = false
        selectedNoteIds = emptySet()
    }

    fun toggleNoteSelection(noteId: Long) {
        val updated = if (noteId in selectedNoteIds) {
            selectedNoteIds - noteId
        } else {
            selectedNoteIds + noteId
        }
        selectedNoteIds = updated
        if (updated.isEmpty()) {
            isSelectionMode = false
        }
    }

    BackHandler(enabled = isSelectionMode) {
        exitSelectionMode()
    }

    val formatTypes = listOf("All Formats", NOTE_FORMAT_NOTES, NOTE_FORMAT_CHECKLIST)
    val noteSortMenuOptions = remember {
        NotesSortOption.entries.map { SortMenuOption(it.name, it.label) }
    }

    // Sorting ignores pinned notes: pinned notes always stay on top in stable pinned order
    val sortedNotes = remember(filteredNotes, selectedSort, pinnedNoteTimestamps) {
        val pinned = filteredNotes
            .filter { it.isPinned }
            .sortedByDescending { pinnedNoteTimestamps[it.id] ?: it.updatedAt }
        val unpinned = filteredNotes.filter { !it.isPinned }
        val sortedUnpinned = when (selectedSort) {
            NotesSortOption.NEWEST -> unpinned.sortedByDescending { it.createdAt }
            NotesSortOption.OLDEST -> unpinned.sortedBy { it.createdAt }
            NotesSortOption.A_Z -> unpinned.sortedBy { it.title.lowercase() }
        }
        pinned + sortedUnpinned
    }

    val selectedNotes = remember(allActiveNotes, selectedNoteIds) {
        allActiveNotes.filter { it.id in selectedNoteIds }
    }
    val hasSelectedPlainNotes = remember(selectedNotes) {
        selectedNotes.any { normalizeNoteFormat(it.noteType) == NOTE_FORMAT_NOTES }
    }
    val allSelectedPinned = remember(selectedNotes) {
        selectedNotes.isNotEmpty() && selectedNotes.all { it.isPinned }
    }
    val allSelectedLocked = remember(selectedNotes, lockedNoteIds) {
        selectedNotes.isNotEmpty() && selectedNotes.all { it.id in lockedNoteIds }
    }
    val anySelectedProtected = remember(selectedNotes, lockedNoteIds, categories) {
        selectedNotes.any { note ->
            note.id in lockedNoteIds || categories.any { it.id == note.categoryId && it.isLocked }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(18.dp))

            if (isSelectionMode) {
                val allVisibleIds = sortedNotes.map { it.id }.toSet()
                val allSelected = allVisibleIds.isNotEmpty() && selectedNoteIds.containsAll(allVisibleIds)

                SelectionTopBar(
                    selectedCount = selectedNoteIds.size,
                    allSelected = allSelected,
                    onClose = { exitSelectionMode() },
                    onToggleSelectAll = {
                        selectedNoteIds = if (allSelected) emptySet() else allVisibleIds
                        if (selectedNoteIds.isEmpty()) isSelectionMode = false
                    },
                    testTagPrefix = "notes"
                )
            } else {
                // Standard Screen Header: [Search] [Select] [Sort] [Settings]
                TippaniTopHeader(
                    title = "Notes",
                    subtitle = null,
                    isSearchActive = isSearchActive,
                    onToggleSearch = { isSearchActive = !isSearchActive },
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onOpenSettings = onOpenSettings,
                    isSelectionMode = isSelectionMode,
                    onToggleSelect = {
                        if (sortedNotes.isNotEmpty()) {
                            isSelectionMode = !isSelectionMode
                        }
                    },
                    sortOptions = noteSortMenuOptions,
                    selectedSortId = selectedSort.name,
                    onSortSelected = { sortOptionName = it }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Filter Chips Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Filter 1: Format Selector
                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.surface,
                        border = colors.cardBorder,
                        shadowElevation = if (colors.isDark) 0.dp else 0.5.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTypeFilterMenu = true }
                            .testTag("filter_modes_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = null,
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = selectedTypeFilter ?: "All Formats",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary,
                                    maxLines = 1
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showTypeFilterMenu,
                        onDismissRequest = { showTypeFilterMenu = false },
                        containerColor = colors.surface
                    ) {
                        formatTypes.forEach { typeName ->
                            DropdownMenuItem(
                                text = { Text(typeName, color = colors.textPrimary) },
                                onClick = {
                                    viewModel.setTypeFilter(if (typeName == "All Formats") null else typeName)
                                    showTypeFilterMenu = false
                                }
                            )
                        }
                    }
                }

                // Filter 2: Books Selector
                Box(modifier = Modifier.weight(1f)) {
                    val currentCatName = categories.find { it.id == selectedCategoryFilter }?.name ?: "All Books"
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.surface,
                        border = colors.cardBorder,
                        shadowElevation = if (colors.isDark) 0.dp else 0.5.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCategoryFilterMenu = true }
                            .testTag("filter_categories_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = currentCatName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary,
                                    maxLines = 1
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showCategoryFilterMenu,
                        onDismissRequest = { showCategoryFilterMenu = false },
                        containerColor = colors.surface
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Books", color = colors.textPrimary) },
                            onClick = {
                                viewModel.setCategoryFilter(null)
                                showCategoryFilterMenu = false
                            }
                        )
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name, color = colors.textPrimary) },
                                onClick = {
                                    viewModel.setCategoryFilter(cat.id)
                                    showCategoryFilterMenu = false
                                }
                            )
                        }
                    }
                }

            }

            Spacer(modifier = Modifier.height(16.dp))

            if (sortedNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No notes found",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap + to write a note or adjust filters",
                            fontSize = 13.sp,
                            color = colors.textMuted
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .testTag("notes_list_view"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 104.dp)
                ) {
                    items(sortedNotes, key = { it.id }) { note ->
                        val cat = categories.find { it.id == note.categoryId } ?: categories.firstOrNull() ?: FALLBACK_BOOK
                        val icon = getCategoryIcon(cat.iconName)
                        val tint = Color(cat.colorHex)
                        val bg = getSoftBgForColor(cat.colorHex)
                        val isSelected = note.id in selectedNoteIds
                        val isLocked = note.id in lockedNoteIds || cat.isLocked

                        NoteItemCard(
                            note = note,
                            categoryName = cat.name,
                            categoryIcon = icon,
                            iconTint = tint,
                            iconBgColor = bg,
                            isSelectionMode = isSelectionMode,
                            isSelected = isSelected,
                            isLocked = isLocked,
                            onClick = {
                                if (isSelectionMode) {
                                    toggleNoteSelection(note.id)
                                } else {
                                    onNoteClick(note)
                                }
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    isSelectionMode = true
                                    selectedNoteIds = setOf(note.id)
                                } else {
                                    toggleNoteSelection(note.id)
                                }
                            }
                        )
                    }
                }
            }
        }

        if (isSelectionMode && selectedNoteIds.isNotEmpty()) {
            // Floating Bulk Action Bar for Selected Notes
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = colors.surface,
                border = colors.cardBorder,
                shadowElevation = if (colors.isDark) 4.dp else 10.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 18.dp)
                    .fillMaxWidth()
                    .testTag("notes_bulk_action_bar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BulkActionButton(
                        icon = Icons.Default.PushPin,
                        label = if (allSelectedPinned) "Unpin" else "Pin",
                        tint = if (allSelectedPinned) colors.accent else colors.textPrimary,
                        onClick = {
                            viewModel.setNotesPinned(selectedNoteIds, !allSelectedPinned)
                            exitSelectionMode()
                        },
                        testTag = "bulk_pin_notes_btn"
                    )

                    BulkActionButton(
                        icon = if (allSelectedLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                        label = if (allSelectedLocked) "Unlock" else "Lock",
                        tint = if (allSelectedLocked) colors.accent else colors.textPrimary,
                        onClick = {
                            if (allSelectedLocked) {
                                showVerifyPinForBulkUnlock = true
                            } else if (!hasLockPin) {
                                showSetPinForBulkLock = true
                            } else {
                                viewModel.setNotesLocked(selectedNoteIds, true)
                                exitSelectionMode()
                            }
                        },
                        testTag = "bulk_lock_notes_btn"
                    )

                    BulkActionButton(
                        icon = Icons.AutoMirrored.Filled.DriveFileMove,
                        label = "Book",
                        tint = colors.textPrimary,
                        onClick = { showMoveNotebookDialog = true },
                        testTag = "bulk_move_notebook_btn"
                    )

                    if (hasSelectedPlainNotes) {
                        BulkActionButton(
                            icon = Icons.Default.Tag,
                            label = "Add Tag",
                            tint = colors.textPrimary,
                            onClick = {
                                tagInputText = ""
                                showAddTagDialog = true
                            },
                            testTag = "bulk_add_tag_btn"
                        )
                    }

                    BulkActionButton(
                        icon = Icons.Default.SwapHoriz,
                        label = "Format",
                        tint = colors.textPrimary,
                        onClick = { showChangeFormatDialog = true },
                        testTag = "bulk_change_format_btn"
                    )

                    BulkActionButton(
                        icon = Icons.Default.Delete,
                        label = "Delete",
                        tint = DangerRed,
                        onClick = {
                            if (anySelectedProtected) {
                                showVerifyPinForBulkDelete = true
                            } else {
                                showBulkDeleteDialog = true
                            }
                        },
                        testTag = "bulk_delete_btn"
                    )
                }
            }
        } else if (!isSelectionMode) {
            // Standard Floating Action Button
            FloatingActionButton(
                onClick = onAddClick,
                shape = CircleShape,
                containerColor = colors.accent,
                contentColor = colors.onAccent,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 24.dp, end = 20.dp)
                    .size(56.dp)
                    .testTag("add_note_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Note",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }

    // 1. Move to Book Dialog
    if (showMoveNotebookDialog) {
        AlertDialog(
            onDismissRequest = { showMoveNotebookDialog = false },
            containerColor = colors.surface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Move to Book",
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.moveNotesToNotebook(selectedNoteIds, cat.id, cat.name)
                                    showMoveNotebookDialog = false
                                    exitSelectionMode()
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                                .testTag("move_target_notebook_${cat.id}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(cat.colorHex))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = cat.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMoveNotebookDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    // 2. Add Tag to Selected Notes Dialog
    if (showAddTagDialog) {
        AlertDialog(
            onDismissRequest = { showAddTagDialog = false },
            containerColor = colors.surface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Add Tag to Selected Notes",
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Adds a #tag to the selected notes so they organize automatically in the Tags page.",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tagInputText,
                        onValueChange = { tagInputText = it },
                        placeholder = { Text("e.g. imp", color = colors.textMuted) },
                        prefix = { Text("#", color = colors.accent, fontWeight = FontWeight.SemiBold) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = colors.surfaceVariant,
                            unfocusedContainerColor = colors.surfaceVariant,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            cursorColor = colors.accent,
                            focusedIndicatorColor = colors.accent,
                            unfocusedIndicatorColor = colors.border
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bulk_tag_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addTagToNotes(selectedNoteIds, tagInputText)
                        showAddTagDialog = false
                        exitSelectionMode()
                    },
                    enabled = tagInputText.trim().removePrefix("#").isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.onAccent
                    ),
                    modifier = Modifier.testTag("bulk_tag_confirm_btn")
                ) {
                    Text("Add Tag")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTagDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    // 3. Change Format Dialog (Notes / Checklist)
    if (showChangeFormatDialog) {
        AlertDialog(
            onDismissRequest = { showChangeFormatDialog = false },
            containerColor = colors.surface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Change Format",
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AVAILABLE_NOTE_FORMATS.forEach { format ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.convertNotesFormat(selectedNoteIds, format)
                                    showChangeFormatDialog = false
                                    exitSelectionMode()
                                }
                                .testTag("bulk_format_${format.lowercase()}")
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                                Text(
                                    text = format,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = if (format == NOTE_FORMAT_CHECKLIST) {
                                        "Convert lines into interactive checklist items"
                                    } else {
                                        "Convert into standard text notes"
                                    },
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showChangeFormatDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    // 4. Bulk Delete Confirmation Dialog
    if (showBulkDeleteDialog) {
        val count = selectedNoteIds.size
        AlertDialog(
            onDismissRequest = { showBulkDeleteDialog = false },
            containerColor = colors.surface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = if (count == 1) "Delete 1 Note?" else "Delete $count Notes?",
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "This will permanently delete the selected ${if (count == 1) "note" else "notes"}.",
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteNotes(selectedNoteIds)
                        showBulkDeleteDialog = false
                        exitSelectionMode()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    modifier = Modifier.testTag("bulk_delete_confirm_btn")
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    // 5. Set Lock PIN Dialog for bulk locking notes
    if (showSetPinForBulkLock) {
        SetLockPinDialog(
            onPinSet = { pin ->
                viewModel.setLockPin(pin)
                viewModel.setNotesLocked(selectedNoteIds, true)
                showSetPinForBulkLock = false
                exitSelectionMode()
            },
            onDismiss = { showSetPinForBulkLock = false }
        )
    }

    // 6. Verify Lock PIN before unlocking selected notes
    if (showVerifyPinForBulkUnlock) {
        VerifyLockPinDialog(
            title = "Unlock Selected Notes",
            subtitle = "Enter your PIN to remove the lock from selected notes.",
            confirmLabel = "Unlock",
            onVerify = { pin -> viewModel.verifyLockPin(pin) },
            onSuccess = {
                viewModel.setNotesLocked(selectedNoteIds, false)
                showVerifyPinForBulkUnlock = false
                exitSelectionMode()
            },
            onDismiss = { showVerifyPinForBulkUnlock = false }
        )
    }

    // 7. Verify Lock PIN before deleting protected notes
    if (showVerifyPinForBulkDelete) {
        VerifyLockPinDialog(
            title = "Protected Notes Selected",
            subtitle = "Enter your PIN to confirm deleting locked notes.",
            confirmLabel = "Continue",
            onVerify = { pin -> viewModel.verifyLockPin(pin) },
            onSuccess = {
                showVerifyPinForBulkDelete = false
                showBulkDeleteDialog = true
            },
            onDismiss = { showVerifyPinForBulkDelete = false }
        )
    }
}

fun getCategoryIcon(iconName: String): ImageVector {
    return when (iconName) {
        "person" -> Icons.Default.Person
        "work" -> Icons.Default.Work
        "lightbulb" -> Icons.Default.Lightbulb
        "menu_book" -> Icons.Default.MenuBook
        "auto_stories" -> Icons.Default.AutoStories
        "check_circle" -> Icons.Default.CheckCircle
        else -> Icons.Default.Category
    }
}

fun getSoftBgForColor(colorHex: Long): Color {
    return when (colorHex) {
        0xFF6366F1 -> SoftIconBgPurple
        0xFF0EA5E9 -> SoftIconBgBlue
        0xFFF59E0B -> SoftIconBgAmber
        0xFF10B981 -> SoftIconBgGreen
        0xFFEC4899 -> Color(0xFFFCE7F3)
        0xFF8B5CF6 -> SoftIconBgPurple
        else -> SoftIconBgSlate
    }
}
