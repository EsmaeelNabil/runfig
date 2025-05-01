package dev.supersam.runfig.android.features

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.FeatureInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.DisplayMetrics
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.PackageInfoCompat
import dev.supersam.runfig.android.api.DebugInfoProvider
import dev.supersam.runfig.android.api.DebugOverlay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun CollapsibleInfoSection(
    modifier: Modifier = Modifier,
    title: String,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }
    val rotationState by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "rotation"
    )
    val interactionSource = remember { MutableInteractionSource() }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { isExpanded = !isExpanded }
                    .padding(
                        horizontal = 12.dp,
                        vertical = 8.dp
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier.rotate(rotationState)
                )
            }


            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(
                        start = 12.dp,
                        end = 12.dp,
                        top = 4.dp,
                        bottom = 12.dp
                    )
                ) {
                    Divider(modifier = Modifier.padding(bottom = 8.dp))
                    content()
                }
            }
        }
    }
}

internal object DefaultInfoProviders {

    fun registerDefaults(context: Context) {
        DebugOverlay.addInfoProvider(AppInfoProvider(context))
        DebugOverlay.addInfoProvider(DeviceInfoProvider(context))
        DebugOverlay.addInfoProvider(PermissionsProvider(context))
        DebugOverlay.addInfoProvider(FeaturesProvider(context))

    }

    private class AppInfoProvider(context: Context) : DebugInfoProvider {

        override val title: String = "Application"


        private val packageName: String = context.packageName
        private val packageInfo: PackageInfo? = try {
            context.packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_PERMISSIONS or PackageManager.GET_META_DATA
            )
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
        private val applicationInfo: ApplicationInfo? = packageInfo?.applicationInfo

        private val appName: String =
            applicationInfo?.loadLabel(context.packageManager)?.toString() ?: context.packageName
        private val versionName: String = packageInfo?.versionName ?: "N/A"
        private val versionCode: Long =
            packageInfo?.let { PackageInfoCompat.getLongVersionCode(it) } ?: -1L
        private val targetSdk: Int = applicationInfo?.targetSdkVersion ?: -1
        private val minSdk: Int = applicationInfo?.minSdkVersion ?: -1
        private val firstInstallTime: String =
            packageInfo?.firstInstallTime?.let { formatDate(it) } ?: "N/A"
        private val lastUpdateTime: String =
            packageInfo?.lastUpdateTime?.let { formatDate(it) } ?: "N/A"
        private val installerPackage: String? = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.packageManager.getInstallSourceInfo(packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(packageName)
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
        private val dataDir: String = applicationInfo?.dataDir ?: "N/A"
        private val processName: String = applicationInfo?.processName ?: "N/A"
        private val isDebuggable: Boolean =
            applicationInfo?.flags?.let { (it and ApplicationInfo.FLAG_DEBUGGABLE) != 0 } ?: false

        @Composable
        override fun Content(context: Context) {

            InfoRow("App Name:", appName)
            InfoRow("Package:", packageName)
            InfoRow("Version Name:", versionName)
            InfoRow("Version Code:", versionCode.toString())
            InfoRow("Target SDK:", targetSdk.takeIf { it > 0 }?.toString() ?: "N/A")
            InfoRow("Min SDK:", minSdk.takeIf { it > 0 }?.toString() ?: "N/A")
            InfoRow("Debuggable:", isDebuggable.toString())
            InfoRow("Installer:", installerPackage ?: "N/A")
            InfoRow("Installed:", firstInstallTime)
            InfoRow("Updated:", lastUpdateTime)
            InfoRow("Data Dir:", dataDir)
            InfoRow("Process Name:", processName)

            Spacer(Modifier.height(8.dp))

            Text(
                "Note: Build Type & Flavor require host app integration (custom DebugInfoProvider accessing BuildConfig).",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }


    private class DeviceInfoProvider(context: Context) : DebugInfoProvider {

        override val title: String = "Device"


        private val model: String = Build.MODEL
        private val manufacturer: String = Build.MANUFACTURER
        private val androidVersion: String = Build.VERSION.RELEASE
        private val sdkLevel: Int = Build.VERSION.SDK_INT
        private val buildId: String = Build.DISPLAY
        private val fingerprint: String = Build.FINGERPRINT
        private val hardware: String = Build.HARDWARE
        private val board: String = Build.BOARD
        private val bootloader: String = Build.BOOTLOADER
        private val supportedAbis: List<String> = Build.SUPPORTED_ABIS.toList()
        private val processorCount: Int = Runtime.getRuntime().availableProcessors()


        @Composable
        private fun getDisplayInfo(context: Context): Map<String, String> {
            val displayMetrics = context.resources.displayMetrics
            val density = displayMetrics.density
            val densityDpi = displayMetrics.densityDpi
            val densityClass = when (densityDpi) {
                in 0..DisplayMetrics.DENSITY_LOW -> "ldpi"
                in DisplayMetrics.DENSITY_LOW + 1..DisplayMetrics.DENSITY_MEDIUM -> "mdpi"
                in DisplayMetrics.DENSITY_MEDIUM + 1..DisplayMetrics.DENSITY_TV -> "tvdpi"
                in DisplayMetrics.DENSITY_TV + 1..DisplayMetrics.DENSITY_HIGH -> "hdpi"
                in DisplayMetrics.DENSITY_HIGH + 1..DisplayMetrics.DENSITY_XHIGH -> "xhdpi"
                in DisplayMetrics.DENSITY_XHIGH + 1..DisplayMetrics.DENSITY_XXHIGH -> "xxhdpi"
                else -> "xxxhdpi"
            }
            return mapOf(
                "Screen Width (px)" to displayMetrics.widthPixels.toString(),
                "Screen Height (px)" to displayMetrics.heightPixels.toString(),
                "Density (Scale)" to density.toString(),
                "Density (DPI)" to densityDpi.toString(),
                "Density Class" to densityClass
            )
        }

        @Composable
        private fun getMemoryInfo(context: Context): Map<String, String> {
            val activityManager =
                context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(memoryInfo)

            return if (activityManager != null) {
                mapOf(
                    "Total Memory" to formatBytes(memoryInfo.totalMem),
                    "Available Memory" to formatBytes(memoryInfo.availMem),
                    "Low Memory Threshold" to formatBytes(memoryInfo.threshold),
                    "Is Low Memory" to memoryInfo.lowMemory.toString()
                )
            } else {
                mapOf("Memory Info" to "Error: Could not get ActivityManager")
            }
        }

        @Composable
        private fun getStorageInfo(): Map<String, String> {

            val internalStat = StatFs(Environment.getDataDirectory().path)
            val internalTotalBytes = internalStat.blockCountLong * internalStat.blockSizeLong
            val internalAvailableBytes =
                internalStat.availableBlocksLong * internalStat.blockSizeLong


            val externalStorageState = Environment.getExternalStorageState()
            val externalInfo = if (externalStorageState == Environment.MEDIA_MOUNTED) {
                try {
                    val externalStat = StatFs(Environment.getExternalStorageDirectory().path)
                    val externalTotalBytes =
                        externalStat.blockCountLong * externalStat.blockSizeLong
                    val externalAvailableBytes =
                        externalStat.availableBlocksLong * externalStat.blockSizeLong
                    mapOf(
                        "External Storage (Total)" to formatBytes(externalTotalBytes),
                        "External Storage (Avail)" to formatBytes(externalAvailableBytes)
                    )
                } catch (e: Exception) {
                    mapOf("External Storage" to "Error reading: ${e.message}")
                }
            } else {
                mapOf("External Storage" to "State: $externalStorageState")
            }

            return mapOf(
                "Internal Storage (Total)" to formatBytes(internalTotalBytes),
                "Internal Storage (Avail)" to formatBytes(internalAvailableBytes)
            ) + externalInfo
        }


        @Composable
        override fun Content(context: Context) {

            InfoRow("Model:", model)
            InfoRow("Manufacturer:", manufacturer)
            InfoRow("Android Version:", androidVersion)
            InfoRow("SDK Level:", sdkLevel.toString())
            InfoRow("Build ID:", buildId)
            InfoRow("Fingerprint:", fingerprint)
            InfoRow("Hardware:", hardware)
            InfoRow("Board:", board)
            InfoRow("Bootloader:", bootloader)
            InfoRow("Processors:", processorCount.toString())
            InfoRow("Supported ABIs:", supportedAbis.joinToString(", "))

            Spacer(Modifier.height(10.dp))
            Divider()
            Spacer(Modifier.height(10.dp))


            Text(
                "Display",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            getDisplayInfo(context).forEach { (label, value) -> InfoRow(label, value) }

            Spacer(Modifier.height(10.dp))
            Divider()
            Spacer(Modifier.height(10.dp))

            Text(
                "Memory",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            getMemoryInfo(context).forEach { (label, value) -> InfoRow(label, value) }

            Spacer(Modifier.height(10.dp))
            Divider()
            Spacer(Modifier.height(10.dp))

            Text(
                "Storage",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            getStorageInfo().forEach { (label, value) -> InfoRow(label, value) }
        }
    }


    private class PermissionsProvider(context: Context) : DebugInfoProvider {
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

            LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
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
                    InfoRow(permission.substringAfterLast('.'), status)
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }

    private class FeaturesProvider(context: Context) : DebugInfoProvider {
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
            LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
                items(reqFeatures) { feature ->
                    val featureName = feature.name ?: "Unnamed Feature"
                    val required = (feature.flags and FeatureInfo.FLAG_REQUIRED) != 0
                    val version = if (feature.version > 0) " (v${feature.version})" else ""
                    InfoRow(featureName, "Required: $required$version")
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
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

    private fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes < 0) return "N/A"
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "${"%.1f".format(kb)} KB"
        val mb = kb / 1024.0
        if (mb < 1024) return "${"%.1f".format(mb)} MB"
        val gb = mb / 1024.0
        return "${"%.1f".format(gb)} GB"
    }
}