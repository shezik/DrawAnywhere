package com.shezik.drawanywhere

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import java.util.UUID

data class PathWrapper(
    val id: String = UUID.randomUUID().toString(),
    val points: SnapshotStateList<Offset>,
    private var _cachedPath: MutableState<Path> = mutableStateOf(Path()),
    val color: Color,
    val width: Float,
    val alpha: Float
) {
    val cachedPath: Path get() = _cachedPath.value

    init {
        invalidatePath()
    }

    fun invalidatePath() {  // TODO: Find a way to append points to the cached path instead of complete recalculation
        _cachedPath.value = pointsToPath(points)
    }
}

class DrawPathController {
    private val _pathList = mutableStateListOf<PathWrapper>()
    val pathList: List<PathWrapper>
        get() = _pathList

    fun updateLatestPath(newPoint: Offset) {
        _pathList.lastOrNull()?.let { latestPath ->
            latestPath.points.add(newPoint)
            latestPath.invalidatePath()
        }
    }

    fun createPath(newPoint: Offset) {
        _pathList.add(PathWrapper(
            points = mutableStateListOf(newPoint),
            color = Color.Red,  // TODO
            width = 5f,         // TODO
            alpha = 1f          // TODO
        ))
    }

    fun erasePath(point: Offset, eraserRadius: Float = 16f /* TODO */) {
        for (i in _pathList.indices.reversed()) {
            val pathWrapper = _pathList[i]
            val compensatedRadius = pathWrapper.width / 2 + eraserRadius

            pathWrapper.points.zipWithNext().forEach { (p1, p2) ->
                val dist = distancePointToLineSegment(point, p1, p2)
                if (dist <= compensatedRadius) {
                    _pathList.removeAt(i)
                    return
                }
            }

            // In case the path contains only one point
            pathWrapper.points.firstOrNull()?.let {
                val dist = distance(point, it)
                if (dist <= compensatedRadius) {
                    _pathList.removeAt(i)
                    return
                }
            }
        }
    }
}

@Composable
fun rememberDrawPathController(): DrawPathController =
    remember { DrawPathController() }