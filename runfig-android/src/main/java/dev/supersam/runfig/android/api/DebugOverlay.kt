package dev.supersam.runfig.android.api

import dev.supersam.runfig.android.internal.DebugOverlayManager

/**
 * Public entry point for developers to register custom components.
 * 
 * This is the main API that developers use to extend the debug overlay with their own
 * custom information providers and actions. The library works automatically without
 * any setup, but developers can use this API to add additional functionality.
 * 
 * Usage:
 * ```kotlin
 * // Add a custom info provider
 * DebugOverlay.addInfoProvider(object : DebugInfoProvider {
 *     override val title = "My Custom Info"
 *     @Composable
 *     override fun Content(context: Context) {
 *         Text("Custom debug information")
 *     }
 * })
 * 
 * // Add a custom action
 * DebugOverlay.addAction(object : DebugAction {
 *     override val title = "My Custom Action"
 *     override val description = "Does something custom"
 *     override suspend fun onAction(context: Context) {
 *         // Custom action logic
 *     }
 * })
 * ```
 */
object DebugOverlay {
    
    /**
     * Adds a custom debug info provider to the overlay.
     * 
     * The provider will be displayed in the "Info" tab of the debug overlay,
     * allowing developers to add their own debug information.
     * 
     * @param provider The debug info provider to add
     */
    fun addInfoProvider(provider: DebugInfoProvider) {
        val container = DebugOverlayManager.getContainer()
        container?.registry()?.addProvider(provider)
    }

    /**
     * Adds a custom debug action to the overlay.
     * 
     * The action will be displayed in the "Actions" tab of the debug overlay,
     * allowing developers to add their own debug actions.
     * 
     * @param action The debug action to add
     */
    fun addAction(action: DebugAction) {
        val container = DebugOverlayManager.getContainer()
        container?.registry()?.addAction(action)
    }
}