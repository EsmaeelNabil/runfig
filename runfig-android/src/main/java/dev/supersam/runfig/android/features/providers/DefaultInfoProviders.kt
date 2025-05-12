package dev.supersam.runfig.android.features.providers

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.supersam.runfig.android.api.DebugOverlay
import java.text.SimpleDateFormat
import java.util.*

internal object DefaultInfoProviders {

    fun registerDefaults(context: Context) {
        DebugOverlay.addInfoProvider(AppInfoProvider(context))
        DebugOverlay.addInfoProvider(DeviceInfoProvider(context))
        DebugOverlay.addInfoProvider(PermissionsProvider(context))
        DebugOverlay.addInfoProvider(FeaturesProvider(context))
    }

    @Composable
    fun InfoRow(label: String, value: String?) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                label,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.widthIn(max = 120.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(value ?: "N/A")
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}

fun formatBytes(bytes: Long): String {
    if (bytes < 0) return "N/A"
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "${"%.1f".format(kb)} KB"
    val mb = kb / 1024.0
    if (mb < 1024) return "${"%.1f".format(mb)} MB"
    val gb = mb / 1024.0
    return "${"%.1f".format(gb)} GB"
}

