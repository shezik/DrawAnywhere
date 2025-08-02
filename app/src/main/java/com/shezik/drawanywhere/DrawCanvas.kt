package com.shezik.drawanywhere

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

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
            drawPath(
                path = pathWrapper.cachedPath,
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