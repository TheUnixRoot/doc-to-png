package com.antigravity.doctopng.ui

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.doctopng.model.*
import com.antigravity.doctopng.util.ImageExporter
import com.antigravity.doctopng.util.SvgPresets

@Composable
fun CanvasPreview(
    uiState: EditorUiState,
    onMoveLayer: (String, Float, Float) -> Unit,
    onSelectLayer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Generate bitmap preview dynamically
    val previewBitmap by remember(
        uiState.currentRatio,
        uiState.layers,
        uiState.bgPhotoBitmap,
        uiState.transparentMode
    ) {
        derivedStateOf {
            ImageExporter.renderToBitmap(uiState, scale = 1, forceTransparent = uiState.transparentMode)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Aspect ratio container box
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .aspectRatio(uiState.currentRatio.ratioFloat)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF2E3549), RoundedCornerShape(8.dp))
                .pointerInput(uiState.activeLayerId, uiState.layers) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val activeId = uiState.activeLayerId ?: return@detectDragGestures
                        val dxNorm = dragAmount.x / size.width.toFloat()
                        val dyNorm = dragAmount.y / size.height.toFloat()
                        onMoveLayer(activeId, dxNorm, dyNorm)
                    }
                }
        ) {
            // 1. Checkerboard Pattern for transparent preview
            Canvas(modifier = Modifier.fillMaxSize()) {
                val squareSize = 24f
                val cols = (size.width / squareSize).toInt() + 1
                val rows = (size.height / squareSize).toInt() + 1
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        val isDark = (r + c) % 2 == 0
                        drawRect(
                            color = if (isDark) Color(0xFF1E2430) else Color(0xFF141720),
                            topLeft = Offset(c * squareSize, r * squareSize),
                            size = Size(squareSize, squareSize)
                        )
                    }
                }
            }

            // 2. Rendered Design Bitmap
            previewBitmap.let { bmp ->
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawImage(
                        image = bmp.asImageBitmap(),
                        dstSize = IntSize(size.width.toInt(), size.height.toInt())
                    )
                }
            }

            // 3. Rule of Thirds & Center Guidelines
            if (uiState.showGuides) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Thirds
                    val gridColor = Color(0x33FFFFFF)
                    drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth = 1.dp.toPx())
                    drawLine(gridColor, Offset(w * 2f / 3f, 0f), Offset(w * 2f / 3f, h), strokeWidth = 1.dp.toPx())
                    drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth = 1.dp.toPx())
                    drawLine(gridColor, Offset(0f, h * 2f / 3f), Offset(w, h * 2f / 3f), strokeWidth = 1.dp.toPx())

                    // Center Axes
                    val centerColor = Color(0x4D38BDF8)
                    drawLine(centerColor, Offset(w / 2f, 0f), Offset(w / 2f, h), strokeWidth = 1.5.dp.toPx())
                    drawLine(centerColor, Offset(0f, h / 2f), Offset(w, h / 2f), strokeWidth = 1.5.dp.toPx())
                }
            }

            // 4. Active Layer Selection Highlight Box
            uiState.activeLayer?.let { activeLayer ->
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val boxW = maxWidth
                    val boxH = maxHeight
                    val xPx = activeLayer.xNorm * boxW.value
                    val yPx = activeLayer.yNorm * boxH.value

                    Box(
                        modifier = Modifier
                            .offset { IntOffset((xPx * density - 30.dp.toPx()).toInt(), (yPx * density - 20.dp.toPx()).toInt()) }
                            .size(60.dp, 40.dp)
                            .border(1.5.dp, Color(0xFF38BDF8), RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}

@Composable
fun RatioSelector(
    currentRatio: AspectRatio,
    onRatioSelected: (AspectRatio) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Color(0xFF1E2230), RoundedCornerShape(20.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AspectRatio.entries.forEach { ratio ->
            val isSelected = ratio == currentRatio
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) Color(0xFF6366F1) else Color.Transparent)
                    .clickable { onRatioSelected(ratio) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = ratio.displayName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else Color(0xFF94A3B8)
                )
            }
        }
    }
}

@Composable
fun ColorPaletteRow(
    selectedColorArgb: Long,
    onColorSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val presetColors = listOf(
        0xFFFFFFFF, // Blanco
        0xFF000000, // Negro
        0xFF38BDF8, // Cyan
        0xFF6366F1, // Indigo
        0xFFF43F5E, // Rosa
        0xFF10B981, // Esmeralda
        0xFFF59E0B, // Ámbar
        0xFF8B5CF6  // Violeta
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        presetColors.forEach { colorVal ->
            val isSelected = colorVal == selectedColorArgb
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(colorVal))
                    .border(
                        width = if (isSelected) 2.5.dp else 1.dp,
                        color = if (isSelected) Color(0xFF38BDF8) else Color(0x44FFFFFF),
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(colorVal) }
            )
        }
    }
}
