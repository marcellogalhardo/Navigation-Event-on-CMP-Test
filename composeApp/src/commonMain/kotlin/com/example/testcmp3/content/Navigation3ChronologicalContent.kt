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
import androidx.navigation3.NavDisplay

@Composable
fun Navigation3ChronologicalContent(onResetNavType: () -> Unit) {
    // In Chronological navigation, the backstack represents the history of visits.
    var backStack by remember { mutableStateOf(listOf(1)) }

    Column {
        Button(onClick = onResetNavType) {
            Text("Back to Selection")
        }

        Text("Chronological Model", fontSize = 18.sp, modifier = Modifier.padding(vertical = 8.dp))
        Text("Current Stack: ${backStack.joinToString(" > ")}")
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        NavDisplay(
            backstack = backStack,
            onBack = { if (backStack.size > 1) backStack = backStack.dropLast(1) }
        ) { page ->
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
