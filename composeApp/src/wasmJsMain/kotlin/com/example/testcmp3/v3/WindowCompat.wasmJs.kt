// ExperimentalWasmJsInterop is only available in Kotlin 2.2 and newer versions.
@file:Suppress("OPT_IN_USAGE")

package com.example.testcmp3.v3

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.w3c.dom.PopStateEvent
import org.w3c.dom.Window
import org.w3c.dom.events.Event

/**
 * A compatibility interface for interacting with the browser's window and history APIs in a
 * coroutine-friendly way.
 *
 * This interface abstracts the standard Web [Window] and History APIs, converting event-based
 * navigation into [Flow]s and suspending functions.
 *
 * [JsAny] is used here for compatibility with the Kotlin/Wasm interop layer.
 */
internal interface WindowCompat {
    /**
     * The title of the current document.
     *
     * This title is typically displayed in the browser's title bar or tab. Synchronizing this with
     * the history stack allows browsers to display meaningful names in the history dropdown
     * (back/forward button long-press), helping users identify past destinations.
     */
    var title: String

    /** Returns the current history state as a JavaScript object. */
    val state: JsAny?

    /**
     * A flow of [PopStateEvent]s dispatched by the window whenever the active history entry
     * changes.
     */
    val popStateEvents: Flow<PopStateEvent>

    /**
     * Pushes a new entry onto the history stack.
     *
     * @param data A state object associated with the new history entry.
     * @param url The URL for the new history entry.
     */
    fun pushState(data: JsAny?, url: String? = null)

    /**
     * Replaces the current entry on the history stack.
     *
     * @param data A state object associated with the history entry.
     * @param url The URL for the history entry.
     */
    fun replaceState(data: JsAny?, url: String? = null)

    /** Navigates to a specific [delta] in history and waits for the resulting popstate event. */
    suspend fun go(delta: Int)

    companion object {
        /** The event type for [PopStateEvent]. */
        const val TYPE_POP_STATE = "popstate"
    }
}

/** Creates a [WindowCompat] instance for the given [window]. */
internal fun WindowCompat(window: Window): WindowCompat {
    return WindowCompatImpl(window)
}

private class WindowCompatImpl(private val window: Window) : WindowCompat {
    override var title: String
        get() = window.document.title
        set(value) {
            window.document.title = value
        }

    override val state: JsAny?
        get() = window.history.state

    override val popStateEvents: Flow<PopStateEvent> = callbackFlow {
        val callback: (Event) -> Unit = { event: Event -> trySend(event as PopStateEvent) }
        window.addEventListener(WindowCompat.TYPE_POP_STATE, callback)
        awaitClose { window.removeEventListener(WindowCompat.TYPE_POP_STATE, callback) }
    }

    override fun pushState(data: JsAny?, url: String?) {
        // 'title' is intentionally an empty string as it is ignored by almost all modern browsers.
        window.history.pushState(data, title = "", url)
    }

    override fun replaceState(data: JsAny?, url: String?) {
        // 'title' is intentionally an empty string as it is ignored by almost all modern browsers.
        window.history.replaceState(data, title = "", url)
    }

    override suspend fun go(delta: Int) {
        // If delta is 0, the browser would reload the current page. Since we are only
        // interested in navigating between history entries, we return early.
        if (delta == 0) return

        window.history.go(delta)

        // If we go out of range (e.g., calling go(10) when there are only 2 entries),
        // the browser ignores the call and no 'popstate' event is fired.
        // We use a 100ms timeout to prevent suspending indefinitely in these cases.
        withTimeoutOrNull(100) { popStateEvents.first() }
    }
}
