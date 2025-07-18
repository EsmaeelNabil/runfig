package dev.supersam.runfig.android.core // Ensure this matches your package

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
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
import dev.supersam.runfig.android.api.DebugOverlayRegistry
import dev.supersam.runfig.android.ui.DebugOverlayScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import java.lang.ref.WeakReference

internal object DebugOverlayController : Application.ActivityLifecycleCallbacks {

    private const val TAG = "DebugOverlayCtl"
    private const val LONG_PRESS_DURATION_MS = 2000L

    @Volatile
    private var isLongPressRunnablePending = false

    private lateinit var applicationContext: Context
    private val handler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null
    private var currentActivityRef: WeakReference<Activity>? = null

    // Scope for coroutines launched by actions within the overlay UI
    // This scope should be managed carefully. Cancelling it when the overlay
    // is dismissed might prematurely stop background work initiated by an action.
    // A SupervisorJob ensures failure of one action doesn't cancel others.
    // Consider providing a dedicated scope per action if needed.
    private var overlayScope: CoroutineScope? = null

    fun initialize(appContext: Context) {
        if (this::applicationContext.isInitialized) {
            Log.w(TAG, "Controller already initialized.")
            return
        }
        Log.d(TAG, "Initializing DebugOverlayController...")
        applicationContext = appContext
        if (appContext is Application) {
            appContext.registerActivityLifecycleCallbacks(this)
            Log.i(TAG, "Activity lifecycle callbacks registered.")
        } else {
            Log.e(TAG, "Initialization failed: Provided context is not an Application context.")
        }
    }

    // --- Touch Detection Logic ---

    private fun startLongPressTimer(activity: Activity) {
        if (isLongPressRunnablePending) {
            Log.v(TAG, "startLongPressTimer: Timer already pending, ignoring.")
            return
        }
        Log.v(TAG, "startLongPressTimer: Scheduling long press timer for activity: ${activity.localClassName}")
        cancelLongPressTimer() // Ensure no previous timer is running

        longPressRunnable = Runnable {
            Log.i(TAG, "Long press detected! Attempting to show overlay for activity: ${activity.localClassName}")
            isLongPressRunnablePending = false
            showOverlayView(activity) // Call the new view-based show method
        }
        isLongPressRunnablePending = true
        handler.postDelayed(longPressRunnable!!, LONG_PRESS_DURATION_MS)
    }

    private fun cancelLongPressTimer() {
        if (longPressRunnable != null) {
            Log.v(TAG, "cancelLongPressTimer: Cancelling pending long press timer.")
            handler.removeCallbacks(longPressRunnable!!)
            longPressRunnable = null
        }
        isLongPressRunnablePending = false
    }

    // --- Overlay View Management ---

    private fun showOverlayView(activity: Activity) {
        if (!activity.isFinishing && !activity.isDestroyed) {
            val window = activity.window ?: run {
                Log.e(TAG, "Cannot show overlay: Activity window is null for ${activity.localClassName}")
                return
            }
            val decorView = window.decorView as? ViewGroup ?: run {
                Log.e(TAG, "Cannot show overlay: Activity decorView is not a ViewGroup for ${activity.localClassName}")
                return
            }

            // Check if overlay is already added using our specific tag ID
            if (decorView.findViewById<View>(R.id.tag_debug_overlay_view) != null) {
                Log.d(TAG, "showOverlayView: Overlay view already present in ${activity.localClassName}")
                return
            }

            Log.d(TAG, "showOverlayView: Adding overlay view to ${activity.localClassName}")

            try {
                // Cancel any previous scope before creating a new one
                overlayScope?.cancel()
                overlayScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

                val composeView = ComposeView(activity).apply {
                    id = R.id.tag_debug_overlay_view // Use the ID resource for reliable lookup

                    // Dispose the Composition when the view is detached
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

                    setContent {
                        // Apply your library's theme if needed, or use Mdc3Theme from Accompanist
                        DebugOverlayScreen(
                            providers = DebugOverlayRegistry.infoProviders,
                            actions = DebugOverlayRegistry.actions,
                            onDismiss = { removeOverlayView(activity) }, // Pass lambda to remove the view
                            dialogScope = overlayScope!! // Pass the scope for actions
                        )
                    }
                }

                // Add the ComposeView to the DecorView
                val layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                decorView.addView(composeView, layoutParams)

                // Make the overlay consume touch events and be focusable
                composeView.isClickable = true
                composeView.isFocusable = true
                composeView.isFocusableInTouchMode = true // Allow focus gain on touch
                composeView.bringToFront() // Ensure it's on top
                composeView.requestFocus() // Request focus to potentially handle back press


                // Handle back press specifically for the overlay if possible
                if (activity is ComponentActivity) {
                    val onBackPressedCallback = object : OnBackPressedCallback(true) { // Enabled = true
                        override fun handleOnBackPressed() {
                            Log.d(TAG, "Overlay back press detected.")
                            removeOverlayView(activity)
                            // Callback is automatically removed when its LifecycleOwner is Destroyed.
                            // If we want one-shot removal, disable or remove manually here:
                            // isEnabled = false
                            // remove()
                        }
                    }.also {
                        // Store the callback in the view's tag to remove it explicitly later
                        composeView.setTag(R.id.tag_on_back_pressed_callback, it)
                    }
                    // Add the callback to the activity's dispatcher, tied to the activity's lifecycle
                    activity.onBackPressedDispatcher.addCallback(activity, onBackPressedCallback)
                    Log.d(TAG,"Registered OnBackPressedCallback for overlay.")

                } else {
                    Log.w(TAG, "Cannot automatically handle back press for non-ComponentActivity (${activity.localClassName}). User must use close button.")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error adding overlay view to ${activity.localClassName}", e)
                // Clean up scope if view creation failed
                overlayScope?.cancel()
                overlayScope = null
            }
        } else {
            Log.w(TAG, "showOverlayView: Activity ${activity.localClassName} is finishing or destroyed.")
        }
    }

    // Helper function to remove the overlay view
    private fun removeOverlayView(activity: Activity?) {
        val currentActivity = activity ?: currentActivityRef?.get() ?: return

        // Check if activity is finishing or destroyed before accessing window/view
        if (currentActivity.isFinishing || currentActivity.isDestroyed) {
            Log.d(TAG,"removeOverlayView: Activity ${currentActivity.localClassName} is finishing/destroyed, skip removal.")
            // Also ensure scope is cancelled if overlay persists somehow
            if (overlayScope?.isActive == true) {
                Log.d(TAG, "Cancelling dangling overlay scope during activity destruction.")
                overlayScope?.cancel()
                overlayScope = null
            }
            return
        }

        val window = currentActivity.window ?: return
        val decorView = window.decorView as? ViewGroup ?: return

        // Find the view using the ID resource
        val overlayView = decorView.findViewById<View>(R.id.tag_debug_overlay_view)
        if (overlayView != null) {
            Log.d(TAG, "Removing overlay view from ${currentActivity.localClassName}")

            // Retrieve and remove the OnBackPressedCallback if it exists
            (overlayView.getTag(R.id.tag_on_back_pressed_callback) as? OnBackPressedCallback)?.let {
                Log.d(TAG,"Removing OnBackPressedCallback for overlay.")
                it.remove() // Explicitly remove the callback
                overlayView.setTag(R.id.tag_on_back_pressed_callback, null) // Clear tag
            }

            // Remove the view from the parent
            decorView.removeView(overlayView)

            // Cancel the coroutine scope associated with the dismissed overlay
            overlayScope?.cancel()
            overlayScope = null
            Log.d(TAG,"Cancelled overlay coroutine scope.")

        } else {
            // If view not found, ensure scope is cancelled anyway if active
            if (overlayScope?.isActive == true) {
                Log.d(TAG, "Overlay view not found, but cancelling active scope.")
                overlayScope?.cancel()
                overlayScope = null
            }
        }
    }


    // --- Activity Lifecycle Callbacks ---

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        Log.d(TAG, "onActivityCreated: ${activity.localClassName}")
        val originalCallback = activity.window?.callback // Safe call
        if (originalCallback == null) {
            Log.w(TAG, "Original Window.Callback is null for ${activity.localClassName}. Cannot intercept touch events.")
            return
        }
        if (originalCallback is TouchInterceptorCallback) {
            Log.d(TAG, "Window.Callback already wrapped for ${activity.localClassName}. Skipping.")
            return
        }

        Log.d(TAG, "Wrapping Window.Callback for ${activity.localClassName}")
        try {
            // Use the explicit delegation implementation from previous refinement
            activity.window.callback = TouchInterceptorCallback(activity, originalCallback) { motionEvent ->
                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> {
                        // Only start timer if overlay isn't already showing
                        if (activity.window?.decorView?.findViewById<View>(R.id.tag_debug_overlay_view) == null) {
                            Log.v(TAG, "ACTION_DOWN detected on ${activity.localClassName}")
                            startLongPressTimer(activity)
                        } else {
                            Log.v(TAG,"ACTION_DOWN ignored, overlay already visible on ${activity.localClassName}")
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        Log.v(TAG, "ACTION_UP detected on ${activity.localClassName}")
                        cancelLongPressTimer()
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        Log.v(TAG, "ACTION_CANCEL detected on ${activity.localClassName}")
                        cancelLongPressTimer()
                    }
                }
            }
        } catch (e: Exception) {
            // Catch potential errors during callback wrapping
            Log.e(TAG, "Error wrapping Window.Callback for ${activity.localClassName}", e)
        }
    }

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityResumed(activity: Activity) {
        Log.d(TAG, "onActivityResumed: ${activity.localClassName}")
        currentActivityRef = WeakReference(activity)

        // Verify callback wrapping (optional sanity check)
        val currentCallback = activity.window?.callback
        if (currentCallback !is TouchInterceptorCallback && activity.window?.peekDecorView() != null) {
            Log.w(TAG, "Window.Callback for ${activity.localClassName} might have been replaced after creation. Interceptor may not work.")
        }
    }

    override fun onActivityPaused(activity: Activity) {
        Log.d(TAG, "onActivityPaused: ${activity.localClassName}")
        cancelLongPressTimer() // Stop timer if user leaves activity
        removeOverlayView(activity) // Remove overlay if user leaves activity
        if (currentActivityRef?.get() == activity) {
            currentActivityRef?.clear()
        }
    }

    override fun onActivityStopped(activity: Activity) {
        // Defensive cleanup if overlay somehow persisted past onPause
        if (currentActivityRef?.get() == activity) {
            removeOverlayView(activity)
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        Log.d(TAG, "onActivityDestroyed: ${activity.localClassName}")

        // Ensure overlay is removed
        removeOverlayView(activity)

        // Clean up the callback IF it's ours
        val currentCallback = activity.window?.callback // Safe call
        if (currentCallback is TouchInterceptorCallback) {
            Log.d(TAG, "Unwrapping Window.Callback for ${activity.localClassName}")
            // Restore the original callback stored within our wrapper
            activity.window.callback = currentCallback.originalCallback
        }
        cancelLongPressTimer() // Should be stopped already, but defensive call
        if (currentActivityRef?.get() == activity) {
            currentActivityRef?.clear()
        }
    }

    // --- Helper Class for Touch Interception (Using explicit delegation) ---

    private class TouchInterceptorCallback(
        private val activity: Activity,
        val originalCallback: Window.Callback, // Assumed not null based on check
        private val onTouchEvent: (MotionEvent) -> Unit
    ) : Window.Callback {

        override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
            // Allow touch events to pass through if the overlay is visible
            // This prevents the timer from restarting if the user touches the overlay itself
            if (activity.window?.decorView?.findViewById<View>(R.id.tag_debug_overlay_view) == null) {
                event?.let { onTouchEvent(it) }
            } else {
                // If overlay is visible, cancel timer just in case user touched outside then inside quickly
                cancelLongPressTimer()
            }
            // Let the original callback (and the view hierarchy) handle the event
            return originalCallback.dispatchTouchEvent(event)
        }

        // --- Delegate other methods manually ---
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