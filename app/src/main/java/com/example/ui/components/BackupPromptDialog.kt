package com.example.ui.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LocalBackupHelper
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.CardBackground
import com.example.ui.theme.CleanBackground
import com.example.ui.theme.DarkNavActive
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.TippaniViewModel

@Composable
fun BackupPromptDialog(
    viewModel: TippaniViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val hasBackupFolder by viewModel.hasBackupFolder.collectAsState()
    val backupFolderName by viewModel.backupFolderName.collectAsState()
    val promptBackupOnOpen by viewModel.promptBackupOnOpen.collectAsState()

    val dontPromptChecked = !promptBackupOnOpen

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            val appCtx = context.applicationContext
            viewModel.onBackupFolderSelected(appCtx, treeUri) { _, message ->
                Toast.makeText(appCtx, message, Toast.LENGTH_LONG).show()
            }
            onDismiss()
        }
    }

    val exportDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val appCtx = context.applicationContext
            viewModel.exportBackupToUri(appCtx, uri) { _, message ->
                Toast.makeText(appCtx, message, Toast.LENGTH_LONG).show()
            }
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("backup_prompt_dialog"),
        containerColor = CardBackground,
        shape = RoundedCornerShape(28.dp),
        icon = {
            Icon(
                imageVector = Icons.Default.Storage,
                contentDescription = "Backup Notes",
                tint = TextPrimary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Backup Notes",
                fontSize = 21.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (hasBackupFolder) {
                    Text(
                        text = "Keep your notes safe even if the app is uninstalled. Would you like to create a new backup now?",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = CleanBackground,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = "Saved Folder",
                                tint = AccentGreen,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Saved Folder",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                                Text(
                                    text = backupFolderName ?: "Selected Folder",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Pick a folder on your phone for automatic daily backups that survive app uninstalls, or export an offline backup file now.",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = {
                            exportDocumentLauncher.launch(LocalBackupHelper.defaultBackupFileName())
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("backup_dialog_export_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Export Backup JSON File",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            val newChecked = !dontPromptChecked
                            viewModel.setPromptBackupOnOpen(!newChecked)
                        }
                        .padding(vertical = 4.dp)
                        .testTag("backup_dialog_dont_prompt_row"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = dontPromptChecked,
                        onCheckedChange = { checked ->
                            viewModel.setPromptBackupOnOpen(!checked)
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = DarkNavActive,
                            checkmarkColor = Color.White
                        ),
                        modifier = Modifier.testTag("backup_dialog_dont_prompt_checkbox")
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Don't prompt on app open (manage in Settings)",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 17.sp
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("backup_dialog_later_btn")
            ) {
                Text(
                    text = "Later",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            if (hasBackupFolder) {
                Button(
                    onClick = {
                        val appCtx = context.applicationContext
                        viewModel.performLocalBackup(appCtx) { success, result ->
                            val msg = if (success) "Backup saved: $result" else "Backup failed: $result"
                            Toast.makeText(appCtx, msg, Toast.LENGTH_LONG).show()
                        }
                        onDismiss()
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkNavActive),
                    modifier = Modifier.testTag("backup_dialog_backup_btn")
                ) {
                    Text(
                        text = "Backup Now",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            } else {
                Button(
                    onClick = { folderPickerLauncher.launch(null) },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkNavActive),
                    modifier = Modifier.testTag("backup_dialog_select_folder_btn")
                ) {
                    Text(
                        text = "Select Folder",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    )
}
