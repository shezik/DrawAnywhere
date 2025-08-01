package com.shezik.drawanywhere

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import java.util.UUID

data class PathWrapper(
    val id: String = UUID.randomUUID().toString(),
    val points: SnapshotStateList<Offset>,
    val color: Color,
    val width: Float,
    val alpha: Float
)

class DrawController {
    private val _pathList = mutableStateListOf<PathWrapper>()
    val pathList: List<PathWrapper>
        get() = _pathList

    fun updateLatestPath(newPoint: Offset) =
        _pathList.lastOrNull()?.points?.add(newPoint)

    fun createPath(newPoint: Offset) {
        _pathList.add(PathWrapper(
            points = mutableStateListOf(newPoint),
            color = Color.Red,  // TODO
            width = 5f,         // TODO
            alpha = 1f          // TODO
        ))
    }
}

@Composable
fun rememberDrawController(): DrawController =
    remember { DrawController() }