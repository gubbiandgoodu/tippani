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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FALLBACK_BOOK
import com.example.model.NOTE_FORMAT_NOTES
import com.example.model.Note
import com.example.model.TagInfo
import com.example.model.TagUtils
import com.example.model.normalizeNoteFormat
import com.example.ui.components.BulkActionButton
import com.example.ui.components.DirectorySortOption
import com.example.ui.components.NoteItemCard
import com.example.ui.components.NotesSortOption
import com.example.ui.components.SelectionTopBar
import com.example.ui.components.SortMenuOption
import com.example.ui.components.TagListItem
import com.example.ui.components.TippaniTopHeader
import com.example.ui.theme.DangerRed
import com.example.ui.theme.TippaniTheme
import com.example.viewmodel.TippaniViewModel

@Composable
fun TagsScreen(
    viewModel: TippaniViewModel,
    onTagSelected: (String) -> Unit,
    onNoteClick: (Note) -> Unit,
    onOpenSettings: () -> Unit,
    onAddClick: () -> Unit
) {
    val colors = TippaniTheme.colors
    val activeNotes by viewModel.activeNotes.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val lockedNoteIds by viewModel.lockedNoteIds.collectAsState()
    val pinnedNoteTimestamps by viewModel.pinnedNoteTimestamps.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var activeTagFilter by rememberSaveable { mutableStateOf<String?>(null) }

    // Sorting state for directory vs tag detail view
    var directorySortName by rememberSaveable { mutableStateOf(DirectorySortOption.COUNT.name) }
    val selectedDirectorySort = DirectorySortOption.entries.find { it.name == directorySortName } ?: DirectorySortOption.COUNT
    var detailSortName by rememberSaveable { mutableStateOf(NotesSortOption.NEWEST.name) }
    val selectedDetailSort = NotesSortOption.entries.find { it.name == detailSortName } ?: NotesSortOption.NEWEST

    // Multi-selection for Tags directory
    var isTagSelectionMode by remember { mutableStateOf(false) }
    var selectedTagNames by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showRenameMergeDialog by remember { mutableStateOf(false) }
    var targetTagInput by remember { mutableStateOf("") }
    var showRemoveTagsDialog by remember { mutableStateOf(false) }

    // Multi-selection for Notes inside a Tag detail view
    var isNoteSelectionMode by remember { mutableStateOf(false) }
    var selectedNoteIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showMoveNotesDialog by remember { mutableStateOf(false) }
    var showAddTagDialog by remember { mutableStateOf(false) }
    var addTagInput by remember { mutableStateOf("") }
    var showDeleteNotesDialog by remember { mutableStateOf(false) }

    fun exitTagSelection() {
        isTagSelectionMode = false
        selectedTagNames = emptySet()
    }

    fun toggleTagSelection(tagName: String) {
        val updated = if (tagName in selectedTagNames) selectedTagNames - tagName else selectedTagNames + tagName
        selectedTagNames = updated
        if (updated.isEmpty()) isTagSelectionMode = false
    }

    fun exitNoteSelection() {
        isNoteSelectionMode = false
        selectedNoteIds = emptySet()
    }

    fun toggleNoteSelection(noteId: Long) {
        val updated = if (noteId in selectedNoteIds) selectedNoteIds - noteId else selectedNoteIds + noteId
        selectedNoteIds = updated
        if (updated.isEmpty()) isNoteSelectionMode = false
    }

    BackHandler(enabled = isNoteSelectionMode || isTagSelectionMode || activeTagFilter != null) {
        when {
            isNoteSelectionMode -> exitNoteSelection()
            isTagSelectionMode -> exitTagSelection()
            activeTagFilter != null -> {
                activeTagFilter = null
                searchQuery = ""
                isSearchActive = false
            }
        }
    }

    val tagColors = listOf(
        0xFF6366F1,
        0xFF0EA5E9,
        0xFFF59E0B,
        0xFF10B981,
        0xFFEC4899,
        0xFF8B5CF6,
        0xFFF97316,
        0xFF14B8A6
    )

    // Tags apply strictly to Notes (never Checklists)
    val noteOnlyEntries = remember(activeNotes) {
        activeNotes.filter { normalizeNoteFormat(it.noteType) == NOTE_FORMAT_NOTES }
    }

    val allTagsList = remember(noteOnlyEntries) {
        val tagMap = mutableMapOf<String, MutableList<Note>>()
        noteOnlyEntries.forEach { note ->
            val detectedTags = TagUtils.getTagsForNote(note)
            detectedTags.forEach { tag ->
                tagMap.getOrPut(tag) { mutableListOf() }.add(note)
            }
        }
        tagMap.entries.mapIndexed { index, entry ->
            val color = tagColors[index % tagColors.size]
            TagInfo(
                name = entry.key,
                noteCount = entry.value.size,
                wordCount = entry.value.sumOf { it.wordCount },
                colorHex = color
            )
        }.sortedByDescending { it.noteCount }
    }

    // Keep selections pruned if tags/notes change externally
    LaunchedEffect(allTagsList) {
        val validNames = allTagsList.map { it.name }.toSet()
        val pruned = selectedTagNames.intersect(validNames)
        if (pruned != selectedTagNames) {
            selectedTagNames = pruned
            if (pruned.isEmpty()) isTagSelectionMode = false
        }
    }

    LaunchedEffect(activeNotes) {
        val activeIds = activeNotes.map { it.id }.toSet()
        val pruned = selectedNoteIds.intersect(activeIds)
        if (pruned != selectedNoteIds) {
            selectedNoteIds = pruned
            if (pruned.isEmpty()) isNoteSelectionMode = false
        }
    }

    val directorySortMenuOptions = remember {
        DirectorySortOption.entries.map { SortMenuOption(it.name, it.label) }
    }
    val detailSortMenuOptions = remember {
        NotesSortOption.entries.map { SortMenuOption(it.name, it.label) }
    }

    val filteredTags = remember(allTagsList, searchQuery, selectedDirectorySort) {
        val base = if (searchQuery.isBlank()) allTagsList
        else allTagsList.filter { it.name.contains(searchQuery, ignoreCase = true) }

        when (selectedDirectorySort) {
            DirectorySortOption.A_Z -> base.sortedBy { it.name.lowercase() }
            DirectorySortOption.COUNT -> base.sortedWith(
                compareByDescending<TagInfo> { it.noteCount }
                    .thenBy { it.name.lowercase() }
            )
        }
    }

    val notesForActiveTag = remember(noteOnlyEntries, activeTagFilter, searchQuery, selectedDetailSort, pinnedNoteTimestamps) {
        val selectedTag = activeTagFilter ?: return@remember emptyList()
        val matching = noteOnlyEntries.filter { note ->
            TagUtils.getTagsForNote(note).any { it.equals(selectedTag, ignoreCase = true) }
        }
        val searched = if (searchQuery.isBlank()) matching else {
            val q = searchQuery.lowercase()
            matching.filter { it.title.lowercase().contains(q) || it.content.lowercase().contains(q) }
        }
        val pinned = searched.filter { it.isPinned }
            .sortedByDescending { pinnedNoteTimestamps[it.id] ?: it.updatedAt }
        val unpinned = searched.filter { !it.isPinned }
        val sortedUnpinned = when (selectedDetailSort) {
            NotesSortOption.NEWEST -> unpinned.sortedByDescending { it.createdAt }
            NotesSortOption.OLDEST -> unpinned.sortedBy { it.createdAt }
            NotesSortOption.A_Z -> unpinned.sortedBy { it.title.lowercase() }
        }
        pinned + sortedUnpinned
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(18.dp))

            if (activeTagFilter != null) {
                if (isNoteSelectionMode) {
                    val allVisibleIds = notesForActiveTag.map { it.id }.toSet()
                    val allSelected = allVisibleIds.isNotEmpty() && selectedNoteIds.containsAll(allVisibleIds)
                    SelectionTopBar(
                        selectedCount = selectedNoteIds.size,
                        allSelected = allSelected,
                        onClose = { exitNoteSelection() },
                        onToggleSelectAll = {
                            selectedNoteIds = if (allSelected) emptySet() else allVisibleIds
                            if (selectedNoteIds.isEmpty()) isNoteSelectionMode = false
                        },
                        testTagPrefix = "tag_notes"
                    )
                } else {
                    val count = notesForActiveTag.size
                    TippaniTopHeader(
                        title = "#$activeTagFilter",
                        subtitle = if (count == 1) "1 note tagged" else "$count notes tagged",
                        isSearchActive = isSearchActive,
                        onToggleSearch = { isSearchActive = !isSearchActive },
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onOpenSettings = onOpenSettings,
                        isSelectionMode = isNoteSelectionMode,
                        onToggleSelect = {
                            if (notesForActiveTag.isNotEmpty()) {
                                isNoteSelectionMode = !isNoteSelectionMode
                            }
                        },
                        sortOptions = detailSortMenuOptions,
                        selectedSortId = selectedDetailSort.name,
                        onSortSelected = { detailSortName = it }
                    )
                }
            } else {
                if (isTagSelectionMode) {
                    val allVisibleNames = filteredTags.map { it.name }.toSet()
                    val allSelected = allVisibleNames.isNotEmpty() && selectedTagNames.containsAll(allVisibleNames)
                    SelectionTopBar(
                        selectedCount = selectedTagNames.size,
                        allSelected = allSelected,
                        onClose = { exitTagSelection() },
                        onToggleSelectAll = {
                            selectedTagNames = if (allSelected) emptySet() else allVisibleNames
                            if (selectedTagNames.isEmpty()) isTagSelectionMode = false
                        },
                        testTagPrefix = "tags"
                    )
                } else {
                    TippaniTopHeader(
                        title = "Tags",
                        subtitle = null,
                        isSearchActive = isSearchActive,
                        onToggleSearch = { isSearchActive = !isSearchActive },
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onOpenSettings = onOpenSettings,
                        isSelectionMode = isTagSelectionMode,
                        onToggleSelect = {
                            if (filteredTags.isNotEmpty()) {
                                isTagSelectionMode = !isTagSelectionMode
                            }
                        },
                        sortOptions = directorySortMenuOptions,
                        selectedSortId = selectedDirectorySort.name,
                        onSortSelected = { directorySortName = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (activeTagFilter != null) {
                if (notesForActiveTag.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No notes found with this tag", fontSize = 16.sp, color = colors.textSecondary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .testTag("tagged_notes_list"),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 104.dp)
                    ) {
                        items(notesForActiveTag, key = { it.id }) { note ->
                            val cat = categories.find { it.id == note.categoryId } ?: categories.firstOrNull() ?: FALLBACK_BOOK
                            val isLocked = note.id in lockedNoteIds || cat.isLocked
                            NoteItemCard(
                                note = note,
                                categoryName = cat.name,
                                categoryIcon = getCategoryIcon(cat.iconName),
                                iconTint = Color(cat.colorHex),
                                iconBgColor = getSoftBgForColor(cat.colorHex),
                                isSelectionMode = isNoteSelectionMode,
                                isSelected = note.id in selectedNoteIds,
                                isLocked = isLocked,
                                onClick = {
                                    if (isNoteSelectionMode) {
                                        toggleNoteSelection(note.id)
                                    } else {
                                        onNoteClick(note)
                                    }
                                },
                                onLongClick = {
                                    if (!isNoteSelectionMode) {
                                        isNoteSelectionMode = true
                                        selectedNoteIds = setOf(note.id)
                                    } else {
                                        toggleNoteSelection(note.id)
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                if (filteredTags.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tag,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No tags found", fontSize = 16.sp, color = colors.textSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Write #tags like #imp inside your notes to auto-organize them here",
                                fontSize = 12.sp,
                                color = colors.textMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .testTag("tags_list_view"),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 104.dp)
                    ) {
                        items(filteredTags, key = { it.name }) { tag ->
                            TagListItem(
                                tag = tag,
                                isSelectionMode = isTagSelectionMode,
                                isSelected = tag.name in selectedTagNames,
                                onClick = {
                                    if (isTagSelectionMode) {
                                        toggleTagSelection(tag.name)
                                    } else {
                                        searchQuery = ""
                                        isSearchActive = false
                                        activeTagFilter = tag.name
                                    }
                                },
                                onLongClick = {
                                    if (!isTagSelectionMode) {
                                        isTagSelectionMode = true
                                        selectedTagNames = setOf(tag.name)
                                    } else {
                                        toggleTagSelection(tag.name)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Floating Bulk Action Bar OR Add FAB
        if (activeTagFilter == null && isTagSelectionMode && selectedTagNames.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = colors.surface,
                border = colors.cardBorder,
                shadowElevation = if (colors.isDark) 4.dp else 10.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp, vertical = 18.dp)
                    .fillMaxWidth()
                    .testTag("tags_bulk_action_bar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BulkActionButton(
                        icon = Icons.Default.Edit,
                        label = if (selectedTagNames.size == 1) "Rename" else "Merge",
                        tint = colors.textPrimary,
                        onClick = {
                            targetTagInput = if (selectedTagNames.size == 1) selectedTagNames.first() else ""
                            showRenameMergeDialog = true
                        },
                        testTag = "tags_bulk_rename_btn"
                    )

                    BulkActionButton(
                        icon = Icons.Default.Delete,
                        label = "Remove",
                        tint = DangerRed,
                        onClick = { showRemoveTagsDialog = true },
                        testTag = "tags_bulk_remove_btn"
                    )
                }
            }
        } else if (activeTagFilter != null && isNoteSelectionMode && selectedNoteIds.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = colors.surface,
                border = colors.cardBorder,
                shadowElevation = if (colors.isDark) 4.dp else 10.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp, vertical = 18.dp)
                    .fillMaxWidth()
                    .testTag("tag_notes_bulk_action_bar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BulkActionButton(
                        icon = Icons.AutoMirrored.Filled.DriveFileMove,
                        label = "Book",
                        tint = colors.textPrimary,
                        onClick = { showMoveNotesDialog = true },
                        testTag = "tag_notes_bulk_move_btn"
                    )

                    BulkActionButton(
                        icon = Icons.Default.Tag,
                        label = "Add Tag",
                        tint = colors.textPrimary,
                        onClick = {
                            addTagInput = ""
                            showAddTagDialog = true
                        },
                        testTag = "tag_notes_bulk_tag_btn"
                    )

                    BulkActionButton(
                        icon = Icons.Default.Delete,
                        label = "Delete",
                        tint = DangerRed,
                        onClick = { showDeleteNotesDialog = true },
                        testTag = "tag_notes_bulk_delete_btn"
                    )
                }
            }
        } else if (!isTagSelectionMode && !isNoteSelectionMode) {
            FloatingActionButton(
                onClick = onAddClick,
                shape = CircleShape,
                containerColor = colors.accent,
                contentColor = colors.onAccent,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 24.dp, end = 20.dp)
                    .size(56.dp)
                    .testTag("tags_add_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Note",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }

    // Dialog: Rename / Merge Selected Tags
    if (showRenameMergeDialog) {
        val isSingle = selectedTagNames.size == 1
        AlertDialog(
            onDismissRequest = { showRenameMergeDialog = false },
            containerColor = colors.surface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = if (isSingle) "Rename #${selectedTagNames.first()}" else "Merge ${selectedTagNames.size} Tags",
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isSingle) {
                            "Replaces #${selectedTagNames.first()} across all notes with the new tag name."
                        } else {
                            "Combines the selected tags across all notes into a single #tag."
                        },
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = targetTagInput,
                        onValueChange = { targetTagInput = it },
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
                            .testTag("tags_rename_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renameOrMergeTags(selectedTagNames, targetTagInput)
                        showRenameMergeDialog = false
                        exitTagSelection()
                    },
                    enabled = targetTagInput.trim().removePrefix("#").isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.onAccent
                    ),
                    modifier = Modifier.testTag("tags_rename_confirm_btn")
                ) {
                    Text(if (isSingle) "Rename" else "Merge")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameMergeDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    // Dialog: Remove Selected Tags from Notes (notes themselves are preserved)
    if (showRemoveTagsDialog) {
        val count = selectedTagNames.size
        AlertDialog(
            onDismissRequest = { showRemoveTagsDialog = false },
            containerColor = colors.surface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = if (count == 1) "Remove #${selectedTagNames.first()}?" else "Remove $count Tags?",
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "This removes the selected ${if (count == 1) "#tag" else "#tags"} from all notes. Your notes will not be deleted.",
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeTagsFromAllNotes(selectedTagNames)
                        showRemoveTagsDialog = false
                        exitTagSelection()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    modifier = Modifier.testTag("tags_remove_confirm_btn")
                ) {
                    Text("Remove", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveTagsDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    // Detail View Dialog 1: Move Selected Notes to Book
    if (showMoveNotesDialog) {
        AlertDialog(
            onDismissRequest = { showMoveNotesDialog = false },
            containerColor = colors.surface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text("Move to Book", fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
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
                                    showMoveNotesDialog = false
                                    exitNoteSelection()
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
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
                TextButton(onClick = { showMoveNotesDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    // Detail View Dialog 2: Add Tag to Selected Notes
    if (showAddTagDialog) {
        AlertDialog(
            onDismissRequest = { showAddTagDialog = false },
            containerColor = colors.surface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text("Add Tag to Selected Notes", fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            },
            text = {
                Column {
                    Text(
                        text = "Adds a #tag to the selected notes.",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = addTagInput,
                        onValueChange = { addTagInput = it },
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
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addTagToNotes(selectedNoteIds, addTagInput)
                        showAddTagDialog = false
                        exitNoteSelection()
                    },
                    enabled = addTagInput.trim().removePrefix("#").isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.onAccent
                    )
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

    // Detail View Dialog 3: Delete Selected Notes
    if (showDeleteNotesDialog) {
        val count = selectedNoteIds.size
        AlertDialog(
            onDismissRequest = { showDeleteNotesDialog = false },
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
                        showDeleteNotesDialog = false
                        exitNoteSelection()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteNotesDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }
}
