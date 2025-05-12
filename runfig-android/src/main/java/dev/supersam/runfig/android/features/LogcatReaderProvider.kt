package dev.supersam.runfig.android.features

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import dev.supersam.runfig.android.api.DebugInfoProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

internal class LogcatReaderProvider : DebugInfoProvider {
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
            state = scrollState, modifier = Modifier.Companion
                .heightIn(max = 300.dp)
                .fillMaxWidth()
        ) {
            items(logs) { logLine ->
                Text(
                    logLine,
                    fontFamily = FontFamily.Companion.Monospace,
                    fontSize = 10.sp,
                    lineHeight = 12.sp
                )
            }
        }
    }
}