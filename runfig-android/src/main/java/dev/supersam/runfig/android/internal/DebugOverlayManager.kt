package dev.supersam.runfig.android.internal

import android.app.Application
import android.content.Context
import dev.supersam.runfig.android.features.actions.ClearAppDataAction
import dev.supersam.runfig.android.features.actions.ClearCacheAction
import dev.supersam.runfig.android.features.actions.RestartAppAction
import dev.supersam.runfig.android.features.providers.AppInfoProvider
import dev.supersam.runfig.android.features.providers.DeviceInfoProvider
import dev.supersam.runfig.android.features.NetworkInfoProvider
import dev.supersam.runfig.android.features.SharedPreferencesInfoProvider

/**
 * Internal manager responsible for auto-initializing the debug overlay system.
 * 
 * This manager handles the zero-configuration setup of the debug overlay:
 * - Automatically creates and configures all internal components
 * - Registers default providers and actions
 * - Sets up activity lifecycle callbacks
 * - Ensures thread-safe initialization
 * 
 * This class is internal and should never be accessed directly by users.
 * The overlay works automatically when the library is added to the project.
 */
internal object DebugOverlayManager {
    
    /**
     * The dependency injection container holding all components.
     * Null until auto-initialization is complete.
     */
    private var container: DebugOverlayContainer? = null
    
    /**
     * Thread-safe initialization flag to prevent multiple initialization.
     */
    @Volatile
    private var isInitialized = false
    
    /**
     * Automatically initializes the debug overlay system.
     * 
     * This method is called by the ContentProvider during app startup.
     * It performs the following steps:
     * 1. Creates the dependency injection container
     * 2. Registers default providers and actions
     * 3. Sets up activity lifecycle callbacks
     * 4. Configures automatic touch detection
     * 
     * @param context The application context from ContentProvider
     */
    fun autoInitialize(context: Context) {
        if (isInitialized) return
        
        synchronized(this) {
            if (isInitialized) return
            
            val appContext = context.applicationContext
            if (appContext is Application) {
                // Create the dependency injection container
                container = DebugOverlayContainer.create(appContext)
                
                // Register default components automatically
                registerDefaultComponents(appContext)
                
                // Set up activity lifecycle callbacks for automatic management
                appContext.registerActivityLifecycleCallbacks(
                    DebugActivityCallbacks(container!!)
                )
                
                isInitialized = true
            }
        }
    }
    
    /**
     * Registers all default providers and actions automatically.
     * 
     * This method is called during initialization to populate the overlay
     * with useful default functionality without requiring any user setup.
     * 
     * @param context The application context to use for provider initialization
     */
    private fun registerDefaultComponents(context: Context) {
        val registry = container?.registry() ?: return
        
        // Auto-register built-in providers
        // These provide useful information for debugging without any setup
        registry.addProvider(AppInfoProvider(context))
        registry.addProvider(DeviceInfoProvider(context))
        registry.addProvider(NetworkInfoProvider())
        registry.addProvider(SharedPreferencesInfoProvider())
        
        // Auto-register built-in actions
        // These provide common debugging tasks
        registry.addAction(ClearCacheAction())
        registry.addAction(RestartAppAction())
        registry.addAction(ClearAppDataAction())
    }
    
    /**
     * Provides access to the dependency injection container.
     * 
     * @return The container instance, or null if not initialized
     */
    internal fun getContainer(): DebugOverlayContainer? = container
    
    /**
     * Checks if the debug overlay system is initialized.
     * 
     * @return True if initialized, false otherwise
     */
    internal fun isInitialized(): Boolean = isInitialized
}