package com.example.testcmp3

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationEventHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.example.testcmp3.Destination.About
import com.example.testcmp3.Destination.Battery
import com.example.testcmp3.Destination.Connection
import com.example.testcmp3.Destination.General
import com.example.testcmp3.Destination.Health
import com.example.testcmp3.Destination.Language
import com.example.testcmp3.Destination.Settings
import org.jetbrains.compose.ui.tooling.preview.Preview

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
@Preview
fun App() {
    MaterialTheme {
        Box(modifier = Modifier.padding(8.dp)) {
            Content()
        }
    }
}

@Composable
fun Content() {
    val backStack = remember { mutableStateListOf<Destination>(Settings) }

    val (current, parent, children) = when (backStack.lastOrNull()) {
        Settings ->
            Entry(
                current = Settings,
                parent = null,
                children = listOf(Connection, Battery, General)
            )

        Connection -> Entry(
            current = Connection,
            parent = Settings,
            children = listOf()
        )

        Battery -> Entry(
            current = Battery,
            parent = Settings,
            children = listOf(Health),
        )

        Health -> Entry(
            current = Health,
            parent = Battery,
            children = listOf(),
        )

        General -> Entry(
            current = General,
            parent = Settings,
            children = listOf(About, Language),
        )

        About -> Entry(
            current = About,
            parent = General,
            children = listOf(),
        )

        Language -> Entry(
            current = Language,
            parent = General,
            children = listOf(),
        )

        null -> {
            error("Error: Back stack is empty!")
        }
    }
    Screen(current, parent, children, backStack)
}

@Composable
fun Screen(
    current: Destination,
    parent: Destination?,
    children: List<Destination>,
    backStack: MutableList<Destination>
) {
    Column {
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
