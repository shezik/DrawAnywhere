package com.shezik.drawanywhere

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.scrollFadingEdges(
    scrollState: ScrollState,
    isVertical: Boolean = true,
    fadeSize: Dp = 16.dp,
    maxAlphaDistanceFactor: Float = 0.3f
): Modifier {
    return this
        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        .drawWithContent {
            val fadePx = fadeSize.toPx()
            val currentScroll = scrollState.value.toFloat()
            val maxScroll = scrollState.maxValue.toFloat()

            if (maxScroll <= 0f) {
                drawContent()
                return@drawWithContent
            }

            // alpha proportional to scroll percentage
            fun fadeAlpha(distance: Float): Float {
                val normalized = (distance / (fadePx * maxAlphaDistanceFactor))
                    .coerceIn(0f, 1f)
                return 1f - normalized
            }

            val startFadeAlpha = fadeAlpha(currentScroll)
            val endFadeAlpha = fadeAlpha(maxScroll - currentScroll)

            drawContent()

            if (isVertical) {
                // Top gradient
                if (startFadeAlpha < 1f) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = startFadeAlpha),
                                Color.Black
                            ),
                            startY = 0f,
                            endY = fadePx
                        ),
                        size = Size(size.width, fadePx),
                        topLeft = Offset(0f, 0f),
                        blendMode = BlendMode.DstIn
                    )
                }
                // Bottom gradient
                if (endFadeAlpha < 1f) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black,
                                Color.Black.copy(alpha = endFadeAlpha)
                            ),
                            startY = size.height - fadePx,
                            endY = size.height
                        ),
                        size = Size(size.width, fadePx),
                        topLeft = Offset(0f, size.height - fadePx),
                        blendMode = BlendMode.DstIn
                    )
                }
            } else {
                // Left gradient
                if (startFadeAlpha < 1f) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = startFadeAlpha),
                                Color.Black,
                            ),
                            startX = 0f,
                            endX = fadePx
                        ),
                        size = Size(fadePx, size.height),
                        topLeft = Offset(0f, 0f),
                        blendMode = BlendMode.DstIn
                    )
                }
                // Right gradient
                if (endFadeAlpha < 1f) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black,
                                Color.Black.copy(alpha = endFadeAlpha),
                            ),
                            startX = size.width - fadePx,
                            endX = size.width
                        ),
                        size = Size(fadePx, size.height),
                        topLeft = Offset(size.width - fadePx, 0f),
                        blendMode = BlendMode.DstIn
                    )
                }
            }
        }
}
