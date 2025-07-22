package dev.supersam.runfig.android.features.providers

import android.content.Context
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.supersam.runfig.android.api.DebugInfoProvider
import dev.supersam.runfig.android.features.ActivityTracker

internal class AppLifecycleInfoProvider : DebugInfoProvider {
    override val title: String = "App Lifecycle"

    @Composable
    override fun Content(context: Context) {
        val lifecycleEvents = ActivityTracker.getLog()

        if (lifecycleEvents.isEmpty()) {
            Text("No lifecycle events recorded yet (ActivityTracker might be disabled or just started).")
            return
        }

        val scrollState = rememberLazyListState()
        LaunchedEffect(lifecycleEvents.size) {
            if (lifecycleEvents.isNotEmpty()) {
                scrollState.animateScrollToItem(lifecycleEvents.size - 1)
            }
        }

        LazyColumn(
            state = scrollState, modifier = Modifier.Companion
                .heightIn(max = 200.dp)
                .fillMaxWidth()
        ) {
            items(lifecycleEvents) { event ->
                Text(event, fontFamily = FontFamily.Companion.Monospace, fontSize = 10.sp)
            }
        }
        Spacer(modifier = Modifier.Companion.height(8.dp))
    }
}