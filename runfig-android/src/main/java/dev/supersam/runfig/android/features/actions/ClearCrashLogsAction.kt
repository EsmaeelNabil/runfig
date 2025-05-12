package dev.supersam.runfig.android.features.actions

import android.content.Context
import android.widget.Toast
import dev.supersam.runfig.android.api.DebugAction
import dev.supersam.runfig.android.initialization.CrashLogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class ClearCrashLogsAction : DebugAction {
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