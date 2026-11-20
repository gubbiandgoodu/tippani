package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TippaniTheme

enum class DirectorySortOption(val label: String) {
    A_Z("Alphabetically (A-Z)"),
    COUNT("Count")
}

enum class NotesSortOption(val label: String) {
    NEWEST("Newest to Oldest"),
    OLDEST("Oldest to Newest"),
    A_Z("Alphabetically (A-Z)")
}

data class SortMenuOption(
    val id: String,
    val label: String
)

@Composable
fun TippaniTopHeader(
    title: String,
    subtitle: String? = null,
    isSearchActive: Boolean,
    onToggleSearch: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onOpenSettings: () -> Unit,
    isSelectionMode: Boolean = false,
    onToggleSelect: (() -> Unit)? = null,
    sortOptions: List<SortMenuOption> = emptyList(),
    selectedSortId: String? = null,
    onSortSelected: ((String) -> Unit)? = null,
    backLabel: String? = null,
    onBackClick: (() -> Unit)? = null
) {
    val colors = TippaniTheme.colors
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (backLabel != null && onBackClick != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onBackClick)
                    .padding(end = 8.dp, top = 4.dp, bottom = 6.dp)
                    .testTag("top_header_back_btn"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = backLabel,
                    tint = colors.accent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = backLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.accent
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Normal,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                // 1. Search icon
                IconButton(
                    onClick = onToggleSearch,
                    modifier = Modifier.testTag("top_search_btn")
                ) {
                    Icon(
                        imageVector = if (isSearchActive) Icons.Filled.Search else Icons.Outlined.Search,
                        contentDescription = "Search",
                        tint = if (isSearchActive) colors.accent else colors.textPrimary,
                        modifier = Modifier.size(23.dp)
                    )
                }

                // 2. Select icon (beside Search)
                if (onToggleSelect != null) {
                    IconButton(
                        onClick = onToggleSelect,
                        modifier = Modifier.testTag("top_select_btn")
                    ) {
                        Icon(
                            imageVector = if (isSelectionMode) Icons.Filled.CheckBox else Icons.Outlined.CheckBox,
                            contentDescription = if (isSelectionMode) "Exit selection" else "Select items",
                            tint = if (isSelectionMode) colors.accent else colors.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // 3. Sort icon (beside Select)
                if (sortOptions.isNotEmpty() && onSortSelected != null) {
                    Box {
                        IconButton(
                            onClick = { sortMenuExpanded = true },
                            modifier = Modifier.testTag("top_sort_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(23.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false },
                            containerColor = colors.surface
                        ) {
                            sortOptions.forEach { option ->
                                val isSelected = option.id == selectedSortId
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option.label,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (isSelected) colors.accent else colors.textPrimary
                                        )
                                    },
                                    trailingIcon = if (isSelected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = colors.accent,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    } else null,
                                    onClick = {
                                        onSortSelected(option.id)
                                        sortMenuExpanded = false
                                    },
                                    modifier = Modifier.testTag("top_sort_option_${option.id.lowercase()}")
                                )
                            }
                        }
                    }
                }

                // 4. Settings icon on the far top right
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("top_settings_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(23.dp)
                    )
                }
            }
        }

        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (isSearchActive) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search...", fontSize = 14.sp, color = colors.textMuted) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("top_search_input"),
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    cursorColor = colors.accent,
                    focusedIndicatorColor = colors.accent,
                    unfocusedIndicatorColor = colors.border
                ),
                singleLine = true,
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = colors.textMuted)
                }
            )
        }
    }
}
