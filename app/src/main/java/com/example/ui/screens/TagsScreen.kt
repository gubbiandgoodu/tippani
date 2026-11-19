package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Note
import com.example.model.TagInfo
import com.example.ui.components.NoteItemCard
import com.example.ui.components.TagListItem
import com.example.ui.components.TippaniTopHeader
import com.example.ui.theme.DarkNavActive
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.TippaniViewModel

@Composable
fun TagsScreen(
    viewModel: TippaniViewModel,
    onTagSelected: (String) -> Unit,
    onNoteClick: (Note) -> Unit,
    onOpenSettings: () -> Unit,
    onAddClick: () -> Unit
) {
    val activeNotes by viewModel.activeNotes.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var activeTagFilter by remember { mutableStateOf<String?>(null) } // null = view all tags

    val tagColors = listOf(
        0xFF6366F1, // Indigo
        0xFF0EA5E9, // Sky
        0xFFF59E0B, // Amber
        0xFF10B981, // Emerald
        0xFFEC4899, // Pink
        0xFF8B5CF6, // Purple
        0xFFF97316, // Orange
        0xFF14B8A6  // Teal
    )

    val allTagsList = remember(activeNotes) {
        val tagMap = mutableMapOf<String, MutableList<Note>>()
        activeNotes.forEach { note ->
            if (note.tags.isNotBlank()) {
                val splitTags = note.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                splitTags.forEach { t ->
                    tagMap.getOrPut(t) { mutableListOf() }.add(note)
                }
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

    val filteredTags = remember(allTagsList, searchQuery) {
        if (searchQuery.isBlank()) allTagsList
        else allTagsList.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    val notesForActiveTag = remember(activeNotes, activeTagFilter) {
        if (activeTagFilter == null) emptyList()
        else activeNotes.filter { n ->
            n.tags.split(",").map { it.trim() }.any { it.equals(activeTagFilter, ignoreCase = true) }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(18.dp))

            // Screen Header: Default list mode, no grid/icon view toggle button
            if (activeTagFilter != null) {
                // When viewing notes under a tag
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { activeTagFilter = null },
                        modifier = Modifier.testTag("tag_detail_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to all tags",
                            tint = TextPrimary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "#$activeTagFilter",
                            fontSize = 28.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${notesForActiveTag.size} notes tagged",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextPrimary
                        )
                    }
                }
            } else {
                // Standard Directory TopHeader
                TippaniTopHeader(
                    title = "Tags",
                    subtitle = "${allTagsList.size} tags • ${activeNotes.count { it.tags.isNotBlank() }} tagged notes",
                    isSearchActive = isSearchActive,
                    onToggleSearch = { isSearchActive = !isSearchActive },
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onOpenSettings = onOpenSettings
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Body: Default List mode for tags or tagged notes
            if (activeTagFilter != null) {
                if (notesForActiveTag.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No notes found with this tag", fontSize = 16.sp, color = TextSecondary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .testTag("tagged_notes_list"),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 90.dp)
                    ) {
                        items(notesForActiveTag, key = { it.id }) { note ->
                            val cat = categories.find { it.id == note.categoryId } ?: categories.first()
                            NoteItemCard(
                                note = note,
                                categoryName = cat.name,
                                categoryIcon = getCategoryIcon(cat.iconName),
                                iconTint = Color(cat.colorHex),
                                iconBgColor = getSoftBgForColor(cat.colorHex),
                                onClick = { onNoteClick(note) }
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
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Tag,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No tags found", fontSize = 16.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Add tags to notes (e.g. Work, Ideas, Travel) to browse them here",
                                fontSize = 12.sp,
                                color = TextMuted
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
                        contentPadding = PaddingValues(bottom = 90.dp)
                    ) {
                        items(filteredTags, key = { it.name }) { tag ->
                            TagListItem(
                                tag = tag,
                                onClick = { activeTagFilter = tag.name }
                            )
                        }
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onAddClick,
            shape = CircleShape,
            containerColor = DarkNavActive,
            contentColor = Color.White,
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
