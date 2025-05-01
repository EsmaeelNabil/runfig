package dev.supersam.runfig.android.features

import android.app.ActivityManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import dev.supersam.runfig.android.api.DebugAction
import dev.supersam.runfig.android.api.DebugInfoProvider
import dev.supersam.runfig.android.api.DebugOverlay
import dev.supersam.runfig.android.initialization.CrashLogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InfoRow(label: String, value: String?, allowCopy: Boolean = true) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(enabled = allowCopy && value != null, onClick = {}, onLongClick = {
                value?.let {
                    clipboardManager.setText(AnnotatedString(it))
                    Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
                }
            })
            .padding(vertical = 2.dp)
    ) {
        Text(
            label,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.widthIn(max = 130.dp),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(value ?: "N/A")
    }
    Spacer(modifier = Modifier.height(4.dp))
}


internal abstract class ExportFileAction(private val fileExtension: String) : DebugAction {
    abstract suspend fun getFilesToExport(context: Context): List<File>
    abstract fun getExportSubdirectory(context: Context): File
    abstract fun getAuthority(context: Context): String

    override suspend fun onAction(context: Context) {
        val files = getFilesToExport(context)
        if (files.isEmpty()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context, "No $fileExtension files found to export.", Toast.LENGTH_SHORT
                ).show()
            }
            return
        }

        val exportDir = getExportSubdirectory(context)
        exportDir.mkdirs()

        val exportedUris = mutableListOf<Uri>()

        withContext(Dispatchers.IO) {
            files.forEach { file ->
                try {
                    val exportFile = File(exportDir, file.name)
                    file.copyTo(exportFile, overwrite = true)
                    val uri = FileProvider.getUriForFile(context, getAuthority(context), exportFile)
                    exportedUris.add(uri)
                } catch (e: Exception) {
                    Log.e("ExportAction", "Error exporting ${file.name}: ${e.message}", e)

                }
            }
        }

        if (exportedUris.isEmpty()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context, "Failed to export any $fileExtension files.", Toast.LENGTH_LONG
                ).show()
            }
            return
        }


        withContext(Dispatchers.Main) {
            val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(exportedUris))
                putExtra(Intent.EXTRA_SUBJECT, "Exported ${fileExtension.uppercase()} Files")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(
                    Intent.createChooser(shareIntent, "Share Exported Files").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
            } catch (e: Exception) {
                Log.e("ExportAction", "Error starting share intent", e)
                Toast.makeText(
                    context,
                    "Error sharing files. Do you have an app that can handle sharing?",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}


internal object DefaultActions {

    private const val FILE_PROVIDER_AUTHORITY = ".debugoverlay.provider"


    fun registerDefaults() {

        DebugOverlay.addAction(NavigateToStoreAction())
        DebugOverlay.addAction(NavigateToSettingsAction())
        DebugOverlay.addAction(OpenDeveloperOptionsAction())
        DebugOverlay.addAction(ClearCacheAction())
        DebugOverlay.addAction(ClearCrashLogsAction())
        DebugOverlay.addAction(ExportSharedPreferencesAction())
        DebugOverlay.addAction(ExportDatabaseAction())
        DebugOverlay.addAction(RestartAppAction())
        DebugOverlay.addAction(KillAppAction())
        DebugOverlay.addAction(ClearAppDataAction())


        DebugOverlay.addInfoProvider(SharedPreferencesInfoProvider())
        DebugOverlay.addInfoProvider(CrashLogInfoProvider())
        DebugOverlay.addInfoProvider(DatabaseInfoProvider())
        DebugOverlay.addInfoProvider(NetworkInfoProvider())
        DebugOverlay.addInfoProvider(AppLifecycleInfoProvider())
        DebugOverlay.addInfoProvider(LogcatReaderProvider())
        DebugOverlay.addInfoProvider(FeatureFlagInfoProvider())
    }


    private class NavigateToStoreAction : DebugAction {
        override val title: String = "Open Store Listing"
        override val description: String?
            get() = "Open the app's store listing in Google Play Store or browser."


        override suspend fun onAction(context: Context) {
            val packageName = context.packageName
            try {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
            } catch (e: ActivityNotFoundException) {
                try {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                        ).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                } catch (e2: Exception) {
                    Log.e("DevAssistAction", "Could not open store listing", e2)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context, "Could not open store listing", Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private class NavigateToSettingsAction : DebugAction {
        override val title: String = "Open App Settings"
        override val description: String?
            get() = "Open the app's settings page in system settings."


        override suspend fun onAction(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e("DevAssistAction", "Could not open app settings", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context, "Could not open app settings", Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private class OpenDeveloperOptionsAction : DebugAction {
        override val title: String = "Open Developer Options"
        override val description: String?
            get() = "Open the Developer Options screen in system settings."

        override suspend fun onAction(context: Context) {
            try {
                context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (e: Exception) {
                Log.e("DevAssistAction", "Could not open Developer Options", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context, "Could not open Developer Options", Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private class ClearCacheAction : DebugAction {
        override val title: String = "Clear Cache"
        override val description: String?
            get() = "Clear app cache directories (internal, external, code cache)."

        override suspend fun onAction(context: Context) {
            var result = false
            withContext(Dispatchers.IO) {
                val internalCache = context.cacheDir?.deleteRecursively() ?: false
                val codeCache = context.codeCacheDir?.deleteRecursively() ?: false
                val externalCache = context.externalCacheDir?.deleteRecursively() ?: false
                result = internalCache || codeCache || externalCache
            }
            withContext(Dispatchers.Main) {
                val message =
                    if (result) "Cache cleared" else "Failed to clear some cache directories"
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private class RestartAppAction : DebugAction {
        override val title: String = "Restart App"
        override val description: String?
            get() = "Restart the app process. This may not work on all devices."

        override suspend fun onAction(context: Context) {


            try {
                val packageManager = context.packageManager
                val intent = packageManager.getLaunchIntentForPackage(context.packageName)
                if (intent?.component != null) {
                    val mainIntent = Intent.makeRestartActivityTask(intent.component)
                    context.startActivity(mainIntent)

                    Process.killProcess(Process.myPid())
                    exitProcess(0)
                } else {
                    Log.e("DevAssistAction", "Could not get launch intent for restart")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context, "Restart failed: Cannot find launch intent", Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("DevAssistAction", "Exception during app restart", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context, "Restart failed: ${e.message}", Toast.LENGTH_LONG
                    ).show()
                }

            }
        }
    }

    private class KillAppAction : DebugAction {
        override val title: String = "⚠️ Kill App Process"
        override val description: String?
            get() = "Forcefully kill the app process. This may not work on all devices."

        override suspend fun onAction(context: Context) {

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Killing app process...", Toast.LENGTH_SHORT).show()
            }
            delay(300)
            Process.killProcess(Process.myPid())
            exitProcess(0)
        }
    }

    private class ClearAppDataAction : DebugAction {
        override val title: String = "⛔ Clear App Data"
        override val description: String?
            get() = "Clear app data (cache, files, shared preferences). This may not work on all devices."

        override suspend fun onAction(context: Context) {
            var success = false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Attempting to clear app data...", Toast.LENGTH_LONG)
                        .show()
                }
                delay(500)

                success =
                    (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.clearApplicationUserData()
                        ?: false
            }

            if (success) {

                Log.i(
                    "DevAssistAction",
                    "App data cleared successfully (via ActivityManager). App might restart."
                )
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Failed to clear app data (requires API 19+ or system permission)",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private class ClearCrashLogsAction : DebugAction {
        override val title: String = "Clear Crash Logs"
        override val description: String?
            get() = "Clear crash logs from the device. This may not work on all devices."


        override suspend fun onAction(context: Context) {
            var success = false
            withContext(Dispatchers.IO) {
                success = CrashLogManager.clearCrashLogs(context)
            }
            withContext(Dispatchers.Main) {
                val message = if (success) "Crash logs cleared" else "Failed to clear crash logs"
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private class ExportSharedPreferencesAction : ExportFileAction("prefs") {
        override val title: String = "Export SharedPreferences"
        override val description: String?
            get() = "Export SharedPreferences files to a shareable directory."

        private var lastSelectedFile: String? = null


        override suspend fun getFilesToExport(context: Context): List<File> {
            val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
            return withContext(Dispatchers.IO) {
                prefsDir.listFiles { _, name -> name.endsWith(".xml") }?.toList() ?: emptyList()
            }

        }

        override fun getExportSubdirectory(context: Context): File =
            File(context.filesDir, "runfig_export/shared_prefs")

        override fun getAuthority(context: Context): String =
            context.packageName + FILE_PROVIDER_AUTHORITY
    }

    private class ExportDatabaseAction : ExportFileAction("db") {
        override val title: String = "Export Databases"
        override val description: String?
            get() = "Export database files to a shareable directory."

        override suspend fun getFilesToExport(context: Context): List<File> {
            return withContext(Dispatchers.IO) {
                val dbDir = context.getDatabasePath("anyname.db").parentFile
                dbDir?.listFiles { file ->
                    file.isFile && (file.name.endsWith(".db") || file.name.endsWith(".sqlite") || file.name.contains(
                        "-journal"
                    ) || file.name.contains("-wal"))
                }?.toList() ?: emptyList()
            }
        }

        override fun getExportSubdirectory(context: Context): File {

            val exportDir = File(context.filesDir, "runfig_export/databases")
            exportDir.mkdirs()
            return exportDir


        }

        override fun getAuthority(context: Context): String =
            context.packageName + FILE_PROVIDER_AUTHORITY


        override suspend fun onAction(context: Context) {
            val files = getFilesToExport(context)
            if (files.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context, "No DB files found.", Toast.LENGTH_SHORT
                    ).show()
                }
                return
            }
            val exportDir = getExportSubdirectory(context)
            val exportedUris = mutableListOf<Uri>()

            withContext(Dispatchers.IO) {
                files.forEach { file ->
                    try {
                        val exportFile = File(exportDir, file.name)
                        file.copyTo(exportFile, overwrite = true)
                        val uri =
                            FileProvider.getUriForFile(context, getAuthority(context), exportFile)
                        exportedUris.add(uri)
                    } catch (e: Exception) {
                        Log.e(
                            "ExportDbAction",
                            "Error copying ${file.name} for export: ${e.message}",
                            e
                        )
                    }
                }
            }

            if (exportedUris.isNotEmpty()) {
                withContext(Dispatchers.Main) {

                    val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                        type = "*/*"
                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(exportedUris))
                        putExtra(Intent.EXTRA_SUBJECT, "Exported Database Files")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        context.startActivity(
                            Intent.createChooser(
                                shareIntent, "Share Database Files"
                            ).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                    } catch (e: Exception) {
                        Log.e("ExportDbAction", "Error sharing DB files", e)
                        Toast.makeText(context, "Error sharing DB files.", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context, "Failed to copy DB files for export.", Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }


    private class SharedPreferencesInfoProvider : DebugInfoProvider {
        override val title: String = "SharedPreferences"

        @OptIn(ExperimentalMaterial3Api::class)
        @Composable
        override fun Content(context: Context) {
            val prefsDir =
                remember(context) { File(context.applicationInfo.dataDir, "shared_prefs") }
            val allPrefFiles = remember(prefsDir) {
                prefsDir.listFiles { _, name -> name.endsWith(".xml") }
                    ?.map { it.name.removeSuffix(".xml") }?.sorted() ?: emptyList()
            }

            var selectedPrefFile by rememberSaveable { mutableStateOf<String?>(null) }
            var prefData by remember { mutableStateOf<Map<String, Any?>>(emptyMap()) }
            var showEditDialog by remember { mutableStateOf<Triple<String, String, String?>?>(null) }
            var showAddDialog by remember { mutableStateOf<String?>(null) }
            var filterText by rememberSaveable { mutableStateOf("") }


            LaunchedEffect(selectedPrefFile) {
                selectedPrefFile?.let { fileName ->
                    prefData = loadSharedPreferences(context, fileName)
                }
            }

            val filteredPrefData = remember(prefData, filterText) {
                if (filterText.isBlank()) {
                    prefData.entries.toList()
                } else {
                    prefData.entries.filter {
                        it.key.contains(
                            filterText, ignoreCase = true
                        ) || it.value.toString().contains(filterText, ignoreCase = true)
                    }.toList()
                }
            }


            Column {

                ExposedDropdownMenuBox(
                    expanded = false, onExpandedChange = {}) {
                    OutlinedTextField(
                        value = selectedPrefFile ?: "Select a file...",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Preferences File") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = false) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .clickable { /* TODO: Show file selection dialog/menu */


                            })
                }


                LazyColumn(modifier = Modifier.heightIn(max = 100.dp)) {
                    items(allPrefFiles) { fileName ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { selectedPrefFile = fileName }) {
                            RadioButton(
                                selected = selectedPrefFile == fileName,
                                onClick = { selectedPrefFile = fileName })
                            Text(fileName)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (selectedPrefFile != null) {

                    OutlinedTextField(
                        value = filterText,
                        onValueChange = { filterText = it },
                        label = { Text("Filter by Key or Value") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            if (filterText.isNotEmpty()) {
                                IconButton(onClick = { filterText = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear Filter")
                                }
                            }
                        })
                    Spacer(modifier = Modifier.height(8.dp))


                    Button(
                        onClick = { showAddDialog = selectedPrefFile },
                        enabled = selectedPrefFile != null
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Add Key")
                    }


                    LazyColumn(
                        modifier = Modifier
                            .heightIn(max = 300.dp)
                            .fillMaxWidth()
                    ) {
                        items(filteredPrefData, key = { it.key }) { (key, value) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(key, fontWeight = FontWeight.Bold)
                                    Text(
                                        value.toString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row {
                                    IconButton(onClick = {
                                        showEditDialog =
                                            Triple(selectedPrefFile!!, key, value?.toString())
                                    }) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(onClick = {
                                        selectedPrefFile?.let { file ->
                                            removeSharedPrefKey(context, file, key)
                                            prefData = loadSharedPreferences(context, file)
                                        }
                                    }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                            Divider()
                        }
                        if (filteredPrefData.isEmpty() && prefData.isNotEmpty()) {
                            item {
                                Text(
                                    "No results matching filter.", modifier = Modifier.padding(8.dp)
                                )
                            }
                        } else if (prefData.isEmpty()) {
                            item {
                                Text(
                                    "No entries in this file.", modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }


            showEditDialog?.let { (file, key, currentValue) ->
                EditSharedPreferencesDialog(
                    prefFile = file,
                    prefKey = key,
                    initialValue = currentValue ?: "",
                    isNew = false,
                    onDismiss = { showEditDialog = null },
                    onSave = { newKey, newValue, type ->
                        saveSharedPrefKey(context, file, newKey, newValue, type)
                        prefData = loadSharedPreferences(context, file)
                        showEditDialog = null
                    })
            }
            showAddDialog?.let { file ->
                EditSharedPreferencesDialog(
                    prefFile = file,
                    prefKey = "",
                    initialValue = "",
                    isNew = true,
                    onDismiss = { showAddDialog = null },
                    onSave = { newKey, newValue, type ->
                        if (newKey.isNotBlank()) {
                            saveSharedPrefKey(context, file, newKey, newValue, type)
                            prefData = loadSharedPreferences(context, file)
                            showAddDialog = null
                        } else {
                            Toast.makeText(context, "Key cannot be empty", Toast.LENGTH_SHORT)
                                .show()
                        }
                    })
            }
        }


        private fun loadSharedPreferences(
            context: Context, fileName: String
        ): Map<String, Any?> { /* ... unchanged ... */
            return try {
                val prefs = context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
                prefs.all.toSortedMap()
            } catch (e: Exception) {
                Log.e("SharedPrefsProvider", "Error loading prefs '$fileName'", e)
                emptyMap()
            }
        }

        private fun removeSharedPrefKey(
            context: Context, fileName: String, key: String
        ) { /* ... unchanged ... */
            try {
                context.getSharedPreferences(fileName, Context.MODE_PRIVATE).edit().remove(key)
                    .apply()
            } catch (e: Exception) {
                Log.e("SharedPrefsProvider", "Error removing key '$key' from '$fileName'", e)
            }
        }


        private fun saveSharedPrefKey(
            context: Context, fileName: String, key: String, value: String, type: PrefValueType
        ) {
            try {
                val editor = context.getSharedPreferences(fileName, Context.MODE_PRIVATE).edit()
                when (type) {
                    PrefValueType.STRING -> editor.putString(key, value)
                    PrefValueType.BOOLEAN -> editor.putBoolean(
                        key, value.toBooleanStrictOrNull() ?: false
                    )

                    PrefValueType.INT -> editor.putInt(key, value.toIntOrNull() ?: 0)
                    PrefValueType.LONG -> editor.putLong(key, value.toLongOrNull() ?: 0L)
                    PrefValueType.FLOAT -> editor.putFloat(key, value.toFloatOrNull() ?: 0f)

                }
                editor.apply()
            } catch (e: Exception) {
                Log.e("SharedPrefsProvider", "Error saving key '$key' ($type) to '$fileName'", e)

            }
        }


        enum class PrefValueType { STRING, BOOLEAN, INT, LONG, FLOAT /*, STRING_SET */ }


        @OptIn(ExperimentalMaterial3Api::class)
        @Composable
        private fun EditSharedPreferencesDialog(
            prefFile: String,
            prefKey: String,
            initialValue: String,
            isNew: Boolean,
            onDismiss: () -> Unit,
            onSave: (key: String, value: String, type: PrefValueType) -> Unit
        ) {
            var keyState by rememberSaveable(prefKey) { mutableStateOf(TextFieldValue(prefKey)) }
            var valueState by rememberSaveable(initialValue) {
                mutableStateOf(
                    TextFieldValue(
                        initialValue
                    )
                )
            }
            var selectedType by rememberSaveable { mutableStateOf(PrefValueType.STRING) }
            var typeMenuExpanded by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(if (isNew) "Add New Entry" else "Edit '$prefKey'") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("File: $prefFile", style = MaterialTheme.typography.bodySmall)

                        OutlinedTextField(
                            value = keyState,
                            onValueChange = { keyState = it },
                            label = { Text("Key") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = !isNew,
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = valueState,
                            onValueChange = { valueState = it },
                            label = { Text("Value") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        ExposedDropdownMenuBox(
                            expanded = typeMenuExpanded,
                            onExpandedChange = { typeMenuExpanded = !typeMenuExpanded }) {
                            OutlinedTextField(
                                value = selectedType.name,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Value Type") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = typeMenuExpanded,
                                onDismissRequest = { typeMenuExpanded = false }) {
                                PrefValueType.values().forEach { type ->
                                    DropdownMenuItem(text = { Text(type.name) }, onClick = {
                                        selectedType = type
                                        typeMenuExpanded = false
                                    })
                                }
                            }
                        }
                        if (selectedType == PrefValueType.BOOLEAN) {
                            Text(
                                "Use 'true' or 'false'", style = MaterialTheme.typography.labelSmall
                            )
                        }/* if(selectedType == PrefValueType.STRING_SET) {
                            Text("Enter comma-separated values", style = MaterialTheme.typography.labelSmall)
                        }*/
                    }
                },
                confirmButton = {
                    Button(onClick = { onSave(keyState.text, valueState.text, selectedType) }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    Button(onClick = onDismiss) {
                        Text("Cancel")
                    }
                })
        }
    }


    private class CrashLogInfoProvider : DebugInfoProvider {
        override val title: String = "Crash Logs"

        @OptIn(ExperimentalFoundationApi::class)
        @Composable
        override fun Content(context: Context) {
            var allLogs by remember { mutableStateOf(emptyList<File>()) }
            var filteredLogs by remember { mutableStateOf(emptyList<File>()) }
            var selectedLogContent by remember { mutableStateOf<Pair<String, String>?>(null) }
            var filterText by rememberSaveable { mutableStateOf("") }
            val dialogScope = rememberCoroutineScope()


            LaunchedEffect(Unit) {
                allLogs = withContext(Dispatchers.IO) { CrashLogManager.getCrashLogs(context) }
                filteredLogs = allLogs
            }


            LaunchedEffect(filterText, allLogs) {
                filteredLogs = if (filterText.isBlank()) {
                    allLogs
                } else {
                    allLogs.filter { it.name.contains(filterText, ignoreCase = true) }
                }
            }

            Column {

                OutlinedTextField(
                    value = filterText,
                    onValueChange = { filterText = it },
                    label = { Text("Filter by Filename") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    singleLine = true,
                    trailingIcon = {
                        if (filterText.isNotEmpty()) {
                            IconButton(onClick = { filterText = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear Filter")
                            }
                        }
                    })


                if (allLogs.isEmpty()) {
                    Text("No crash logs found.")
                } else if (filteredLogs.isEmpty()) {
                    Text("No logs matching filter.")
                } else {
                    Text(
                        "Tap to view, long press to share.",
                        style = MaterialTheme.typography.labelSmall
                    )
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(filteredLogs, key = { it.name }) { logFile ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(onClick = {
                                        dialogScope.launch(Dispatchers.IO) {
                                            try {
                                                val content = logFile.readText()
                                                withContext(Dispatchers.Main) {
                                                    selectedLogContent = logFile.name to content
                                                }
                                            } catch (e: Exception) {
                                                Log.e(
                                                    "CrashLog",
                                                    "Error reading log ${logFile.name}",
                                                    e
                                                )

                                            }
                                        }
                                    }, onLongClick = { shareLog(context, logFile) })
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    logFile.name,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                IconButton(
                                    onClick = { shareLog(context, logFile) },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Share Log")
                                }
                            }
                            Divider()
                        }
                    }
                }
            }


            selectedLogContent?.let { (fileName, content) ->
                AlertDialog(
                    onDismissRequest = { selectedLogContent = null },
                    title = { Text(fileName, style = MaterialTheme.typography.titleMedium) },
                    text = {
                        val scrollState = rememberLazyListState()
                        LazyColumn(
                            state = scrollState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp)
                        ) {
                            item {
                                Text(
                                    content, fontFamily = FontFamily.Monospace, fontSize = 12.sp
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = { selectedLogContent = null }) { Text("Close") }
                    })
            }
        }


        private fun shareLog(context: Context, logFile: File) { /* ... unchanged ... */
            try {
                val authority = context.packageName + FILE_PROVIDER_AUTHORITY
                val logUri = FileProvider.getUriForFile(context, authority, logFile)

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_STREAM, logUri)
                    putExtra(Intent.EXTRA_SUBJECT, "Crash Log: ${logFile.name}")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Crash Log").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (e: Exception) {
                Log.e("CrashLog", "Error sharing log", e)
                Toast.makeText(context, "Error sharing log: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }


    private class DatabaseInfoProvider : DebugInfoProvider {
        override val title: String = "Databases"

        @Composable
        override fun Content(context: Context) {
            var dbFiles by remember { mutableStateOf<List<File>>(emptyList()) }

            LaunchedEffect(Unit) {
                dbFiles = withContext(Dispatchers.IO) {
                    val dbDir = context.getDatabasePath("anyname.db").parentFile
                    dbDir?.listFiles { file ->
                        file.isFile && (file.name.endsWith(".db") || file.name.endsWith(".sqlite"))
                    }?.sortedBy { it.name }?.toList() ?: emptyList()
                }
            }

            Column {
                if (dbFiles.isEmpty()) {
                    Text("No database files found in default location.")
                } else {
                    Text(
                        "Database Files (Long press to copy path):",
                        style = MaterialTheme.typography.labelMedium
                    )
                    dbFiles.forEach { file ->
                        val fileSize = remember(file) { formatBytes(file.length()) }
                        InfoRow(label = file.name, value = fileSize, allowCopy = true)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Use 'Export Databases' action to share/inspect files. Direct viewing/editing requires host app integration (e.g., custom provider using Room/SQLite API).",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        private fun formatBytes(bytes: Long): String {
            if (bytes < 0) return "N/A"
            if (bytes < 1024) return "$bytes B"
            val kb = bytes / 1024.0
            if (kb < 1024) return "${"%.1f".format(kb)} KB"
            val mb = kb / 1024.0
            if (mb < 1024) return "${"%.1f".format(mb)} MB"
            val gb = mb / 1024.0
            return "${"%.1f".format(gb)} GB"
        }
    }


    private class NetworkInfoProvider : DebugInfoProvider {
        override val title: String = "Network"

        @Composable
        override fun Content(context: Context) {
            val connectivityManager =
                remember { context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager }
            var networkState by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

            LaunchedEffect(connectivityManager) {
                if (connectivityManager == null) {
                    networkState = mapOf("Error" to "Could not get ConnectivityManager")
                    return@LaunchedEffect
                }

                val activeNetwork = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

                val info = mutableMapOf<String, String>()
                if (activeNetwork == null || capabilities == null) {
                    info["Status"] = "Disconnected"
                } else {
                    info["Status"] = "Connected"
                    when {
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                            info["Type"] = "WiFi"

                        }

                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                            info["Type"] = "Cellular"

                        }

                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> info["Type"] =
                            "Ethernet"

                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> info["Type"] =
                            "VPN"

                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> info["Type"] =
                            "Bluetooth"

                        else -> info["Type"] = "Other"
                    }
                    info["Validated"] =
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                            .toString()
                    info["Metered"] =
                        (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)).toString()
                }
                networkState = info
            }

            if (networkState.isEmpty()) {
                Text("Loading network state...")
            } else {
                networkState.forEach { (key, value) -> InfoRow(label = key, value = value) }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Note: More details (IP, SSID, etc.) may require additional permissions (ACCESS_WIFI_STATE, ACCESS_NETWORK_STATE).",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }


    private class AppLifecycleInfoProvider : DebugInfoProvider {
        override val title: String = "App Lifecycle"

        @Composable
        override fun Content(context: Context) {
            val lifecycleEvents = ActivityTracker.getLog()

            if (lifecycleEvents.isEmpty()) {
                Text("No lifecycle events recorded yet (ActivityTracker might be disabled or just started).")
                return
            }

            val scrollState = rememberLazyListState()
            LaunchedEffect(lifecycleEvents.size) {
                if (lifecycleEvents.isNotEmpty()) {
                    scrollState.animateScrollToItem(lifecycleEvents.size - 1)
                }
            }

            LazyColumn(
                state = scrollState, modifier = Modifier
                    .heightIn(max = 200.dp)
                    .fillMaxWidth()
            ) {
                items(lifecycleEvents) { event ->
                    Text(event, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Requires ActivityTracker to be implemented and registered.",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }


    private class LogcatReaderProvider : DebugInfoProvider {
        override val title: String = "Logcat (App Only)"

        @Composable
        override fun Content(context: Context) {
            var logs by remember { mutableStateOf<List<String>>(listOf("Reading logs...")) }
            var job by remember { mutableStateOf<Job?>(null) }
            val lifecycleOwner = LocalLifecycleOwner.current

            DisposableEffect(lifecycleOwner) {
                job = lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    val logLines = mutableListOf<String>()
                    var reader: BufferedReader? = null
                    try {

                        val process = Runtime.getRuntime().exec("logcat -d -v brief *:V")
                        reader = BufferedReader(InputStreamReader(process.inputStream))
                        var line: String? = ""
                        while (isActive && reader.readLine().also { line = it } != null) {


                            logLines.add(line!!)
                            if (logLines.size > 200) {
                                logLines.removeAt(0)
                            }

                            if (logLines.size % 20 == 0) {
                                withContext(Dispatchers.Main) {
                                    logs = logLines.toList().reversed()
                                }
                            }
                        }
                        withContext(Dispatchers.Main) {
                            logs = logLines.toList().reversed()
                        }
                    } catch (e: SecurityException) {
                        Log.w("LogcatReader", "READ_LOGS permission not granted?", e)
                        withContext(Dispatchers.Main) {
                            logs =
                                listOf("Error: READ_LOGS permission likely missing. Grant via ADB:\n'adb shell pm grant ${context.packageName} android.permission.READ_LOGS'")
                        }
                    } catch (e: Exception) {
                        Log.e("LogcatReader", "Error reading logcat", e)
                        withContext(Dispatchers.Main) {
                            logs = listOf("Error reading logcat: ${e.message}")
                        }
                    } finally {
                        reader?.close()
                    }
                }
                onDispose {
                    job?.cancel()
                }
            }

            val scrollState = rememberLazyListState()
            LazyColumn(
                state = scrollState, modifier = Modifier
                    .heightIn(max = 300.dp)
                    .fillMaxWidth()
            ) {
                items(logs) { logLine ->
                    Text(
                        logLine,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        lineHeight = 12.sp
                    )
                }
            }
        }
    }


    interface FeatureFlagProvider {
        val featureFlags: Map<String, MutableState<Boolean>>
    }


    object FeatureFlagRegistry {
        val providers = mutableListOf<FeatureFlagProvider>()
        fun addProvider(provider: FeatureFlagProvider) {
            providers.add(provider)
        }
    }


    private class FeatureFlagInfoProvider : DebugInfoProvider {
        override val title: String = "Feature Flags (Host)"

        @Composable
        override fun Content(context: Context) {
            val allFlags =
                remember { FeatureFlagRegistry.providers.flatMap { it.featureFlags.entries } }

            if (allFlags.isEmpty()) {
                Text("No feature flag providers registered by the host app.")
                return
            }

            LazyColumn(
                modifier = Modifier
                    .heightIn(max = 200.dp)
                    .fillMaxWidth()
            ) {
                items(allFlags, key = { it.key }) { (key, state) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(key)
                        Switch(
                            checked = state.value,
                            onCheckedChange = { state.value = it },
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    Divider()
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Toggles reflect MutableState provided by host app.",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}


object ActivityTracker {
    private val logMessages = mutableListOf<String>()
    private const val MAX_LOG_SIZE = 100
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    fun log(activityName: String, event: String) {
        synchronized(logMessages) {
            if (logMessages.size >= MAX_LOG_SIZE) {
                logMessages.removeAt(0)
            }
            logMessages.add("${dateFormat.format(Date())} $activityName: $event")
        }
    }

    fun getLog(): List<String> {
        return synchronized(logMessages) { logMessages.toList() }
    }

    fun register(context: Context) {


        Log.w("ActivityTracker", "ActivityTracker.register() not fully implemented!")
    }
}