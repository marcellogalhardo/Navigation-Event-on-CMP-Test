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
    val prevSceneStateHolder = remember { arrayOf<SceneState<T>?>(null) }
    val prevForwardScenes = remember { mutableListOf<Scene<T>>() }

    val forwardScenes = remember(sceneState) {
        val prevSceneState = prevSceneStateHolder[0]
        if (prevSceneState == null) {
            return@remember emptyList<Scene<T>>()
        }

        val prevEntriesSize = prevSceneState.entries.size
        val currentEntriesSize = sceneState.entries.size
        val currentKey = sceneState.currentScene.key
        
        val isBackStep = currentEntriesSize < prevEntriesSize && 
                prevSceneState.previousScenes.any { it.key == currentKey }
        val isForwardStep = currentKey == prevForwardScenes.firstOrNull()?.key

        val prevAllScenes = prevSceneState.previousScenes + prevSceneState.currentScene + prevForwardScenes
        val indexInPrev = prevAllScenes.indexOfFirst { it.key == currentKey }
        val result = if (isBackStep || isForwardStep) {
            if (indexInPrev != -1 && indexInPrev < prevAllScenes.size - 1) {
                prevAllScenes.subList(indexInPrev + 1, prevAllScenes.size)
            } else {
                emptyList()
            }
        } else {
            emptyList()
        }

        result
    }

    SideEffect {
        prevSceneStateHolder[0] = sceneState
        prevForwardScenes.clear()
        prevForwardScenes.addAll(forwardScenes)
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
