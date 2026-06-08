package com.example.testcmp3.content

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationEventHandler
import androidx.navigationevent.compose.rememberNavigationEventState

sealed class Destination : NavigationEventInfo() {
    data object Settings : Destination()
    data object Connection : Destination()
    data object Battery : Destination()
    data object Health : Destination()
    data object General : Destination()
    data object About : Destination()
    data object Language : Destination()
}

@Composable
fun HardcodedContent(onResetNavType: () -> Unit) {
    val backStack = remember { mutableStateListOf<Destination>(Destination.Settings) }

    val (current, parent, children) = when (backStack.lastOrNull()) {
        Destination.Settings ->
            Entry(
                current = Destination.Settings,
                parent = null,
                children = listOf(Destination.Connection, Destination.Battery, Destination.General)
            )

        Destination.Connection -> Entry(
            current = Destination.Connection,
            parent = Destination.Settings,
            children = listOf()
        )

        Destination.Battery -> Entry(
            current = Destination.Battery,
            parent = Destination.Settings,
            children = listOf(Destination.Health),
        )

        Destination.Health -> Entry(
            current = Destination.Health,
            parent = Destination.Battery,
            children = listOf(),
        )

        Destination.General -> Entry(
            current = Destination.General,
            parent = Destination.Settings,
            children = listOf(Destination.About, Destination.Language),
        )

        Destination.About -> Entry(
            current = Destination.About,
            parent = Destination.General,
            children = listOf(),
        )

        Destination.Language -> Entry(
            current = Destination.Language,
            parent = Destination.General,
            children = listOf(),
        )

        null -> {
            error("Error: Back stack is empty!")
        }
    }
    Screen(current, parent, children, backStack, onResetNavType)
}

@Composable
private fun Screen(
    current: Destination,
    parent: Destination?,
    children: List<Destination>,
    backStack: MutableList<Destination>,
    onResetNavType: () -> Unit
) {
    Column {
        Button(onClick = onResetNavType) {
            Text("Back to Selection")
        }

        Button(
            onClick = { backStack.removeAll { it != backStack.first() } },
            shape = ButtonDefaults.elevatedShape,
            border = BorderStroke(width = 2.dp, color = Color.Red),
            content = { Text(text = "Clear BackStack") }
        )

        if (parent == null) {
            Text("This is the root screen")
        } else {
            Button(onClick = {
                backStack.removeLastOrNull() // pop
            }) {
                Text("Back to $parent")
            }
        }

        Text(text = backStack.joinToString(" > "), fontSize = 24.sp)

        for (child in children) {
            Button(onClick = {
                backStack.add(child) // push
            }) {
                Text(text = "$child")
            }
        }
    }

    NavigationEventHandler(
        state = rememberNavigationEventState(
            currentInfo = current,
            backInfo = if (backStack.isEmpty()) emptyList() else backStack.dropLast(1),
            forwardInfo = if (children.size == 1) listOf(children.single()) else emptyList(),
        ),
        onBackCompleted = {
            println("gyz:onBackCompleted")
            backStack.removeLastOrNull()
        },
        onForwardCompleted = {
            println("gyz:onForwardCompleted")
            if (children.size == 1) {
                backStack.add(children.single())
            }
        }
    )
}

private data class Entry(
    val current: Destination,
    val parent: Destination?,
    val children: List<Destination>,
)
