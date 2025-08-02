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

private fun calculateMidpoint(start: Offset, end: Offset) =
    Offset((start.x + end.x) / 2, (start.y + end.y) / 2)

private fun pointsToPath(points: List<Offset>) = Path().apply {
    if (points.size < 2)
        return@apply

    moveTo(points.first().x, points.first().y)
    points.zipWithNext().forEachIndexed { index, (start, end) ->
        val mid = calculateMidpoint(start, end)
        if (index == 0)
            lineTo(mid.x, mid.y)
        else
            quadraticTo(start.x, start.y, mid.x, mid.y)
    }
    lineTo(points.last().x, points.last().y)
}

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

class DrawController {
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
}

@Composable
fun rememberDrawController(): DrawController =
    remember { DrawController() }