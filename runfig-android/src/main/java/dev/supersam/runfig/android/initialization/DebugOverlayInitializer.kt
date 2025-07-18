package dev.supersam.runfig.android.initialization

import android.content.Context
import androidx.startup.Initializer
import dev.supersam.runfig.android.internal.DebugOverlayManager

/**
 * App Startup initializer for the debug overlay system.
 * 
 * This initializer is automatically called by the AndroidX App Startup library
 * when the app starts. It performs zero-configuration initialization of the
 * debug overlay system.
 * 
 * The initializer:
 * - Calls DebugOverlayManager.autoInitialize() to set up the system
 * - Registers default providers and actions
 * - Sets up activity lifecycle callbacks
 * - Configures crash logging
 * 
 * This class is automatically invoked by the App Startup library and should
 * not be called directly by users.
 */
class DebugOverlayInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        // Initialize the debug overlay system using the new architecture
        DebugOverlayManager.autoInitialize(context.applicationContext)

        // Set up crash logging
        setupCrashLogging(context.applicationContext)

        return
    }

    /**
     * Sets up crash logging to capture uncaught exceptions.
     * 
     * @param context The application context
     */
    private fun setupCrashLogging(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                CrashLogManager.logCrash(context, throwable)
            } catch (e: Exception) {
                // Ignore crash logging errors to prevent infinite loops
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        // No dependencies required for zero-configuration setup
        return emptyList()
    }
}