package dev.supersam.runfig.android.features

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.supersam.runfig.android.api.DebugOverlay
import dev.supersam.runfig.android.features.actions.ClearAppDataAction
import dev.supersam.runfig.android.features.actions.ClearCacheAction
import dev.supersam.runfig.android.features.actions.ClearCrashLogsAction
import dev.supersam.runfig.android.features.actions.NavigateToSettingsAction
import dev.supersam.runfig.android.features.actions.NavigateToStoreAction
import dev.supersam.runfig.android.features.actions.OpenDeveloperOptionsAction
import dev.supersam.runfig.android.features.actions.RestartAppAction
import dev.supersam.runfig.android.features.providers.AppLifecycleInfoProvider
import dev.supersam.runfig.android.features.providers.CrashLogInfoProvider
import dev.supersam.runfig.android.features.providers.FeatureFlagInfoProvider

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InfoRow(label: String, value: String?, allowCopy: Boolean = true) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(enabled = allowCopy && value != null, onClick = {}, onLongClick = {
                value?.let {
                    clipboardManager.setText(AnnotatedString(it))
                    Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
                }
            })
            .padding(vertical = 2.dp)
    ) {
        Text(
            label,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.widthIn(max = 130.dp),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(value ?: "N/A")
    }
    Spacer(modifier = Modifier.height(4.dp))
}

internal object DefaultActions {

    fun registerDefaults() {

        DebugOverlay.addAction(NavigateToStoreAction())
        DebugOverlay.addAction(NavigateToSettingsAction())
        DebugOverlay.addAction(OpenDeveloperOptionsAction())
        DebugOverlay.addAction(ClearCacheAction())
        DebugOverlay.addAction(ClearCrashLogsAction())
        DebugOverlay.addAction(RestartAppAction())
        DebugOverlay.addAction(KillAppAction())
        DebugOverlay.addAction(ClearAppDataAction())


        DebugOverlay.addInfoProvider(SharedPreferencesInfoProvider())
        DebugOverlay.addInfoProvider(CrashLogInfoProvider())
        DebugOverlay.addInfoProvider(NetworkInfoProvider())
        DebugOverlay.addInfoProvider(AppLifecycleInfoProvider())
        DebugOverlay.addInfoProvider(LogcatReaderProvider())
        DebugOverlay.addInfoProvider(FeatureFlagInfoProvider())
    }
}