package com.example.testcmp3.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneInfo
import androidx.navigation3.scene.SceneState
import androidx.navigationevent.compose.NavigationEventState
import androidx.navigationevent.compose.rememberNavigationEventState

@Composable
internal fun <T : Any> rememberNavigationEventState(
    sceneState: SceneState<T>
): NavigationEventState<SceneInfo<T>> {
    var prevSceneState by remember { mutableStateOf<SceneState<T>?>(null) }
    var prevForwardScenes by remember { mutableStateOf(emptyList<Scene<T>>()) }

    val forwardScenes = remember(sceneState, prevSceneState) {
        if (prevSceneState == null) {
            println("rememberNavigationEventState: prevSceneState is null, returning empty forwardScenes")
            return@remember emptyList<Scene<T>>()
        }

        val prevAllScenes = prevSceneState!!.previousScenes + prevSceneState!!.currentScene + prevForwardScenes
        val currentKey = sceneState.currentScene.key
        println("rememberNavigationEventState: sceneState.previousScenes=${sceneState.previousScenes.map { it.key }}")

        val indexInPrev = prevAllScenes.indexOfFirst { it.key == currentKey }
        val result = if (indexInPrev != -1 && indexInPrev < prevAllScenes.size - 1) {
            prevAllScenes.subList(indexInPrev + 1, prevAllScenes.size)
        } else {
            emptyList()
        }

        println("rememberNavigationEventState: currentKey=$currentKey, indexInPrev=$indexInPrev, prevAllScenes=${prevAllScenes.map { it.key }}, forwardScenes=${result.map { it.key }}")
        result
    }

    SideEffect {
        println("rememberNavigationEventState: SideEffect updating prevSceneState to key=${sceneState.currentScene.key}, prevForwardScenes=${forwardScenes.map { it.key }}")
        prevSceneState = sceneState
        prevForwardScenes = forwardScenes
    }

    val currentInfo = SceneInfo(sceneState.currentScene)
    val previousSceneInfos = sceneState.previousScenes.map { SceneInfo(it) }
    val forwardSceneInfos = forwardScenes.map { SceneInfo(it) }

    return rememberNavigationEventState(
        currentInfo = currentInfo,
        backInfo = previousSceneInfos,
        forwardInfo = forwardSceneInfos,
    )
}
