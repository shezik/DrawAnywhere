package com.shezik.drawanywhere

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DrawToolbar(
    viewModel: DrawViewModel,
    modifier: Modifier = Modifier
) {
    // Collect the UI state - this automatically recomposes when state changes
    val uiState by viewModel.uiState.collectAsState()
    val haptics = LocalHapticFeedback.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        viewModel.setToolbarPosition(uiState.toolbarPosition + dragAmount)
                    },
                    onDragEnd = {
                        viewModel.saveToolbarPosition()
                    }
                )
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Canvas Controls Row
            CanvasControlsRow(
                canvasVisible = uiState.canvasVisible,
                canvasPassthrough = uiState.canvasPassthrough,
                onVisibilityToggle = viewModel::setCanvasVisible,
                onPassthroughToggle = viewModel::setCanvasPassthrough,
                onClearCanvas = { viewModel.controller.clearPaths() }
            )

            HorizontalDivider()

            // Pen Controls
            PenControls(
                penConfig = uiState.penConfig,
                onColorChange = viewModel::setPenColor,
                onStrokeWidthChange = viewModel::setStrokeWidth,
                onAlphaChange = viewModel::setStrokeAlpha
            )
        }
    }
}

@Composable
private fun CanvasControlsRow(
    canvasVisible: Boolean,
    canvasPassthrough: Boolean,
    onVisibilityToggle: (Boolean) -> Unit,
    onPassthroughToggle: (Boolean) -> Unit,
    onClearCanvas: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Visibility Toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (canvasVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                contentDescription = "Canvas visibility"
            )
            Switch(
                checked = canvasVisible,
                onCheckedChange = onVisibilityToggle
            )
        }

        // Passthrough Toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Passthrough", fontSize = 14.sp)
            Switch(
                checked = canvasPassthrough,
                onCheckedChange = onPassthroughToggle,
                enabled = canvasVisible
            )
        }

        // Clear Button
        IconButton(
            onClick = onClearCanvas,
            enabled = canvasVisible
        ) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Clear canvas",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun PenControls(
    penConfig: PenConfig,
    onColorChange: (Color) -> Unit,
    onStrokeWidthChange: (Float) -> Unit,
    onAlphaChange: (Float) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Color Picker
        ColorPicker(
            selectedColor = penConfig.color,
            onColorSelected = onColorChange
        )

        // Stroke Width Slider
        SliderControl(
            label = "Stroke Width",
            value = penConfig.width,
            valueRange = 1f..50f,
            onValueChange = onStrokeWidthChange,
            valueDisplay = { "${it.toInt()}px" }
        )

        // Alpha Slider
        SliderControl(
            label = "Opacity",
            value = penConfig.alpha,
            valueRange = 0.1f..1f,
            onValueChange = onAlphaChange,
            valueDisplay = { "${(it * 100).toInt()}%" }
        )
    }
}

@Composable
private fun ColorPicker(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit
) {
    val colors = listOf(
        Color.Red, Color.Blue, Color.Green, Color.Yellow,
        Color.Magenta, Color.Cyan, Color.Black, Color.Gray
    )

    Column {
        Text(
            text = "Color",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(colors.size) { index ->
                val color = colors[index]
                val isSelected = color.toArgb() == selectedColor.toArgb()

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        )
                        .clickable { onColorSelected(color) }
                )
            }
        }
    }
}

@Composable
private fun SliderControl(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    valueDisplay: (Float) -> String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = valueDisplay(value),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}