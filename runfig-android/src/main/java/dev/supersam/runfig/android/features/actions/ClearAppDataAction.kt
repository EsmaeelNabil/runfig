package dev.supersam.runfig.android.features.actions

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import android.widget.Toast
import dev.supersam.runfig.android.api.DebugAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

internal class ClearAppDataAction : DebugAction {
    override val title: String = "⛔ Clear App Data"
    override val description: String?
        get() = "Clear app data (cache, files, shared preferences). This may not work on all devices."

    override suspend fun onAction(context: Context) {
        var success = false

        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Attempting to clear app data...", Toast.LENGTH_LONG)
                .show()
        }
        delay(500)

        success =
            (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.clearApplicationUserData()
                ?: false

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