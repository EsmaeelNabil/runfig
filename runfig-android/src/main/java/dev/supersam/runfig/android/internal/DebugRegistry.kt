package dev.supersam.runfig.android.internal

import android.content.Context
import dev.supersam.runfig.android.api.DebugAction
import dev.supersam.runfig.android.api.DebugInfoProvider
import dev.supersam.runfig.android.features.NetworkInfoProvider
import dev.supersam.runfig.android.features.SharedPreferencesInfoProvider
import dev.supersam.runfig.android.features.actions.ClearAppDataAction
import dev.supersam.runfig.android.features.actions.ClearCacheAction
import dev.supersam.runfig.android.features.actions.RestartAppAction
import dev.supersam.runfig.android.features.providers.AppInfoProvider
import dev.supersam.runfig.android.features.providers.DeviceInfoProvider

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
     * Gets all registered debug info providers.
     *
     *
     * @return List of all registered providers
     */
    fun getProviders(): List<DebugInfoProvider> {
        return _providers.toList()
    }

    /**
     * Gets all registered debug actions.
     *
     *
     * @return List of all registered actions
     */
    fun getActions(): List<DebugAction> {
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
    }

    /**
     * Registers providers that require Context.
     * This should be called when Context is available.
     *
     * @param context The context to use for provider initialization
     */
    fun registerContextProviders(context: Context) {
        _providers.add(AppInfoProvider(context))
        _providers.add(DeviceInfoProvider())
    }
}