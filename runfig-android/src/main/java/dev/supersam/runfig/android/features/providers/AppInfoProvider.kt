package dev.supersam.runfig.android.features.providers

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.PackageInfoCompat
import dev.supersam.runfig.android.api.DebugInfoProvider

internal class AppInfoProvider(context: Context) : DebugInfoProvider {

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

        DefaultInfoProviders.InfoRow("App Name:", appName)
        DefaultInfoProviders.InfoRow("Package:", packageName)
        DefaultInfoProviders.InfoRow("Version Name:", versionName)
        DefaultInfoProviders.InfoRow("Version Code:", versionCode.toString())
        DefaultInfoProviders.InfoRow("Target SDK:", targetSdk.takeIf { it > 0 }?.toString() ?: "N/A")
        DefaultInfoProviders.InfoRow("Min SDK:", minSdk.takeIf { it > 0 }?.toString() ?: "N/A")
        DefaultInfoProviders.InfoRow("Debuggable:", isDebuggable.toString())
        DefaultInfoProviders.InfoRow("Installer:", installerPackage ?: "N/A")
        DefaultInfoProviders.InfoRow("Installed:", firstInstallTime)
        DefaultInfoProviders.InfoRow("Updated:", lastUpdateTime)
        DefaultInfoProviders.InfoRow("Data Dir:", dataDir)
        DefaultInfoProviders.InfoRow("Process Name:", processName)

        Spacer(Modifier.Companion.height(8.dp))

        Text(
            "Note: Build Type & Flavor require host app integration (custom DebugInfoProvider accessing BuildConfig).",
            style = MaterialTheme.typography.labelSmall
        )
    }
}