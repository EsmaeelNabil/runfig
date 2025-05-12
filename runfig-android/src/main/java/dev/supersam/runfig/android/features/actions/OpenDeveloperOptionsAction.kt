package dev.supersam.runfig.android.features.actions

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import dev.supersam.runfig.android.api.DebugAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class OpenDeveloperOptionsAction : DebugAction {
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