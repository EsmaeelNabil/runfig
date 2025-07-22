package dev.supersam.runfig.android.internal

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import androidx.core.content.edit

/**
 * Repository for managing debug overlay configuration.
 *
 * This repository handles loading, saving, and providing configuration for the debug overlay.
 * It automatically detects the build type and provides appropriate defaults for different
 * environments (debug, staging, release).
 *
 * The repository uses SharedPreferences for persistence and provides smart defaults
 * that work well for most use cases without requiring any user configuration.
 *
 * This class is internal and should never be accessed directly by users.
 */
internal class ConfigRepository(private val application: Application) {

    /**
     * SharedPreferences instance for storing configuration.
     */
    private val prefs: SharedPreferences by lazy {
        application.getSharedPreferences("runfig_internal_config", Context.MODE_PRIVATE)
    }

    /**
     * Cached configuration instance, loaded lazily when first accessed.
     */
    private val _config: DebugOverlayConfig by lazy { loadOrCreateDefaultConfig() }

    /**
     * Gets the current configuration.
     *
     * @return The current configuration with smart defaults
     */
    fun getConfig(): DebugOverlayConfig = _config

    /**
     * Saves a new configuration to persistent storage.
     *
     * @param config The configuration to save
     */
    fun saveConfig(config: DebugOverlayConfig) {
        if (prefs.contains("long_press_delay").not())
            prefs.edit {
                putLong("long_press_delay", config.longPressDelayMs)
            }
    }

    /**
     * Loads configuration from persistent storage or creates default configuration.
     *
     * @return The loaded or default configuration
     */
    private fun loadOrCreateDefaultConfig(): DebugOverlayConfig {
        return DebugOverlayConfig(
            longPressDelayMs = prefs.getLong("long_press_delay", 2000),
        )
    }

    /**
     * Gets default providers based on build type.
     *
     * Debug builds get more providers for comprehensive debugging,
     * while release builds get minimal providers for basic information.
     *
     * @return Set of default provider identifiers
     */
    private fun getDefaultProviders(): Set<String> {
        return if (isDebugBuild()) {
            setOf(
                "app_info",
                "device_info",
                "network_info",
                "shared_preferences",
                "crash_logs",
                "feature_flags",
                "permissions"
            )
        } else {
            setOf(
                "app_info",
                "device_info",
                "network_info"
            )
        }
    }

    /**
     * Gets default actions based on build type.
     *
     * Debug builds get more actions including potentially destructive ones,
     * while release builds get only safe actions.
     *
     * @return Set of default action identifiers
     */
    private fun getDefaultActions(): Set<String> {
        return if (isDebugBuild()) {
            setOf(
                "clear_cache",
                "restart_app",
                "clear_app_data",
                "kill_app",
                "navigate_to_settings",
                "navigate_to_store"
            )
        } else {
            setOf(
                "clear_cache",
                "restart_app",
                "navigate_to_settings"
            )
        }
    }

    /**
     * Checks if the current build is a debug build.
     *
     * @return True if debug build, false otherwise
     */
    private fun isDebugBuild(): Boolean {
        return (application.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
}

/**
 * Configuration data class for the debug overlay.
 *

 * @property longPressDelayMs Delay in milliseconds for long press activation
 */
internal data class DebugOverlayConfig(val longPressDelayMs: Long = 2000)

/**
 * Available themes for the debug overlay.
 */
internal enum class OverlayTheme {
    /**
     * Light theme with bright colors.
     */
    LIGHT,

    /**
     * Dark theme with dark colors.
     */
    DARK,

    /**
     * System theme that follows the device's dark mode setting.
     */
    SYSTEM
}