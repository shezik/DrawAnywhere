package com.shezik.drawanywhere

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged

fun Modifier.stylusAwareDrawing(
    controller: DrawController
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val initialDown = awaitFirstDown()

        val initialEvent = awaitPointerEvent()
        val initialChange = initialEvent.changes.firstOrNull { it.id == initialDown.id }

        // Just maybe.
        if (initialChange == null || !initialChange.pressed)
            return@awaitEachGesture

        val isEraser = initialDown.type == PointerType.Stylus && initialEvent.buttons.isPrimaryPressed

        if (isEraser) {
            // TODO: controller eraser method
        } else {
            controller.createPath(initialDown.position)
            controller.updateLatestPath(initialChange.position)
        }
        initialChange.consume()

        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == initialDown.id }

            if (change == null || !change.pressed)
                break

            if (change.positionChanged()) {
                if (isEraser) {
                    // TODO: controller eraser method
                } else {
                    controller.updateLatestPath(change.position)
                }
                change.consume()
            }
        }
    }
}