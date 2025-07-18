package dev.supersam.runfig.android.api

import dev.supersam.runfig.android.internal.DebugOverlayManager

/**
 * Legacy registry for custom providers and actions.
 * 
 * This registry is deprecated and maintained for backward compatibility.
 * It routes calls to the new architecture while maintaining the same API.
 * 
 * @deprecated This registry is deprecated and will be removed in a future version.
 * Use DebugOverlay.addInfoProvider() and DebugOverlay.addAction() instead.
 */
@Deprecated("Use DebugOverlay.addInfoProvider() and DebugOverlay.addAction() instead")
internal object DebugOverlayRegistry {
    
    /**
     * Gets all registered info providers from the new architecture.
     * 
     * @return List of registered info providers
     */
    internal val infoProviders: List<DebugInfoProvider>
        get() = DebugOverlayManager.getContainer()?.registry()?.getProviders() ?: emptyList()
    
    /**
     * Gets all registered actions from the new architecture.
     * 
     * @return List of registered actions
     */
    internal val actions: List<DebugAction>
        get() = DebugOverlayManager.getContainer()?.registry()?.getActions() ?: emptyList()

    /**
     * Adds a provider to the new architecture.
     * 
     * @param provider The provider to add
     */
    fun addProvider(provider: DebugInfoProvider) {
        DebugOverlayManager.getContainer()?.registry()?.addProvider(provider)
    }

    /**
     * Adds an action to the new architecture.
     * 
     * @param action The action to add
     */
    fun addAction(action: DebugAction) {
        DebugOverlayManager.getContainer()?.registry()?.addAction(action)
    }
}

