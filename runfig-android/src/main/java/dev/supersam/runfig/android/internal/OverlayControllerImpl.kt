package dev.supersam.runfig.android.internal

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.ActionMode
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.SearchEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import dev.supersam.runfig.android.R
import dev.supersam.runfig.android.ui.DebugOverlayScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.lang.ref.WeakReference

/**
 * Implementation of the overlay controller that manages debug overlay display and interaction.
 * 
 * This controller handles:
 * - Touch detection for long press gesture
 * - Overlay view creation and management
 * - Activity lifecycle integration
 * - Resource cleanup and memory management
 * 
 * The controller integrates with the new architecture by using dependency injection
 * and working with the state manager and registry.
 */
internal class OverlayControllerImpl(
    private val stateManager: OverlayStateManager,
    private val registry: DebugRegistry,
    private val configRepository: ConfigRepository
) : OverlayController {
    
    private companion object {
        private const val TAG = "OverlayController"
    }
    
    /**
     * Handler for managing long press timer on the main thread.
     */
    private val handler = Handler(Looper.getMainLooper())
    
    /**
     * Runnable for handling long press timeout.
     */
    private var longPressRunnable: Runnable? = null
    
    /**
     * Flag to track if long press timer is currently running.
     */
    @Volatile
    private var isLongPressRunnablePending = false
    
    /**
     * Weak reference to the current activity to prevent memory leaks.
     */
    private var currentActivityRef: WeakReference<Activity>? = null
    
    /**
     * Coroutine scope for overlay UI operations.
     */
    private var overlayScope: CoroutineScope? = null
    
    override fun attachToActivity(activity: ComponentActivity) {
        currentActivityRef = WeakReference(activity)
        setupTouchDetection(activity)
    }
    
    override fun enableTouchDetection(activity: Activity) {
        currentActivityRef = WeakReference(activity)
        // Touch detection is set up in attachToActivity
    }
    
    override fun disableTouchDetection() {
        cancelLongPressTimer()
    }
    
    override fun onActivityResumed(activity: Activity) {
        currentActivityRef = WeakReference(activity)
    }
    
    override fun onActivityPaused(activity: Activity) {
        cancelLongPressTimer()
        hideOverlay()
    }
    
    override fun showOverlay() {
        val activity = currentActivityRef?.get() ?: return
        showOverlayView(activity)
        stateManager.setVisibility(true)
    }
    
    override fun hideOverlay() {
        val activity = currentActivityRef?.get()
        if (activity != null) {
            removeOverlayView(activity)
        }
        stateManager.setVisibility(false)
    }
    
    override fun cleanup() {
        cancelLongPressTimer()
        hideOverlay()
        overlayScope?.cancel()
        overlayScope = null
        currentActivityRef = null
    }
    
    /**
     * Sets up touch detection for long press gesture.
     * 
     * @param activity The activity to set up touch detection for
     */
    private fun setupTouchDetection(activity: ComponentActivity) {
        val originalCallback = activity.window?.callback ?: return
        
        if (originalCallback is TouchInterceptorCallback) {
            // Already wrapped
            return
        }
        
        try {
            activity.window.callback = TouchInterceptorCallback(activity, originalCallback) { motionEvent ->
                handleTouchEvent(motionEvent, activity)
            }
        } catch (e: Exception) {
            // Log error but don't crash
        }
    }
    
    /**
     * Handles touch events for long press detection.
     * 
     * @param motionEvent The touch event
     * @param activity The activity receiving the touch
     */
    private fun handleTouchEvent(motionEvent: MotionEvent, activity: Activity) {
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> {
                // Only start timer if overlay isn't already showing
                if (!stateManager.isVisible) {
                    startLongPressTimer(activity)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                cancelLongPressTimer()
            }
        }
    }
    
    /**
     * Starts the long press timer.
     * 
     * @param activity The activity to show overlay on when timer expires
     */
    private fun startLongPressTimer(activity: Activity) {
        if (isLongPressRunnablePending) return
        
        cancelLongPressTimer()
        
        val config = configRepository.getConfig()
        longPressRunnable = Runnable {
            isLongPressRunnablePending = false
            showOverlayView(activity)
            stateManager.setVisibility(true)
        }
        
        isLongPressRunnablePending = true
        handler.postDelayed(longPressRunnable!!, config.longPressDelayMs)
    }
    
    /**
     * Cancels the long press timer.
     */
    private fun cancelLongPressTimer() {
        longPressRunnable?.let { runnable ->
            handler.removeCallbacks(runnable)
            longPressRunnable = null
        }
        isLongPressRunnablePending = false
    }
    
    /**
     * Shows the overlay view on the given activity.
     * 
     * @param activity The activity to show the overlay on
     */
    private fun showOverlayView(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed) return
        
        val window = activity.window ?: return
        val decorView = window.decorView as? ViewGroup ?: return
        
        // Check if overlay is already added
        if (decorView.findViewById<View>(R.id.tag_debug_overlay_view) != null) {
            return
        }
        
        try {
            // Cancel any previous scope
            overlayScope?.cancel()
            overlayScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
            
            val composeView = ComposeView(activity).apply {
                id = R.id.tag_debug_overlay_view
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                
                setContent {
                    DebugOverlayScreen(
                        providers = registry.getProviders(),
                        actions = registry.getActions(),
                        onDismiss = { hideOverlay() },
                        dialogScope = overlayScope!!
                    )
                }
            }
            
            // Add to decor view
            val layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            decorView.addView(composeView, layoutParams)
            
            // Set up view properties
            composeView.isClickable = true
            composeView.isFocusable = true
            composeView.isFocusableInTouchMode = true
            composeView.bringToFront()
            composeView.requestFocus()
            
            // Handle back press for ComponentActivity
            if (activity is ComponentActivity) {
                val onBackPressedCallback = object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        hideOverlay()
                    }
                }
                composeView.setTag(R.id.tag_on_back_pressed_callback, onBackPressedCallback)
                activity.onBackPressedDispatcher.addCallback(activity, onBackPressedCallback)
            }
            
        } catch (e: Exception) {
            // Clean up on error
            overlayScope?.cancel()
            overlayScope = null
        }
    }
    
    /**
     * Removes the overlay view from the activity.
     * 
     * @param activity The activity to remove the overlay from
     */
    private fun removeOverlayView(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed) {
            overlayScope?.cancel()
            overlayScope = null
            return
        }
        
        val window = activity.window ?: return
        val decorView = window.decorView as? ViewGroup ?: return
        
        val overlayView = decorView.findViewById<View>(R.id.tag_debug_overlay_view)
        if (overlayView != null) {
            // Remove back press callback
            (overlayView.getTag(R.id.tag_on_back_pressed_callback) as? OnBackPressedCallback)?.let {
                it.remove()
                overlayView.setTag(R.id.tag_on_back_pressed_callback, null)
            }
            
            // Remove view
            decorView.removeView(overlayView)
            
            // Cancel scope
            overlayScope?.cancel()
            overlayScope = null
        }
    }
    
    /**
     * Touch interceptor callback for detecting long press gestures.
     */
    private class TouchInterceptorCallback(
        private val activity: Activity,
        val originalCallback: Window.Callback,
        private val onTouchEvent: (MotionEvent) -> Unit
    ) : Window.Callback {
        
        override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
            // Check if overlay is visible before processing touch
            if (activity.window?.decorView?.findViewById<View>(R.id.tag_debug_overlay_view) == null) {
                event?.let { onTouchEvent(it) }
            }
            return originalCallback.dispatchTouchEvent(event)
        }
        
        // Delegate all other methods to original callback
        override fun dispatchKeyEvent(event: KeyEvent?): Boolean = originalCallback.dispatchKeyEvent(event)
        override fun dispatchKeyShortcutEvent(event: KeyEvent?): Boolean = originalCallback.dispatchKeyShortcutEvent(event)
        override fun dispatchTrackballEvent(event: MotionEvent?): Boolean = originalCallback.dispatchTrackballEvent(event)
        override fun dispatchGenericMotionEvent(event: MotionEvent?): Boolean = originalCallback.dispatchGenericMotionEvent(event)
        override fun dispatchPopulateAccessibilityEvent(event: AccessibilityEvent?): Boolean = originalCallback.dispatchPopulateAccessibilityEvent(event)
        override fun onCreatePanelView(featureId: Int): View? = originalCallback.onCreatePanelView(featureId)
        override fun onCreatePanelMenu(featureId: Int, menu: Menu): Boolean = originalCallback.onCreatePanelMenu(featureId, menu)
        override fun onPreparePanel(featureId: Int, view: View?, menu: Menu): Boolean = originalCallback.onPreparePanel(featureId, view, menu)
        override fun onMenuOpened(featureId: Int, menu: Menu): Boolean = originalCallback.onMenuOpened(featureId, menu)
        override fun onMenuItemSelected(featureId: Int, item: MenuItem): Boolean = originalCallback.onMenuItemSelected(featureId, item)
        override fun onWindowAttributesChanged(attrs: WindowManager.LayoutParams?) = originalCallback.onWindowAttributesChanged(attrs)
        override fun onContentChanged() = originalCallback.onContentChanged()
        override fun onWindowFocusChanged(hasFocus: Boolean) = originalCallback.onWindowFocusChanged(hasFocus)
        override fun onAttachedToWindow() = originalCallback.onAttachedToWindow()
        override fun onDetachedFromWindow() = originalCallback.onDetachedFromWindow()
        override fun onPanelClosed(featureId: Int, menu: Menu) = originalCallback.onPanelClosed(featureId, menu)
        override fun onSearchRequested(): Boolean = originalCallback.onSearchRequested()
        override fun onSearchRequested(searchEvent: SearchEvent?): Boolean = originalCallback.onSearchRequested(searchEvent)
        override fun onWindowStartingActionMode(callback: ActionMode.Callback?): ActionMode? = originalCallback.onWindowStartingActionMode(callback)
        override fun onWindowStartingActionMode(callback: ActionMode.Callback?, type: Int): ActionMode? = originalCallback.onWindowStartingActionMode(callback, type)
        override fun onActionModeStarted(mode: ActionMode?) = originalCallback.onActionModeStarted(mode)
        override fun onActionModeFinished(mode: ActionMode?) = originalCallback.onActionModeFinished(mode)
    }
}