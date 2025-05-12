package dev.supersam.runfig.android.features.providers

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.supersam.runfig.android.api.DebugInfoProvider
import dev.supersam.runfig.android.features.FeatureFlagRegistry

internal class FeatureFlagInfoProvider : DebugInfoProvider {
    override val title: String = "Feature Flags (Host)"

    @Composable
    override fun Content(context: Context) {
        val allFlags =
            remember { FeatureFlagRegistry.providers.flatMap { it.featureFlags.entries } }

        if (allFlags.isEmpty()) {
            Text("No feature flag providers registered by the host app., use FeatureFlagRegistry")
            return
        }

        LazyColumn(
            modifier = Modifier.Companion
                .heightIn(max = 200.dp)
                .fillMaxWidth()
        ) {
            items(allFlags, key = { it.key }) { (key, state) ->
                Row(
                    modifier = Modifier.Companion
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Companion.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(key)
                    Switch(
                        checked = state.value,
                        onCheckedChange = { state.value = it },
                        modifier = Modifier.Companion.padding(start = 8.dp)
                    )
                }
                Divider()
            }
        }
        Spacer(modifier = Modifier.Companion.height(8.dp))
        Text(
            "Toggles reflect MutableState provided by host app.",
            style = MaterialTheme.typography.labelSmall
        )
    }
}