package com.example.testcmp3.content

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.sp
import androidx.navigation3.NavDisplay

@Composable
fun Navigation3HierarchicalContent(onResetNavType: () -> Unit) {
    val backStack = remember { mutableStateListOf<Int>(1) }

    Column {
        Button(onClick = onResetNavType) {
            Text("Back to Selection")
        }

        NavDisplay(
            backstack = backStack,
            onBack = { backStack.removeLastOrNull() }
        ) { page ->
            NumberedPage(
                number = page,
                onForward = { backStack.add(page + 1) },
                onBack = { backStack.removeLastOrNull() }
            )
        }
    }
}

@Composable
private fun NumberedPage(
    number: Int,
    onForward: () -> Unit,
    onBack: () -> Unit
) {
    Column {
        Text(text = "Page $number", fontSize = 32.sp)
        Button(onClick = onForward) {
            Text("Move Forward")
        }
        if (number > 1) {
            Button(onClick = onBack) {
                Text("Move Backstack")
            }
        }
    }
}
