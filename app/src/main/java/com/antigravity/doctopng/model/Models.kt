package com.antigravity.doctopng.model

import android.graphics.Bitmap
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.ui.text.style.TextAlign

enum class AspectRatio(val displayName: String, val baseWidth: Int, val baseHeight: Int) {
    RATIO_3_4("3:4", 1080, 1440),
    RATIO_1_1("1:1", 1080, 1080),
    RATIO_4_3("4:3", 1440, 1080);

    val ratioFloat: Float
        get() = baseWidth.toFloat() / baseHeight.toFloat()
}

sealed interface CanvasLayer {
    val id: String
    val title: String
    var xNorm: Float       // Normalized relative to center (0.5 = center, 0..1)
    var yNorm: Float
    var rotationDeg: Float
    var opacity: Float
    var isVisible: Boolean
}

data class TextLayer(
    override val id: String,
    override val title: String = "Texto",
    var text: String = "FOTOGRAFÍA\nURBANA",
    override var xNorm: Float = 0.5f,
    override var yNorm: Float = 0.42f,
    var fontSizeSp: Float = 48f,
    var fontFamilyName: String = "Bebas",
    var customTypeface: Typeface? = null,
    var textColorArgb: Long = 0xFFFFFFFF,
    var letterSpacingEm: Float = 0.08f,
    var lineHeightMultiplier: Float = 1.1f,
    override var rotationDeg: Float = 0f,
    var textAlign: TextAlign = TextAlign.Center,
    var isBold: Boolean = false,
    var isItalic: Boolean = false,
    var isUppercase: Boolean = true,
    var strokeEnabled: Boolean = true,
    var strokeColorArgb: Long = 0xFF000000,
    var strokeWidthPx: Float = 4f,
    var shadowEnabled: Boolean = true,
    var shadowColorArgb: Long = 0xFF000000,
    var shadowRadius: Float = 12f,
    var shadowDx: Float = 0f,
    var shadowDy: Float = 4f,
    var bgBoxEnabled: Boolean = false,
    var bgBoxColorArgb: Long = 0xFF000000,
    var bgBoxAlpha: Float = 0.75f,
    var bgBoxPadding: Float = 24f,
    var bgBoxCornerRadius: Float = 16f,
    override var opacity: Float = 1.0f,
    override var isVisible: Boolean = true
) : CanvasLayer

data class SvgLayer(
    override val id: String,
    override val title: String = "Gráfico SVG",
    val rawSvg: String,
    override var xNorm: Float = 0.5f,
    override var yNorm: Float = 0.72f,
    var scale: Float = 1.0f,
    override var rotationDeg: Float = 0f,
    var tintEnabled: Boolean = false,
    var tintColorArgb: Long = 0xFF38BDF8,
    override var opacity: Float = 0.95f,
    override var isVisible: Boolean = true
) : CanvasLayer

data class ImageLayer(
    override val id: String,
    override val title: String = "Imagen PNG",
    val bitmap: Bitmap,
    val uri: Uri? = null,
    override var xNorm: Float = 0.5f,
    override var yNorm: Float = 0.5f,
    var scale: Float = 1.0f,
    override var rotationDeg: Float = 0f,
    override var opacity: Float = 1.0f,
    override var isVisible: Boolean = true
) : CanvasLayer

data class CustomFont(
    val id: String,
    val name: String,
    val typeface: Typeface,
    val uri: Uri? = null
)

data class EditorUiState(
    val currentRatio: AspectRatio = AspectRatio.RATIO_3_4,
    val layers: List<CanvasLayer> = emptyList(),
    val activeLayerId: String? = null,
    val bgPhotoBitmap: Bitmap? = null,
    val transparentMode: Boolean = true,
    val showGuides: Boolean = true,
    val exportScale: Int = 2, // 1x, 2x, 3x
    val customFonts: List<CustomFont> = emptyList(),
    val isExporting: Boolean = false,
    val lastExportedUri: Uri? = null,
    val lastExportedFileDetails: String? = null
) {
    val activeLayer: CanvasLayer?
        get() = layers.find { it.id == activeLayerId }
}
