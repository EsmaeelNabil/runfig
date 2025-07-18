package dev.supersam.runfig.android.features.actions

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import dev.supersam.runfig.android.api.DebugAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.net.toUri

internal class NavigateToStoreAction : DebugAction {
    override val title: String = "Open Store Listing"
    override val description: String?
        get() = "Open the app's store listing in Google Play Store or browser."


    override suspend fun onAction(context: Context) {
        val packageName = context.packageName
        try {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW, "market://details?id=$packageName".toUri()
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
        } catch (e: ActivityNotFoundException) {
            try {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        "https://play.google.com/store/apps/details?id=$packageName".toUri()
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