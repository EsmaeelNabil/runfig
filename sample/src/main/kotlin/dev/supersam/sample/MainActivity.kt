package dev.supersam.sample

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.supersam.runfig.sample.BuildConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                App()
            }
        }
    }

    @Composable
    private fun App() {
        var currentTheme by remember { mutableStateOf(BuildConfig.APP_THEME) }
        var animationSpeed by remember { mutableFloatStateOf(BuildConfig.ANIMATION_SPEED) }
        var apiUrl by remember { mutableStateOf(BuildConfig.API_BASE_URL) }
        var analyticsEnabled by remember { mutableStateOf(BuildConfig.ENABLE_ANALYTICS) }
        var networkTimeout by remember { mutableIntStateOf(BuildConfig.NETWORK_TIMEOUT) }

        // Refresh configs periodically to catch Runfig changes
        LaunchedEffect(Unit) {
            while (true) {
                delay(500) // Check every 500ms
                currentTheme = BuildConfig.APP_THEME
                animationSpeed = BuildConfig.ANIMATION_SPEED
                apiUrl = BuildConfig.API_BASE_URL
                analyticsEnabled = BuildConfig.ENABLE_ANALYTICS
                networkTimeout = BuildConfig.NETWORK_TIMEOUT
            }
        }

        // Theme-based colors
        val backgroundColor by animateColorAsState(
            targetValue = if (currentTheme == "dark") Color.DarkGray else Color.White,
            animationSpec = tween((1000 * animationSpeed).toInt()),
            label = "background"
        )

        val textColor = if (currentTheme == "dark") Color.White else Color.Black

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = backgroundColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Text(
                    text = "Runfig Sample App",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )

                Text(
                    text = "Long press anywhere to open Runfig overlay!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor.copy(alpha = 0.7f)
                )

                Divider()

                // Current Configuration Display
                ConfigCard(
                    title = "Current Configuration",
                    textColor = textColor
                ) {
                    ConfigRow("API URL", apiUrl, textColor)
                    ConfigRow("Network Timeout", "$networkTimeout ms", textColor)
                    ConfigRow(
                        "Analytics",
                        if (analyticsEnabled) "Enabled" else "Disabled",
                        textColor
                    )
                    ConfigRow("Theme", currentTheme, textColor)
                    ConfigRow("Animation Speed", "${animationSpeed}x", textColor)
                }

                // Interactive Demo
                DemoSection(
                    animationSpeed = animationSpeed,
                    textColor = textColor,
                    apiUrl = apiUrl,
                    timeout = networkTimeout
                )

                // Instructions
                InfoCard(textColor = textColor)
            }
        }
    }

    @Composable
    fun ConfigCard(
        title: String,
        textColor: Color,
        content: @Composable ColumnScope.() -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = textColor.copy(alpha = 0.1f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                content()
            }
        }
    }

    @Composable
    fun ConfigRow(label: String, value: String, textColor: Color) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = textColor.copy(alpha = 0.7f)
            )
            Text(
                text = value,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
        }
    }

    @Composable
    fun DemoSection(
        animationSpeed: Float,
        textColor: Color,
        apiUrl: String,
        timeout: Int
    ) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        var isLoading by remember { mutableStateOf(false) }
        var progress by remember { mutableFloatStateOf(0f) }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Interactive Demo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Animated Progress Bar
                val animatedProgress by animateFloatAsState(
                    targetValue = progress,
                    animationSpec = tween((500 * animationSpeed).toInt()),
                    label = "progress"
                )

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth(),
                )


                // Test API Button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isLoading = true
                            progress = 0f

                            // Simulate API call with timeout
                            for (i in 1..10) {
                                delay((timeout / 10).toLong())
                                progress = i / 10f
                            }

                            isLoading = false
                            Toast.makeText(
                                context,
                                "API Call to $apiUrl completed!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Test API Call")
                    }
                }

                Text(
                    text = "Animation speed affects the progress bar. " +
                            "Timeout affects the simulated API call duration.",
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.7f)
                )
            }
        }
    }

    @Composable
    fun InfoCard(textColor: Color) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "How to use:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                val instructions = listOf(
                    "1. Long press anywhere (2 seconds) to open Runfig overlay",
                    "2. Go to 'Preferences' tab to see BuildConfig values",
                    "3. Change any value and save",
                    "4. Watch this screen update in real-time!",
                    "5. Try changing APP_THEME to 'dark'",
                    "6. Adjust ANIMATION_SPEED to see effects"
                )

                instructions.forEach { instruction ->
                    Text(
                        text = instruction,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

