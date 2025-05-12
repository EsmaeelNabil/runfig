package dev.supersam.runfig.android.features

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.supersam.runfig.android.api.DebugInfoProvider

internal class NetworkInfoProvider : DebugInfoProvider {
    override val title: String = "Network"

    @Composable
    override fun Content(context: Context) {
        val connectivityManager =
            remember { context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager }
        var networkState by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

        LaunchedEffect(connectivityManager) {
            if (connectivityManager == null) {
                networkState = mapOf("Error" to "Could not get ConnectivityManager")
                return@LaunchedEffect
            }

            val activeNetwork = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

            val info = mutableMapOf<String, String>()
            if (activeNetwork == null || capabilities == null) {
                info["Status"] = "Disconnected"
            } else {
                info["Status"] = "Connected"
                when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                        info["Type"] = "WiFi"

                    }

                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                        info["Type"] = "Cellular"

                    }

                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> info["Type"] =
                        "Ethernet"

                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> info["Type"] =
                        "VPN"

                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> info["Type"] =
                        "Bluetooth"

                    else -> info["Type"] = "Other"
                }
                info["Validated"] =
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                        .toString()
                info["Metered"] =
                    (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)).toString()
            }
            networkState = info
        }

        if (networkState.isEmpty()) {
            Text("Loading network state...")
        } else {
            networkState.forEach { (key, value) -> InfoRow(label = key, value = value) }
        }
        Spacer(modifier = Modifier.Companion.height(8.dp))
        Text(
            "Note: More details (IP, SSID, etc.) may require additional permissions (ACCESS_WIFI_STATE, ACCESS_NETWORK_STATE).",
            style = MaterialTheme.typography.bodySmall
        )
    }
}