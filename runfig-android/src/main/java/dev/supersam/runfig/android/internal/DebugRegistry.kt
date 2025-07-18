package dev.supersam.runfig.android.internal

import android.content.Context
import dev.supersam.runfig.android.api.DebugAction
import dev.supersam.runfig.android.api.DebugInfoProvider
import dev.supersam.runfig.android.features.actions.ClearAppDataAction
import dev.supersam.runfig.android.features.actions.ClearCacheAction
import dev.supersam.runfig.android.features.actions.RestartAppAction
import dev.supersam.runfig.android.features.providers.AppInfoProvider
import dev.supersam.runfig.android.features.providers.DeviceInfoProvider
import dev.supersam.runfig.android.features.NetworkInfoProvider
import dev.supersam.runfig.android.features.SharedPreferencesInfoProvider

/**
 * Internal registry for debug providers and actions.
 * 
 * This registry manages all debug providers and actions, automatically registering
 * default components when first accessed. It replaces the global mutable state
 * from the original implementation with a cleaner, more manageable approach.
 * 
 * The registry automatically provides useful default functionality without requiring
 * any user setup, while still allowing users to add custom providers and actions.
 * 
 * This class is internal and should never be accessed directly by users.
 */
internal class DebugRegistry {
    
    /**
     * List of registered debug info providers.
     */
    private val _providers = mutableListOf<DebugInfoProvider>()
    
    /**
     * List of registered debug actions.
     */
    private val _actions = mutableListOf<DebugAction>()
    
    /**
     * Flag to track if default components have been registered.
     * This ensures defaults are only registered once.
     */
    private var defaultsRegistered = false
    
    /**
     * Gets all registered debug info providers.
     * 
     * Automatically registers default providers on first access to ensure
     * the overlay has useful functionality without any user setup.
     * 
     * @return List of all registered providers
     */
    fun getProviders(): List<DebugInfoProvider> {
        ensureDefaultsRegistered()
        return _providers.toList()
    }
    
    /**
     * Gets all registered debug actions.
     * 
     * Automatically registers default actions on first access to ensure
     * the overlay has useful functionality without any user setup.
     * 
     * @return List of all registered actions
     */
    fun getActions(): List<DebugAction> {
        ensureDefaultsRegistered()
        return _actions.toList()
    }
    
    /**
     * Adds a custom debug info provider.
     * 
     * This method allows users to extend the overlay with their own
     * debug information providers.
     * 
     * @param provider The provider to add
     */
    fun addProvider(provider: DebugInfoProvider) {
        _providers.add(provider)
    }
    
    /**
     * Adds a custom debug action.
     * 
     * This method allows users to extend the overlay with their own
     * debug actions.
     * 
     * @param action The action to add
     */
    fun addAction(action: DebugAction) {
        _actions.add(action)
    }
    
    /**
     * Removes a previously added provider.
     * 
     * @param provider The provider to remove
     * @return True if the provider was removed, false if not found
     */
    fun removeProvider(provider: DebugInfoProvider): Boolean {
        return _providers.remove(provider)
    }
    
    /**
     * Removes a previously added action.
     * 
     * @param action The action to remove
     * @return True if the action was removed, false if not found
     */
    fun removeAction(action: DebugAction): Boolean {
        return _actions.remove(action)
    }
    
    /**
     * Clears all registered providers and actions.
     * 
     * This method is useful for testing or when completely reinitializing
     * the registry.
     */
    fun clear() {
        _providers.clear()
        _actions.clear()
        defaultsRegistered = false
    }
    
    /**
     * Ensures that default components are registered.
     * 
     * This method is called automatically when providers or actions are accessed
     * to ensure the overlay has useful default functionality.
     */
    private fun ensureDefaultsRegistered() {
        if (!defaultsRegistered) {
            registerDefaultComponents()
            defaultsRegistered = true
        }
    }
    
    /**
     * Registers all default providers and actions.
     * 
     * This method automatically adds useful debug functionality:
     * - App information (version, package name, etc.)
     * - Device information (model, OS version, etc.)
     * - Network information (connection status, etc.)
     * - SharedPreferences viewer
     * - Cache clearing action
     * - App restart action
     * - App data clearing action
     * 
     * Note: This method registers providers that don't require Context.
     * Context-dependent providers should be registered when Context is available.
     */
    private fun registerDefaultComponents() {
        // Register default info providers that don't need Context
        _providers.add(NetworkInfoProvider())
        _providers.add(SharedPreferencesInfoProvider())
        
        // Register default actions
        _actions.add(ClearCacheAction())
        _actions.add(RestartAppAction())
        _actions.add(ClearAppDataAction())
    }
    
    /**
     * Registers providers that require Context.
     * This should be called when Context is available.
     * 
     * @param context The context to use for provider initialization
     */
    fun registerContextProviders(context: Context) {
        _providers.add(AppInfoProvider(context))
        _providers.add(DeviceInfoProvider(context))
    }
}