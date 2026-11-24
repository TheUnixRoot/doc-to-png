package com.antigravity.doctopng.util

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.text.style.TextAlign
import com.antigravity.doctopng.model.*
import com.caverock.androidsvg.SVG
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object ImageExporter {

    fun renderToBitmap(
        state: EditorUiState,
        scale: Int = state.exportScale,
        forceTransparent: Boolean = state.transparentMode
    ): Bitmap {
        val baseW = state.currentRatio.baseWidth
        val baseH = state.currentRatio.baseHeight
        val targetW = baseW * scale
        val targetH = baseH * scale

        // Create transparent 32-bit ARGB bitmap
        val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Draw Background Photo (if not transparent mode and photo exists)
        if (!forceTransparent && state.bgPhotoBitmap != null) {
            drawCenterCropImage(canvas, state.bgPhotoBitmap, targetW, targetH)
        } else {
            // Guarantee pure alpha channel
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        }

        // 2. Draw Layers in order
        state.layers.forEach { layer ->
            if (!layer.isVisible) return@forEach
            canvas.save()
            when (layer) {
                is TextLayer -> drawTextLayer(canvas, layer, targetW, targetH, scale)
                is SvgLayer -> drawSvgLayer(canvas, layer, targetW, targetH, scale)
                is ImageLayer -> drawImageLayer(canvas, layer, targetW, targetH, scale)
            }
            canvas.restore()
        }

        return bitmap
    }

    private fun drawCenterCropImage(canvas: Canvas, src: Bitmap, destW: Int, destH: Int) {
        val srcW = src.width.toFloat()
        val srcH = src.height.toFloat()
        val srcAspect = srcW / srcH
        val destAspect = destW.toFloat() / destH.toFloat()

        val srcRect: Rect
        if (srcAspect > destAspect) {
            val cropW = srcH * destAspect
            val left = ((srcW - cropW) / 2f).toInt()
            srcRect = Rect(left, 0, (left + cropW).toInt(), srcH.toInt())
        } else {
            val cropH = srcW / destAspect
            val top = ((srcH - cropH) / 2f).toInt()
            srcRect = Rect(0, top, srcW.toInt(), (top + cropH).toInt())
        }

        val destRect = Rect(0, 0, destW, destH)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(src, srcRect, destRect, paint)
    }

    private fun drawTextLayer(
        canvas: Canvas,
        layer: TextLayer,
        canvasW: Int,
        canvasH: Int,
        scale: Int
    ) {
        val cx = layer.xNorm * canvasW
        val cy = layer.yNorm * canvasH
        val textSizePx = layer.fontSizeSp * 2.5f * scale
        val lineHeightPx = textSizePx * layer.lineHeightMultiplier

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = textSizePx
            color = layer.textColorArgb.toInt()
            alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
            typeface = if (layer.customTypeface != null) {
                layer.customTypeface
            } else {
                var style = Typeface.NORMAL
                if (layer.isBold && layer.isItalic) style = Typeface.BOLD_ITALIC
                else if (layer.isBold) style = Typeface.BOLD
                else if (layer.isItalic) style = Typeface.ITALIC
                Typeface.create(Typeface.DEFAULT, style)
            }
            letterSpacing = layer.letterSpacingEm
        }

        canvas.translate(cx, cy)
        if (layer.rotationDeg != 0f) {
            canvas.rotate(layer.rotationDeg)
        }

        val rawText = if (layer.isUppercase) layer.text.uppercase() else layer.text
        val lines = rawText.split("\n")
        val totalBlockHeight = lines.size * lineHeightPx

        // Calculate max line width for bounding box and alignment
        var maxLineW = 0f
        lines.forEach { line ->
            val w = textPaint.measureText(line)
            if (w > maxLineW) maxLineW = w
        }
        maxLineW = maxLineW.coerceAtLeast(10f)

        // Optional background box / pill
        if (layer.bgBoxEnabled) {
            val pad = layer.bgBoxPadding * scale
            val boxW = maxLineW + pad * 2
            val boxH = totalBlockHeight + pad * 2
            val radius = layer.bgBoxCornerRadius * scale

            val boxLeft = -boxW / 2f
            val boxTop = -(totalBlockHeight / 2f) - pad

            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = layer.bgBoxColorArgb.toInt()
                alpha = (layer.bgBoxAlpha * layer.opacity * 255).toInt().coerceIn(0, 255)
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(
                RectF(boxLeft, boxTop, boxLeft + boxW, boxTop + boxH),
                radius,
                radius,
                bgPaint
            )
        }

        val fontMetrics = textPaint.fontMetrics
        val textBaselineOffset = (fontMetrics.descent + fontMetrics.ascent) / 2f
        val startY = -(totalBlockHeight / 2f) + (lineHeightPx / 2f) - textBaselineOffset

        lines.forEachIndexed { i, line ->
            val y = startY + (i * lineHeightPx)
            val isLastLine = (i == lines.size - 1)

            // Draw Shadow
            if (layer.shadowEnabled) {
                val shadowPaint = Paint(textPaint).apply {
                    color = layer.shadowColorArgb.toInt()
                    setShadowLayer(
                        layer.shadowRadius * scale,
                        layer.shadowDx * scale,
                        layer.shadowDy * scale,
                        layer.shadowColorArgb.toInt()
                    )
                }
                drawAlignedLine(canvas, line, y, isLastLine, lines.size, maxLineW, layer.textAlign, shadowPaint)
            }

            // Draw Stroke / Outline
            if (layer.strokeEnabled && layer.strokeWidthPx > 0) {
                val strokePaint = Paint(textPaint).apply {
                    style = Paint.Style.STROKE
                    color = layer.strokeColorArgb.toInt()
                    strokeWidth = layer.strokeWidthPx * scale
                    strokeJoin = Paint.Join.ROUND
                }
                drawAlignedLine(canvas, line, y, isLastLine, lines.size, maxLineW, layer.textAlign, strokePaint)
            }

            // Draw Main Text Fill
            drawAlignedLine(canvas, line, y, isLastLine, lines.size, maxLineW, layer.textAlign, textPaint)
        }
    }

    private fun drawAlignedLine(
        canvas: Canvas,
        line: String,
        y: Float,
        isLastLine: Boolean,
        totalLines: Int,
        maxLineW: Float,
        alignment: TextAlign,
        paint: Paint
    ) {
        when (alignment) {
            TextAlign.Left -> {
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText(line, -maxLineW / 2f, y, paint)
            }
            TextAlign.Right -> {
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText(line, maxLineW / 2f, y, paint)
            }
            TextAlign.Justify -> {
                val words = line.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
                // If line has multiple words and is either the only line or not the last line of a paragraph
                if (words.size > 1 && (!isLastLine || totalLines == 1)) {
                    paint.textAlign = Paint.Align.LEFT
                    val wordsTotalW = words.sumOf { paint.measureText(it).toDouble() }.toFloat()
                    val totalSpaceToDistribute = maxLineW - wordsTotalW
                    val spaceBetweenWords = (totalSpaceToDistribute / (words.size - 1)).coerceAtLeast(0f)

                    var curX = -maxLineW / 2f
                    words.forEach { word ->
                        canvas.drawText(word, curX, y, paint)
                        curX += paint.measureText(word) + spaceBetweenWords
                    }
                } else {
                    // Last line or single word is standard left aligned within the justified block
                    paint.textAlign = Paint.Align.LEFT
                    canvas.drawText(line, -maxLineW / 2f, y, paint)
                }
            }
            else -> { // TextAlign.Center
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText(line, 0f, y, paint)
            }
        }
    }

    private fun drawSvgLayer(
        canvas: Canvas,
        layer: SvgLayer,
        canvasW: Int,
        canvasH: Int,
        scale: Int
    ) {
        val cx = layer.xNorm * canvasW
        val cy = layer.yNorm * canvasH

        val svg = try {
            SVG.getFromString(layer.rawSvg)
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        val baseSvgW = if (svg.documentWidth > 0) svg.documentWidth else 200f
        val baseSvgH = if (svg.documentHeight > 0) svg.documentHeight else 200f
        val renderW = baseSvgW * layer.scale * scale
        val renderH = baseSvgH * layer.scale * scale

        canvas.translate(cx, cy)
        if (layer.rotationDeg != 0f) {
            canvas.rotate(layer.rotationDeg)
        }

        // Draw SVG into offscreen picture/canvas
        val picture = svg.renderToPicture(renderW.toInt().coerceAtLeast(1), renderH.toInt().coerceAtLeast(1))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
            if (layer.tintEnabled) {
                colorFilter = PorterDuffColorFilter(layer.tintColorArgb.toInt(), PorterDuff.Mode.SRC_IN)
            }
        }

        canvas.saveLayer(RectF(-renderW / 2f, -renderH / 2f, renderW / 2f, renderH / 2f), paint)
        canvas.translate(-renderW / 2f, -renderH / 2f)
        canvas.drawPicture(picture)
        canvas.restore()
    }

    private fun drawImageLayer(
        canvas: Canvas,
        layer: ImageLayer,
        canvasW: Int,
        canvasH: Int,
        scale: Int
    ) {
        val cx = layer.xNorm * canvasW
        val cy = layer.yNorm * canvasH
        val srcW = layer.bitmap.width.toFloat()
        val srcH = layer.bitmap.height.toFloat()

        // Base initial size: 35% of min canvas dimension scaled
        val maxBaseDim = Math.min(canvasW, canvasH) * 0.35f
        val aspect = srcW / srcH.coerceAtLeast(1f)
        val drawW = (if (aspect >= 1f) maxBaseDim else maxBaseDim * aspect) * layer.scale
        val drawH = (if (aspect >= 1f) maxBaseDim / aspect else maxBaseDim) * layer.scale

        canvas.translate(cx, cy)
        if (layer.rotationDeg != 0f) {
            canvas.rotate(layer.rotationDeg)
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
        }

        val destRect = RectF(-drawW / 2f, -drawH / 2f, drawW / 2f, drawH / 2f)
        canvas.drawBitmap(layer.bitmap, null, destRect, paint)
    }

    fun savePngToDevice(
        context: Context,
        bitmap: Bitmap,
        ratioName: String,
        isTransparent: Boolean
    ): Pair<Uri?, String> {
        val timeStamp = System.currentTimeMillis()
        val ratioClean = ratioName.replace(":", "x")
        val typeDesc = if (isTransparent) "transparente" else "foto"
        val fileName = "DocToPNG_${ratioClean}_${typeDesc}_$timeStamp.png"

        var outputStream: OutputStream? = null
        var resultUri: Uri? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/DocToPNG"
                    )
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (imageUri != null) {
                    outputStream = resolver.openOutputStream(imageUri)
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream!!)
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(imageUri, contentValues, null, null)
                    resultUri = imageUri
                }
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val appDir = File(picturesDir, "DocToPNG")
                if (!appDir.exists()) appDir.mkdirs()
                val file = File(appDir, fileName)
                outputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                resultUri = Uri.fromFile(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            outputStream?.close()
        }

        val details = "${bitmap.width} × ${bitmap.height} px • Formato $ratioName • ${if (isTransparent) "Transparente (Canal Alfa)" else "Fondo Sólido"}"
        return Pair(resultUri, details)
    }
}
