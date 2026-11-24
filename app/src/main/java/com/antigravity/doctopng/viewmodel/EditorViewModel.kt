package com.antigravity.doctopng.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.doctopng.model.*
import com.antigravity.doctopng.util.ImageExporter
import com.antigravity.doctopng.util.SvgPresets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class EditorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private fun createInitialState(): EditorUiState {
        val initialText = TextLayer(
            id = "txt_" + UUID.randomUUID().toString().take(8),
            title = "Texto Principal",
            text = "FOTOGRAFÍA\nURBANA",
            xNorm = 0.5f,
            yNorm = 0.42f,
            fontSizeSp = 48f,
            isUppercase = true
        )

        val initialSvg = SvgLayer(
            id = "svg_" + UUID.randomUUID().toString().take(8),
            title = "Sello Fotográfico",
            rawSvg = SvgPresets.list.first().svgContent,
            xNorm = 0.5f,
            yNorm = 0.72f,
            scale = 1.0f,
            tintEnabled = false,
            tintColorArgb = 0xFF38BDF8
        )

        return EditorUiState(
            currentRatio = AspectRatio.RATIO_3_4,
            layers = listOf(initialText, initialSvg),
            activeLayerId = initialText.id,
            transparentMode = true,
            showGuides = true,
            exportScale = 2
        )
    }

    fun setAspectRatio(ratio: AspectRatio) {
        _uiState.update { it.copy(currentRatio = ratio) }
    }

    fun setTransparentMode(enabled: Boolean) {
        _uiState.update { it.copy(transparentMode = enabled) }
    }

    fun setShowGuides(enabled: Boolean) {
        _uiState.update { it.copy(showGuides = enabled) }
    }

    fun setExportScale(scale: Int) {
        _uiState.update { it.copy(exportScale = scale) }
    }

    fun selectLayer(id: String) {
        _uiState.update { it.copy(activeLayerId = id) }
    }

    fun updateLayerPosition(id: String, dxNorm: Float, dyNorm: Float) {
        _uiState.update { state ->
            val updated = state.layers.map { layer ->
                if (layer.id == id) {
                    val newX = (layer.xNorm + dxNorm).coerceIn(0.05f, 0.95f)
                    val newY = (layer.yNorm + dyNorm).coerceIn(0.05f, 0.95f)
                    when (layer) {
                        is TextLayer -> layer.copy(xNorm = newX, yNorm = newY)
                        is SvgLayer -> layer.copy(xNorm = newX, yNorm = newY)
                        is ImageLayer -> layer.copy(xNorm = newX, yNorm = newY)
                    }
                } else {
                    layer
                }
            }
            state.copy(layers = updated)
        }
    }

    fun updateTextLayer(id: String, transform: (TextLayer) -> TextLayer) {
        _uiState.update { state ->
            val updated = state.layers.map { layer ->
                if (layer.id == id && layer is TextLayer) {
                    transform(layer)
                } else {
                    layer
                }
            }
            state.copy(layers = updated)
        }
    }

    fun updateSvgLayer(id: String, transform: (SvgLayer) -> SvgLayer) {
        _uiState.update { state ->
            val updated = state.layers.map { layer ->
                if (layer.id == id && layer is SvgLayer) {
                    transform(layer)
                } else {
                    layer
                }
            }
            state.copy(layers = updated)
        }
    }

    fun updateImageLayer(id: String, transform: (ImageLayer) -> ImageLayer) {
        _uiState.update { state ->
            val updated = state.layers.map { layer ->
                if (layer.id == id && layer is ImageLayer) {
                    transform(layer)
                } else {
                    layer
                }
            }
            state.copy(layers = updated)
        }
    }

    fun addTextLayer() {
        val newText = TextLayer(
            id = "txt_" + UUID.randomUUID().toString().take(8),
            title = "Texto ${uiState.value.layers.count { it is TextLayer } + 1}",
            text = "NUEVO TEXTO",
            xNorm = 0.5f,
            yNorm = 0.55f,
            fontSizeSp = 40f
        )
        _uiState.update { state ->
            state.copy(
                layers = state.layers + newText,
                activeLayerId = newText.id
            )
        }
    }

    fun addSvgPreset(preset: SvgPresets.Preset) {
        val newSvg = SvgLayer(
            id = "svg_" + UUID.randomUUID().toString().take(8),
            title = preset.title,
            rawSvg = preset.svgContent,
            xNorm = 0.5f,
            yNorm = 0.5f,
            scale = 1.0f
        )
        _uiState.update { state ->
            state.copy(
                layers = state.layers + newSvg,
                activeLayerId = newSvg.id
            )
        }
    }

    fun deleteLayer(id: String) {
        _uiState.update { state ->
            if (state.layers.size <= 1) return@update state
            val filtered = state.layers.filter { it.id != id }
            val nextActiveId = if (state.activeLayerId == id) {
                filtered.lastOrNull()?.id
            } else {
                state.activeLayerId
            }
            state.copy(layers = filtered, activeLayerId = nextActiveId)
        }
    }

    fun moveLayerUp(id: String) {
        _uiState.update { state ->
            val idx = state.layers.indexOfFirst { it.id == id }
            if (idx != -1 && idx < state.layers.size - 1) {
                val list = state.layers.toMutableList()
                val item = list.removeAt(idx)
                list.add(idx + 1, item)
                state.copy(layers = list)
            } else state
        }
    }

    fun moveLayerDown(id: String) {
        _uiState.update { state ->
            val idx = state.layers.indexOfFirst { it.id == id }
            if (idx > 0) {
                val list = state.layers.toMutableList()
                val item = list.removeAt(idx)
                list.add(idx - 1, item)
                state.copy(layers = list)
            } else state
        }
    }

    fun importTtfFont(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var fileName = "custom_font.ttf"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex != -1) {
                        fileName = cursor.getString(nameIndex)
                    }
                }

                // Copy font file to cache
                val tempFile = File(context.cacheDir, fileName)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }

                val typeface = Typeface.createFromFile(tempFile)
                val cleanName = fileName.substringBeforeLast(".")
                val customFont = CustomFont(
                    id = UUID.randomUUID().toString(),
                    name = cleanName,
                    typeface = typeface,
                    uri = uri
                )

                withContext(Dispatchers.Main) {
                    _uiState.update { state ->
                        val updatedFonts = state.customFonts + customFont
                        // If current active layer is TextLayer, apply this font immediately!
                        val updatedLayers = state.layers.map { layer ->
                            if (layer.id == state.activeLayerId && layer is TextLayer) {
                                layer.copy(fontFamilyName = cleanName, customTypeface = typeface)
                            } else {
                                layer
                            }
                        }
                        state.copy(customFonts = updatedFonts, layers = updatedLayers)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun importSvgFile(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var fileName = "Grafico.svg"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex != -1) {
                        fileName = cursor.getString(nameIndex)
                    }
                }

                val svgString = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                    it.readText()
                } ?: return@launch

                if (svgString.contains("<svg")) {
                    val cleanTitle = fileName.substringBeforeLast(".")
                    val newSvg = SvgLayer(
                        id = "svg_" + UUID.randomUUID().toString().take(8),
                        title = cleanTitle,
                        rawSvg = svgString,
                        xNorm = 0.5f,
                        yNorm = 0.5f
                    )
                    withContext(Dispatchers.Main) {
                        _uiState.update { state ->
                            state.copy(
                                layers = state.layers + newSvg,
                                activeLayerId = newSvg.id
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun importPngImage(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var fileName = "Sticker.png"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex != -1) {
                        fileName = cursor.getString(nameIndex)
                    }
                }

                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                } ?: return@launch

                val cleanTitle = fileName.substringBeforeLast(".")
                val newImageLayer = ImageLayer(
                    id = "img_" + UUID.randomUUID().toString().take(8),
                    title = cleanTitle,
                    bitmap = bitmap,
                    uri = uri,
                    xNorm = 0.5f,
                    yNorm = 0.5f,
                    scale = 1.0f
                )

                withContext(Dispatchers.Main) {
                    _uiState.update { state ->
                        state.copy(
                            layers = state.layers + newImageLayer,
                            activeLayerId = newImageLayer.id
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setBackgroundPhoto(bitmap: Bitmap?) {
        _uiState.update {
            it.copy(
                bgPhotoBitmap = bitmap,
                transparentMode = if (bitmap != null) false else it.transparentMode
            )
        }
    }

    fun removeBackgroundPhoto() {
        _uiState.update {
            it.copy(
                bgPhotoBitmap = null,
                transparentMode = true
            )
        }
    }

    fun exportPng(context: Context, onComplete: (Uri?, String) -> Unit) {
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { it.copy(isExporting = true) }
            val state = _uiState.value
            val bitmap = ImageExporter.renderToBitmap(state, state.exportScale, state.transparentMode)
            val (uri, details) = ImageExporter.savePngToDevice(
                context,
                bitmap,
                state.currentRatio.displayName,
                state.transparentMode
            )

            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        lastExportedUri = uri,
                        lastExportedFileDetails = details
                    )
                }
                onComplete(uri, details)
            }
        }
    }

    fun clearExportResult() {
        _uiState.update { it.copy(lastExportedUri = null, lastExportedFileDetails = null) }
    }
}
