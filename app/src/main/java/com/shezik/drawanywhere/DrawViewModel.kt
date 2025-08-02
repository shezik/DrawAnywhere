package com.shezik.drawanywhere

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
    val canvasVisible: Boolean = true,
    val canvasPassthrough: Boolean = false,
    val penConfig: PenConfig = PenConfig(),
    val toolbarPosition: Offset = Offset(0f, 0f)  // TODO: Read penConfig and toolbarPosition from preferences
)

class DrawViewModel : ViewModel() {
    val controller = DrawController()

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val canUndo: StateFlow<Boolean> = controller.canUndo
    val canRedo: StateFlow<Boolean> = controller.canRedo
    val canClearCanvas: StateFlow<Boolean> = controller.canClearPaths

    init {
        viewModelScope.launch {
            _uiState.collect { state->
                // TODO: Save penConfig to preferences, excluding toolbarPosition. Actually, don't save anything when toolbarPosition changes.
                controller.setPenConfig(state.penConfig)
            }
//            controller.setPenConfig(_uiState.value.penConfig)  // TODO: ?
        }
    }

    fun setCanvasVisible(visible: Boolean) =
        _uiState.update { it.copy(canvasVisible = visible) }

    fun setCanvasPassthrough(passthrough: Boolean) =
        _uiState.update { it.copy(canvasPassthrough = passthrough) }

    fun setPenColor(color: Color) =
        _uiState.update {
            val newPenConfig = it.penConfig.copy(color = color)
            it.copy(penConfig = newPenConfig)
        }

    fun setStrokeWidth(width: Float) =
        _uiState.update {
            val newPenConfig = it.penConfig.copy(width = width)
            it.copy(penConfig = newPenConfig)
        }

    fun setStrokeAlpha(alpha: Float) =
        _uiState.update {
            val newPenConfig = it.penConfig.copy(alpha = alpha)
            it.copy(penConfig = newPenConfig)
        }

    fun setToolbarPosition(position: Offset) =
        _uiState.update { it.copy(toolbarPosition = position) }

    fun saveToolbarPosition() {
        // TODO: Save toolbarPosition to preferences
    }
}

@Composable
fun rememberDrawViewModel(): DrawViewModel =
    remember { DrawViewModel() }