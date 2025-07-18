package dev.supersam.runfig.android.features

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import dev.supersam.runfig.android.api.DebugInfoProvider
import java.io.File

internal class SharedPreferencesInfoProvider : DebugInfoProvider {
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
                    modifier = Modifier.Companion
                        .menuAnchor()
                        .fillMaxWidth()
                        .clickable { /* TODO: Show file selection dialog/menu */


                        })
            }


            LazyColumn(modifier = Modifier.Companion.heightIn(max = 100.dp)) {
                items(allPrefFiles) { fileName ->
                    Row(
                        verticalAlignment = Alignment.Companion.CenterVertically,
                        modifier = Modifier.Companion.clickable { selectedPrefFile = fileName }) {
                        RadioButton(
                            selected = selectedPrefFile == fileName,
                            onClick = { selectedPrefFile = fileName })
                        Text(fileName)
                    }
                }
            }

            Spacer(modifier = Modifier.Companion.height(8.dp))

            if (selectedPrefFile != null) {

                OutlinedTextField(
                    value = filterText,
                    onValueChange = { filterText = it },
                    label = { Text("Filter by Key or Value") },
                    modifier = Modifier.Companion.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        if (filterText.isNotEmpty()) {
                            IconButton(onClick = { filterText = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear Filter")
                            }
                        }
                    })
                Spacer(modifier = Modifier.Companion.height(8.dp))


                Button(
                    onClick = { showAddDialog = selectedPrefFile },
                    enabled = selectedPrefFile != null
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.Companion.size(18.dp)
                    )
                    Spacer(Modifier.Companion.width(4.dp))
                    Text("Add Key")
                }


                LazyColumn(
                    modifier = Modifier.Companion
                        .heightIn(max = 300.dp)
                        .fillMaxWidth()
                ) {
                    items(filteredPrefData, key = { it.key }) { (key, value) ->
                        Row(
                            modifier = Modifier.Companion
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Companion.CenterVertically
                        ) {

                            Column(modifier = Modifier.Companion.weight(1f)) {
                                Text(key, fontWeight = FontWeight.Companion.Bold)
                                Text(
                                    value.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 3,
                                    overflow = TextOverflow.Companion.Ellipsis
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
                                "No results matching filter.", modifier = Modifier.Companion.padding(8.dp)
                            )
                        }
                    } else if (prefData.isEmpty()) {
                        item {
                            Text(
                                "No entries in this file.", modifier = Modifier.Companion.padding(8.dp)
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
    ): Map<String, Any?> {
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
    ) {
        try {
            context.getSharedPreferences(fileName, Context.MODE_PRIVATE).edit {
                remove(key)
            }
        } catch (e: Exception) {
            Log.e("SharedPrefsProvider", "Error removing key '$key' from '$fileName'", e)
        }
    }


    private fun saveSharedPrefKey(
        context: Context, fileName: String, key: String, value: String, type: PrefValueType
    ) {
        try {
            context.getSharedPreferences(fileName, Context.MODE_PRIVATE).edit {
                when (type) {
                    PrefValueType.STRING -> putString(key, value)
                    PrefValueType.BOOLEAN -> putBoolean(
                        key, value.toBooleanStrictOrNull() ?: false
                    )

                    PrefValueType.INT -> putInt(key, value.toIntOrNull() ?: 0)
                    PrefValueType.LONG -> putLong(key, value.toLongOrNull() ?: 0L)
                    PrefValueType.FLOAT -> putFloat(key, value.toFloatOrNull() ?: 0f)

                }
            }
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
        var keyState by rememberSaveable(prefKey) { mutableStateOf(prefKey) }
        var valueState by rememberSaveable(initialValue) { mutableStateOf(initialValue) }
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
                        modifier = Modifier.Companion.fillMaxWidth(),
                        readOnly = !isNew,
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = valueState,
                        onValueChange = { valueState = it },
                        label = { Text("Value") },
                        modifier = Modifier.Companion.fillMaxWidth()
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
                            modifier = Modifier.Companion
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = typeMenuExpanded,
                            onDismissRequest = { typeMenuExpanded = false }) {
                            PrefValueType.entries.forEach { type ->
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
                    }
                }
            },
            confirmButton = {
                Button(onClick = { onSave(keyState, valueState, selectedType) }) {
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