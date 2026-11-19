package com.example.data

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LocalBackupHelper {

    private const val PREFS_NAME = "tippani_local_backup_prefs"
    private const val KEY_BACKUP_FOLDER_URI = "local_backup_folder_uri"
    private const val KEY_BACKUP_FOLDER_NAME = "local_backup_folder_name"
    private const val KEY_LAST_BACKUP_TIME = "last_local_backup_time"
    private const val KEY_LAST_BACKUP_DATE = "last_local_backup_date"
    private const val KEY_PROMPT_BACKUP_ON_OPEN = "prompt_backup_on_open"

    private const val FILE_PREFIX = "tippani_backup"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getBackupFolderUriString(context: Context): String? {
        return getPrefs(context).getString(KEY_BACKUP_FOLDER_URI, null)?.takeIf { it.isNotBlank() }
    }

    fun getBackupFolderUri(context: Context): Uri? {
        return getBackupFolderUriString(context)?.let { Uri.parse(it) }
    }

    fun getBackupFolderName(context: Context): String? {
        return getPrefs(context).getString(KEY_BACKUP_FOLDER_NAME, null)?.takeIf { it.isNotBlank() }
    }

    fun hasBackupFolder(context: Context): Boolean {
        return getBackupFolderUri(context) != null
    }

    fun getLastBackupTime(context: Context): String? {
        return getPrefs(context).getString(KEY_LAST_BACKUP_TIME, null)?.takeIf { it.isNotBlank() }
    }

    fun getLastBackupDate(context: Context): String? {
        return getPrefs(context).getString(KEY_LAST_BACKUP_DATE, null)?.takeIf { it.isNotBlank() }
    }

    fun isPromptBackupOnOpen(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_PROMPT_BACKUP_ON_OPEN, true)
    }

    fun setPromptBackupOnOpen(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_PROMPT_BACKUP_ON_OPEN, enabled).apply()
    }

    fun getTodayDateIso(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun getTodayDateCompact(): String {
        return SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
    }

    fun defaultBackupFileName(): String {
        return "${FILE_PREFIX}_${getTodayDateCompact()}.json"
    }

    fun shouldRunDailyAutoBackup(context: Context): Boolean {
        if (!hasBackupFolder(context)) return false
        val lastDate = getLastBackupDate(context)
        return lastDate != getTodayDateIso()
    }

    /**
     * Takes persistable read/write URI permission for the SAF tree URI and stores
     * the URI string and human-readable folder display name.
     */
    fun saveBackupFolder(context: Context, treeUri: Uri): String {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            context.contentResolver.takePersistableUriPermission(treeUri, flags)
        } catch (_: SecurityException) {
            // Some providers may only grant persistable permission if explicitly offered
        }

        val documentFolder = DocumentFile.fromTreeUri(context, treeUri)
        val displayName = documentFolder?.name?.takeIf { it.isNotBlank() }
            ?: treeUri.lastPathSegment?.substringAfterLast(':')?.substringAfterLast('/')
            ?: "Selected Folder"

        getPrefs(context).edit()
            .putString(KEY_BACKUP_FOLDER_URI, treeUri.toString())
            .putString(KEY_BACKUP_FOLDER_NAME, displayName)
            .apply()

        return displayName
    }

    private fun recordBackupTimestamp(context: Context) {
        val now = Date()
        val formattedTime = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(now)
        val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now)

        getPrefs(context).edit()
            .putString(KEY_LAST_BACKUP_TIME, formattedTime)
            .putString(KEY_LAST_BACKUP_DATE, formattedDate)
            .apply()
    }

    /**
     * Writes a brand-new, non-overwriting date-tagged JSON backup file into the
     * user-selected SAF folder on [Dispatchers.IO].
     * Returns [Result.success] with the created filename on success.
     */
    suspend fun writeBackupToConfiguredFolder(
        context: Context,
        jsonContent: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val treeUri = getBackupFolderUri(context)
                ?: return@withContext Result.failure(IllegalStateException("No backup folder configured"))

            val folder = DocumentFile.fromTreeUri(context, treeUri)
                ?: return@withContext Result.failure(IllegalStateException("Unable to access backup folder"))

            if (!folder.exists() || !folder.canWrite()) {
                return@withContext Result.failure(
                    SecurityException("Backup folder is no longer writable. Please re-select the folder.")
                )
            }

            val dateTag = getTodayDateCompact()
            val baseName = "${FILE_PREFIX}_${dateTag}"
            var uniqueFileName = "$baseName.json"
            var counter = 1

            while (
                folder.findFile(uniqueFileName) != null ||
                folder.findFile("$uniqueFileName.json") != null
            ) {
                uniqueFileName = "${baseName}_${counter}.json"
                counter++
            }

            val targetFile = folder.createFile("application/json", uniqueFileName)
                ?: return@withContext Result.failure(IllegalStateException("Failed to create backup file in folder"))

            val outputStream = context.contentResolver.openOutputStream(targetFile.uri, "wt")
                ?: return@withContext Result.failure(IllegalStateException("Unable to open output stream for backup"))

            outputStream.use { stream ->
                stream.write(jsonContent.toByteArray(StandardCharsets.UTF_8))
                stream.flush()
            }

            recordBackupTimestamp(context)
            Result.success(targetFile.name ?: uniqueFileName)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Writes UTF-8 JSON content to a user-chosen document URI (e.g. from CreateDocument).
     */
    suspend fun writeJsonToUri(
        context: Context,
        uri: Uri,
        jsonContent: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val outputStream = context.contentResolver.openOutputStream(uri, "wt")
                ?: return@withContext Result.failure(IllegalStateException("Unable to open export file for writing"))

            outputStream.use { stream ->
                stream.write(jsonContent.toByteArray(StandardCharsets.UTF_8))
                stream.flush()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reads UTF-8 text from a user-chosen document URI (e.g. from OpenDocument).
     */
    suspend fun readTextFromUri(
        context: Context,
        uri: Uri
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(IllegalStateException("Unable to open selected backup file"))

            val text = inputStream.use { stream ->
                BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            }
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
