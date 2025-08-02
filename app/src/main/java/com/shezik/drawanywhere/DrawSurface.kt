package com.shezik.drawanywhere

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun DrawSurface(
    modifier: Modifier = Modifier,
    pathController: DrawPathController = rememberDrawPathController()
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .stylusAwareDrawing(pathController)
    ) {
        DrawCanvas(
            modifier = Modifier.fillMaxSize(),
            pathController = pathController,
            backgroundColor = Color.Transparent
        )
    }
}