package com.shezik.drawanywhere

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

// TODO: Toggle methods in ViewModel, events implementation, modifiers (esp. size) unification

sealed class ToolbarEvents {
    object ToggleExpanded : ToolbarEvents()
    data class ChangeOrientation(val orientation: ToolbarOrientation) : ToolbarEvents()

    data class ChangePenType(val penType: PenType) : ToolbarEvents()
    data class ChangeAlpha(val alpha: Float) : ToolbarEvents()
    data class ChangeColor(val color: Color) : ToolbarEvents()
    data class ChangeStrokeWidth(val width: Float) : ToolbarEvents()

    object Undo : ToolbarEvents()
    object Redo : ToolbarEvents()
    object ClearCanvas : ToolbarEvents()
    object ToggleCanvasVisibility : ToolbarEvents()
    object ToggleCanvasPassthrough : ToolbarEvents()
}

// Unified button data class
data class ToolbarButton(
    val id: String,
    val icon: ImageVector,
    val contentDescription: String,
    val isEnabled: Boolean = true,
    val isVisible: Boolean = true,
    val isAlwaysVisible: Boolean = false,
    val onClick: (() -> Unit)? = null,
    val expandableContent: (@Composable () -> Unit)? = null
) {
    val isExpandable: Boolean
        get() = expandableContent != null
}

enum class ToolbarOrientation {
    HORIZONTAL, VERTICAL
}

@Composable
fun DrawToolbar(
    viewModel: DrawViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val canClearCanvas by viewModel.canClearCanvas.collectAsState()

    val orientation = uiState.toolbarOrientation
    val isToolbarExpanded = uiState.toolbarExpanded

    val haptics = LocalHapticFeedback.current

    val allButtons = createAllToolbarButtons(
        uiState = uiState,
        canUndo = canUndo,
        canRedo = canRedo,
        canClearCanvas = canClearCanvas,
        onCanvasVisibilityToggle = viewModel::setCanvasVisible,
        onCanvasPassthroughToggle = viewModel::setCanvasPassthrough,
        onClearCanvas = viewModel::clearCanvas,
        onUndo = viewModel::undo,
        onRedo = viewModel::redo,
        onPenTypeSwitch = viewModel::switchToPen,
        onColorChange = viewModel::setPenColor,
        onStrokeWidthChange = viewModel::setStrokeWidth,
        onAlphaChange = viewModel::setStrokeAlpha,
    )

    // Root composable
    DraggableToolbarCard(
        modifier = modifier
            .padding(5.dp),
        uiState = uiState,
        haptics = haptics,
        onPositionChange = viewModel::setToolbarPosition,
        onPositionSaved = viewModel::saveToolbarPosition
    ) {
        ToolbarButtonsContainer(
            modifier = Modifier.padding(8.dp),  // Padding for the overall button group
            buttons = allButtons,
            orientation = orientation,
            isToolbarExpanded = isToolbarExpanded,
            onExpandToggleClick = { viewModel.setToolbarExpanded(!isToolbarExpanded) }
        )
    }
}

@Composable
private fun DraggableToolbarCard(
    modifier: Modifier = Modifier,
    uiState: UiState,
    haptics: HapticFeedback,
    onPositionChange: (Offset) -> Unit,
    onPositionSaved: () -> Unit,
    content: @Composable () -> Unit
) {
    var currentPosition by remember { mutableStateOf(uiState.toolbarPosition) }

    LaunchedEffect(uiState.toolbarPosition) {
        currentPosition = uiState.toolbarPosition
    }

    Card(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        currentPosition += dragAmount
                        onPositionChange(currentPosition)
                    },
                    onDragEnd = {
                        onPositionSaved()
                    }
                )
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = CircleShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f)
        )
    ) {
        content()
    }
}

@Composable
private fun ToolbarButtonsContainer(
    modifier: Modifier = Modifier,
    buttons: List<ToolbarButton>,
    orientation: ToolbarOrientation,
    isToolbarExpanded: Boolean,
    onExpandToggleClick: () -> Unit
) {
    val arrangement = Arrangement.spacedBy(8.dp)
    val popupAlignment = when (orientation) {
        ToolbarOrientation.HORIZONTAL -> Alignment.TopCenter
        ToolbarOrientation.VERTICAL -> Alignment.CenterEnd
    }

    val alwaysVisibleButtons = remember(buttons) {
        buttons.filter { it.isAlwaysVisible }
    }
    val expandableButtons = remember(buttons) {
        buttons.filter { !it.isAlwaysVisible }
    }

    // Animate the size of the container holding the expandable buttons
    val animatedContentSize = Modifier.animateContentSize(
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
    )

    when (orientation) {
        ToolbarOrientation.HORIZONTAL -> {
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Always visible buttons
                Row(
                    horizontalArrangement = arrangement,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false) // Allow it to shrink if needed
                ) {
                    alwaysVisibleButtons.forEach { button ->
                        RenderButton(button, popupAlignment)
                    }
                }

                AnimatedVisibility(
                    modifier = Modifier
                        .padding(arrangement.spacing),
                    visible = expandableButtons.isNotEmpty() && isToolbarExpanded,
                    enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.5f),
                    exit = fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.5f)
                ) {
                    VerticalDivider(
                        modifier = Modifier
                            .height(24.dp),
                        thickness = 2.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }

                // Expandable buttons
                Row(
                    modifier = Modifier.then(animatedContentSize), // Apply animatedContentSize here
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = arrangement
                ) {
                    // Iterate and apply AnimatedVisibility to each expandable button
                    expandableButtons.forEach { button ->
                        AnimatedVisibility(
                            visible = isToolbarExpanded,
                            // Use scaleIn/Out for a cleaner circular appearance during animation
                            // No need for slide as animateContentSize handles position change
                            enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.5f),
                            exit = fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.5f)
                        ) {
                            // The modifier passed to RenderButton will include the size and circular clip
                            RenderButton(button, popupAlignment)
                        }
                    }
                }

                ToolbarExpandButton(
                    modifier = Modifier.padding(start = 8.dp),
                    isExpanded = isToolbarExpanded,
                    onClick = onExpandToggleClick
                )
            }
        }
        ToolbarOrientation.VERTICAL -> {
            Column(
                modifier = modifier,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Always visible buttons
                Column(
                    verticalArrangement = arrangement,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f, fill = false) // Allow it to shrink if needed
                ) {
                    alwaysVisibleButtons.forEach { button ->
                        RenderButton(button, popupAlignment)
                    }
                }

                AnimatedVisibility(
                    modifier = Modifier
                        .padding(arrangement.spacing),
                    visible = expandableButtons.isNotEmpty() && isToolbarExpanded,
                    enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.5f),
                    exit = fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.5f)
                ) {
                    VerticalDivider(
                        modifier = Modifier
                            .height(24.dp),
                        thickness = 2.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }

                // Expandable buttons
                Column(
                    modifier = Modifier.then(animatedContentSize), // Apply animatedContentSize here
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = arrangement
                ) {
                    // Iterate and apply AnimatedVisibility to each expandable button
                    expandableButtons.forEach { button ->
                        AnimatedVisibility(
                            visible = isToolbarExpanded,
                            // Use scaleIn/Out for a cleaner circular appearance during animation
                            enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.5f),
                            exit = fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.5f)
                        ) {
                            // The modifier passed to RenderButton will include the size and circular clip
                            RenderButton(button, popupAlignment)
                        }
                    }
                }

                ToolbarExpandButton(
                    modifier = Modifier.padding(top = 8.dp),
                    isExpanded = isToolbarExpanded,
                    onClick = onExpandToggleClick
                )
            }
        }
    }
}

@Composable
private fun RenderButton(button: ToolbarButton, popupAlignment: Alignment, modifier: Modifier = Modifier) {
    if (button.isExpandable) {
        ExpandableToolbarButton(
            modifier = modifier,
            button = button,
            popupAlignment = popupAlignment
        )
    } else {
        AnimatedToolbarButton(
            modifier = modifier,
            button = button
        )
    }
}

@Composable
private fun ToolbarExpandButton(
    modifier: Modifier,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "toggle_rotation"
    )

    IconButton(
        onClick = onClick,
        modifier = modifier
//            .size(40.dp)
            .background(
                color = if (isExpanded)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                shape = CircleShape // Apply CircleShape here for background
            )
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = if (isExpanded) "Collapse toolbar" else "Expand toolbar",
            tint = if (isExpanded)
                MaterialTheme.colorScheme.onPrimaryContainer
            else
                MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.graphicsLayer { rotationZ = rotationAngle }
        )
    }
}

@Composable
private fun AnimatedToolbarButton(modifier: Modifier, button: ToolbarButton) {
    val scale by animateFloatAsState(
        targetValue = if (button.isEnabled) 1f else 0.8f,
        animationSpec = tween(200),
        label = "button_scale"
    )

    IconButton(
        onClick = button.onClick ?: {},
        enabled = button.isEnabled,
        modifier = modifier
//            .size(40.dp)
            // Apply clip and graphicsLayer after size for correct visual effects
            .graphicsLayer { scaleX = scale; scaleY = scale } // Apply scaling to the whole button
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                shape = CircleShape // This applies the circular background and clips it
            )
        // No need for a separate .clip(CircleShape) if background has shape
    ) {
        Icon(
            imageVector = button.icon,
            contentDescription = button.contentDescription,
            tint = if (button.isEnabled)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
    }
}

@Composable
private fun ExpandableToolbarButton(
    modifier: Modifier,
    button: ToolbarButton,
    popupAlignment: Alignment
) {
    var isExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(
            onClick = { isExpanded = !isExpanded },
            enabled = button.isEnabled,
            modifier = Modifier
//                .size(40.dp)
                .background(
                    color = if (isExpanded)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    shape = CircleShape // Apply CircleShape here for background
                )
        ) {
            Icon(
                imageVector = button.icon,
                contentDescription = button.contentDescription,
                tint = if (isExpanded)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else if (button.isEnabled)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        }

        if (isExpanded) {
            Popup(
                alignment = popupAlignment,
                offset = when (popupAlignment) {
                    Alignment.TopCenter -> androidx.compose.ui.unit.IntOffset(0, -60)
                    Alignment.CenterEnd -> androidx.compose.ui.unit.IntOffset(60, 0)
                    else -> androidx.compose.ui.unit.IntOffset(0, 0)
                },
                onDismissRequest = { isExpanded = false },
                properties = PopupProperties(focusable = true)
            ) {
                Card(
                    modifier = Modifier.padding(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        button.expandableContent?.invoke()
                    }
                }
            }
        }
    }
}

@Composable
private fun PenTypeSelector(
    currentPenType: PenType,
    onPenTypeSwitch: (PenType) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.width(120.dp)
    ) {
        Text(
            text = "Tool",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        val penTypes = listOf(
            PenType.Pen to "Pen",
            PenType.StrokeEraser to "Eraser"
        )

        penTypes.forEach { (penType, label) ->
            val isSelected = currentPenType == penType
            val backgroundColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
            val contentColor = if (isSelected)
                MaterialTheme.colorScheme.onPrimaryContainer
            else
                MaterialTheme.colorScheme.onSurface

            Button(
                onClick = { onPenTypeSwitch(penType) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = backgroundColor,
                    contentColor = contentColor
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun ColorSwatchButton(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.2f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "color_button_scale"
    )

    Box(
        modifier = Modifier
            .size(24.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape) // Ensure clipping is applied to the box
            .background(color)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = CircleShape
            )
            .clickable { onClick() }
    )
}

@Composable
private fun ColorPicker(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit
) {
    val colors = listOf(
        Color.Red, Color.Blue, Color.Green, Color.Yellow,
        Color.Magenta, Color.Cyan, Color.Black, Color.Gray,
        Color.White, Color(0xFF8BC34A), Color(0xFFFF9800), Color(0xFF9C27B0)
    )

    Column {
        Text(
            text = "Color",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Using regular Column/Row for color swatches
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.width(120.dp)
        ) {
            colors.chunked(4).forEach { colorRow ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    colorRow.forEach { color ->
                        ColorSwatchButton(
                            color = color,
                            isSelected = color.toArgb() == selectedColor.toArgb(),
                            onClick = { onColorSelected(color) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PenControls(
    penConfig: PenConfig,
    onStrokeWidthChange: (Float) -> Unit,
    onAlphaChange: (Float) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.width(200.dp)
    ) {
        SliderControl(
            label = "Width",
            value = penConfig.width,
            valueRange = 1f..50f,
            onValueChange = onStrokeWidthChange,
            valueDisplay = { "${it.toInt()}px" }
        )

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
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = valueDisplay(value),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
            )
        )
    }
}

private fun createAllToolbarButtons(
    uiState: UiState,
    canUndo: Boolean,
    canRedo: Boolean,
    canClearCanvas: Boolean,
    onCanvasVisibilityToggle: (Boolean) -> Unit,
    onCanvasPassthroughToggle: (Boolean) -> Unit,
    onClearCanvas: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onPenTypeSwitch: (PenType) -> Unit,
    onColorChange: (Color) -> Unit,
    onStrokeWidthChange: (Float) -> Unit,
    onAlphaChange: (Float) -> Unit,
): List<ToolbarButton> {
    return listOf(
        ToolbarButton(
            id = "visibility",
            icon = if (uiState.canvasVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
            contentDescription = if (uiState.canvasVisible) "Hide canvas" else "Show canvas",
            onClick = { onCanvasVisibilityToggle(!uiState.canvasVisible) },
            isAlwaysVisible = true
        ),

        ToolbarButton(
            id = "passthrough",
            icon = Icons.Default.TouchApp,
            contentDescription = "Toggle passthrough",
            isEnabled = uiState.canvasVisible,
            onClick = { onCanvasPassthroughToggle(!uiState.canvasPassthrough) },
            isAlwaysVisible = false
        ),

        ToolbarButton(
            id = "undo",
            icon = Icons.AutoMirrored.Filled.Undo,
            contentDescription = "Undo",
            isEnabled = uiState.canvasVisible && canUndo,
            onClick = onUndo,
            isAlwaysVisible = true
        ),

        ToolbarButton(
            id = "redo",
            icon = Icons.AutoMirrored.Filled.Redo,
            contentDescription = "Redo",
            isEnabled = uiState.canvasVisible && canRedo,
            onClick = onRedo,
            isAlwaysVisible = false
        ),

        ToolbarButton(
            id = "clear",
            icon = Icons.Default.Delete,
            contentDescription = "Clear canvas",
            isEnabled = uiState.canvasVisible && canClearCanvas,
            onClick = onClearCanvas,
            isAlwaysVisible = true
        ),

        ToolbarButton(
            id = "pen_type",
            icon = when (uiState.currentPenType) {
                PenType.Pen -> Icons.Default.Edit
                PenType.StrokeEraser -> Icons.Default.Delete
            },
            contentDescription = "Tool selector",
            expandableContent = {
                PenTypeSelector(
                    currentPenType = uiState.currentPenType,
                    onPenTypeSwitch = onPenTypeSwitch
                )
            },
            isAlwaysVisible = true
        ),

        ToolbarButton(
            id = "color_picker",
            icon = Icons.Default.Palette,
            contentDescription = "Color picker",
            expandableContent = {
                ColorPicker(
                    selectedColor = uiState.currentPenConfig.color,
                    onColorSelected = onColorChange
                )
            },
            isAlwaysVisible = true
        ),

        ToolbarButton(
            id = "pen_controls",
            icon = Icons.Default.Tune,
            contentDescription = "Pen settings",
            expandableContent = {
                PenControls(
                    penConfig = uiState.currentPenConfig,
                    onStrokeWidthChange = onStrokeWidthChange,
                    onAlphaChange = onAlphaChange
                )
            },
            isAlwaysVisible = false
        )
    )
}