package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TippaniTheme

@Composable
fun TippaniBottomNavigation(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = TippaniTheme.colors

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("bottom_nav_bar"),
        color = colors.surface,
        shadowElevation = if (colors.isDark) 0.dp else 8.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (colors.isDark) {
                HorizontalDivider(
                    thickness = 0.75.dp,
                    color = colors.border,
                    modifier = Modifier.height(0.75.dp)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 12.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tab 0: Notes
                BottomNavItem(
                    label = "Notes",
                    isSelected = selectedTab == 0,
                    selectedIcon = Icons.Filled.Description,
                    unselectedIcon = Icons.Outlined.Description,
                    onClick = { onTabSelected(0) },
                    testTag = "nav_tab_notes"
                )

                // Tab 1: Books
                BottomNavItem(
                    label = "Books",
                    isSelected = selectedTab == 1,
                    selectedIcon = Icons.Filled.MenuBook,
                    unselectedIcon = Icons.Outlined.MenuBook,
                    onClick = { onTabSelected(1) },
                    testTag = "nav_tab_books"
                )

                // Tab 2: Tags (Exclusive tags exploration page)
                BottomNavItem(
                    label = "Tags",
                    isSelected = selectedTab == 2,
                    selectedIcon = Icons.Filled.LocalOffer,
                    unselectedIcon = Icons.Outlined.LocalOffer,
                    onClick = { onTabSelected(2) },
                    testTag = "nav_tab_tags"
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    isSelected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    val colors = TippaniTheme.colors

    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(width = 64.dp, height = 32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = selectedIcon,
                    contentDescription = label,
                    tint = colors.onAccent,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(width = 64.dp, height = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = unselectedIcon,
                    contentDescription = label,
                    tint = colors.textMuted,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (isSelected) colors.textPrimary else colors.textMuted,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
