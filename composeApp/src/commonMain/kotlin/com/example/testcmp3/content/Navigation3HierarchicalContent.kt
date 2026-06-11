package com.example.testcmp3.content

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneInfo
import androidx.navigation3.scene.rememberSceneState
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.NavEntry
import androidx.navigationevent.compose.NavigationEventHandler
import androidx.navigationevent.compose.rememberNavigationEventState

@Composable
fun Navigation3HierarchicalContent(onResetNavType: () -> Unit) {
    // In Hierarchical navigation, the backstack represents the path to the root.
    var backStack by remember { mutableStateOf(listOf(1)) }
    var forwardStack by remember { mutableStateOf(listOf<Int>()) }
    val currentPage = backStack.last()

    val entries = rememberDecoratedNavEntries(
        backStack = backStack,
        entryProvider = entryProvider {
            entry<Int> { page ->
                NumberedPage(
                    number = page,
                    onForward = {
                        // Move forward: push the next page onto the stack
                        backStack = backStack + (page + 1)
                        forwardStack = emptyList()
                    },
                    onBackstack = {
                        // Move backstack: in hierarchical, this means navigating to the parent
                        if (backStack.size > 1) {
                            val popped = backStack.last()
                            backStack = backStack.dropLast(1)
                            forwardStack = listOf(popped) + forwardStack
                        }
                    },
                    onJumpToFive = {
                        // Demonstrate hierarchy: jumping to 5 builds the full path [1, 2, 3, 4, 5]
                        backStack = listOf(1, 2, 3, 4, 5)
                        forwardStack = emptyList()
                    }
                )
            }
        }
    )

    val sceneState = rememberSceneState(
        entries = entries,
        sceneStrategies = listOf(SinglePaneSceneStrategy()),
        onBack = {
            if (backStack.size > 1) {
                val popped = backStack.last()
                backStack = backStack.dropLast(1)
                forwardStack = listOf(popped) + forwardStack
            }
        },
    )

    val gestureState = rememberNavigationEventState(sceneState = sceneState)

    Column {
        Button(onClick = onResetNavType) {
            Text("Back to Selection")
        }

        Text("Hierarchical Model", fontSize = 18.sp, modifier = Modifier.padding(vertical = 8.dp))
        Text("Current Stack: ${backStack.joinToString(" > ")}")
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        NavDisplay(
            sceneState = sceneState,
            navigationEventState = gestureState,
        )
    }

    NavigationEventHandler(
        state = gestureState,
        onBackCompleted = {
            if (backStack.size > 1) {
                val popped = backStack.last()
                backStack = backStack.dropLast(1)
                forwardStack = listOf(popped) + forwardStack
            }
        },
        onForwardCompleted = {
            if (forwardStack.isNotEmpty()) {
                val next = forwardStack.first()
                forwardStack = forwardStack.drop(1)
                backStack = backStack + next
            }
        }
    )
}

@Composable
private fun NumberedPage(
    number: Int,
    onForward: () -> Unit,
    onBackstack: () -> Unit,
    onJumpToFive: () -> Unit
) {
    Column {
        Text(text = "Page $number", fontSize = 32.sp)
        Button(onClick = onForward) {
            Text("Move Forward (to ${number + 1})")
        }
        if (number > 1) {
            Button(onClick = onBackstack) {
                Text("Move Backstack (to Parent)")
            }
        }
        if (number == 1) {
            Button(onClick = onJumpToFive) {
                Text("Jump to Page 5 (Hierarchical Path)")
            }
        }
    }
}
