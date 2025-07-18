package dev.supersam.runfig.android.internal

import android.app.Activity
import androidx.activity.ComponentActivity

/**
 * Interface for controlling debug overlay display and interaction.
 * 
 * This interface abstracts the overlay controller functionality, making it easier
 * to test and maintain the overlay system. It provides methods for managing
 * overlay visibility, touch detection, and activity lifecycle integration.
 */
internal interface OverlayController {
    
    /**
     * Attaches the overlay controller to an activity.
     * 
     * This method sets up touch detection and prepares the overlay for display
     * on the given activity.
     * 
     * @param activity The activity to attach to
     */
    fun attachToActivity(activity: ComponentActivity)
    
    /**
     * Enables touch detection for the overlay.
     * 
     * @param activity The activity to enable touch detection for
     */
    fun enableTouchDetection(activity: Activity)
    
    /**
     * Disables touch detection for the overlay.
     */
    fun disableTouchDetection()
    
    /**
     * Called when an activity is resumed.
     * 
     * @param activity The activity that was resumed
     */
    fun onActivityResumed(activity: Activity)
    
    /**
     * Called when an activity is paused.
     * 
     * @param activity The activity that was paused
     */
    fun onActivityPaused(activity: Activity)
    
    /**
     * Shows the overlay on the current activity.
     */
    fun showOverlay()
    
    /**
     * Hides the overlay from the current activity.
     */
    fun hideOverlay()
    
    /**
     * Cleans up resources and references.
     */
    fun cleanup()
}