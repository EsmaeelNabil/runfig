package dev.supersam.runfig.android.features.providers

import android.content.Context
import android.content.pm.FeatureInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.supersam.runfig.android.api.DebugInfoProvider

internal class FeaturesProvider(context: Context) : DebugInfoProvider {
    override val title: String = "Required Features"

    private val packageInfo: PackageInfo? = try {
        context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_CONFIGURATIONS
        )
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    private val reqFeatures: List<FeatureInfo> =
        packageInfo?.reqFeatures?.toList() ?: emptyList()

    @Composable
    override fun Content(context: Context) {
        if (reqFeatures.isEmpty()) {
            Text("No specific features required.")
            return
        }
        LazyColumn(modifier = Modifier.Companion.heightIn(max = 150.dp)) {
            items(reqFeatures) { feature ->
                val featureName = feature.name ?: "Unnamed Feature"
                val required = (feature.flags and FeatureInfo.FLAG_REQUIRED) != 0
                val version = if (feature.version > 0) " (v${feature.version})" else ""
                DefaultInfoProviders.InfoRow(featureName, "Required: $required$version")
                Spacer(Modifier.Companion.height(4.dp))
            }
        }
    }
}