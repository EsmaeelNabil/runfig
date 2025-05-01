package dev.supersam.runfig.android.api

import android.content.Context

/** Provider for adding custom action buttons/items to the overlay. */
interface DebugAction {
    /** Title/Label for the action button. */
    val title: String
    val description: String?

    /** Action to perform when the button is clicked. Receives context. */
    suspend fun onAction(context: Context)
}