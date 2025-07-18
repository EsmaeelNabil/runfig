package dev.supersam.runfig.android.internal

import android.app.Application

/**
 * Internal dependency injection container for the debug overlay system.
 * 
 * This container manages all internal dependencies and provides them to components
 * that need them. It uses lazy initialization to create dependencies only when needed,
 * improving app startup performance.
 * 
 * The container follows a simple factory pattern rather than using heavy DI frameworks
 * to keep the library lightweight and easy to understand.
 * 
 * This class is internal and should never be accessed directly by users.
 */
internal class DebugOverlayContainer private constructor(
    private val application: Application
) {
    
    /**
     * Manages overlay state and visibility.
     * Created lazily when first accessed.
     */
    private val _stateManager by lazy { OverlayStateManager() }
    
    /**
     * Registry for debug providers and actions.
     * Created lazily when first accessed.
     */
    private val _registry by lazy { DebugRegistry() }
    
    /**
     * Handles configuration storage and retrieval.
     * Created lazily when first accessed.
     */
    private val _configRepository by lazy { ConfigRepository(application) }
    
    /**
     * Controls overlay display and touch detection.
     * Created lazily when first accessed.
     */
    private val _overlayController by lazy { 
        OverlayControllerImpl(_stateManager, _registry, _configRepository) 
    }
    
    /**
     * Manages activity lifecycle and overlay lifecycle.
     * Created lazily when first accessed.
     */
    private val _lifecycleManager by lazy { 
        OverlayLifecycleManager(_stateManager, _registry, _overlayController) 
    }
    
    /**
     * Provides access to the state manager.
     * 
     * @return The overlay state manager instance
     */
    internal fun stateManager(): OverlayStateManager = _stateManager
    
    /**
     * Provides access to the debug registry.
     * 
     * @return The debug registry instance
     */
    internal fun registry(): DebugRegistry = _registry
    
    /**
     * Provides access to the configuration repository.
     * 
     * @return The configuration repository instance
     */
    internal fun configRepository(): ConfigRepository = _configRepository
    
    /**
     * Provides access to the overlay controller.
     * 
     * @return The overlay controller instance
     */
    internal fun overlayController(): OverlayController = _overlayController
    
    /**
     * Provides access to the lifecycle manager.
     * 
     * @return The lifecycle manager instance
     */
    internal fun lifecycleManager(): OverlayLifecycleManager = _lifecycleManager
    
    companion object {
        /**
         * Creates a new dependency injection container.
         * 
         * @param application The application instance
         * @return A new container instance with all dependencies configured
         */
        fun create(application: Application): DebugOverlayContainer {
            return DebugOverlayContainer(application)
        }
    }
}