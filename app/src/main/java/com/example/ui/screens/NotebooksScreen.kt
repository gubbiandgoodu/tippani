package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import com.example.model.NotebookCategory
import com.example.ui.components.NotebookListItem
import com.example.ui.components.TippaniTopHeader
import com.example.ui.theme.DarkNavActive
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.TippaniViewModel

@Composable
fun NotebooksScreen(
    viewModel: TippaniViewModel,
    onNotebookSelected: (NotebookCategory) -> Unit,
    onOpenSettings: () -> Unit,
    onAddClick: () -> Unit
) {
    val categories by viewModel.categories.collectAsState()
    val activeNotes by viewModel.activeNotes.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredCategories = remember(categories, searchQuery) {
        if (searchQuery.isBlank()) categories
        else categories.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    val notesByCategory = remember(activeNotes) {
        activeNotes.groupBy { it.categoryId }
    }

    val totalNotebooks = categories.size
    val totalNotes = activeNotes.size

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(18.dp))

            // Top Header: Title, Subtitle, Search icon, and Settings icon on top right
            TippaniTopHeader(
                title = "Notebooks",
                subtitle = "$totalNotebooks notebooks • $totalNotes notes",
                isSearchActive = isSearchActive,
                onToggleSearch = { isSearchActive = !isSearchActive },
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onOpenSettings = onOpenSettings
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredCategories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No notebooks found",
                        fontSize = 16.sp,
                        color = TextSecondary
                    )
                }
            } else {
                // List View as clean default
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .testTag("notebooks_list_view"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(filteredCategories, key = { it.id }) { cat ->
                        val notes = notesByCategory[cat.id] ?: emptyList()
                        NotebookListItem(
                            category = cat,
                            noteCount = notes.size,
                            onClick = { onNotebookSelected(cat) }
                        )
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
                .testTag("notebooks_add_fab")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "New Note",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
