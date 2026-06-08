package com.example.testcmp3

import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import com.example.testcmp3.v4.BrowserInput
import kotlinx.browser.window

internal class WebNavigationEventDispatcherOwner : NavigationEventDispatcherOwner {
    override val navigationEventDispatcher = NavigationEventDispatcher()

    init {
        val input = BrowserInput(window)
        navigationEventDispatcher.addInput(input)
    }
}