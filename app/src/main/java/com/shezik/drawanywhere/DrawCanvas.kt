/*
DrawAnywhere: An Android application that lets you draw on top of other apps.
Copyright (C) 2025 shezik

This program is free software: you can redistribute it and/or modify it under the
terms of the GNU Affero General Public License as published by the Free Software
Foundation, either version 3 of the License, or any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License along
with this program. If not, see <https://www.gnu.org/licenses/>.
 */

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
    modifier: Modifier,
    controller: DrawController,
    backgroundColor: Color = MaterialTheme.colorScheme.background
) {
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