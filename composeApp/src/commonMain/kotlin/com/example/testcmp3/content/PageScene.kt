package com.example.testcmp3.content

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene

internal class PageScene(
    override val key: Any,
    override val entries: List<NavEntry<Int>> = emptyList(),
    override val previousEntries: List<NavEntry<Int>> = emptyList(),
    override val content: @Composable () -> Unit = {},
) : Scene<Int>