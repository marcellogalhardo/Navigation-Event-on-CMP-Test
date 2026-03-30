// ExperimentalWasmJsInterop is only available in Kotlin 2.2 and newer versions.
@file:Suppress("OPT_IN_USAGE")

package com.example.testcmp3.v3

import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventHistory
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventInput
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.w3c.dom.PopStateEvent
import org.w3c.dom.Window
import kotlin.math.abs

/**
 * A [androidx.navigationevent.NavigationEventInput] that translates browser history navigation events (popstate) into
 * [androidx.navigationevent.NavigationEventDispatcher] events.
 *
 * This implementation uses the browser's History API to synchronize the application's internal
 * navigation state with the browser's history stack.
 */
internal class BrowserInput(
    private val window: WindowCompat,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Main,
) : NavigationEventInput() {

    /** Creates a [BrowserInput] for the given [window]. */
    constructor(window: Window) : this(WindowCompat(window))

    private var coroutineScope: CoroutineScope? = null

    /**
     * Controls whether to process [onPopState] from the [WindowCompat].
     *
     * This is used to suppress the 'echo' effect, where programmatic history changes (like
     * [WindowCompat.go]) trigger a [WindowCompat.TYPE_POP_STATE] event that should not be
     * re-processed as a user-initiated navigation.
     */
    private var isOnPopStateEnabled = true

    /**
     * Controls whether to process [onHistoryChanged] from the [androidx.navigationevent.NavigationEventDispatcher].
     *
     * This is used to prevent redundant history synchronization requests while we are manually
     * winding or unwinding the state in response to a multistep browser navigation.
     */
    private var isOnHistoryChangedEnabled = true

    /**
     * The current index in the browser's history stack that matches our application's state. This
     * corresponds to the integer value stored in the browser's history state object.
     */
    private var browserIndex = 0

    /**
     * The number of valid navigation entries currently managed by the [androidx.navigationevent.NavigationEventDispatcher].
     * Any browser history entry with an index equal to or greater than this is considered
     * "invalid".
     */
    private var logicalHistorySize = 1

    /**
     * The total number of entries we have pushed to the browser's history stack. This helps us
     * determine if we need to call [WindowCompat.pushState] or if we can simply use
     * [WindowCompat.go].
     */
    private var pushedHistorySize = 1

    override fun onAdded(dispatcher: NavigationEventDispatcher) {
        // Since BrowserInput listens to global browser window events, we must ensure only one
        // instance is active at a time to avoid duplicate event dispatches and history
        // state corruption.
        check(ACTIVE_INSTANCE == null || ACTIVE_INSTANCE === this) {
            "Only one BrowserInput can be active at a time. Ensure you remove the existing " +
                    "BrowserInput before adding a new one."
        }
        ACTIVE_INSTANCE = this

        coroutineScope = CoroutineScope(Job() + coroutineDispatcher)

        // Starts the main synchronization loop that converts native browser navigation events
        // (e.g., the back button) into application-level navigation events.
        coroutineScope!!.launch { window.popStateEvents.collect { onPopState(it) } }

        // Synchronizes [WindowCompat.title] with the [history] destination so browser tabs and
        // history dropdowns (via back/forward long-press) show meaningful names for each entry.
        coroutineScope!!.launch {
            dispatcher.history.collect { history ->
                // TODO(mgalhardo): Add a 'title' or 'label' property to 'NavigationEventInfo'.
                //  Currently, we fall back to 'toString()', which often results in obscure class
                //  names in the browser history. A dedicated property allows destinations to
                //  provide human-readable titles for tabs and history entries.
                window.title = history.mergedHistory[history.currentIndex].toString()
            }
        }

        // Seed the current history entry with an initial index of 0. This index tracks the
        // application's relative position in the browser's history stack, allowing us to
        // determine the direction and distance of navigation during popstate events.
        window.replaceState(0.toJsNumber())
    }

    /**
     * Handles the browser's `popstate` event, converting it into [dispatchOnBackCompleted] or
     * [dispatchOnForwardCompleted] calls.
     */
    private suspend fun onPopState(popStateEvent: PopStateEvent) {
        if (!isOnPopStateEnabled) return

        val state = popStateEvent.state ?: return
        val newIndex = (state as? JsNumber)?.toInt() ?: return
        if (newIndex == browserIndex) return

        // If the browser attempts to navigate to a history state we no longer track (e.g.,
        // after a manual state replacement), force the browser to revert the native navigation
        // to stay in sync with our internal history stack.
        if (newIndex !in 0 until logicalHistorySize) {
            isOnPopStateEnabled = false
            window.go(browserIndex - newIndex)
            isOnPopStateEnabled = true
            return
        }

        // A user can jump multiple pages at once via the browser's history dropdown. We must
        // unwind/wind our internal state sequentially for each step. We suppress callbacks on
        // intermediate steps to prevent jarring, unnecessary UI churn.
        val steps = abs(newIndex - browserIndex)
        val isForward = newIndex > browserIndex

        isOnHistoryChangedEnabled = false
        repeat(steps) {
            if (isForward) {
                dispatchOnForwardCompleted()
            } else {
                dispatchOnBackCompleted()
            }
        }
        isOnHistoryChangedEnabled = true

        browserIndex = newIndex
    }

    override fun onRemoved() {
        val distanceToStart = -browserIndex
        if (distanceToStart != 0) {
            // We must rewind the browser history to the baseline index 0 to ensure the user is
            // not deep in a history stack that we no longer manage.
            // We use the current scope before cancelling it, or a fire-and-forget launch.
            coroutineScope?.launch {
                isOnPopStateEnabled = false
                window.go(distanceToStart)
                isOnPopStateEnabled = true
            }
        }

        coroutineScope?.cancel()
        coroutineScope = null
        browserIndex = 0
        logicalHistorySize = 1
        pushedHistorySize = 1
        isOnPopStateEnabled = true
        isOnHistoryChangedEnabled = true

        if (ACTIVE_INSTANCE === this) {
            ACTIVE_INSTANCE = null
        }
    }

    override fun onHistoryChanged(history: NavigationEventHistory) {
        if (!isOnHistoryChangedEnabled) return
        if (history.currentIndex < 0) return
        // TODO: We may get None first when disposing the previous Composable destination.
        if (history.mergedHistory[history.currentIndex] == NavigationEventInfo.None) return

        coroutineScope!!.launch {
            isOnPopStateEnabled = false
            updateBrowserHistory(history)
            isOnPopStateEnabled = true
        }
    }

    /**
     * Synchronizes the browser's history stack with the provided [newHistory].
     *
     * If the new history is larger than the current pushed history, it will push new states to
     * increase the stack size before navigating to the target index.
     */
    private suspend fun updateBrowserHistory(newHistory: NavigationEventHistory) {
        val newSize = newHistory.mergedHistory.size
        val newIndex = newHistory.currentIndex

        if (pushedHistorySize >= newSize) {
            window.go(newIndex - browserIndex)

            // Reducing "invalid" entries:
            // If the logical stack shrinks and we are at its new edge, the browser still
            // holds "stale" forward history from the previous longer stack.
            // We truncate this by pushing a single placeholder entry and immediately
            // navigating back. This limits the user's ability to click the "Forward"
            // button into invalid states, while avoiding the heavy cost of full-stack replacement.
            if (newSize < pushedHistorySize && newIndex == newSize - 1) {
                // Disable popstate listener to prevent reacting to our own truncation navigation.
                isOnPopStateEnabled = false
                window.pushState(newSize.toJsNumber())
                window.go(-1)
                isOnPopStateEnabled = true
                pushedHistorySize = newSize + 1
            }
        } else {
            // Browser History API restricts direct stack manipulation. To expand history
            // capacity, we must physically move to the end of the current stack, push new
            // placeholder states to increase the length, and then rewind to the target index.
            // This sequence triggers multiple native PopStateEvents.

            window.go(pushedHistorySize - 1 - browserIndex)

            for (i in pushedHistorySize until newSize) {
                window.pushState(i.toJsNumber())
            }

            window.go(newIndex - (newSize - 1))

            pushedHistorySize = newSize
        }

        browserIndex = newIndex
        logicalHistorySize = newSize
    }

    private companion object {
        /**
         * The currently active [BrowserInput] instance.
         *
         * Since [BrowserInput] listens to global browser window events, only one instance should be
         * active at a time to prevent duplicate event dispatches and state corruption.
         */
        private var ACTIVE_INSTANCE: BrowserInput? = null
    }
}
