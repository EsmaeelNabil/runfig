package dev.supersam.runfig.android.features.providers

import android.content.Context
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.supersam.runfig.android.api.DebugInfoProvider
import dev.supersam.runfig.android.initialization.CrashLogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.io.readText

internal class CrashLogInfoProvider : DebugInfoProvider {
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
                modifier = Modifier.Companion
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
                LazyColumn(modifier = Modifier.Companion.heightIn(max = 200.dp)) {
                    items(filteredLogs, key = { it.name }) { logFile ->
                        Row(
                            modifier = Modifier.Companion
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
                                }, onLongClick = { })
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.Companion.CenterVertically
                        ) {
                            Text(
                                logFile.name,
                                modifier = Modifier.Companion.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Companion.Ellipsis
                            )
                            IconButton(
                                onClick = { },
                                modifier = Modifier.Companion.size(40.dp)
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
                        modifier = Modifier.Companion
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                    ) {
                        item {
                            Text(
                                content, fontFamily = FontFamily.Companion.Monospace, fontSize = 12.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { selectedLogContent = null }) { Text("Close") }
                })
        }
    }

}