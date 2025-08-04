package com.shezik.drawanywhere

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val toolbarPosition: Offset = Offset(0f, 0f),  // TODO: Read penConfig and toolbarPosition from preferences
    val canvasVisible: Boolean = true,
    val canvasPassthrough: Boolean = false,
    val currentPenType: PenType = PenType.Pen,  // This could be morphed into pen IDs later, if multiple pens with the same type is desired.
    val penConfigs: Map<PenType, PenConfig> = defaultPenConfigs(),

    // New properties for sleek toolbar
    val toolbarExpanded: Boolean = true,
    val toolbarOrientation: ToolbarOrientation = ToolbarOrientation.HORIZONTAL,

    // Optional: For future button customization
    val enabledButtons: Set<String> = setOf(
        "visibility", "passthrough", "undo", "redo", "clear",
        "pen_type", "color_picker", "pen_controls"
    ),
    val buttonOrder: List<String> = listOf(
        "visibility", "passthrough", "undo", "redo", "clear",
        "pen_type", "color_picker", "pen_controls"
    )
) {
    val currentPenConfig: PenConfig
        // New PenConfig is not added until modified
        get() = penConfigs[currentPenType] ?: PenConfig()  // Triggering fallback would be weird. Creating a new tool?
}

fun defaultPenConfigs(): Map<PenType, PenConfig> = mapOf(
    PenType.Pen to PenConfig(penType = PenType.Pen),
    PenType.StrokeEraser to PenConfig(penType = PenType.StrokeEraser, width = 50f)
)

class DrawViewModel(private val controller: DrawController) : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val canUndo: StateFlow<Boolean> = controller.canUndo
    val canRedo: StateFlow<Boolean> = controller.canRedo
    val canClearCanvas: StateFlow<Boolean> = controller.canClearPaths

    init {
        viewModelScope.launch {
            _uiState.collect { state->
                // TODO: Save penConfig to preferences, excluding toolbarPosition. Actually, don't save anything when toolbarPosition changes.
                controller.setPenConfig(state.currentPenConfig)
            }
//            controller.setPenConfig(penConfig)  // TODO: ?
        }
    }

    fun switchToPen(type: PenType) =
        _uiState.update { it.copy(currentPenType = type) }

    var previousPenType: PenType? = null
    var isStrokeDown = false

    // TODO: Save to preferences, configurable
    fun resolvePenType(modifier: StrokeModifier) =
        when (modifier) {
            StrokeModifier.PrimaryButton   -> PenType.StrokeEraser
            StrokeModifier.SecondaryButton -> PenType.StrokeEraser  // TODO
            StrokeModifier.Both            -> PenType.StrokeEraser  // You might be experiencing a stroke
            StrokeModifier.None            -> uiState.value.currentPenType
        }

    fun startStroke(point: Offset, modifier: StrokeModifier) {
        finishStroke()  // Oh no! No multitouch! Who cares.

        val newPenType = resolvePenType(modifier)
        if (newPenType != uiState.value.currentPenType) {
            previousPenType = uiState.value.currentPenType
            switchToPen(newPenType)
        }

        controller.createPath(point)
        isStrokeDown = true
    }

    fun updateStroke(point: Offset) {
        if (!isStrokeDown) return
        controller.updateLatestPath(point)
    }

    fun finishStroke() {
        if (!isStrokeDown) return

        controller.finishPath()

        previousPenType?.let {
            switchToPen(it)
            previousPenType = null
        }
        isStrokeDown = false
    }

    fun setCanvasVisible(visible: Boolean) =
        _uiState.update { it.copy(canvasVisible = visible) }

    fun setCanvasPassthrough(passthrough: Boolean) =
        _uiState.update { it.copy(canvasPassthrough = passthrough) }

    fun setPenColor(color: Color) =
        _uiState.update {
            with (it) {
                val newConfigs = penConfigs.toMutableMap()
                val newPenConfig = newConfigs[currentPenType]?.copy(color = color)
                    ?: PenConfig(color = color, penType = currentPenType)
                newConfigs[currentPenType] = newPenConfig
                copy(penConfigs = newConfigs)
            }
        }


    fun setStrokeWidth(width: Float) =
        _uiState.update {
            with (it) {
                val newConfigs = penConfigs.toMutableMap()
                val newPenConfig = newConfigs[currentPenType]?.copy(width = width)
                    ?: PenConfig(width = width, penType = currentPenType)
                newConfigs[currentPenType] = newPenConfig
                copy(penConfigs = newConfigs)
            }
        }

    fun setStrokeAlpha(alpha: Float) =
        _uiState.update {
            with (it) {
                val newConfigs = penConfigs.toMutableMap()
                val newPenConfig = newConfigs[currentPenType]?.copy(alpha = alpha)
                    ?: PenConfig(alpha = alpha, penType = currentPenType)
                newConfigs[currentPenType] = newPenConfig
                copy(penConfigs = newConfigs)
            }
        }

    fun setToolbarPosition(position: Offset) =
        _uiState.update { it.copy(toolbarPosition = position) }

    fun saveToolbarPosition() {
        // TODO: Save toolbarPosition to preferences
    }

    fun clearCanvas() = controller.clearPaths()
    fun undo() = controller.undo()
    fun redo() = controller.redo()

    fun setToolbarExpanded(expanded: Boolean) {
        _uiState.update { it.copy(toolbarExpanded = expanded) }
    }

    fun setToolbarOrientation(orientation: ToolbarOrientation) {
        _uiState.update { it.copy(toolbarOrientation = orientation) }
    }

    // Optional: For future button customization
    fun setEnabledButtons(buttons: Set<String>) {
        _uiState.update { it.copy(enabledButtons = buttons) }
    }

    fun setButtonOrder(order: List<String>) {
        _uiState.update { it.copy(buttonOrder = order) }
    }

    fun toggleButton(buttonId: String) {
        val currentEnabled = _uiState.value.enabledButtons
        val newEnabled = if (currentEnabled.contains(buttonId)) {
            currentEnabled - buttonId
        } else {
            currentEnabled + buttonId
        }
        setEnabledButtons(newEnabled)
    }

    // Enhanced save/restore methods
    fun saveToolbarState() {
        val state = _uiState.value
        // Save to preferences - integrate with your existing preferences system
        // preferences.putBoolean("toolbar_expanded", state.toolbarExpanded)
        // preferences.putString("toolbar_orientation", state.lastToolbarOrientation?.name)
        // preferences.putStringSet("enabled_buttons", state.enabledButtons)
        // preferences.putString("button_order", state.buttonOrder.joinToString(","))
    }

    fun restoreToolbarState() {
        // Restore from preferences - integrate with your existing preferences system
        // val expanded = preferences.getBoolean("toolbar_expanded", true)
        // val orientation = preferences.getString("toolbar_orientation", null)
        //     ?.let { ToolbarOrientation.valueOf(it) }
        // val enabledButtons = preferences.getStringSet("enabled_buttons", null)
        //     ?: setOf("visibility", "passthrough", "undo", "redo", "clear",
        //              "pen_type", "color_picker", "pen_controls")
        // val buttonOrder = preferences.getString("button_order", "")
        //     .split(",").filter { it.isNotEmpty() }
        //     .takeIf { it.isNotEmpty() }
        //     ?: listOf("visibility", "passthrough", "undo", "redo", "clear",
        //               "pen_type", "color_picker", "pen_controls")

        // _uiState.update {
        //     it.copy(
        //         toolbarExpanded = expanded,
        //         lastToolbarOrientation = orientation,
        //         enabledButtons = enabledButtons,
        //         buttonOrder = buttonOrder
        //     )
        // }
    }

//    fun onToolbarEvent(event: ToolbarEvent) {
//        when (event) {
//            is ToolbarEvent.ChangeAlpha -> setStrokeAlpha(event.alpha)
//            is ToolbarEvent.ChangeColor -> setPenColor(event.color)
//            is ToolbarEvent.ChangeStrokeWidth -> setStrokeWidth(event.width)
//            is ToolbarEvent.ClearCanvas -> clearCanvas()
//            is ToolbarEvent.PositionChange -> setToolbarPosition(event.position)
//            is ToolbarEvent.PositionChangeFinished -> saveToolbarPosition()
//            is ToolbarEvent.Redo -> redo()
//            is ToolbarEvent.SetOrientation -> setToolbarOrientation(event.orientation)
//            is ToolbarEvent.SwitchPenType -> switchToPen(event.penType)
//            is ToolbarEvent.ToggleExpanded -> setToolbarExpanded(event.isExpanded)
//            is ToolbarEvent.TogglePassthrough -> setCanvasPassthrough(!uiState.value.canvasPassthrough)
//            is ToolbarEvent.ToggleVisibility -> setCanvasVisible(!uiState.value.canvasVisible)
//            is ToolbarEvent.Undo -> undo()
//        }
//    }
}