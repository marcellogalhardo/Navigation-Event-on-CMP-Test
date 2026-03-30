@file:OptIn(ExperimentalWasmJsInterop::class)

package com.example.testcmp3

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.example.testcmp3.v1.BrowserInput
import kotlinx.browser.document
import kotlinx.browser.window

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val dispatcher = NavigationEventDispatcher()
    val owner = object : NavigationEventDispatcherOwner {
        override val navigationEventDispatcher: NavigationEventDispatcher
            get() = dispatcher
    }
    val input = BrowserInput(window)
    dispatcher.addInput(input)

    ComposeViewport(document.body!!) {
        CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
            App()
            //Test(BrowserHistory(window))
        }
    }
}
