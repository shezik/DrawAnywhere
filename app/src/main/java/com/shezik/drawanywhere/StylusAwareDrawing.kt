package com.shezik.drawanywhere

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged

enum class StrokeModifier {
    None, PrimaryButton, SecondaryButton, Both
}

fun Modifier.stylusAwareDrawing(
    viewModel: DrawViewModel
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val initialEvent = awaitPointerEvent()
        val initialChange = initialEvent.changes.firstOrNull()

        if (initialChange == null || !initialChange.pressed)
            return@awaitEachGesture

        val strokeModifier = when {
            initialChange.type != PointerType.Stylus -> StrokeModifier.None
            initialEvent.buttons.isPrimaryPressed && initialEvent.buttons.isSecondaryPressed -> StrokeModifier.Both
            initialEvent.buttons.isPrimaryPressed -> StrokeModifier.PrimaryButton
            initialEvent.buttons.isSecondaryPressed -> StrokeModifier.SecondaryButton
            else -> StrokeModifier.None
        }

        viewModel.startStroke(initialChange.position, strokeModifier)
        initialChange.consume()

        try {
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == initialChange.id }

                if (change == null || !change.pressed)
                    break

                if (change.positionChanged()) {
                    viewModel.updateStroke(change.position)
                    change.consume()
                }
            }
        } finally {
            viewModel.finishStroke()
        }
    }
}