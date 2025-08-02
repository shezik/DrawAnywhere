package com.shezik.drawanywhere

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged

fun Modifier.stylusAwareDrawing(
    controller: DrawController
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val initialEvent = awaitPointerEvent()
        val initialChange = initialEvent.changes.firstOrNull()

        if (initialChange == null || !initialChange.pressed)
            return@awaitEachGesture

        val isEraser = initialChange.type == PointerType.Stylus && initialEvent.buttons.isPrimaryPressed

        if (isEraser)
            controller.erasePath(initialChange.position)
        else
            controller.createPath(initialChange.position)
        initialChange.consume()

        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == initialChange.id }

            if (change == null || !change.pressed)
                break

            if (change.positionChanged()) {
                if (isEraser) {
                    controller.erasePath(change.position)
                } else {
                    controller.updateLatestPath(change.position)
                }
                change.consume()
            }
        }
    }
}