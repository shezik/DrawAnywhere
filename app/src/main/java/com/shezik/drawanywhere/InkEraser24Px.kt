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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val InkEraser24Px: ImageVector
    get() {
        if (_InkEraser24Px != null) {
            return _InkEraser24Px!!
        }
        _InkEraser24Px = ImageVector.Builder(
            name = "InkEraser24Px",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(690f, 720f)
                lineTo(880f, 720f)
                lineTo(880f, 800f)
                lineTo(610f, 800f)
                lineTo(690f, 720f)
                close()
                moveTo(190f, 800f)
                lineTo(105f, 715f)
                quadTo(82f, 692f, 81.5f, 658f)
                quadTo(81f, 624f, 104f, 600f)
                lineTo(544f, 144f)
                quadTo(567f, 120f, 600.5f, 120f)
                quadTo(634f, 120f, 657f, 143f)
                lineTo(856f, 342f)
                quadTo(879f, 365f, 879f, 399f)
                quadTo(879f, 433f, 856f, 456f)
                lineTo(520f, 800f)
                lineTo(190f, 800f)
                close()
                moveTo(486f, 720f)
                lineTo(800f, 398f)
                quadTo(800f, 398f, 800f, 398f)
                quadTo(800f, 398f, 800f, 398f)
                lineTo(602f, 200f)
                quadTo(602f, 200f, 602f, 200f)
                quadTo(602f, 200f, 602f, 200f)
                lineTo(160f, 656f)
                quadTo(160f, 656f, 160f, 656f)
                quadTo(160f, 656f, 160f, 656f)
                lineTo(224f, 720f)
                lineTo(486f, 720f)
                close()
                moveTo(480f, 480f)
                lineTo(480f, 480f)
                lineTo(480f, 480f)
                quadTo(480f, 480f, 480f, 480f)
                quadTo(480f, 480f, 480f, 480f)
                lineTo(480f, 480f)
                quadTo(480f, 480f, 480f, 480f)
                quadTo(480f, 480f, 480f, 480f)
                lineTo(480f, 480f)
                quadTo(480f, 480f, 480f, 480f)
                quadTo(480f, 480f, 480f, 480f)
                close()
            }
        }.build()

        return _InkEraser24Px!!
    }

@Suppress("ObjectPropertyName")
private var _InkEraser24Px: ImageVector? = null
