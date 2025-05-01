package dev.supersam.runfig.android.api

import android.content.Context
import androidx.compose.runtime.Composable

/** Provider for adding custom information sections to the overlay. */
interface DebugInfoProvider {
    /** Title of the information section. */
    val title: String

    /** Composable content for the information section. */
    @Composable
    fun Content(context: Context)
}