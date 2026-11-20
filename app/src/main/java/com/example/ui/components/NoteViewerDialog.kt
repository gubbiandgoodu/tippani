package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AVAILABLE_NOTE_FORMATS
import com.example.model.ChecklistItem
import com.example.model.ChecklistUtils
import com.example.model.NOTE_FORMAT_CHECKLIST
import com.example.model.NOTE_FORMAT_NOTES
import com.example.model.Note
import com.example.model.NotebookCategory
import com.example.model.TagUtils
import com.example.model.normalizeNoteFormat
import com.example.ui.theme.DangerRed
import com.example.ui.theme.TippaniTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteViewerDialog(
    initialNote: Note?,
    categories: List<NotebookCategory>,
    isLocked: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, categoryId: String, noteType: String) -> Unit,
    onQuickChecklistChange: ((Note, String) -> Unit)? = null,
    onTogglePin: ((Note) -> Unit)? = null,
    onToggleLock: ((Note) -> Unit)? = null,
    onDelete: ((Note) -> Unit)? = null
) {
    val context = LocalContext.current
    val colors = TippaniTheme.colors
    val isNewNote = initialNote == null || initialNote.id == 0L

    // If new note: starts in edit mode. If opening existing note: starts in read-only mode.
    var isEditMode by remember { mutableStateOf(isNewNote) }

    var title by remember { mutableStateOf(initialNote?.title ?: "") }
    var content by remember { mutableStateOf(initialNote?.content ?: "") }
    var selectedCategoryId by remember { mutableStateOf(initialNote?.categoryId ?: "personal") }
    var selectedType by remember {
        mutableStateOf(normalizeNoteFormat(initialNote?.noteType ?: NOTE_FORMAT_NOTES))
    }

    // Checklist state (synchronized when format is Checklist)
    var checklistItems by remember {
        mutableStateOf(
            if (normalizeNoteFormat(initialNote?.noteType) == NOTE_FORMAT_CHECKLIST) {
                ChecklistUtils.parse(initialNote?.content ?: "")
            } else {
                emptyList()
            }
        )
    }
    var nextChecklistId by remember {
        mutableLongStateOf((checklistItems.maxOfOrNull { it.id } ?: 0L) + 1L)
    }
    var newItemInput by remember { mutableStateOf("") }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    val currentCategory = categories.find { it.id == selectedCategoryId }
        ?: categories.firstOrNull()
        ?: NotebookCategory("personal", "Personal", "person", 0xFF6366F1)

    val formattedDate = remember(initialNote) {
        val ts = initialNote?.updatedAt ?: System.currentTimeMillis()
        val sdf = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault())
        sdf.format(Date(ts))
    }

    val isChecklistFormat = selectedType == NOTE_FORMAT_CHECKLIST

    // Live auto-detected hashtags (only for Notes format, never for Checklists)
    val autoDetectedTags = remember(title, content, selectedType) {
        if (selectedType == NOTE_FORMAT_NOTES) {
            TagUtils.extractHashtags(title, content)
        } else {
            emptyList()
        }
    }

    // In read-only view, include auto-detected hashtags (plus legacy tags if present on old Notes)
    val readOnlyDisplayTags = remember(initialNote, title, content, selectedType) {
        if (selectedType != NOTE_FORMAT_NOTES) {
            emptyList()
        } else {
            val detected = TagUtils.extractHashtags(title, content)
            if (detected.isNotEmpty() || initialNote == null) {
                detected
            } else {
                TagUtils.getTagsForNote(initialNote)
            }
        }
    }

    fun addNewChecklistItem() {
        val trimmed = newItemInput.trim()
        if (trimmed.isNotEmpty()) {
            checklistItems = checklistItems + ChecklistItem(
                id = nextChecklistId++,
                text = trimmed,
                isChecked = false
            )
            newItemInput = ""
        }
    }

    fun buildContentForSave(): String {
        return if (isChecklistFormat) {
            val allItems = if (newItemInput.trim().isNotEmpty()) {
                checklistItems + ChecklistItem(
                    id = nextChecklistId++,
                    text = newItemInput.trim(),
                    isChecked = false
                )
            } else {
                checklistItems
            }
            ChecklistUtils.serialize(allItems)
        } else {
            content
        }
    }

    fun switchFormat(targetFormat: String) {
        val normalized = normalizeNoteFormat(targetFormat)
        if (normalized == selectedType) return
        if (normalized == NOTE_FORMAT_CHECKLIST) {
            val parsed = ChecklistUtils.parse(content)
            checklistItems = parsed
            nextChecklistId = (parsed.maxOfOrNull { it.id } ?: 0L) + 1L
            newItemInput = ""
        } else {
            val combined = if (newItemInput.trim().isNotEmpty()) {
                checklistItems + ChecklistItem(
                    id = nextChecklistId++,
                    text = newItemInput.trim(),
                    isChecked = false
                )
            } else {
                checklistItems
            }
            content = ChecklistUtils.toPlainText(combined)
            newItemInput = ""
        }
        selectedType = normalized
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isNewNote) {
                            if (isChecklistFormat) "New Checklist" else "New Note"
                        } else if (isEditMode) {
                            if (isChecklistFormat) "Edit Checklist" else "Edit Note"
                        } else {
                            ""
                        },
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isEditMode && !isNewNote) {
                                // Revert back to read-only view
                                isEditMode = false
                            } else {
                                onDismiss()
                            }
                        },
                        modifier = Modifier.testTag("note_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary
                        )
                    }
                },
                actions = {
                    if (!isEditMode) {
                        // READ ONLY MODE: Pin, Lock, Edit, Copy, Delete at top right
                        if (initialNote != null && initialNote.id > 0 && onTogglePin != null) {
                            IconButton(
                                onClick = { onTogglePin(initialNote) },
                                modifier = Modifier.testTag("note_pin_btn")
                            ) {
                                Icon(
                                    imageVector = if (initialNote.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                    contentDescription = if (initialNote.isPinned) "Unpin Note" else "Pin Note",
                                    tint = if (initialNote.isPinned) colors.accent else colors.textPrimary
                                )
                            }
                        }

                        if (initialNote != null && initialNote.id > 0 && onToggleLock != null) {
                            IconButton(
                                onClick = { onToggleLock(initialNote) },
                                modifier = Modifier.testTag("note_lock_btn")
                            ) {
                                Icon(
                                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = if (isLocked) "Unlock Note" else "Lock Note",
                                    tint = if (isLocked) colors.accent else colors.textPrimary
                                )
                            }
                        }

                        IconButton(
                            onClick = { isEditMode = true },
                            modifier = Modifier.testTag("note_edit_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Note",
                                tint = colors.textPrimary
                            )
                        }

                        IconButton(
                            onClick = {
                                val bodyForCopy = if (isChecklistFormat) {
                                    ChecklistUtils.formatForCopy(checklistItems)
                                } else {
                                    content
                                }
                                val textToCopy = buildString {
                                    if (title.isNotBlank()) {
                                        append(title)
                                        if (bodyForCopy.isNotBlank()) {
                                            append("\n\n")
                                        }
                                    }
                                    append(bodyForCopy)
                                }
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Tippani Note", textToCopy)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Note copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("note_copy_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Note",
                                tint = colors.textPrimary
                            )
                        }

                        if (initialNote != null && initialNote.id > 0 && onDelete != null) {
                            IconButton(onClick = { showDeleteConfirm = true }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Note",
                                    tint = colors.textSecondary
                                )
                            }
                        }
                    } else {
                        // EDIT MODE: Delete (if existing) and Save checkmark
                        if (initialNote != null && initialNote.id > 0 && onDelete != null) {
                            IconButton(onClick = { showDeleteConfirm = true }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Note",
                                    tint = DangerRed
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                val finalContent = buildContentForSave()
                                onSave(title, finalContent, selectedCategoryId, selectedType)
                            },
                            modifier = Modifier.testTag("save_note_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Save Note",
                                tint = colors.accent
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        },
        containerColor = colors.background
    ) { paddingValues ->
        if (!isEditMode) {
            // ==========================================
            // READ ONLY VIEW
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // Title
                Text(
                    text = title.ifBlank { "Untitled Note" },
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    lineHeight = 34.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Metadata Chips: Notebook + Format
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.surface,
                        border = colors.cardBorder,
                        shadowElevation = if (colors.isDark) 0.dp else 0.5.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(currentCategory.colorHex))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentCategory.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.surface,
                        border = colors.cardBorder,
                        shadowElevation = if (colors.isDark) 0.dp else 0.5.dp
                    ) {
                        Text(
                            text = selectedType,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = formattedDate,
                    fontSize = 12.sp,
                    color = colors.textMuted
                )

                // Auto-detected tags shown ONLY for Notes (never for Checklists)
                if (!isChecklistFormat && readOnlyDisplayTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        readOnlyDisplayTags.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.surfaceVariant
                            ) {
                                Text(
                                    text = "#$tag",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (isChecklistFormat) {
                    val activeItems = checklistItems.filter { !it.isChecked }
                    val completedItems = checklistItems.filter { it.isChecked }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = colors.surface,
                        border = colors.cardBorder,
                        shadowElevation = if (colors.isDark) 0.dp else 0.5.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("readonly_checklist_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 14.dp)
                        ) {
                            if (checklistItems.isEmpty()) {
                                Text(
                                    text = "(No checklist items)",
                                    fontSize = 15.sp,
                                    color = colors.textMuted,
                                    modifier = Modifier.padding(8.dp)
                                )
                            } else {
                                activeItems.forEach { item ->
                                    ReadOnlyChecklistRow(
                                        item = item,
                                        onToggle = {
                                            val updated = checklistItems.map {
                                                if (it.id == item.id) it.copy(isChecked = !it.isChecked) else it
                                            }
                                            checklistItems = updated
                                            val serialized = ChecklistUtils.serialize(updated)
                                            content = serialized
                                            if (initialNote != null && initialNote.id > 0L) {
                                                onQuickChecklistChange?.invoke(initialNote, serialized)
                                            }
                                        }
                                    )
                                }

                                if (completedItems.isNotEmpty()) {
                                    if (activeItems.isNotEmpty()) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                            thickness = 0.75.dp,
                                            color = colors.border
                                        )
                                    }

                                    Text(
                                        text = "Completed (${completedItems.size})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.textMuted,
                                        modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
                                    )

                                    completedItems.forEach { item ->
                                        ReadOnlyChecklistRow(
                                            item = item,
                                            onToggle = {
                                                val updated = checklistItems.map {
                                                    if (it.id == item.id) it.copy(isChecked = !it.isChecked) else it
                                                }
                                                checklistItems = updated
                                                val serialized = ChecklistUtils.serialize(updated)
                                                content = serialized
                                                if (initialNote != null && initialNote.id > 0L) {
                                                    onQuickChecklistChange?.invoke(initialNote, serialized)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Content body in read-only Notes mode
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = colors.surface,
                        border = colors.cardBorder,
                        shadowElevation = if (colors.isDark) 0.dp else 0.5.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = content.ifBlank { "(No content)" },
                            fontSize = 16.sp,
                            color = colors.textPrimary,
                            lineHeight = 24.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        } else {
            // ==========================================
            // EDIT / CREATE MODE
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                // Category & Format Pickers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var categoryExpanded by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.surface,
                            border = colors.cardBorder,
                            shadowElevation = if (colors.isDark) 0.dp else 0.5.dp,
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("editor_notebook_picker")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Color(currentCategory.colorHex))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = currentCategory.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.textPrimary
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
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false },
                            containerColor = colors.surface
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name, color = colors.textPrimary) },
                                    onClick = {
                                        selectedCategoryId = cat.id
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    var typeExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = typeExpanded,
                        onExpandedChange = { typeExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.surface,
                            border = colors.cardBorder,
                            shadowElevation = if (colors.isDark) 0.dp else 0.5.dp,
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("editor_format_picker")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = selectedType,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        ExposedDropdownMenu(
                            expanded = typeExpanded,
                            onDismissRequest = { typeExpanded = false },
                            containerColor = colors.surface
                        ) {
                            AVAILABLE_NOTE_FORMATS.forEach { format ->
                                DropdownMenuItem(
                                    text = { Text(format, color = colors.textPrimary) },
                                    onClick = {
                                        switchFormat(format)
                                        typeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val inputColors = TextFieldDefaults.colors(
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    cursorColor = colors.accent,
                    focusedIndicatorColor = colors.accent,
                    unfocusedIndicatorColor = colors.border
                )

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = {
                        Text(
                            text = if (isChecklistFormat) "Checklist title..." else "Note title...",
                            color = colors.textMuted
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_title_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = inputColors,
                    singleLine = true
                )

                // Live auto-detected tags preview (ONLY for Notes when #tags are typed; no manual tags input field)
                if (!isChecklistFormat && autoDetectedTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .testTag("auto_detected_tags_row"),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        autoDetectedTags.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.accentSoft
                            ) {
                                Text(
                                    text = "#$tag",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.accent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (isChecklistFormat) {
                    // ==========================================
                    // INTERACTIVE CHECKLIST EDITOR
                    // ==========================================
                    val activeItems = checklistItems.filter { !it.isChecked }
                    val completedItems = checklistItems.filter { it.isChecked }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.surface,
                        border = colors.cardBorder,
                        shadowElevation = if (colors.isDark) 0.dp else 0.5.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("checklist_editor_container")
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // 1) Active (Unchecked) Checklist Items
                            items(activeItems, key = { it.id }) { item ->
                                EditableChecklistRow(
                                    item = item,
                                    onCheckedChange = { checked ->
                                        checklistItems = checklistItems.map {
                                            if (it.id == item.id) it.copy(isChecked = checked) else it
                                        }
                                    },
                                    onTextChange = { updatedText ->
                                        checklistItems = checklistItems.map {
                                            if (it.id == item.id) it.copy(text = updatedText) else it
                                        }
                                    },
                                    onRemove = {
                                        checklistItems = checklistItems.filterNot { it.id == item.id }
                                    }
                                )
                            }

                            // 2) Add New Checklist Item Input Row
                            item(key = "add_checklist_item_row") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { addNewChecklistItem() },
                                        modifier = Modifier.testTag("checklist_add_item_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add checklist item",
                                            tint = colors.accent,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (newItemInput.isEmpty()) {
                                            Text(
                                                text = "Add checklist item...",
                                                fontSize = 15.sp,
                                                color = colors.textMuted
                                            )
                                        }
                                        BasicTextField(
                                            value = newItemInput,
                                            onValueChange = { newItemInput = it },
                                            singleLine = true,
                                            textStyle = TextStyle(
                                                fontSize = 15.sp,
                                                color = colors.textPrimary
                                            ),
                                            cursorBrush = SolidColor(colors.accent),
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                            keyboardActions = KeyboardActions(
                                                onDone = { addNewChecklistItem() }
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("checklist_new_item_input")
                                        )
                                    }

                                    if (newItemInput.isNotBlank()) {
                                        TextButton(
                                            onClick = { addNewChecklistItem() },
                                            modifier = Modifier.testTag("checklist_confirm_add_btn")
                                        ) {
                                            Text(
                                                text = "Add",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.accent
                                            )
                                        }
                                    }
                                }
                            }

                            // 3) Completed Items Section at the Bottom (greyed out + strikethrough)
                            if (completedItems.isNotEmpty()) {
                                item(key = "completed_divider_header") {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp, bottom = 4.dp)
                                    ) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 8.dp),
                                            thickness = 0.75.dp,
                                            color = colors.border
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "Completed (${completedItems.size})",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.textMuted,
                                            modifier = Modifier
                                                .padding(horizontal = 12.dp)
                                                .testTag("checklist_completed_header")
                                        )
                                    }
                                }

                                items(completedItems, key = { it.id }) { item ->
                                    EditableChecklistRow(
                                        item = item,
                                        onCheckedChange = { checked ->
                                            checklistItems = checklistItems.map {
                                                if (it.id == item.id) it.copy(isChecked = checked) else it
                                            }
                                        },
                                        onTextChange = { updatedText ->
                                            checklistItems = checklistItems.map {
                                                if (it.id == item.id) it.copy(text = updatedText) else it
                                            }
                                        },
                                        onRemove = {
                                            checklistItems = checklistItems.filterNot { it.id == item.id }
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Standard Notes Body Input (auto-detects #tags like #imp)
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        placeholder = {
                            Text(
                                text = "Write your note... Use #tags like #imp to organize",
                                color = colors.textMuted
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("note_content_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = inputColors
                    )
                }
            }
        }
    }

    // Delete Confirmation
    if (showDeleteConfirm && initialNote != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = colors.surface,
            title = { Text("Delete Note?", color = colors.textPrimary) },
            text = { Text("Are you sure you want to delete this note?", color = colors.textSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(initialNote)
                        showDeleteConfirm = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun EditableChecklistRow(
    item: ChecklistItem,
    onCheckedChange: (Boolean) -> Unit,
    onTextChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    val colors = TippaniTheme.colors
    val textColor = if (item.isChecked) colors.textMuted else colors.textPrimary
    val textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .testTag("checklist_edit_row_${item.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = item.isChecked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = colors.textMuted,
                checkmarkColor = colors.surface,
                uncheckedColor = colors.textSecondary
            ),
            modifier = Modifier.testTag("checklist_checkbox_${item.id}")
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (item.text.isEmpty()) {
                Text(
                    text = "List item",
                    fontSize = 15.sp,
                    color = colors.textMuted
                )
            }
            BasicTextField(
                value = item.text,
                onValueChange = onTextChange,
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    color = textColor,
                    textDecoration = textDecoration
                ),
                cursorBrush = SolidColor(colors.accent),
                modifier = Modifier.fillMaxWidth()
            )
        }

        IconButton(
            onClick = onRemove,
            modifier = Modifier.testTag("checklist_remove_${item.id}")
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove item",
                tint = colors.textMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ReadOnlyChecklistRow(
    item: ChecklistItem,
    onToggle: () -> Unit
) {
    val colors = TippaniTheme.colors
    val textColor = if (item.isChecked) colors.textMuted else colors.textPrimary
    val textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .testTag("checklist_view_row_${item.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = item.isChecked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = colors.textMuted,
                checkmarkColor = colors.surface,
                uncheckedColor = colors.textSecondary
            )
        )

        Text(
            text = item.text,
            fontSize = 15.sp,
            color = textColor,
            textDecoration = textDecoration,
            lineHeight = 22.sp,
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp, top = 6.dp, bottom = 6.dp)
        )
    }
}
