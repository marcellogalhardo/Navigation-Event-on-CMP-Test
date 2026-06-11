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
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.NavigationEventHandler
import androidx.navigationevent.compose.rememberNavigationEventState

@Composable
fun Navigation3ChronologicalContent(onResetNavType: () -> Unit) {
    // In Chronological navigation, the backstack represents the history of visits.
    var backStack by remember { mutableStateOf(listOf(1)) }

    val entries = rememberDecoratedNavEntries(
        backStack = backStack,
        entryProvider = entryProvider {
            entry<Int> { page ->
                NumberedPage(
                    number = page,
                    onForward = {
                        // Move forward: push the next page
                        backStack = backStack + (page + 1)
                    },
                    onVisitPrevious = { prev ->
                        // Move backstack: in chronological, we can "push" a previous page again
                        backStack = backStack + prev
                    },
                    onJumpToFive = {
                        // Demonstrate chronological: jumping to 5 just adds 5 to the history [1, 5]
                        backStack = backStack + 5
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
            }
        },
    )

    val gestureState = rememberNavigationEventState(sceneState = sceneState)

    Column {
        Button(onClick = onResetNavType) {
            Text("Back to Selection")
        }

        Text("Chronological Model", fontSize = 18.sp, modifier = Modifier.padding(vertical = 8.dp))
        Text(
            text = "Explanation: In Chronological model, the backstack represents a history trail (duplicates allowed). " +
                   "Try going 1 -> 2, then click 'Visit Page 1 again (Chronological)'. The stack will successfully grow to [1, 2, 1], and the forward history (if any) will be cleared since we performed a new push step.",
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Current Stack: ${backStack.joinToString(" > ")}"
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        NavDisplay(
            sceneState = sceneState,
            navigationEventState = gestureState,
        )

        NavigationEventHandler(
            state = gestureState,
            isBackEnabled = sceneState.currentScene.previousEntries.isNotEmpty(),
            onBackCompleted = {
                repeat(entries.size - sceneState.currentScene.previousEntries.size) {
                    if (backStack.size > 1) {
                        backStack = backStack.dropLast(1)
                    }
                }
            },
            onForwardCompleted = {
                val nextScene = gestureState.forwardInfo.firstOrNull()?.scene
                if (nextScene != null) {
                    val targetEntries = nextScene.previousEntries + nextScene.entries
                    backStack = targetEntries.map { it.contentKey.toString().toInt() }
                }
            }
        )
    }
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
