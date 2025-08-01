package com.shezik.drawanywhere

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

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

@Composable
fun DrawCanvas(
    controller: DrawController,
    modifier: Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.background
) {
//    val pathList by remember { derivedStateOf { controller.pathList } }
    val pathList = controller.pathList

    Canvas(
        modifier.background(backgroundColor)
    ) {
        pathList.forEach { pathWrapper ->
            // TODO: Find a way to cache calculated paths
            drawPath(
                path = pointsToPath(pathWrapper.points),
                color = pathWrapper.color,
                alpha = pathWrapper.alpha,
                style = Stroke(
                    width = pathWrapper.width,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}