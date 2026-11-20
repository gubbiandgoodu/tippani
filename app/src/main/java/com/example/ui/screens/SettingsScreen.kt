package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppAppearanceMode
import com.example.data.AppThemeMode
import com.example.data.LocalBackupHelper
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.DangerRed
import com.example.ui.theme.TippaniTheme
import com.example.viewmodel.TippaniViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: TippaniViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val colors = TippaniTheme.colors
    val currentAppearance by viewModel.currentAppearanceMode.collectAsState()
    val currentTheme by viewModel.currentThemeMode.collectAsState()

    val hasBackupFolder by viewModel.hasBackupFolder.collectAsState()
    val backupFolderName by viewModel.backupFolderName.collectAsState()
    val lastBackupTime by viewModel.lastBackupTime.collectAsState()
    val promptBackupOnOpen by viewModel.promptBackupOnOpen.collectAsState()

    var showAppearanceDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var restoredNotesCount by remember { mutableStateOf<Int?>(null) }

    // SAF Folder Picker (OpenDocumentTree)
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            val appCtx = context.applicationContext
            viewModel.onBackupFolderSelected(appCtx, treeUri) { _, message ->
                Toast.makeText(appCtx, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    // SAF Manual Export (CreateDocument)
    val exportDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val appCtx = context.applicationContext
            viewModel.exportBackupToUri(appCtx, uri) { _, message ->
                Toast.makeText(appCtx, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    // SAF Restore (OpenDocument)
    val restoreDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val appCtx = context.applicationContext
            viewModel.restoreBackupFromUri(appCtx, uri) { success, count, message ->
                if (success) {
                    restoredNotesCount = count
                } else {
                    Toast.makeText(appCtx, "Restore failed: $message", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("settings_back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        },
        containerColor = colors.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 36.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))

                // 1. Unified Backup & Restore Section Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Backup & Restore",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary
                    )

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = colors.greenBadgeBg
                    ) {
                        Text(
                            text = "Daily Default",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentGreen,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Main Backup Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = colors.cardBorder,
                    elevation = CardDefaults.cardElevation(defaultElevation = if (colors.isDark) 0.dp else 0.5.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        // 1) Clickable Backup Folder Row (launches OpenDocumentTree)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { folderPickerLauncher.launch(null) }
                                .padding(vertical = 4.dp)
                                .testTag("backup_folder_row"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (hasBackupFolder) colors.softIconBg(AccentGreen)
                                            else colors.accentSoft
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = "Backup Folder",
                                        tint = if (hasBackupFolder) AccentGreen else colors.accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Backup Folder",
                                        fontSize = 12.sp,
                                        color = colors.textSecondary
                                    )
                                    Text(
                                        text = backupFolderName ?: "No folder selected",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (hasBackupFolder) colors.textPrimary else colors.textMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            TextButton(
                                onClick = { folderPickerLauncher.launch(null) },
                                modifier = Modifier.testTag("change_backup_folder_btn")
                            ) {
                                Text(
                                    text = if (hasBackupFolder) "Change" else "Choose",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.accent
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 2) Last Backup Status Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Last Backup:",
                                fontSize = 13.sp,
                                color = colors.textMuted
                            )
                            Text(
                                text = lastBackupTime ?: "Pending next auto-backup",
                                fontSize = 13.sp,
                                color = if (lastBackupTime != null) AccentGreen else colors.textSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 3) Three Side-by-Side Equal-Width Action Buttons:
                        //    Backup (#10B981), Export (#2563EB), Restore (#7C3AED)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (hasBackupFolder) {
                                        val appCtx = context.applicationContext
                                        viewModel.performLocalBackup(appCtx) { success, result ->
                                            val msg = if (success) {
                                                "Backup saved: $result"
                                            } else {
                                                "Backup failed: $result"
                                            }
                                            Toast.makeText(appCtx, msg, Toast.LENGTH_LONG).show()
                                        }
                                    } else {
                                        folderPickerLauncher.launch(null)
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AccentGreen,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("action_backup_btn"),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = "Backup",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Backup",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }

                            Button(
                                onClick = {
                                    exportDocumentLauncher.launch(LocalBackupHelper.defaultBackupFileName())
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AccentBlue,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("action_export_btn"),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Export",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Export",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }

                            Button(
                                onClick = {
                                    restoreDocumentLauncher.launch(arrayOf("application/json", "text/*"))
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AccentPurple,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("action_restore_btn"),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = "Restore",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Restore",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Separate "Prompt backup on app open" Settings Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = colors.cardBorder,
                    elevation = CardDefaults.cardElevation(defaultElevation = if (colors.isDark) 0.dp else 0.5.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(colors.softIconBg(AccentGreen)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Backup reminder",
                                tint = AccentGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Prompt backup on app open",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Show a reminder dialog to back up whenever the app is opened",
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                lineHeight = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Switch(
                            checked = promptBackupOnOpen,
                            onCheckedChange = { enabled ->
                                viewModel.setPromptBackupOnOpen(enabled)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.onAccent,
                                checkedTrackColor = colors.accent,
                                uncheckedThumbColor = colors.textMuted,
                                uncheckedTrackColor = colors.surfaceVariant,
                                uncheckedBorderColor = colors.border
                            ),
                            modifier = Modifier.testTag("prompt_backup_switch")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 2. Appearance Section Header
                Text(
                    text = "Appearance",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = colors.cardBorder,
                    elevation = CardDefaults.cardElevation(defaultElevation = if (colors.isDark) 0.dp else 0.5.dp)
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        // App Theme Row (Light / Dark / Black AMOLED / System)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAppearanceDialog = true }
                                .padding(horizontal = 18.dp, vertical = 14.dp)
                                .testTag("app_theme_mode_row"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(colors.accentSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (currentAppearance) {
                                        AppAppearanceMode.LIGHT -> Icons.Default.LightMode
                                        AppAppearanceMode.DARK -> Icons.Default.DarkMode
                                        AppAppearanceMode.BLACK -> Icons.Default.DarkMode
                                        AppAppearanceMode.SYSTEM -> Icons.Default.Contrast
                                    },
                                    contentDescription = "App Theme",
                                    tint = colors.accent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "App Theme",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${currentAppearance.title} • ${currentAppearance.subtitle}",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                            }

                            AppearancePreviewBadge(mode = currentAppearance)
                        }

                        // Theme Accent Color Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showThemeDialog = true }
                                .padding(horizontal = 18.dp, vertical = 14.dp)
                                .testTag("theme_color_row"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(colors.accentSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Theme Accent",
                                    tint = colors.accent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Theme Accent",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${currentTheme.title} (Tap to change)",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(colors.accent)
                                    .border(1.5.dp, colors.border, CircleShape)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 4. Data Management Section
                Text(
                    text = "Data Management",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = colors.cardBorder,
                    elevation = CardDefaults.cardElevation(defaultElevation = if (colors.isDark) 0.dp else 0.5.dp)
                ) {
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        // Load Sample Data
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.loadSampleData() }
                                .padding(horizontal = 18.dp, vertical = 14.dp)
                                .testTag("load_samples_row"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(colors.softIconBg(AccentGreen)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = AccentGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Load Sample Notes",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Populate realistic sample notes to explore layouts",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        // Clear All Notes
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showClearConfirmDialog = true }
                                .padding(horizontal = 18.dp, vertical = 14.dp)
                                .testTag("clear_all_row"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(colors.softIconBg(DangerRed)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = DangerRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Clear All Notes",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = DangerRed
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Permanently remove all notes and journals",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 5. Privacy Section Header
                Text(
                    text = "Privacy",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = colors.cardBorder,
                    elevation = CardDefaults.cardElevation(defaultElevation = if (colors.isDark) 0.dp else 0.5.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(colors.softIconBg(AccentGreen)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = AccentGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "100% On-Device & Private",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Your notes and thoughts stay strictly on your device and inside your chosen backup folder. No third-party servers.",
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 6. Developer Section Header
                Text(
                    text = "Developer",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = colors.cardBorder,
                    elevation = CardDefaults.cardElevation(defaultElevation = if (colors.isDark) 0.dp else 0.5.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "Tippani Notes Studio",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Minimal Local Architecture",
                                fontSize = 12.sp,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }

    // Restore Confirmation Dialog
    restoredNotesCount?.let { count ->
        AlertDialog(
            onDismissRequest = { restoredNotesCount = null },
            modifier = Modifier.testTag("restore_confirmation_dialog"),
            containerColor = colors.surface,
            shape = RoundedCornerShape(24.dp),
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Restore Complete",
                    tint = AccentGreen,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Restore Complete",
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
            },
            text = {
                val label = if (count == 1) "1 note was" else "$count notes were"
                Text(
                    text = "$label restored from your backup file and added to Tippani.",
                    fontSize = 14.sp,
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { restoredNotesCount = null },
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.onAccent
                    ),
                    modifier = Modifier.testTag("restore_confirm_ok_btn")
                ) {
                    Text("Done")
                }
            }
        )
    }

    // App Theme Chooser Dialog (Light, Dark, Black AMOLED, System Default)
    if (showAppearanceDialog) {
        AlertDialog(
            onDismissRequest = { showAppearanceDialog = false },
            modifier = Modifier.testTag("app_theme_dialog"),
            containerColor = colors.surface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Choose App Theme",
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppAppearanceMode.entries.forEach { mode ->
                        val isSelected = mode == currentAppearance
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) colors.accentSoft else colors.surfaceVariant,
                            border = if (isSelected) BorderStroke(1.5.dp, colors.accent) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    viewModel.setAppearanceMode(mode)
                                    showAppearanceDialog = false
                                }
                                .testTag("theme_option_${mode.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppearancePreviewBadge(mode = mode)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mode.title,
                                        fontSize = 15.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = mode.subtitle,
                                        fontSize = 12.sp,
                                        color = colors.textSecondary
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = colors.accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAppearanceDialog = false }) {
                    Text("Close", color = colors.accent)
                }
            }
        )
    }

    // Theme Accent Chooser Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            modifier = Modifier.testTag("theme_accent_dialog"),
            containerColor = colors.surface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Choose Theme Accent",
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppThemeMode.entries.forEach { mode ->
                        val isSelected = mode == currentTheme
                        val swatchColor = Color(if (colors.isDark) mode.darkColorHex else mode.colorHex)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) colors.accentSoft else colors.surfaceVariant,
                            border = if (isSelected) BorderStroke(1.5.dp, colors.accent) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .testTag("accent_option_${mode.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(swatchColor)
                                        .border(1.dp, colors.border, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = mode.title,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = colors.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = colors.accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Close", color = colors.accent)
                }
            }
        )
    }

    // Clear All Confirmation Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            containerColor = colors.surface,
            title = { Text("Clear All Notes?", color = colors.textPrimary) },
            text = {
                Text(
                    text = "This action will permanently delete all active and archived notes. Make sure you have created a backup.",
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllNotes()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Clear All", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun AppearancePreviewBadge(mode: AppAppearanceMode) {
    val borderColor = TippaniTheme.colors.border
    when (mode) {
        AppAppearanceMode.LIGHT -> {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF8FAFC))
                    .border(1.5.dp, Color(0xFFCBD5E1), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F172A))
                )
            }
        }
        AppAppearanceMode.DARK -> {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF18202C))
                    .border(1.5.dp, Color(0xFF475569), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF94A3B8))
                )
            }
        }
        AppAppearanceMode.BLACK -> {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF000000))
                    .border(1.5.dp, Color(0xFF3F3F46), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF8FAFC))
                )
            }
        }
        AppAppearanceMode.SYSTEM -> {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F172A))
                    .border(1.5.dp, borderColor, CircleShape),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 13.dp, height = 26.dp)
                        .background(Color(0xFFF8FAFC))
                )
            }
        }
    }
}
