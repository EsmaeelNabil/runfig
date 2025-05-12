package dev.supersam.runfig.android.features.providers

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.DisplayMetrics
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.supersam.runfig.android.api.DebugInfoProvider

internal class DeviceInfoProvider(context: Context) : DebugInfoProvider {

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

        DefaultInfoProviders.InfoRow("Model:", model)
        DefaultInfoProviders.InfoRow("Manufacturer:", manufacturer)
        DefaultInfoProviders.InfoRow("Android Version:", androidVersion)
        DefaultInfoProviders.InfoRow("SDK Level:", sdkLevel.toString())
        DefaultInfoProviders.InfoRow("Build ID:", buildId)
        DefaultInfoProviders.InfoRow("Fingerprint:", fingerprint)
        DefaultInfoProviders.InfoRow("Hardware:", hardware)
        DefaultInfoProviders.InfoRow("Board:", board)
        DefaultInfoProviders.InfoRow("Bootloader:", bootloader)
        DefaultInfoProviders.InfoRow("Processors:", processorCount.toString())
        DefaultInfoProviders.InfoRow("Supported ABIs:", supportedAbis.joinToString(", "))

        Spacer(Modifier.Companion.height(10.dp))
        Divider()
        Spacer(Modifier.Companion.height(10.dp))


        Text(
            "Display",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Companion.Bold
        )
        getDisplayInfo(context).forEach { (label, value) -> DefaultInfoProviders.InfoRow(label, value) }

        Spacer(Modifier.Companion.height(10.dp))
        Divider()
        Spacer(Modifier.Companion.height(10.dp))

        Text(
            "Memory",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Companion.Bold
        )
        getMemoryInfo(context).forEach { (label, value) -> DefaultInfoProviders.InfoRow(label, value) }

        Spacer(Modifier.Companion.height(10.dp))
        Divider()
        Spacer(Modifier.Companion.height(10.dp))

        Text(
            "Storage",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Companion.Bold
        )
        getStorageInfo().forEach { (label, value) -> DefaultInfoProviders.InfoRow(label, value) }
    }
}