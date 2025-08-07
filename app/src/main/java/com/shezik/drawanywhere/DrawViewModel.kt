package com.shezik.drawanywhere

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val canvasVisible: Boolean = true,
    val canvasPassthrough: Boolean = false,
    val currentPenType: PenType = PenType.Pen,  // This could be morphed into pen IDs later, if multiple pens with the same type is desired.
    val penConfigs: Map<PenType, PenConfig> = defaultPenConfigs(),

    val toolbarActive: Boolean = true,
    val toolbarPosition: Offset = Offset(0f, 0f),  // TODO: Read penConfig and toolbarPosition from preferences
    val toolbarOrientation: ToolbarOrientation = ToolbarOrientation.HORIZONTAL,

    val firstDrawerOpen: Boolean = !canvasPassthrough,
    val secondDrawerOpen: Boolean = false,

    // Second drawer expand/collapse button is UI-specific, we don't (and shouldn't) see it here
    // Buttons that don't appear in either drawer are "standalone"s, e.g. the visibility button
    val firstDrawerButtons: Set<String> = setOf(
        "undo", "clear", "pen_controls", "color_picker"
    ),
    val secondDrawerButtons: Set<String> = setOf(
        "passthrough", "redo", "settings"
    ),
    // Buttons that stay in second drawer but do not collapse
    val secondDrawerPinnedButtons: Set<String> = emptySet(),

    val autoClearCanvas: Boolean = false
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
        }
        resetToolbarTimer()
    }

    fun switchToPen(type: PenType) =
        _uiState.update { it.copy(currentPenType = type) }

    // TODO: Save to preferences, configurable
    fun resolvePenType(modifier: StrokeModifier) =
        when (modifier) {
            StrokeModifier.PrimaryButton   -> PenType.StrokeEraser
            StrokeModifier.SecondaryButton -> PenType.StrokeEraser  // TODO
            StrokeModifier.Both            -> PenType.StrokeEraser  // You might be experiencing a stroke
            StrokeModifier.None            -> uiState.value.currentPenType
        }



    var previousPenType: PenType? = null
    var isStrokeDown = false

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



    fun toggleCanvasVisibility() =
        setCanvasVisibility(!uiState.value.canvasVisible)

    fun setCanvasVisibility(visible: Boolean) {
        if (uiState.value.autoClearCanvas && !visible)
            clearCanvas()
        _uiState.update { it.copy(canvasVisible = visible) }
    }



    fun toggleCanvasPassthrough() =
        setCanvasPassthrough(!uiState.value.canvasPassthrough)

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



    private var dimmingJob: Job? = null

    fun resetToolbarTimer() {
        dimmingJob?.cancel()
        setToolbarActive(true)
        dimmingJob = viewModelScope.launch {
            delay(3000L)  // 5 seconds
            setToolbarActive(false)
        }
    }

    fun setToolbarActive(state: Boolean) =
        _uiState.update { it.copy(toolbarActive = state) }



    fun toggleToolbarOrientation() =
        setToolbarOrientation(
            when (uiState.value.toolbarOrientation) {
                ToolbarOrientation.VERTICAL -> ToolbarOrientation.HORIZONTAL
                ToolbarOrientation.HORIZONTAL -> ToolbarOrientation.VERTICAL
            }
        )

    fun setToolbarOrientation(orientation: ToolbarOrientation) =
        _uiState.update { it.copy(toolbarOrientation = orientation) }



    fun toggleFirstDrawer() =
        setFirstDrawerExpanded(!uiState.value.firstDrawerOpen)

    fun setFirstDrawerExpanded(state: Boolean) =
        _uiState.update { it.copy(firstDrawerOpen = state) }

    fun toggleSecondDrawer() =
        setSecondDrawerExpanded(!uiState.value.secondDrawerOpen)

    fun setSecondDrawerExpanded(state: Boolean) =
        _uiState.update { it.copy(secondDrawerOpen = state) }



    fun toggleSecondDrawerPinned(id: String) {
        val currentPinned = uiState.value.secondDrawerPinnedButtons
        pinSecondDrawerButton(id, !currentPinned.contains(id))
    }

    fun pinSecondDrawerButton(id: String, pinned: Boolean) {
        val currentPinned = uiState.value.secondDrawerPinnedButtons
        if (currentPinned.contains(id) == pinned)
            return

        val newPinned = if (pinned)
            currentPinned + id
        else
            currentPinned - id

        _uiState.update { it.copy(secondDrawerPinnedButtons = newPinned) }
    }



    fun setAutoClearCanvas(state: Boolean) =
        _uiState.update { it.copy(autoClearCanvas = state) }



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
        //              "pen_type", "color_picker", "pen_config")
        // val buttonOrder = preferences.getString("button_order", "")
        //     .split(",").filter { it.isNotEmpty() }
        //     .takeIf { it.isNotEmpty() }
        //     ?: listOf("visibility", "passthrough", "undo", "redo", "clear",
        //               "pen_type", "color_picker", "pen_config")

        // _uiState.update {
        //     it.copy(
        //         toolbarExpanded = expanded,
        //         lastToolbarOrientation = orientation,
        //         enabledButtons = enabledButtons,
        //         buttonOrder = buttonOrder
        //     )
        // }
    }
}