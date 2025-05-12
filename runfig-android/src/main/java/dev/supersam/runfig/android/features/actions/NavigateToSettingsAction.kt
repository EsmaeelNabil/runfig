package dev.supersam.runfig.android.features.actions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import dev.supersam.runfig.android.api.DebugAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class NavigateToSettingsAction : DebugAction {
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