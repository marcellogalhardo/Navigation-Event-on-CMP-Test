package com.example.testcmp3.content

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun Navigation3ChronologicalContent(onResetNavType: () -> Unit) {
    Column {
        Button(onClick = onResetNavType) {
            Text("Back to Selection")
        }
        Text("Navigation3 Chronological (Not implemented yet)")
    }
}
