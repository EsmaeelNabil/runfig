package dev.supersam.runfig.android.internal

/**
 * Simple state manager for the debug overlay.
 * 
 * This class manages the current state of the debug overlay including visibility,
 * current tab, and configuration. It uses a simple callback system instead of
 * complex reactive frameworks to keep the library lightweight.
 * 
 * The state manager notifies interested components when state changes occur,
 * allowing for reactive UI updates without heavy dependencies.
 * 
 * This class is internal and should never be accessed directly by users.
 */
internal class OverlayStateManager {
    
    /**
     * Current overlay visibility state.
     */
    private var _isVisible = false
    
    /**
     * Currently selected tab in the overlay.
     */
    private var _currentTab = OverlayTab.PREFERENCES
    
    /**
     * Current configuration being used by the overlay.
     */
    private var _config: DebugOverlayConfig? = null
    
    /**
     * List of callbacks to notify when state changes.
     */
    private val stateCallbacks = mutableListOf<(OverlayState) -> Unit>()
    
    /**
     * Gets the current visibility state.
     * 
     * @return True if overlay is visible, false otherwise
     */
    val isVisible: Boolean get() = _isVisible
    
    /**
     * Gets the currently selected tab.
     * 
     * @return The current tab
     */
    val currentTab: OverlayTab get() = _currentTab
    
    /**
     * Sets the overlay visibility state.
     * 
     * @param visible True to show overlay, false to hide
     */
    fun setVisibility(visible: Boolean) {
        if (_isVisible != visible) {
            _isVisible = visible
            notifyStateChanged()
        }
    }
    
    /**
     * Sets the currently selected tab.
     * 
     * @param tab The tab to select
     */
    fun setCurrentTab(tab: OverlayTab) {
        if (_currentTab != tab) {
            _currentTab = tab
            notifyStateChanged()
        }
    }
    
    /**
     * Updates the configuration and notifies listeners.
     * 
     * @param config The new configuration to use
     */
    fun updateConfig(config: DebugOverlayConfig) {
        _config = config
        notifyStateChanged()
    }
    
    /**
     * Gets the effective configuration, falling back to defaults if none set.
     * 
     * @return The current configuration or default configuration
     */
    fun getEffectiveConfig(): DebugOverlayConfig {
        return _config ?: DebugOverlayConfig()
    }
    
    /**
     * Adds a callback to be notified when state changes.
     * 
     * @param callback The callback to add
     */
    fun addStateCallback(callback: (OverlayState) -> Unit) {
        stateCallbacks.add(callback)
    }
    
    /**
     * Removes a previously added state callback.
     * 
     * @param callback The callback to remove
     */
    fun removeStateCallback(callback: (OverlayState) -> Unit) {
        stateCallbacks.remove(callback)
    }
    
    /**
     * Notifies all registered callbacks of state changes.
     */
    private fun notifyStateChanged() {
        val state = OverlayState(_isVisible, _currentTab, getEffectiveConfig())
        stateCallbacks.forEach { callback ->
            try {
                callback(state)
            } catch (e: Exception) {
                // Ignore callback exceptions to prevent one bad callback from breaking others
            }
        }
    }
}

/**
 * Represents the current state of the debug overlay.
 * 
 * @property isVisible Whether the overlay is currently visible
 * @property currentTab The currently selected tab
 * @property config The current configuration being used
 */
internal data class OverlayState(
    val isVisible: Boolean,
    val currentTab: OverlayTab,
    val config: DebugOverlayConfig
)

/**
 * Available tabs in the debug overlay.
 */
internal enum class OverlayTab {
    /**
     * Tab showing editable preferences and configuration values.
     */
    PREFERENCES,
    
    /**
     * Tab showing debug information from various providers.
     */
    INFO,
    
    /**
     * Tab showing executable debug actions.
     */
    ACTIONS
}