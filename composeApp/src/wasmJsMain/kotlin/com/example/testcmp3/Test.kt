package com.example.testcmp3

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.testcmp3.v1.BrowserHistory
import kotlinx.browser.document
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@OptIn(DelicateCoroutinesApi::class)
@Composable
fun Test(browserHistory: BrowserHistory) {
    Column {
        var text by remember { mutableStateOf("n/a") }
        val hist = remember { mutableStateListOf<String>("?") }
        var index by remember { mutableStateOf(0) }
        var count by remember { mutableStateOf(0) }

        val historyEntries = hist.withIndex().joinToString { (i, info) ->
            if (i == index) {
                "$info*"
            } else {
                "$info"
            }
        }
        Text(text = "[$historyEntries]")

        Button(onClick = {
            val oldState = browserHistory.state
            browserHistory.replace(count.toString().toJsString(), null)
            document.title = count.toString()
            println("replace $oldState with $count")
            text = count.toString()
            hist[index] = text
            count++
        }) {
            Text("Replace")
        }
        Button(onClick = {
            browserHistory.push(count.toString().toJsString(), null)
            document.title = count.toString()
            println("push $count")
            text = count.toString()
            hist.removeRange(index + 1, hist.size)
            hist.add(count.toString())
            index++
            count++
        }) {
            Text("Push")
        }
        Button(onClick = {
            GlobalScope.launch {
                browserHistory.go(-1)
                text = browserHistory.state.toString()
                if (index > 0) index--
            }
        }) {
            Text("Go back")
        }
        Button(onClick = {
            GlobalScope.launch {
                browserHistory.go(1)
                text = browserHistory.state.toString()
                if (index < hist.size - 1) index++
            }
        }) {
            Text("Go forward")
        }
    }
}
