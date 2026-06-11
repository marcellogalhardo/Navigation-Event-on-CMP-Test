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

    val forwardScenes = remember(sceneState, prevSceneState) {
        if (prevSceneState == null) return@remember emptyList()

        val prevAllScenes = prevSceneState!!.previousScenes + prevSceneState!!.currentScene
        val currentKey = sceneState.currentScene.key

        val indexInPrev = prevAllScenes.indexOfFirst { it.key == currentKey }
        if (indexInPrev != -1 && indexInPrev < prevAllScenes.size - 1) {
            prevAllScenes.subList(indexInPrev + 1, prevAllScenes.size)
        } else {
            emptyList()
        }
    }

    SideEffect {
        prevSceneState = sceneState
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
