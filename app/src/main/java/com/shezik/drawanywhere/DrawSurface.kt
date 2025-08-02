package com.shezik.drawanywhere

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun DrawSurface(
    modifier: Modifier = Modifier,
    controller: DrawController = rememberDrawController()
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .stylusAwareDrawing(controller)
    ) {
        DrawCanvas(
            controller = controller,
            modifier = Modifier.fillMaxSize(),
            backgroundColor = Color.Transparent
        )
    }
}