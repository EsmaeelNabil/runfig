package dev.supersam.runfig.android.features.providers

import android.content.Context
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

internal class PermissionsProvider(context: Context) : DebugInfoProvider {
    override val title: String = "App Permissions"

    private val packageInfo: PackageInfo? = try {
        context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    private val requestedPermissions: List<String> =
        packageInfo?.requestedPermissions?.toList() ?: emptyList()
    private val permissionFlags: IntArray =
        packageInfo?.requestedPermissionsFlags ?: IntArray(0)

    @Composable
    override fun Content(context: Context) {
        if (requestedPermissions.isEmpty()) {
            Text("No specific permissions requested.")
            return
        }

        LazyColumn(modifier = Modifier.Companion.heightIn(max = 200.dp)) {
            items(requestedPermissions.indices.toList()) { index ->
                val permission = requestedPermissions[index]
                val isGranted = try {
                    context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
                } catch (e: Exception) {
                    false
                }
                val isRequested = permissionFlags.getOrNull(index)
                    ?.let { (it and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0 } ?: false

                val status = when {
                    isGranted -> "Granted"

                    else -> "Denied / Not Granted"
                }
                DefaultInfoProviders.InfoRow(permission.substringAfterLast('.'), status)
                Spacer(Modifier.Companion.height(4.dp))
            }
        }
    }
}