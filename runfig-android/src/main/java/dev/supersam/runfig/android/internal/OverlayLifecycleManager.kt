package dev.supersam.runfig.android.internal

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import java.lang.ref.WeakReference

/**
 * Manages the lifecycle of the debug overlay in relation to activities.
 * 
 * This manager handles the automatic setup and teardown of the debug overlay
 * as activities are created and destroyed. It ensures proper resource management
 * and prevents memory leaks by cleaning up resources when activities are destroyed.
 * 
 * The manager automatically:
 * - Sets up touch detection for new activities
 * - Enables/disables overlay based on activity state
 * - Cleans up resources when activities are destroyed
 * - Manages overlay visibility during activity transitions
 * 
 * This class is internal and should never be accessed directly by users.
 */
internal class OverlayLifecycleManager(
    private val stateManager: OverlayStateManager,
    private val registry: DebugRegistry,
    private val overlayController: OverlayController
) {
    
    /**
     * Weak reference to the current activity to prevent memory leaks.
     */
    private var currentActivity: WeakReference<Activity>? = null
    
    /**
     * Called when an activity is created.
     * 
     * Sets up the overlay for the new activity and establishes touch detection.
     * 
     * @param activity The newly created activity
     */
    fun onActivityCreated(activity: Activity) {
        currentActivity = WeakReference(activity)
        
        // Set up touch detection for ComponentActivity instances
        if (activity is ComponentActivity) {
            setupTouchDetection(activity)
        }
    }
    
    /**
     * Called when an activity is started.
     * 
     * Enables touch detection and prepares the overlay for user interaction.
     * 
     * @param activity The activity being started
     */
    fun onActivityStarted(activity: Activity) {
        // Enable touch detection when activity becomes visible
        overlayController.enableTouchDetection(activity)
    }
    
    /**
     * Called when an activity is resumed.
     * 
     * Ensures the overlay is ready for interaction in the foreground activity.
     * 
     * @param activity The activity being resumed
     */
    fun onActivityResumed(activity: Activity) {
        // Update current activity reference
        currentActivity = WeakReference(activity)
        
        // Ensure overlay is ready for interaction
        overlayController.onActivityResumed(activity)
    }
    
    /**
     * Called when an activity is paused.
     * 
     * Handles overlay state when the activity moves to the background.
     * 
     * @param activity The activity being paused
     */
    fun onActivityPaused(activity: Activity) {
        // Hide overlay when activity is paused to avoid issues
        stateManager.setVisibility(false)
        
        // Notify controller of activity pause
        overlayController.onActivityPaused(activity)
    }
    
    /**
     * Called when an activity is stopped.
     * 
     * Disables touch detection and hides the overlay when activity is not visible.
     * 
     * @param activity The activity being stopped
     */
    fun onActivityStopped(activity: Activity) {
        // Disable touch detection when activity is not visible
        overlayController.disableTouchDetection()
        
        // Ensure overlay is hidden
        stateManager.setVisibility(false)
    }
    
    /**
     * Called when an activity is destroyed.
     * 
     * Cleans up resources and references to prevent memory leaks.
     * 
     * @param activity The activity being destroyed
     */
    fun onActivityDestroyed(activity: Activity) {
        // Clean up if this was the current activity
        if (currentActivity?.get() == activity) {
            cleanup()
        }
    }
    
    /**
     * Sets up touch detection for the given activity.
     * 
     * This method configures the overlay controller to detect long press gestures
     * on the given activity.
     * 
     * @param activity The activity to set up touch detection for
     */
    private fun setupTouchDetection(activity: ComponentActivity) {
        try {
            overlayController.attachToActivity(activity)
        } catch (e: Exception) {
            // Log error but don't crash the app
            // In a real implementation, you might want to log this properly
        }
    }
    
    /**
     * Cleans up resources and references.
     * 
     * This method is called when the current activity is destroyed or when
     * the overlay system needs to be reset.
     */
    private fun cleanup() {
        // Clean up overlay controller
        overlayController.cleanup()
        
        // Hide overlay
        stateManager.setVisibility(false)
        
        // Clear activity reference
        currentActivity = null
    }
    
    /**
     * Gets the current activity if available.
     * 
     * @return The current activity or null if not available
     */
    fun getCurrentActivity(): Activity? {
        return currentActivity?.get()
    }
}

/**
 * Activity lifecycle callbacks that bridge Android's lifecycle system with our overlay lifecycle manager.
 * 
 * This class is automatically registered with the Application to receive lifecycle callbacks
 * for all activities in the app.
 */
internal class DebugActivityCallbacks(
    private val container: DebugOverlayContainer
) : Application.ActivityLifecycleCallbacks {
    
    /**
     * Lazy reference to the lifecycle manager to avoid creating it unnecessarily.
     */
    private val lifecycleManager by lazy { container.lifecycleManager() }
    
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        lifecycleManager.onActivityCreated(activity)
    }
    
    override fun onActivityStarted(activity: Activity) {
        lifecycleManager.onActivityStarted(activity)
    }
    
    override fun onActivityResumed(activity: Activity) {
        lifecycleManager.onActivityResumed(activity)
    }
    
    override fun onActivityPaused(activity: Activity) {
        lifecycleManager.onActivityPaused(activity)
    }
    
    override fun onActivityStopped(activity: Activity) {
        lifecycleManager.onActivityStopped(activity)
    }
    
    override fun onActivityDestroyed(activity: Activity) {
        lifecycleManager.onActivityDestroyed(activity)
    }
    
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        // No action needed for overlay
    }
}