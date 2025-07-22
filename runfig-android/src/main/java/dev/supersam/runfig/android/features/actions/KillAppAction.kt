package dev.supersam.runfig.android.features.actions

import android.content.Context
import android.os.Process
import android.widget.Toast
import dev.supersam.runfig.android.api.DebugAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.system.exitProcess

internal class KillAppAction : DebugAction {
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