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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

// TODO: Toggle methods in ViewModel, modifiers (esp. size) unification

// Unified button data class
data class ToolbarButton(
    val id: String,
    val icon: ImageVector,
    val contentDescription: String,
    val isEnabled: Boolean = true,
    val onClick: (() -> Unit)? = null,
    val popupContent: (@Composable () -> Unit)? = null
) {
    val hasPopup: Boolean
        get() = popupContent != null
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

    val haptics = LocalHapticFeedback.current

    val allButtonsMap = createAllToolbarButtons(
        uiState = uiState,
        canUndo = canUndo,
        canRedo = canRedo,
        canClearCanvas = canClearCanvas,
        onCanvasVisibilityToggle = { state: Boolean ->
            viewModel.setCanvasVisible(state)
            viewModel.setFirstDrawerExpanded(state)  // TODO
        },
        onCanvasPassthroughToggle = { state: Boolean ->
            viewModel.setCanvasPassthrough(state)
            viewModel.pinSecondDrawerButton("passthrough", state)  // TODO
        },
        onClearCanvas = viewModel::clearCanvas,
        onUndo = viewModel::undo,
        onRedo = viewModel::redo,
        onPenTypeSwitch = viewModel::switchToPen,
        onColorChange = viewModel::setPenColor,
        onStrokeWidthChange = viewModel::setStrokeWidth,
        onAlphaChange = viewModel::setStrokeAlpha,
    ).associateBy { it.id }

    // Root composable
    DraggableToolbarCard(
        modifier = modifier
            // Leave space for defaultElevation shadows, should be as small as possible
            // since user can't start a stroke on the outer padding.
            .padding(5.dp),
        uiState = uiState,
        haptics = haptics,
        onPositionChange = viewModel::setToolbarPosition,
        onPositionSaved = viewModel::saveToolbarPosition
    ) {
        ToolbarButtonsContainer(
            modifier = Modifier.padding(8.dp),
            uiState = uiState,
            allButtonsMap = allButtonsMap,
            onExpandToggleClick = viewModel::toggleSecondDrawer
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
    uiState: UiState,
    allButtonsMap: Map<String, ToolbarButton>,
    onExpandToggleClick: () -> Unit
) {
    val orientation = uiState.toolbarOrientation
    val isFirstDrawerOpen = uiState.firstDrawerOpen
    val isSecondDrawerOpen = uiState.secondDrawerOpen
    val firstDrawerButtonIds = uiState.firstDrawerButtons
    val secondDrawerButtonIds = uiState.secondDrawerButtons
    val secondDrawerPinnedButtons = uiState.secondDrawerPinnedButtons

    val standaloneButtonIds = allButtonsMap.keys.filter {
        it !in firstDrawerButtonIds &&
        it !in secondDrawerButtonIds
    }

    val arrangement = Arrangement.spacedBy(8.dp)
    val popupAlignment = when (orientation) {
        ToolbarOrientation.HORIZONTAL -> Alignment.TopCenter
        ToolbarOrientation.VERTICAL -> Alignment.CenterEnd
    }

    // Animate the size of the container holding the expandable buttons
    val animatedContentSize = Modifier.animateContentSize(
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
    )

    when (orientation) {
        // TODO: REMEMBER TO SYNC VERTICAL CODE WITH HORIZONTAL CODE
        // TODO: HORIZONTAL CODE IS THE MOST ACCURATE
        ToolbarOrientation.HORIZONTAL -> {
            Row(
                modifier = modifier.then(animatedContentSize),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = arrangement
            ) {
                standaloneButtonIds.forEach { buttonId ->
                    val button = allButtonsMap[buttonId] ?: return@forEach
                    RenderButton(button, popupAlignment)
                }

                firstDrawerButtonIds.forEach { buttonId ->
                    val button = allButtonsMap[buttonId] ?: return@forEach
                    AnimatedVisibility(
                        visible = isFirstDrawerOpen,
                        enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.5f),
                        exit = fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.5f)
                    ) {
                        RenderButton(button, popupAlignment)
                    }
                }

                val isDividerVisible = isFirstDrawerOpen && (
                        secondDrawerPinnedButtons.isNotEmpty()
                                || (isSecondDrawerOpen && secondDrawerButtonIds.isNotEmpty())
                        )
                AnimatedVisibility(
                    visible = isDividerVisible,
                    enter = fadeIn(tween(300)),
                    exit = fadeOut(tween(300))
                ) {
                    VerticalDivider(
                        modifier = Modifier
                            .height(24.dp)
                            .padding(horizontal = 8.dp),
                        thickness = 2.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }

                secondDrawerButtonIds.forEach { buttonId ->
                    val button = allButtonsMap[buttonId] ?: return@forEach
                    val isVisible = isFirstDrawerOpen && (isSecondDrawerOpen || buttonId in secondDrawerPinnedButtons)

                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.5f),
                        exit = fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.5f)
                    ) {
                        RenderButton(button, popupAlignment)
                    }
                }

                val isExpandButtonVisible = isFirstDrawerOpen && secondDrawerButtonIds.isNotEmpty()
                AnimatedVisibility(
                    visible = isExpandButtonVisible,
                    enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.5f),
                    exit = fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.5f)
                ) {
                    ToolbarExpandButton(
                        modifier = Modifier,
                        isExpanded = isSecondDrawerOpen,
                        onClick = onExpandToggleClick
                    )
                }
            }
        }

        ToolbarOrientation.VERTICAL -> {
            Column(
                modifier = modifier,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = arrangement
            ) {
                standaloneButtonIds.forEach { buttonId ->
                    val button = allButtonsMap[buttonId] ?: return@forEach
                    RenderButton(button, popupAlignment)
                }

                firstDrawerButtonIds.forEach { buttonId ->
                    val button = allButtonsMap[buttonId] ?: return@forEach
                    AnimatedVisibility(
                        visible = isFirstDrawerOpen,
                        enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.5f),
                        exit = fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.5f)
                    ) {
                        RenderButton(button, popupAlignment)
                    }
                }

                val isDividerVisible = isFirstDrawerOpen && (
                        secondDrawerPinnedButtons.isNotEmpty()
                                || (isSecondDrawerOpen && secondDrawerButtonIds.isNotEmpty())
                        )
                AnimatedVisibility(
                    visible = isDividerVisible,
                    enter = fadeIn(tween(300)),
                    exit = fadeOut(tween(300))
                ) {
                    HorizontalDivider(
                        modifier = Modifier
                            .width(24.dp)
                            .padding(horizontal = 8.dp),
                        thickness = 2.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }

                secondDrawerButtonIds.forEach { buttonId ->
                    val button = allButtonsMap[buttonId] ?: return@forEach
                    val isVisible = isFirstDrawerOpen && (isSecondDrawerOpen || buttonId in secondDrawerPinnedButtons)

                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.5f),
                        exit = fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.5f)
                    ) {
                        RenderButton(button, popupAlignment)
                    }
                }

                val isExpandButtonVisible = isFirstDrawerOpen && secondDrawerButtonIds.isNotEmpty()
                AnimatedVisibility(
                    visible = isExpandButtonVisible,
                    enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.5f),
                    exit = fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.5f)
                ) {
                    ToolbarExpandButton(
                        modifier = Modifier,
                        isExpanded = isSecondDrawerOpen,
                        onClick = onExpandToggleClick
                    )
                }
            }
        }
    }
}

@Composable
private fun RenderButton(button: ToolbarButton, popupAlignment: Alignment, modifier: Modifier = Modifier) {
    if (button.hasPopup) {
        PopupToolbarButton(
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
                shape = CircleShape  // Apply CircleShape here for background
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
private fun PopupToolbarButton(
    modifier: Modifier,
    button: ToolbarButton,
    popupAlignment: Alignment
) {
    var isPopupOpen by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(
            onClick = { isPopupOpen = !isPopupOpen },
            enabled = button.isEnabled,
            modifier = Modifier
//                .size(40.dp)
                .background(
                    color = if (isPopupOpen)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    shape = CircleShape // Apply CircleShape here for background
                )
        ) {
            Icon(
                imageVector = button.icon,
                contentDescription = button.contentDescription,
                tint = if (isPopupOpen)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else if (button.isEnabled)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        }

        if (isPopupOpen) {
            Popup(
                alignment = popupAlignment,
                offset = when (popupAlignment) {
                    Alignment.TopCenter -> androidx.compose.ui.unit.IntOffset(0, -60)
                    Alignment.CenterEnd -> androidx.compose.ui.unit.IntOffset(60, 0)
                    else -> androidx.compose.ui.unit.IntOffset(0, 0)
                },
                onDismissRequest = { isPopupOpen = false },
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
                        button.popupContent?.invoke()
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
            onClick = { onCanvasVisibilityToggle(!uiState.canvasVisible) }
        ),

        ToolbarButton(
            id = "undo",
            icon = Icons.AutoMirrored.Filled.Undo,
            contentDescription = "Undo",
            isEnabled = uiState.canvasVisible && canUndo,
            onClick = onUndo
        ),

        ToolbarButton(
            id = "clear",
            icon = Icons.Default.Delete,
            contentDescription = "Clear canvas",
            isEnabled = uiState.canvasVisible && canClearCanvas,
            onClick = onClearCanvas
        ),

        ToolbarButton(
            id = "pen_type",
            icon = when (uiState.currentPenType) {
                PenType.Pen -> Icons.Default.Edit
                PenType.StrokeEraser -> Icons.Default.Delete
            },
            contentDescription = "Tool selector",
            popupContent = {
                PenTypeSelector(
                    currentPenType = uiState.currentPenType,
                    onPenTypeSwitch = onPenTypeSwitch
                )
            }
        ),

        ToolbarButton(
            id = "color_picker",
            icon = Icons.Default.Palette,
            contentDescription = "Color picker",
            popupContent = {
                ColorPicker(
                    selectedColor = uiState.currentPenConfig.color,
                    onColorSelected = onColorChange
                )
            }
        ),

        ToolbarButton(
            id = "passthrough",
            icon = Icons.Default.TouchApp,
            contentDescription = "Toggle passthrough",
            isEnabled = uiState.canvasVisible,
            onClick = { onCanvasPassthroughToggle(!uiState.canvasPassthrough) }
        ),

        ToolbarButton(
            id = "redo",
            icon = Icons.AutoMirrored.Filled.Redo,
            contentDescription = "Redo",
            isEnabled = uiState.canvasVisible && canRedo,
            onClick = onRedo
        ),

        ToolbarButton(
            id = "pen_config",
            icon = Icons.Default.Tune,
            contentDescription = "Pen settings",
            popupContent = {
                PenControls(
                    penConfig = uiState.currentPenConfig,
                    onStrokeWidthChange = onStrokeWidthChange,
                    onAlphaChange = onAlphaChange
                )
            }
        )
    )
}