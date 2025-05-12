package dev.supersam.runfig.android.features.actions

import android.content.Context
import android.content.Intent
import android.os.Process
import android.util.Log
import android.widget.Toast
import dev.supersam.runfig.android.api.DebugAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.system.exitProcess

internal class RestartAppAction : DebugAction {
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