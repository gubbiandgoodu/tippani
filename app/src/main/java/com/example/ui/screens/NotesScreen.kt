package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Note
import com.example.ui.components.NoteItemCard
import com.example.ui.components.TippaniTopHeader
import com.example.ui.theme.CardBackground
import com.example.ui.theme.DarkNavActive
import com.example.ui.theme.SoftIconBgAmber
import com.example.ui.theme.SoftIconBgBlue
import com.example.ui.theme.SoftIconBgGreen
import com.example.ui.theme.SoftIconBgPurple
import com.example.ui.theme.SoftIconBgSlate
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.TippaniViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun NotesScreen(
    viewModel: TippaniViewModel,
    onNoteClick: (Note) -> Unit,
    onOpenSettings: () -> Unit,
    onAddClick: () -> Unit
) {
    val filteredNotes by viewModel.filteredNotes.collectAsState()
    val allActiveNotes by viewModel.activeNotes.collectAsState()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsState()
    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }
    var showTypeFilterMenu by remember { mutableStateOf(false) }
    var showCategoryFilterMenu by remember { mutableStateOf(false) }

    val formatTypes = listOf("All Formats", "Quick Note", "Checklist", "Meeting Notes", "Daily Journal", "Study Summary", "Tech Architecture", "Recipe")

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(18.dp))

            // Screen Header: Notes + Notes count + Search + Top-right Settings Icon (view type removed)
            TippaniTopHeader(
                title = "Notes",
                subtitle = "${allActiveNotes.size} notes recorded",
                isSearchActive = isSearchActive,
                onToggleSearch = { isSearchActive = !isSearchActive },
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                onOpenSettings = onOpenSettings
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Filter Chips Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Filter 1: Format / Mode Selector
                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = CardBackground,
                        shadowElevation = 0.5.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTypeFilterMenu = true }
                            .testTag("filter_modes_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = selectedTypeFilter ?: "All Formats",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showTypeFilterMenu,
                        onDismissRequest = { showTypeFilterMenu = false }
                    ) {
                        formatTypes.forEach { typeName ->
                            DropdownMenuItem(
                                text = { Text(typeName) },
                                onClick = {
                                    viewModel.setTypeFilter(if (typeName == "All Formats") null else typeName)
                                    showTypeFilterMenu = false
                                }
                            )
                        }
                    }
                }

                // Filter 2: Notebooks / Categories Selector
                Box(modifier = Modifier.weight(1f)) {
                    val currentCatName = categories.find { it.id == selectedCategoryFilter }?.name ?: "All Notebooks"
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = CardBackground,
                        shadowElevation = 0.5.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCategoryFilterMenu = true }
                            .testTag("filter_categories_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = currentCatName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showCategoryFilterMenu,
                        onDismissRequest = { showCategoryFilterMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Notebooks") },
                            onClick = {
                                viewModel.setCategoryFilter(null)
                                showCategoryFilterMenu = false
                            }
                        )
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
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

            // Group notes by relative day (Today, Yesterday, Earlier)
            val groupedNotes = remember(filteredNotes) {
                groupNotesByDate(filteredNotes)
            }

            if (filteredNotes.isEmpty()) {
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
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap + to write a note or adjust filters",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                // List view as permanent clean default
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .testTag("notes_list_view"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    groupedNotes.forEach { (header, notesInGroup) ->
                        item {
                            DateSectionHeader(headerText = header)
                        }

                        items(notesInGroup, key = { it.id }) { note ->
                            val cat = categories.find { it.id == note.categoryId } ?: categories.first()
                            val icon = getCategoryIcon(cat.iconName)
                            val tint = Color(cat.colorHex)
                            val bg = getSoftBgForColor(cat.colorHex)

                            NoteItemCard(
                                note = note,
                                categoryName = cat.name,
                                categoryIcon = icon,
                                iconTint = tint,
                                iconBgColor = bg,
                                onClick = { onNoteClick(note) }
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

@Composable
private fun DateSectionHeader(headerText: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = headerText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
    }
}

private fun groupNotesByDate(notes: List<Note>): Map<String, List<Note>> {
    val cal = Calendar.getInstance()
    val todayCal = Calendar.getInstance()
    val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

    val fullDateSdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    val grouped = mutableMapOf<String, MutableList<Note>>()

    notes.forEach { note ->
        cal.timeInMillis = note.updatedAt
        val isToday = cal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
        val isYesterday = cal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR)

        val header = when {
            isToday -> "Today, ${fullDateSdf.format(Date(note.updatedAt))}"
            isYesterday -> "Yesterday, ${fullDateSdf.format(Date(note.updatedAt))}"
            else -> fullDateSdf.format(Date(note.updatedAt))
        }

        grouped.getOrPut(header) { mutableListOf() }.add(note)
    }

    return grouped
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
