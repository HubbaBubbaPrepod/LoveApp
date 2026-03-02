package com.example.loveapp.viewmodel

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loveapp.data.api.models.ArtCanvasResponse
import com.example.loveapp.data.repository.ArtRepository
import com.example.loveapp.data.repository.AuthRepository
import com.example.loveapp.ui.art.ArtSocketManager
import com.example.loveapp.ui.art.DrawActionEvent
import com.example.loveapp.ui.art.DrawPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A single drawn stroke with its rendering parameters. */
data class DrawPath(
    val path: Path,
    val color: Color,
    val strokeWidth: Float,
    val isFromPartner: Boolean = false
)

@HiltViewModel
class ArtViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val artRepository: ArtRepository,
    private val authRepository: AuthRepository,
    val socketManager: ArtSocketManager
) : ViewModel() {

    // ── Gallery state ───────────────────────────────────────────────────────
    private val _canvases = MutableStateFlow<List<ArtCanvasResponse>>(emptyList())
    val canvases: StateFlow<List<ArtCanvasResponse>> = _canvases.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ── Active canvas editor state ──────────────────────────────────────────
    private val _activeCanvas = MutableStateFlow<ArtCanvasResponse?>(null)
    val activeCanvas: StateFlow<ArtCanvasResponse?> = _activeCanvas.asStateFlow()

    /** All committed strokes (local + partner). */
    private val _strokes = MutableStateFlow<List<DrawPath>>(emptyList())
    val strokes: StateFlow<List<DrawPath>> = _strokes.asStateFlow()

    /** The stroke currently being drawn by the local user (not yet committed). */
    private val _liveStroke = MutableStateFlow<DrawPath?>(null)
    val liveStroke: StateFlow<DrawPath?> = _liveStroke.asStateFlow()

    /** Undo stack – each entry is the full path list before an action. */
    private val undoStack = ArrayDeque<List<DrawPath>>()
    private val redoStack = ArrayDeque<List<DrawPath>>()

    /** Partner's in-progress stroke (shown as semi-transparent). */
    private val _partnerLiveStroke = MutableStateFlow<DrawPath?>(null)
    val partnerLiveStroke: StateFlow<DrawPath?> = _partnerLiveStroke.asStateFlow()

    // Current tool settings (exposed for UI)
    private val _selectedColor  = MutableStateFlow(Color.Black)
    val selectedColor: StateFlow<Color> = _selectedColor.asStateFlow()

    private val _strokeWidth    = MutableStateFlow(6f)
    val strokeWidth: StateFlow<Float> = _strokeWidth.asStateFlow()

    private val _isEraser       = MutableStateFlow(false)
    val isEraser: StateFlow<Boolean> = _isEraser.asStateFlow()

    // ── Thumbnail upload state ──────────────────────────────────────────────
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    // ── Init ────────────────────────────────────────────────────────────────
    init {
        loadGallery()
        connectSocket()
    }

    private fun connectSocket() {
        viewModelScope.launch {
            val token = authRepository.getToken() ?: return@launch
            socketManager.connect(token)
            socketManager.drawActions.collect { event ->
                handlePartnerAction(event)
            }
        }
    }

    // ── Gallery actions ─────────────────────────────────────────────────────
    fun loadGallery() {
        viewModelScope.launch {
            _isLoading.value = true
            artRepository.getCanvases()
                .onSuccess { _canvases.value = it }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    fun createCanvas(title: String = "Без названия", onCreated: (ArtCanvasResponse) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            artRepository.createCanvas(title)
                .onSuccess { canvas ->
                    _canvases.value = listOf(canvas) + _canvases.value
                    onCreated(canvas)
                }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    fun renameCanvas(id: Int, title: String) {
        viewModelScope.launch {
            artRepository.updateCanvas(id, title)
                .onSuccess { updated ->
                    _canvases.value = _canvases.value.map {
                        if (it.id == id) updated else it
                    }
                    if (_activeCanvas.value?.id == id) _activeCanvas.value = updated
                }
                .onFailure { _errorMessage.value = it.message }
        }
    }

    fun deleteCanvas(id: Int) {
        viewModelScope.launch {
            artRepository.deleteCanvas(id)
                .onSuccess {
                    _canvases.value = _canvases.value.filter { it.id != id }
                }
                .onFailure { _errorMessage.value = it.message }
        }
    }

    // ── Canvas editor lifecycle ─────────────────────────────────────────────
    fun openCanvas(canvas: ArtCanvasResponse) {
        _activeCanvas.value = canvas
        _strokes.value = emptyList()
        undoStack.clear(); redoStack.clear()
        _liveStroke.value = null
        _partnerLiveStroke.value = null
        socketManager.joinCanvas(canvas.id)
    }

    fun closeCanvas() {
        _activeCanvas.value?.let { socketManager.leaveCanvas(it.id) }
        _activeCanvas.value = null
        _liveStroke.value = null
        _partnerLiveStroke.value = null
    }

    /** Save current canvas as a thumbnail and refresh gallery. */
    fun saveSnapshot(bitmap: Bitmap) {
        val canvasId = _activeCanvas.value?.id ?: return
        viewModelScope.launch {
            _isSaving.value = true
            artRepository.uploadThumbnail(canvasId, bitmap)
                .onSuccess { url ->
                    _canvases.value = _canvases.value.map {
                        if (it.id == canvasId) it.copy(thumbnailUrl = url) else it
                    }
                }
                .onFailure { _errorMessage.value = it.message }
            _isSaving.value = false
        }
    }

    // ── Drawing ──────────────────────────────────────────────────────────────
    fun setColor(color: Color)       { _selectedColor.value = color; _isEraser.value = false }
    fun setStrokeWidth(w: Float)     { _strokeWidth.value = w }
    fun setEraser(on: Boolean)       { _isEraser.value = on }

    fun strokeStart(x: Float, y: Float) {
        val color = if (_isEraser.value) Color.White else _selectedColor.value
        val p = Path().also { it.moveTo(x, y) }
        _liveStroke.value = DrawPath(p, color, _strokeWidth.value)
        val canvasId = _activeCanvas.value?.id ?: return
        socketManager.sendDrawAction(canvasId, "start", listOf(DrawPoint(x, y)),
            colorToHex(color), _strokeWidth.value)
    }

    fun strokeMove(x: Float, y: Float) {
        _liveStroke.value = _liveStroke.value?.let { dp ->
            dp.copy(path = Path().also { it.addPath(dp.path); it.lineTo(x, y) })
        }
        val canvasId = _activeCanvas.value?.id ?: return
        socketManager.sendDrawAction(canvasId, "move", listOf(DrawPoint(x, y)),
            colorToHex(_liveStroke.value?.color ?: Color.Black), _strokeWidth.value)
    }

    fun strokeEnd() {
        val finished = _liveStroke.value ?: return
        undoStack.addLast(_strokes.value)
        redoStack.clear()
        _strokes.value = _strokes.value + finished
        _liveStroke.value = null
        val canvasId = _activeCanvas.value?.id ?: return
        socketManager.sendDrawAction(canvasId, "end", emptyList(),
            colorToHex(finished.color), finished.strokeWidth)
    }

    fun clearCanvas() {
        undoStack.addLast(_strokes.value)
        redoStack.clear()
        _strokes.value = emptyList()
        _activeCanvas.value?.id?.let { socketManager.sendClear(it) }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.addLast(_strokes.value)
        _strokes.value = undoStack.removeLast()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.addLast(_strokes.value)
        _strokes.value = redoStack.removeLast()
    }

    // ── Partner drawing events ───────────────────────────────────────────────
    private fun handlePartnerAction(event: DrawActionEvent) {
        if (event.canvasId != _activeCanvas.value?.id) return
        val color = parseHexColor(event.color).copy(alpha = 0.75f)
        when (event.action) {
            "start" -> {
                val pt = event.points.firstOrNull() ?: return
                val p = Path().also { it.moveTo(pt.x, pt.y) }
                _partnerLiveStroke.value = DrawPath(p, color, event.strokeWidth, isFromPartner = true)
            }
            "move" -> {
                val pt = event.points.firstOrNull() ?: return
                _partnerLiveStroke.value = _partnerLiveStroke.value?.let { dp ->
                    dp.copy(path = Path().also { it.addPath(dp.path); it.lineTo(pt.x, pt.y) })
                }
            }
            "end" -> {
                val finished = _partnerLiveStroke.value
                if (finished != null) {
                    _strokes.value = _strokes.value + finished
                }
                _partnerLiveStroke.value = null
            }
            "clear" -> {
                _strokes.value = emptyList()
                _partnerLiveStroke.value = null
            }
        }
    }

    fun clearError() { _errorMessage.value = null }

    override fun onCleared() {
        super.onCleared()
        socketManager.disconnect()
    }

    // ── Helpers ─────────────────────────────────────────────────────────────
    private fun colorToHex(color: Color): String {
        val r = (color.red   * 255).toInt()
        val g = (color.green * 255).toInt()
        val b = (color.blue  * 255).toInt()
        return "#%02X%02X%02X".format(r, g, b)
    }

    private fun parseHexColor(hex: String): Color = try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) { Color.Black }
}
