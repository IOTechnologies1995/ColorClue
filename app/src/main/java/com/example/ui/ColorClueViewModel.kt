package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.color.ColorDictionary
import com.example.color.ColorFamily
import com.example.color.DetectedColorSpot
import com.example.color.HarmonyType
import com.example.color.IdentifiedColor
import com.example.color.ImageColorExtractor
import com.example.color.PaletteGenerator
import com.example.color.RecommendedPalette
import com.example.color.SamplingRadius
import com.example.data.CapturedColorEntity
import com.example.data.ColorClueDatabase
import com.example.data.ColorHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class StudioMode {
    CAMERA,
    PHOTO_INSPECTOR,
    SWATCH_INSPECTOR
}

data class ColorClueUiState(
    val studioMode: StudioMode = StudioMode.CAMERA,
    val livePreviewColor: IdentifiedColor? = null,
    val capturedBitmap: Bitmap? = null,
    val sampleNormX: Float = 0.5f,
    val sampleNormY: Float = 0.5f,
    val samplingRadius: SamplingRadius = SamplingRadius.BALANCED,
    val identifiedColor: IdentifiedColor? = null,
    val dominantSpots: List<DetectedColorSpot> = emptyList(),
    val recommendedPalettes: List<RecommendedPalette> = emptyList(),
    val selectedHarmonyFilter: HarmonyType? = null,
    val flashEnabled: Boolean = false,
    val useFrontCamera: Boolean = false,
    val isProcessingImage: Boolean = false,
    val cameraUnavailableError: String? = null,
    val selectedHistoryFamily: ColorFamily? = null
)

class ColorClueViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ColorHistoryRepository by lazy {
        val db = ColorClueDatabase.getInstance(application)
        ColorHistoryRepository(db.capturedColorDao(), application.applicationContext)
    }

    private val _uiState = MutableStateFlow(ColorClueUiState())
    val uiState: StateFlow<ColorClueUiState> = _uiState.asStateFlow()

    val historyItems: StateFlow<List<CapturedColorEntity>> = repository.recentCaptures
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var dominantJob: Job? = null

    fun onLiveFrameColor(color: IdentifiedColor) {
        if (_uiState.value.studioMode == StudioMode.CAMERA) {
            _uiState.update { it.copy(livePreviewColor = color) }
        }
    }

    fun onCameraError(message: String) {
        _uiState.update { it.copy(cameraUnavailableError = message) }
    }

    fun clearCameraError() {
        _uiState.update { it.copy(cameraUnavailableError = null) }
    }

    fun onToggleFlash() {
        _uiState.update { it.copy(flashEnabled = !it.flashEnabled) }
    }

    fun onSwitchCamera() {
        _uiState.update { it.copy(useFrontCamera = !it.useFrontCamera, flashEnabled = false) }
    }

    fun onPhotoCaptured(bitmap: Bitmap) {
        loadBitmapForInspection(bitmap, autoSaveToHistory = true)
    }

    fun onPhotoPicked(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingImage = true) }
            val decoded = withContext(Dispatchers.IO) {
                ImageColorExtractor.decodeBitmapFromUri(getApplication(), uri)
            }
            if (decoded != null) {
                loadBitmapForInspection(decoded, autoSaveToHistory = true)
            } else {
                _uiState.update { it.copy(isProcessingImage = false) }
            }
        }
    }

    private fun loadBitmapForInspection(bitmap: Bitmap, autoSaveToHistory: Boolean) {
        val radius = _uiState.value.samplingRadius
        val centerColor = ImageColorExtractor.sampleColorAtNormalizedPoint(bitmap, 0.5f, 0.5f, radius)
        val palettes = PaletteGenerator.generatePalettes(centerColor)

        _uiState.update {
            it.copy(
                studioMode = StudioMode.PHOTO_INSPECTOR,
                capturedBitmap = bitmap,
                sampleNormX = 0.5f,
                sampleNormY = 0.5f,
                identifiedColor = centerColor,
                dominantSpots = emptyList(),
                recommendedPalettes = palettes,
                selectedHarmonyFilter = null,
                isProcessingImage = false,
                flashEnabled = false
            )
        }

        dominantJob?.cancel()
        dominantJob = viewModelScope.launch {
            val spots = withContext(Dispatchers.Default) {
                ImageColorExtractor.extractDominantColors(bitmap, maxColors = 5)
            }
            _uiState.update { state ->
                if (state.capturedBitmap === bitmap) state.copy(dominantSpots = spots) else state
            }
        }

        if (autoSaveToHistory) {
            viewModelScope.launch {
                repository.saveCapture(centerColor, bitmap)
            }
        }
    }

    fun onSamplePointChanged(normX: Float, normY: Float) {
        val bitmap = _uiState.value.capturedBitmap ?: return
        val clampedX = normX.coerceIn(0f, 1f)
        val clampedY = normY.coerceIn(0f, 1f)
        val sampled = ImageColorExtractor.sampleColorAtNormalizedPoint(
            bitmap, clampedX, clampedY, _uiState.value.samplingRadius
        )
        val palettes = PaletteGenerator.generatePalettes(sampled)
        _uiState.update {
            it.copy(
                sampleNormX = clampedX,
                sampleNormY = clampedY,
                identifiedColor = sampled,
                recommendedPalettes = palettes
            )
        }
    }

    fun onSamplingRadiusChanged(radius: SamplingRadius) {
        val bitmap = _uiState.value.capturedBitmap
        if (bitmap == null) {
            _uiState.update { it.copy(samplingRadius = radius) }
            return
        }
        val state = _uiState.value
        val sampled = ImageColorExtractor.sampleColorAtNormalizedPoint(
            bitmap, state.sampleNormX, state.sampleNormY, radius
        )
        val palettes = PaletteGenerator.generatePalettes(sampled)
        _uiState.update {
            it.copy(
                samplingRadius = radius,
                identifiedColor = sampled,
                recommendedPalettes = palettes
            )
        }
    }

    fun onDominantSpotSelected(spot: DetectedColorSpot) {
        val palettes = PaletteGenerator.generatePalettes(spot.color)
        _uiState.update {
            it.copy(
                sampleNormX = spot.normX,
                sampleNormY = spot.normY,
                identifiedColor = spot.color,
                recommendedPalettes = palettes
            )
        }
    }

    fun onInspectColorSwatch(argb: Int) {
        val identified = ColorDictionary.identifyColor(argb)
        val palettes = PaletteGenerator.generatePalettes(identified)
        _uiState.update {
            it.copy(
                studioMode = StudioMode.SWATCH_INSPECTOR,
                capturedBitmap = null,
                identifiedColor = identified,
                dominantSpots = emptyList(),
                recommendedPalettes = palettes,
                selectedHarmonyFilter = null
            )
        }
    }

    fun onLoadFromHistory(entity: CapturedColorEntity) {
        viewModelScope.launch {
            val loadedBitmap = withContext(Dispatchers.IO) {
                entity.thumbnailPath?.let { path ->
                    runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
                }
            }
            val identified = ColorDictionary.identifyColor(entity.argb)
            val palettes = PaletteGenerator.generatePalettes(identified)
            _uiState.update {
                it.copy(
                    studioMode = if (loadedBitmap != null) StudioMode.PHOTO_INSPECTOR else StudioMode.SWATCH_INSPECTOR,
                    capturedBitmap = loadedBitmap,
                    sampleNormX = 0.5f,
                    sampleNormY = 0.5f,
                    identifiedColor = identified,
                    dominantSpots = emptyList(),
                    recommendedPalettes = palettes,
                    selectedHarmonyFilter = null
                )
            }
            if (loadedBitmap != null) {
                dominantJob?.cancel()
                dominantJob = viewModelScope.launch {
                    val spots = withContext(Dispatchers.Default) {
                        ImageColorExtractor.extractDominantColors(loadedBitmap, maxColors = 5)
                    }
                    _uiState.update { state ->
                        if (state.capturedBitmap === loadedBitmap) state.copy(dominantSpots = spots) else state
                    }
                }
            }
        }
    }

    fun onSaveCurrentSwatch() {
        val color = _uiState.value.identifiedColor ?: return
        val bitmap = _uiState.value.capturedBitmap
        viewModelScope.launch {
            repository.saveCapture(color, bitmap)
        }
    }

    fun onDeleteHistoryItem(entity: CapturedColorEntity) {
        viewModelScope.launch {
            repository.deleteCapture(entity)
        }
    }

    fun onClearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun onRetakePhoto() {
        dominantJob?.cancel()
        _uiState.update {
            it.copy(
                studioMode = StudioMode.CAMERA,
                capturedBitmap = null,
                identifiedColor = null,
                dominantSpots = emptyList(),
                recommendedPalettes = emptyList(),
                selectedHarmonyFilter = null
            )
        }
    }

    fun onHarmonyFilterSelected(type: HarmonyType?) {
        _uiState.update {
            it.copy(selectedHarmonyFilter = if (it.selectedHarmonyFilter == type) null else type)
        }
    }

    fun onHistoryFamilyFilterSelected(family: ColorFamily?) {
        _uiState.update {
            it.copy(selectedHistoryFamily = if (it.selectedHistoryFamily == family) null else family)
        }
    }
}
