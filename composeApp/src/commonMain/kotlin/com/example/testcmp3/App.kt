package com.example.testcmp3

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.testcmp3.content.HardcodedContent
import com.example.testcmp3.content.Navigation3ChronologicalContent
import com.example.testcmp3.content.Navigation3HierarchicalContent
import org.jetbrains.compose.ui.tooling.preview.Preview

sealed class NavType {
    data object Hardcoded : NavType()
    data object Navigation3Chronological : NavType()
    data object Navigation3Hierarchical : NavType()
}

@Composable
@Preview
fun App() {
    var navType by remember { mutableStateOf<NavType?>(null) }
    MaterialTheme {
        Box(modifier = Modifier.padding(8.dp)) {
            if (navType == null) {
                SelectionScreen(onSelected = { navType = it })
            } else {
                when (navType) {
                    NavType.Hardcoded -> {
                        HardcodedContent(onResetNavType = { navType = null })
                    }
                    NavType.Navigation3Hierarchical -> {
                        Navigation3HierarchicalContent(onResetNavType = { navType = null })
                    }
                    NavType.Navigation3Chronological -> {
                        Navigation3ChronologicalContent(onResetNavType = { navType = null })
                    }
                    else -> error("Unknown Navigation Type")
                }
            }
        }
    }
}

@Composable
fun SelectionScreen(onSelected: (NavType) -> Unit) {
    Column {
        Text(
            "Select Navigation Flow",
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Button(onClick = { onSelected(NavType.Hardcoded) }) {
            Text("Hardcoded")
        }
        Button(onClick = { onSelected(NavType.Navigation3Chronological) }) {
            Text("Navigation3 Chronological")
        }
        Button(onClick = { onSelected(NavType.Navigation3Hierarchical) }) {
            Text("Navigation3 Hierarchical")
        }
    }
}
