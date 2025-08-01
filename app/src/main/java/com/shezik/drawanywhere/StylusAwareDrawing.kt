package com.shezik.drawanywhere

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput

fun Modifier.stylusAwareDrawing(
    controller: DrawController
): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        var prevPressId: PointerId? = null
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull() ?: continue

            val id = change.id
            val pos = change.position

            if (change.pressed) {
                if (event.type == PointerEventType.Press || id != prevPressId) {
                    controller.createPath(pos)
                    prevPressId = id
                } else {
                    controller.updateLatestPath(pos)
                }
                change.consume()
            } else {
                prevPressId = null
            }
        }
    }
}