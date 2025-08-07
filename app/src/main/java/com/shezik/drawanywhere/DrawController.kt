package com.shezik.drawanywhere

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class PenType {
    Pen, StrokeEraser, /*PixelEraser*/  // TODO
}

data class PenConfig(
    val penType: PenType = PenType.Pen,
    val color: Color = Color.Red,
    val width: Float = 5f,
    val alpha: Float = 1f
)

data class PathWrapper(
    val id: String = UUID.randomUUID().toString(),  // TODO: Do we really need this?
    val points: SnapshotStateList<Offset>,
    private var _cachedPath: MutableState<Path?> = mutableStateOf(null),
    private var cachedPathInvalid: MutableState<Boolean> = mutableStateOf(true),
    val color: Color,
    val width: Float,
    val alpha: Float
) {
    val cachedPath: Path get() =
        if ((_cachedPath.value == null) or cachedPathInvalid.value)
            rebuildPath().value
        else
            _cachedPath.value!!

    @Suppress("UNCHECKED_CAST")
    private fun rebuildPath(): MutableState<Path> {  // TODO: Find a way to append points to the cached path instead of complete recalculation
        _cachedPath.value = pointsToPath(points)
        cachedPathInvalid.value = false
        return _cachedPath as MutableState<Path>
    }

    fun invalidatePath() {
        cachedPathInvalid.value = true
    }

    fun releasePath(): PathWrapper {
        _cachedPath.value = null
        invalidatePath()
        return this
    }
}

sealed class DrawAction {
    data class AddPath(val pathWrapper: PathWrapper) : DrawAction()
    data class ErasePath(val pathWrapper: PathWrapper) : DrawAction()
    data class ClearPaths(val paths: List<PathWrapper>) : DrawAction()
}

class DrawController {
    private lateinit var penConfig: PenConfig

    fun setPenConfig(config: PenConfig) {
        penConfig = config
    }

    private val _pathList = mutableStateListOf<PathWrapper>()
    val pathList: List<PathWrapper>
        get() = _pathList

    private val maxUndoDepth = 50  // TODO: Make configurable
    private val undoStack = mutableListOf<DrawAction>()
    private val redoStack = mutableListOf<DrawAction>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val _canClear = MutableStateFlow(false)
    val canClearPaths: StateFlow<Boolean> = _canClear.asStateFlow()

    fun updateLatestPath(newPoint: Offset) {
        if (penConfig.penType == PenType.StrokeEraser) {
            erasePath(newPoint)
            return
        }

        _pathList.lastOrNull()?.let { latestPath ->
            latestPath.points.add(newPoint)
            latestPath.invalidatePath()
        }
    }

    fun createPath(newPoint: Offset) {
        if (!this::penConfig.isInitialized)
            throw IllegalStateException("PenConfig used without initialization!")

        if (penConfig.penType == PenType.StrokeEraser) {
            erasePath(newPoint)
            return
        }

        _pathList.add(PathWrapper(
            points = mutableStateListOf(newPoint),
            color = penConfig.color,
            width = penConfig.width,
            alpha = penConfig.alpha
        ))
    }

    fun finishPath() {
        if (penConfig.penType == PenType.StrokeEraser) return
        if (_pathList.isEmpty()) return

        val latestPath = _pathList.last()

        if (latestPath.points.isEmpty()) {
            _pathList.removeAt(_pathList.lastIndex)
            return
        }

        redoStack.clear()
        addToUndoStack(DrawAction.AddPath(latestPath))  // Shallow copy, we aren't touching its cachedPath. Undo/redo methods below depend on shallow copying.
        updateUndoRedoState()
        updateClearPathsState()
    }

    // Erase one path at a time.
    private fun erasePath(point: Offset) {
        val eraserRadius = penConfig.width / 2
        var indexToErase: Int? = null

        for (i in _pathList.indices.reversed()) {
            val pathWrapper = _pathList[i]
            val compensatedRadius = pathWrapper.width / 2 + eraserRadius

            if (pathWrapper.points.size > 1) {
                pathWrapper.points.zipWithNext().forEach { (p1, p2) ->
                    val dist = distancePointToLineSegment(point, p1, p2)
                    if (dist <= compensatedRadius) {
                        indexToErase = i
                        return@forEach
                    }
                }
            } else {
                pathWrapper.points.firstOrNull()?.let {
                    val dist = distance(point, it)
                    if (dist <= compensatedRadius) {
                        indexToErase = i
                    }
                }
            }
            if (indexToErase != null) break
        }

        indexToErase?.let {
            val erasedPath = _pathList.removeAt(it)
            addToUndoStack(DrawAction.ErasePath(erasedPath))
            erasedPath.releasePath()
            redoStack.clear()
            updateUndoRedoState()
            updateClearPathsState()
        }
    }

    fun clearPaths() {
        if (_pathList.isEmpty()) return

        _pathList.forEach {
            it.releasePath()
        }
        addToUndoStack(DrawAction.ClearPaths(_pathList.toList()))
        _pathList.clear()
        redoStack.clear()
        updateUndoRedoState()
        updateClearPathsState()
    }

    private fun updateUndoRedoState() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    private fun updateClearPathsState() {
        _canClear.value = _pathList.isNotEmpty()
    }

    private fun addToUndoStack(action: DrawAction) {
        undoStack.add(action)
        if (undoStack.size > maxUndoDepth) {
            undoStack.removeAt(0)
        }
    }

    fun undo() {
        if (undoStack.isEmpty()) return

        val action = undoStack.removeAt(undoStack.lastIndex)
        when (action) {
            is DrawAction.AddPath -> {
                val whichPath = action.pathWrapper
                if (_pathList.remove(whichPath)) {
                    whichPath.releasePath()
                    redoStack.add(action)
                }
            }
            is DrawAction.ErasePath -> {
                val whichPath = action.pathWrapper
                _pathList.add(whichPath)
                redoStack.add(action)
            }
            is DrawAction.ClearPaths -> {
                val whichPaths = action.paths
                _pathList.addAll(whichPaths)
                redoStack.add(action)
            }
        }
        updateUndoRedoState()
        updateClearPathsState()
    }

    fun redo() {
        if (redoStack.isEmpty()) return

        val action = redoStack.removeAt(redoStack.lastIndex)
        when (action) {
            is DrawAction.AddPath -> {
                val whichPath = action.pathWrapper
                _pathList.add(whichPath)
                addToUndoStack(action)
            }
            is DrawAction.ErasePath -> {
                val whichPath = action.pathWrapper
                if (_pathList.remove(whichPath)) {
                    whichPath.releasePath()
                    addToUndoStack(action)
                }
            }
            is DrawAction.ClearPaths -> {
                val whichPaths = action.paths
                _pathList.removeAll(whichPaths)
                whichPaths.forEach { it.releasePath() }
                addToUndoStack(action)
            }
        }
        updateUndoRedoState()
        updateClearPathsState()
    }
}