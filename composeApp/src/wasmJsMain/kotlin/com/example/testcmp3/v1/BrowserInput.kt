package com.example.testcmp3.v1

import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventHistory
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventInput
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.w3c.dom.PopStateEvent
import org.w3c.dom.Window
import org.w3c.dom.events.Event

internal class BrowserInput(
    private val browserWindow: BrowserWindow,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Main,
) : NavigationEventInput() {

    private var coroutineScope: CoroutineScope? = null

    internal companion object {
        const val TYPE_POPSTATE = "popstate"
    }

    private val currentHistory: SessionHistory = SessionHistory()

    private var processPopState = true

    private var processHistoryChange = true

    private val browserHistory = browserWindow.history

    public constructor(window: Window) : this(BrowserWindowImpl(window))

    override fun onAdded(dispatcher: NavigationEventDispatcher) {
        // Only start listening to popstate events after the input is connected to a dispatcher.
        val scope = CoroutineScope(coroutineDispatcher)
        scope.launch { browserWindow.createPopStateFlow().collect(::onPopState) }
        coroutineScope = scope

        // Initialize the browser history to [entries from other apps or instances, ... , 0*].
        browserHistory.replace(0.toJsNumber(), null)
    }

    override fun onRemoved() {
        coroutineScope?.cancel()
        currentHistory.reset()
        processPopState = true
        processHistoryChange = true
    }

    private fun onPopState(popStateEvent: PopStateEvent) {
        if (!processPopState) {
            return
        }
        val state = popStateEvent.state ?: return
        val newIndex = (state as? JsNumber)?.toInt() ?: return
        if (
            newIndex != currentHistory.index &&
            (newIndex < 0 || newIndex >= currentHistory.actualSize)
        ) {
            // User goes to an invalid entry, so we move them back.
            coroutineScope!!.launch {
                disableOnPopStateCallback { browserHistory.go(newIndex, currentHistory.index) }
            }
        } else {
            if (newIndex < currentHistory.index) {
                // Trigger one or more dispatchOnBackCompleted. Only process onHistoryChanged
                // on the last one.
                val timesToGoBack = currentHistory.index - newIndex
                disableHistoryUpdateCallback {
                    repeat(timesToGoBack - 1) { dispatchOnBackCompleted() }
                }
                dispatchOnBackCompleted()
            } else if (newIndex > currentHistory.index) {
                // Trigger one or more dispatchOnForwardCompleted. Only process onHistoryChanged
                // on the last one.
                val timesToGoForward = newIndex - currentHistory.index
                disableHistoryUpdateCallback {
                    repeat(timesToGoForward - 1) { dispatchOnForwardCompleted() }
                }
                dispatchOnForwardCompleted()
            }
            currentHistory.index = newIndex
        }
    }

    override fun onHistoryChanged(history: NavigationEventHistory) {
        if (!processHistoryChange) {
            return
        }
        // We may get None first when disposing the previous Composable destination.
        if (
            history.currentIndex < 0 ||
            history.mergedHistory[history.currentIndex] == NavigationEventInfo.None
        ) {
            return
        }

        coroutineScope!!.launch { disableOnPopStateCallback { updateBrowserHistory(history) } }
    }

    private suspend fun updateBrowserHistory(newHistory: NavigationEventHistory) {
        if (currentHistory.availableSize >= newHistory.mergedHistory.size) {
            // We have enough entries already. Go to the new currentIndex directly.
            browserHistory.go(currentHistory.index, newHistory.currentIndex)
        } else { // newHistory.entries.size > oldHistory.entries.size
            // We don't have enough entries, so we start pushing at the end.

            // Move to the last entry
            browserHistory.go(currentHistory.index, currentHistory.availableSize - 1)

            var index = currentHistory.availableSize
            while (index < newHistory.mergedHistory.size) {
                browserHistory.push(index.toJsNumber(), null)
                index++
            }

            // Go back to currentIndex.
            browserHistory.go(newHistory.mergedHistory.size - 1, newHistory.currentIndex)

            currentHistory.availableSize = newHistory.mergedHistory.size
        }
        currentHistory.index = newHistory.currentIndex
        currentHistory.actualSize = newHistory.mergedHistory.size
    }

    private inline fun disableOnPopStateCallback(content: () -> Unit) {
        processPopState = false
        content()
        processPopState = true
    }

    private inline fun disableHistoryUpdateCallback(content: () -> Unit) {
        processHistoryChange = false
        content()
        processHistoryChange = true
    }

    // `SessionHistory(1, 2, 3)` means a browser history like [0, 1*#, 2]:
    // We have three entries in the browser history, two entries in the
    // NavigationEventHistory (denoted by #), and the current index is one (denoted by *).
    private class SessionHistory(
        var index: Int = 0,
        var actualSize: Int = 1,
        var availableSize: Int = 1,
    ) {
        fun reset() {
            index = 0
            actualSize = 1
            availableSize = 1
        }

        override fun toString(): String {
            val result = buildString {
                append("[")
                for (i in 0 until availableSize) {
                    append(i)
                    if (i == index) {
                        append("*")
                    }
                    if (i == actualSize - 1) {
                        append("#")
                    }
                    if (i < availableSize - 1) {
                        append(", ")
                    }
                }
                append("]")
            }
            return result
        }
    }

    private suspend fun BrowserHistory.go(source: Int, destination: Int) {
        val delta = destination - source
        if (delta != 0) {
            go(delta)
        }
    }
}

private fun BrowserWindow.createPopStateFlow() = callbackFlow {
    val callback: (Event) -> Unit = { event: Event -> trySend(event as PopStateEvent) }
    addEventListener(BrowserInput.TYPE_POPSTATE, callback)
    awaitClose { removeEventListener(BrowserInput.TYPE_POPSTATE, callback) }
}

interface BrowserWindow {
    val history: BrowserHistory

    fun addEventListener(type: String, callback: (Event) -> Unit)

    fun removeEventListener(type: String, callback: (Event) -> Unit)
}

class BrowserWindowImpl(private val window: Window) : BrowserWindow {
    override val history: BrowserHistory = BrowserHistoryImpl(window)

    override fun addEventListener(type: String, callback: (Event) -> Unit) {
        window.addEventListener(type, callback)
    }

    override fun removeEventListener(type: String, callback: (Event) -> Unit) {
        window.removeEventListener(type, callback)
    }
}

interface BrowserHistory {
    val state: JsAny?

    fun push(data: JsAny?, url: String?)

    fun replace(data: JsAny?, url: String?)

    suspend fun go(delta: Int)
}

class BrowserHistoryImpl(private val window: Window) : BrowserHistory {
    override val state: JsAny?
        get() = window.history.state

    override fun push(data: JsAny?, url: String?) {
        window.history.pushState(data, "", url)
    }

    override fun replace(data: JsAny?, url: String?) {
        window.history.replaceState(data, "", url)
    }

    override suspend fun go(delta: Int) {
        if (delta == 0) return // Ignore "refresh" for now.
        window.history.go(delta)
        // TODO: Will get stuck if we go out of range. For example, if the history is [a, b*, c],
        // and we call `history.go(2)`, we'll be stuck here as the call will be ignored and we
        // won't receive a popstate event.
        window.createPopStateFlow().first()
    }
}

private fun Window.createPopStateFlow() = callbackFlow {
    val callback: (Event) -> Unit = { event: Event -> trySend(event as PopStateEvent) }
    window.addEventListener(BrowserInput.TYPE_POPSTATE, callback)
    awaitClose { window.removeEventListener(BrowserInput.TYPE_POPSTATE, callback) }
}
