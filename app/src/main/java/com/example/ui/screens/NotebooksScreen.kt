package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BOOK_COLOR_OPTIONS
import com.example.model.Note
import com.example.model.NotebookCategory
import com.example.ui.components.BulkActionButton
import com.example.ui.components.DirectorySortOption
import com.example.ui.components.NoteItemCard
import com.example.ui.components.NotebookListItem
import com.example.ui.components.NotesSortOption
import com.example.ui.components.SelectionTopBar
import com.example.ui.components.SetLockPinDialog
import com.example.ui.components.SortMenuOption
import com.example.ui.components.TippaniTopHeader
import com.example.ui.components.VerifyLockPinDialog
import com.example.ui.theme.DangerRed
import com.example.ui.theme.TippaniTheme
import com.example.viewmodel.TippaniViewModel

@Composable
fun NotebooksScreen(
    viewModel: TippaniViewModel,
    onNoteClick: (Note) -> Unit,
    onOpenSettings: () -> Unit,
    onAddClick: (categoryId: String?) -> Unit
) {
    val colors = TippaniTheme.colors
    val books by viewModel.categories.collectAsState()
    val activeNotes by viewModel.activeNotes.collectAsState()
    val lockedNoteIds by viewModel.lockedNoteIds.collectAsState()
    val pinnedNoteTimestamps by viewModel.pinnedNoteTimestamps.collectAsState()
    val hasLockPin by viewModel.hasLockPin.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Active book opened in dedicated detail view (built-in back gesture/button returns to Books)
    var activeBookId by rememberSaveable { mutableStateOf<String?>(null) }
    val activeBook = books.find { it.id == activeBookId }

    // Sorting state for Books directory vs Notes inside a book (sorting ignores pinned items)
    var directorySortName by rememberSaveable { mutableStateOf(DirectorySortOption.A_Z.name) }
    val selectedDirectorySort = DirectorySortOption.entries.find { it.name == directorySortName } ?: DirectorySortOption.A_Z
    var detailSortName by rememberSaveable { mutableStateOf(NotesSortOption.NEWEST.name) }
    val selectedDetailSort = NotesSortOption.entries.find { it.name == detailSortName } ?: NotesSortOption.NEWEST

    // Create / Rename / Delete / Pin / Lock Book dialogs
    var showNewBookDialog by remember { mutableStateOf(false) }
    var newBookName by remember { mutableStateOf("") }
    var newBookColorHex by remember { mutableLongStateOf(BOOK_COLOR_OPTIONS.first()) }

    var longPressedBook by remember { mutableStateOf<NotebookCategory?>(null) }
    var renameBookTarget by remember { mutableStateOf<NotebookCategory?>(null) }
    var renameBookInput by remember { mutableStateOf("") }
    var deleteBookTarget by remember { mutableStateOf<NotebookCategory?>(null) }

    // PIN setup & verification flows for Books and Notes
    var pendingSetPinAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pendingVerifyPinTitle by remember { mutableStateOf<String?>(null) }
    var pendingVerifyPinSubtitle by remember { mutableStateOf("") }
    var pendingVerifyPinConfirmLabel by remember { mutableStateOf("Unlock") }
    var pendingVerifyPinAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Multi-selection for Books directory
    var isBookSelectionMode by remember { mutableStateOf(false) }
    var selectedBookIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showMoveBooksDialog by remember { mutableStateOf(false) }
    var showDeleteBooksDialog by remember { mutableStateOf(false) }

    // Multi-selection for Notes inside an opened Book
    var isNoteSelectionMode by remember { mutableStateOf(false) }
    var selectedNoteIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showMoveNotesDialog by remember { mutableStateOf(false) }
    var showDeleteNotesDialog by remember { mutableStateOf(false) }

    fun requestPinVerification(
        title: String,
        subtitle: String,
        confirmLabel: String = "Unlock",
        onVerified: () -> Unit
    ) {
        pendingVerifyPinTitle = title
        pendingVerifyPinSubtitle = subtitle
        pendingVerifyPinConfirmLabel = confirmLabel
        pendingVerifyPinAction = onVerified
    }

    fun requestLockWithPin(onLock: () -> Unit) {
        if (hasLockPin) {
            onLock()
        } else {
            pendingSetPinAction = onLock
        }
    }

    fun exitBookSelection() {
        isBookSelectionMode = false
        selectedBookIds = emptySet()
    }

    fun toggleBookSelection(id: String) {
        val updated = if (id in selectedBookIds) selectedBookIds - id else selectedBookIds + id
        selectedBookIds = updated
        if (updated.isEmpty()) isBookSelectionMode = false
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

    // Keep selections & activeBookId pruned if books or notes change
    LaunchedEffect(books) {
        val validBookIds = books.map { it.id }.toSet()
        val prunedBooks = selectedBookIds.intersect(validBookIds)
        if (prunedBooks != selectedBookIds) {
            selectedBookIds = prunedBooks
            if (prunedBooks.isEmpty()) isBookSelectionMode = false
        }
        if (activeBookId != null && activeBookId !in validBookIds) {
            activeBookId = null
            exitNoteSelection()
        }
    }

    LaunchedEffect(activeNotes) {
        val validNoteIds = activeNotes.map { it.id }.toSet()
        val prunedNotes = selectedNoteIds.intersect(validNoteIds)
        if (prunedNotes != selectedNoteIds) {
            selectedNoteIds = prunedNotes
            if (prunedNotes.isEmpty()) isNoteSelectionMode = false
        }
    }

    // Built-in back gesture/button handles closing selection or returning from a Book to All Books
    BackHandler(enabled = isNoteSelectionMode || isBookSelectionMode || activeBookId != null) {
        when {
            isNoteSelectionMode -> exitNoteSelection()
            isBookSelectionMode -> exitBookSelection()
            activeBookId != null -> {
                activeBookId = null
                searchQuery = ""
                isSearchActive = false
            }
        }
    }

    val notesByBook = remember(activeNotes) {
        activeNotes.groupBy { it.categoryId }
    }

    val directorySortMenuOptions = remember {
        DirectorySortOption.entries.map { SortMenuOption(it.name, it.label) }
    }
    val detailSortMenuOptions = remember {
        NotesSortOption.entries.map { SortMenuOption(it.name, it.label) }
    }

    // Sorting IGNORES pinned books: pinned books stay at top in pinned order, unpinned books sort
    val filteredBooks = remember(books, searchQuery, notesByBook, selectedDirectorySort) {
        val base = if (searchQuery.isBlank()) books
        else books.filter { book ->
            book.name.contains(searchQuery, ignoreCase = true) ||
                (notesByBook[book.id] ?: emptyList()).any { note ->
                    !book.isLocked && (note.title.contains(searchQuery, ignoreCase = true) ||
                        note.content.contains(searchQuery, ignoreCase = true))
                }
        }

        val pinnedBooks = base
            .filter { it.isPinned }
            .sortedByDescending { it.pinnedAt }
        val unpinnedBooks = base.filter { !it.isPinned }
        val sortedUnpinned = when (selectedDirectorySort) {
            DirectorySortOption.A_Z -> unpinnedBooks.sortedBy { it.name.lowercase() }
            DirectorySortOption.COUNT -> unpinnedBooks.sortedWith(
                compareByDescending<NotebookCategory> { (notesByBook[it.id] ?: emptyList()).size }
                    .thenBy { it.name.lowercase() }
            )
        }
        pinnedBooks + sortedUnpinned
    }

    // Sorting IGNORES pinned notes inside a book: pinned notes stay at top in pinned order
    val notesForActiveBook = remember(activeNotes, activeBookId, searchQuery, selectedDetailSort, pinnedNoteTimestamps) {
        val bookId = activeBookId ?: return@remember emptyList()
        val matching = activeNotes.filter { it.categoryId == bookId }
        val searched = if (searchQuery.isBlank()) matching else {
            val q = searchQuery.lowercase()
            matching.filter { it.title.lowercase().contains(q) || it.content.lowercase().contains(q) }
        }
        val pinned = searched
            .filter { it.isPinned }
            .sortedByDescending { pinnedNoteTimestamps[it.id] ?: it.updatedAt }
        val unpinned = searched.filter { !it.isPinned }
        val sortedUnpinned = when (selectedDetailSort) {
            NotesSortOption.NEWEST -> unpinned.sortedByDescending { it.createdAt }
            NotesSortOption.OLDEST -> unpinned.sortedBy { it.createdAt }
            NotesSortOption.A_Z -> unpinned.sortedBy { it.title.lowercase() }
        }
        pinned + sortedUnpinned
    }

    val totalSelectedBookNoteCount = remember(notesByBook, selectedBookIds) {
        selectedBookIds.sumOf { (notesByBook[it] ?: emptyList()).size }
    }
    val anySelectedBookLocked = remember(books, selectedBookIds) {
        books.any { it.id in selectedBookIds && it.isLocked }
    }

    val selectedNotes = remember(notesForActiveBook, selectedNoteIds) {
        notesForActiveBook.filter { it.id in selectedNoteIds }
    }
    val allSelectedNotesPinned = remember(selectedNotes) {
        selectedNotes.isNotEmpty() && selectedNotes.all { it.isPinned }
    }
    val allSelectedNotesLocked = remember(selectedNotes, lockedNoteIds) {
        selectedNotes.isNotEmpty() && selectedNotes.all { it.id in lockedNoteIds }
    }
    val anySelectedNoteLocked = remember(selectedNotes, lockedNoteIds) {
        selectedNotes.any { it.id in lockedNoteIds }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(18.dp))

            if (activeBook != null) {
                if (isNoteSelectionMode) {
                    val allVisibleIds = notesForActiveBook.map { it.id }.toSet()
                    val allSelected = allVisibleIds.isNotEmpty() && selectedNoteIds.containsAll(allVisibleIds)
                    SelectionTopBar(
                        selectedCount = selectedNoteIds.size,
                        allSelected = allSelected,
                        onClose = { exitNoteSelection() },
                        onToggleSelectAll = {
                            selectedNoteIds = if (allSelected) emptySet() else allVisibleIds
                            if (selectedNoteIds.isEmpty()) isNoteSelectionMode = false
                        },
                        testTagPrefix = "book_notes"
                    )
                } else {
                    // No "All Books" back button — system back gesture/button handles navigating back
                    TippaniTopHeader(
                        title = activeBook.name,
                        subtitle = null,
                        isSearchActive = isSearchActive,
                        onToggleSearch = { isSearchActive = !isSearchActive },
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onOpenSettings = onOpenSettings,
                        isSelectionMode = isNoteSelectionMode,
                        onToggleSelect = {
                            if (notesForActiveBook.isNotEmpty()) {
                                isNoteSelectionMode = !isNoteSelectionMode
                            }
                        },
                        sortOptions = detailSortMenuOptions,
                        selectedSortId = selectedDetailSort.name,
                        onSortSelected = { detailSortName = it }
                    )
                }
            } else {
                if (isBookSelectionMode) {
                    val allVisibleIds = filteredBooks.map { it.id }.toSet()
                    val allSelected = allVisibleIds.isNotEmpty() && selectedBookIds.containsAll(allVisibleIds)
                    SelectionTopBar(
                        selectedCount = selectedBookIds.size,
                        allSelected = allSelected,
                        onClose = { exitBookSelection() },
                        onToggleSelectAll = {
                            selectedBookIds = if (allSelected) emptySet() else allVisibleIds
                            if (selectedBookIds.isEmpty()) isBookSelectionMode = false
                        },
                        testTagPrefix = "books"
                    )
                } else {
                    TippaniTopHeader(
                        title = "Books",
                        subtitle = null,
                        isSearchActive = isSearchActive,
                        onToggleSearch = { isSearchActive = !isSearchActive },
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onOpenSettings = onOpenSettings,
                        isSelectionMode = isBookSelectionMode,
                        onToggleSelect = {
                            if (filteredBooks.isNotEmpty()) {
                                isBookSelectionMode = !isBookSelectionMode
                            }
                        },
                        sortOptions = directorySortMenuOptions,
                        selectedSortId = selectedDirectorySort.name,
                        onSortSelected = { directorySortName = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (activeBook != null) {
                if (notesForActiveBook.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (searchQuery.isNotBlank()) "No matching notes" else "No notes in ${activeBook.name}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap + to add a note to this book",
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
                            .testTag("book_notes_list"),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 104.dp)
                    ) {
                        items(notesForActiveBook, key = { it.id }) { note ->
                            val isLocked = note.id in lockedNoteIds
                            NoteItemCard(
                                note = note,
                                categoryName = activeBook.name,
                                categoryIcon = getCategoryIcon(activeBook.iconName),
                                iconTint = Color(activeBook.colorHex),
                                iconBgColor = getSoftBgForColor(activeBook.colorHex),
                                isSelectionMode = isNoteSelectionMode,
                                isSelected = note.id in selectedNoteIds,
                                isLocked = isLocked,
                                showSubtitle = false,
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
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .testTag("books_list_view"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 104.dp)
                ) {
                    // First row: "New Book"
                    if (!isBookSelectionMode) {
                        item(key = "new_book_row") {
                            NewBookRowCard(
                                onClick = {
                                    newBookName = ""
                                    newBookColorHex = BOOK_COLOR_OPTIONS[books.size % BOOK_COLOR_OPTIONS.size]
                                    showNewBookDialog = true
                                }
                            )
                        }
                    }

                    if (filteredBooks.isEmpty()) {
                        item(key = "empty_books_state") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (searchQuery.isNotBlank()) "No matching books" else "No books yet",
                                    fontSize = 15.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    } else {
                        items(filteredBooks, key = { it.id }) { book ->
                            val rawBookNotes = notesByBook[book.id] ?: emptyList()
                            NotebookListItem(
                                category = book,
                                noteCount = rawBookNotes.size,
                                isSelectionMode = isBookSelectionMode,
                                isSelected = book.id in selectedBookIds,
                                onClick = {
                                    if (isBookSelectionMode) {
                                        toggleBookSelection(book.id)
                                    } else if (book.isLocked) {
                                        requestPinVerification(
                                            title = "Unlock \"${book.name}\"",
                                            subtitle = "Enter your PIN to open this book."
                                        ) {
                                            searchQuery = ""
                                            isSearchActive = false
                                            activeBookId = book.id
                                        }
                                    } else {
                                        searchQuery = ""
                                        isSearchActive = false
                                        activeBookId = book.id
                                    }
                                },
                                onLongClick = {
                                    if (isBookSelectionMode) {
                                        toggleBookSelection(book.id)
                                    } else {
                                        longPressedBook = book
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Floating Bulk Action Bar OR Add FAB
        if (activeBook == null && isBookSelectionMode && selectedBookIds.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = colors.surface,
                border = colors.cardBorder,
                shadowElevation = if (colors.isDark) 4.dp else 10.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp, vertical = 18.dp)
                    .fillMaxWidth()
                    .testTag("books_bulk_action_bar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedBookIds.size == 1) {
                        val singleTarget = books.find { it.id == selectedBookIds.first() }
                        if (singleTarget != null) {
                            BulkActionButton(
                                icon = Icons.Default.PushPin,
                                label = if (singleTarget.isPinned) "Unpin" else "Pin",
                                tint = if (singleTarget.isPinned) colors.accent else colors.textPrimary,
                                onClick = {
                                    viewModel.togglePinBook(singleTarget.id)
                                    exitBookSelection()
                                },
                                testTag = "books_bulk_pin_btn"
                            )
                            BulkActionButton(
                                icon = if (singleTarget.isLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                label = if (singleTarget.isLocked) "Unlock" else "Lock",
                                tint = if (singleTarget.isLocked) colors.accent else colors.textPrimary,
                                onClick = {
                                    if (singleTarget.isLocked) {
                                        requestPinVerification(
                                            title = "Unlock \"${singleTarget.name}\"",
                                            subtitle = "Enter your PIN to remove the lock from this book.",
                                            confirmLabel = "Unlock"
                                        ) {
                                            viewModel.setBookLocked(singleTarget.id, false)
                                            exitBookSelection()
                                        }
                                    } else {
                                        requestLockWithPin {
                                            viewModel.setBookLocked(singleTarget.id, true)
                                            exitBookSelection()
                                        }
                                    }
                                },
                                testTag = "books_bulk_lock_btn"
                            )
                        }
                    }

                    BulkActionButton(
                        icon = Icons.AutoMirrored.Filled.DriveFileMove,
                        label = "Move Notes",
                        tint = colors.textPrimary,
                        onClick = { showMoveBooksDialog = true },
                        testTag = "books_bulk_move_btn"
                    )

                    BulkActionButton(
                        icon = Icons.Default.Delete,
                        label = "Delete",
                        tint = DangerRed,
                        onClick = {
                            if (anySelectedBookLocked) {
                                requestPinVerification(
                                    title = "Protected Book Selected",
                                    subtitle = "Enter your PIN to confirm deleting locked books.",
                                    confirmLabel = "Continue"
                                ) {
                                    showDeleteBooksDialog = true
                                }
                            } else {
                                showDeleteBooksDialog = true
                            }
                        },
                        testTag = "books_bulk_delete_btn"
                    )
                }
            }
        } else if (activeBook != null && isNoteSelectionMode && selectedNoteIds.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = colors.surface,
                border = colors.cardBorder,
                shadowElevation = if (colors.isDark) 4.dp else 10.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 18.dp)
                    .fillMaxWidth()
                    .testTag("book_notes_bulk_action_bar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BulkActionButton(
                        icon = Icons.Default.PushPin,
                        label = if (allSelectedNotesPinned) "Unpin" else "Pin",
                        tint = if (allSelectedNotesPinned) colors.accent else colors.textPrimary,
                        onClick = {
                            viewModel.setNotesPinned(selectedNoteIds, !allSelectedNotesPinned)
                            exitNoteSelection()
                        },
                        testTag = "book_notes_bulk_pin_btn"
                    )

                    BulkActionButton(
                        icon = if (allSelectedNotesLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                        label = if (allSelectedNotesLocked) "Unlock" else "Lock",
                        tint = if (allSelectedNotesLocked) colors.accent else colors.textPrimary,
                        onClick = {
                            if (allSelectedNotesLocked) {
                                requestPinVerification(
                                    title = "Unlock Selected Notes",
                                    subtitle = "Enter your PIN to remove the lock from selected notes.",
                                    confirmLabel = "Unlock"
                                ) {
                                    viewModel.setNotesLocked(selectedNoteIds, false)
                                    exitNoteSelection()
                                }
                            } else {
                                requestLockWithPin {
                                    viewModel.setNotesLocked(selectedNoteIds, true)
                                    exitNoteSelection()
                                }
                            }
                        },
                        testTag = "book_notes_bulk_lock_btn"
                    )

                    BulkActionButton(
                        icon = Icons.AutoMirrored.Filled.DriveFileMove,
                        label = "Book",
                        tint = colors.textPrimary,
                        onClick = { showMoveNotesDialog = true },
                        testTag = "book_notes_bulk_move_btn"
                    )

                    BulkActionButton(
                        icon = Icons.Default.Delete,
                        label = "Delete",
                        tint = DangerRed,
                        onClick = {
                            if (anySelectedNoteLocked) {
                                requestPinVerification(
                                    title = "Protected Notes Selected",
                                    subtitle = "Enter your PIN to confirm deleting locked notes.",
                                    confirmLabel = "Continue"
                                ) {
                                    showDeleteNotesDialog = true
                                }
                            } else {
                                showDeleteNotesDialog = true
                            }
                        },
                        testTag = "book_notes_bulk_delete_btn"
                    )
                }
            }
        } else if (!isBookSelectionMode && !isNoteSelectionMode) {
            FloatingActionButton(
                onClick = { onAddClick(activeBookId) },
                shape = CircleShape,
                containerColor = colors.accent,
                contentColor = colors.onAccent,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 24.dp, end = 20.dp)
                    .size(56.dp)
                    .testTag("books_add_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Note",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }

    // Dialog: Create New Book
    if (showNewBookDialog) {
        AlertDialog(
            onDismissRequest = { showNewBookDialog = false },
            containerColor = colors.surface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "New Book",
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(
                        value = newBookName,
                        onValueChange = { newBookName = it },
                        placeholder = { Text("Book name...", color = colors.textMuted) },
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
                            .testTag("new_book_name_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BOOK_COLOR_OPTIONS.forEach { colorHex ->
                            val isSelected = colorHex == newBookColorHex
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorHex))
                                    .border(
                                        width = if (isSelected) 2.5.dp else 0.dp,
                                        color = if (isSelected) colors.textPrimary else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { newBookColorHex = colorHex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected color",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createBook(newBookName, newBookColorHex)
                        showNewBookDialog = false
                    },
                    enabled = newBookName.trim().isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.onAccent
                    ),
                    modifier = Modifier.testTag("create_book_confirm_btn")
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewBookDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    // Dialog: Long-Press Book Options (Pin, Lock/Protect, Rename, Delete)
    longPressedBook?.let { book ->
        AlertDialog(
            onDismissRequest = { longPressedBook = null },
            containerColor = colors.surface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color(book.colorHex))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = book.name,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionActionRow(
                        icon = Icons.Default.PushPin,
                        label = if (book.isPinned) "Unpin Book" else "Pin to Top",
                        tint = if (book.isPinned) colors.accent else colors.textPrimary,
                        containerColor = if (book.isPinned) colors.accentSoft else colors.surfaceVariant,
                        testTag = "book_option_pin_btn",
                        onClick = {
                            longPressedBook = null
                            viewModel.togglePinBook(book.id)
                        }
                    )

                    OptionActionRow(
                        icon = if (book.isLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                        label = if (book.isLocked) "Unlock Book" else "Lock / Protect Book",
                        tint = if (book.isLocked) colors.accent else colors.textPrimary,
                        containerColor = if (book.isLocked) colors.accentSoft else colors.surfaceVariant,
                        testTag = "book_option_lock_btn",
                        onClick = {
                            longPressedBook = null
                            if (book.isLocked) {
                                requestPinVerification(
                                    title = "Unlock \"${book.name}\"",
                                    subtitle = "Enter your PIN to remove the lock from this book.",
                                    confirmLabel = "Unlock"
                                ) {
                                    viewModel.setBookLocked(book.id, false)
                                }
                            } else {
                                requestLockWithPin {
                                    viewModel.setBookLocked(book.id, true)
                                }
                            }
                        }
                    )

                    OptionActionRow(
                        icon = Icons.Default.Edit,
                        label = "Rename Book",
                        tint = colors.textPrimary,
                        containerColor = colors.surfaceVariant,
                        testTag = "book_option_rename_btn",
                        onClick = {
                            longPressedBook = null
                            if (book.isLocked) {
                                requestPinVerification(
                                    title = "Unlock to Rename",
                                    subtitle = "Enter your PIN to rename \"${book.name}\".",
                                    confirmLabel = "Continue"
                                ) {
                                    renameBookTarget = book
                                    renameBookInput = book.name
                                }
                            } else {
                                renameBookTarget = book
                                renameBookInput = book.name
                            }
                        }
                    )

                    OptionActionRow(
                        icon = Icons.Default.Delete,
                        label = "Delete Book",
                        tint = DangerRed,
                        containerColor = colors.softIconBg(DangerRed),
                        testTag = "book_option_delete_btn",
                        onClick = {
                            longPressedBook = null
                            if (book.isLocked) {
                                requestPinVerification(
                                    title = "Unlock to Delete",
                                    subtitle = "Enter your PIN to delete locked book \"${book.name}\".",
                                    confirmLabel = "Continue"
                                ) {
                                    deleteBookTarget = book
                                }
                            } else {
                                deleteBookTarget = book
                            }
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { longPressedBook = null }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    // Dialog: Rename Book
    renameBookTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameBookTarget = null },
            containerColor = colors.surface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Rename Book",
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
            },
            text = {
                OutlinedTextField(
                    value = renameBookInput,
                    onValueChange = { renameBookInput = it },
                    placeholder = { Text("Book name...", color = colors.textMuted) },
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
                        .testTag("rename_book_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renameBook(target.id, renameBookInput)
                        renameBookTarget = null
                        exitBookSelection()
                    },
                    enabled = renameBookInput.trim().isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.onAccent
                    ),
                    modifier = Modifier.testTag("rename_book_confirm_btn")
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameBookTarget = null }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    // Dialog: Delete Single Book Confirmation
    deleteBookTarget?.let { target ->
        val noteCount = (notesByBook[target.id] ?: emptyList()).size
        AlertDialog(
            onDismissRequest = { deleteBookTarget = null },
            containerColor = colors.surface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Delete \"${target.name}\"?",
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    text = if (noteCount == 0) {
                        "This book will be permanently deleted."
                    } else if (noteCount == 1) {
                        "This book and the 1 note inside it will be permanently deleted."
                    } else {
                        "This book and all $noteCount notes inside it will be permanently deleted."
                    },
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBook(target.id, deleteContainedNotes = true)
                        deleteBookTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    modifier = Modifier.testTag("delete_book_confirm_btn")
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteBookTarget = null }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    // Dialog: Move all notes from selected books to a target book
    if (showMoveBooksDialog) {
        AlertDialog(
            onDismissRequest = { showMoveBooksDialog = false },
            containerColor = colors.surface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Move Notes to Book",
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Move $totalSelectedBookNoteCount ${if (totalSelectedBookNoteCount == 1) "note" else "notes"} from selected ${if (selectedBookIds.size == 1) "book" else "books"} into:",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    books.forEach { book ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.moveAllNotesFromNotebooks(selectedBookIds, book.id, book.name)
                                    showMoveBooksDialog = false
                                    exitBookSelection()
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                                .testTag("books_move_target_${book.id}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(book.colorHex))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = book.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMoveBooksDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    // Dialog: Delete selected books (multi-select)
    if (showDeleteBooksDialog) {
        val count = selectedBookIds.size
        AlertDialog(
            onDismissRequest = { showDeleteBooksDialog = false },
            containerColor = colors.surface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = if (count == 1) "Delete 1 Book?" else "Delete $count Books?",
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    text = if (totalSelectedBookNoteCount == 0) {
                        "The selected ${if (count == 1) "book" else "books"} will be permanently deleted."
                    } else {
                        "This will permanently delete the selected ${if (count == 1) "book" else "books"} and $totalSelectedBookNoteCount ${if (totalSelectedBookNoteCount == 1) "note" else "notes"} inside."
                    },
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBooks(selectedBookIds, deleteContainedNotes = true)
                        showDeleteBooksDialog = false
                        exitBookSelection()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    modifier = Modifier.testTag("books_delete_confirm_btn")
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteBooksDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    // Dialog: Move selected notes inside an opened Book to another Book
    if (showMoveNotesDialog) {
        AlertDialog(
            onDismissRequest = { showMoveNotesDialog = false },
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
                    books.forEach { book ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.moveNotesToNotebook(selectedNoteIds, book.id, book.name)
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
                                    .background(Color(book.colorHex))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = book.name,
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

    // Dialog: Delete selected notes inside an opened Book
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
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    modifier = Modifier.testTag("book_notes_delete_confirm_btn")
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

    // Set Lock PIN Dialog
    pendingSetPinAction?.let { action ->
        SetLockPinDialog(
            onPinSet = { pin ->
                viewModel.setLockPin(pin)
                pendingSetPinAction = null
                action()
            },
            onDismiss = { pendingSetPinAction = null }
        )
    }

    // Verify Lock PIN Dialog
    pendingVerifyPinTitle?.let { title ->
        VerifyLockPinDialog(
            title = title,
            subtitle = pendingVerifyPinSubtitle,
            confirmLabel = pendingVerifyPinConfirmLabel,
            onVerify = { pin -> viewModel.verifyLockPin(pin) },
            onSuccess = {
                val action = pendingVerifyPinAction
                pendingVerifyPinTitle = null
                pendingVerifyPinAction = null
                action?.invoke()
            },
            onDismiss = {
                pendingVerifyPinTitle = null
                pendingVerifyPinAction = null
            }
        )
    }
}

@Composable
private fun OptionActionRow(
    icon: ImageVector,
    label: String,
    tint: Color,
    containerColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = tint
            )
        }
    }
}

@Composable
private fun NewBookRowCard(
    onClick: () -> Unit
) {
    val colors = TippaniTheme.colors

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("new_book_row_btn"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = colors.cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = if (colors.isDark) 0.dp else 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(colors.accentSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Book",
                    tint = colors.accent,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "New Book",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.accent
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Create a new book",
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
            }
        }
    }
}
