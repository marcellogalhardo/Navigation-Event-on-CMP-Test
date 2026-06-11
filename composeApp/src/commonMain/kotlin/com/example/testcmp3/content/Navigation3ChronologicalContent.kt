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
fun Navigation3ChronologicalContent(onResetNavType: () -> Unit) {
    // In Chronological navigation, the backstack represents the history of visits.
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
                        // Move forward: push the next page
                        backStack = backStack + (page + 1)
                        forwardStack = emptyList()
                    },
                    onVisitPrevious = { prev ->
                        // Move backstack: in chronological, we can "push" a previous page again
                        backStack = backStack + prev
                        forwardStack = emptyList()
                    },
                    onJumpToFive = {
                        // Demonstrate chronological: jumping to 5 just adds 5 to the history [1, 5]
                        backStack = backStack + 5
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

        Text("Chronological Model", fontSize = 18.sp, modifier = Modifier.padding(vertical = 8.dp))
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
    onVisitPrevious: (Int) -> Unit,
    onJumpToFive: () -> Unit
) {
    Column {
        Text(text = "Page $number", fontSize = 32.sp)
        Button(onClick = onForward) {
            Text("Move Forward (to ${number + 1})")
        }
        if (number > 1) {
            Button(onClick = { onVisitPrevious(1) }) {
                Text("Visit Page 1 again (Chronological)")
            }
        }
        if (number == 1) {
            Button(onClick = onJumpToFive) {
                Text("Jump to Page 5 (History)")
            }
        }
    }
}
