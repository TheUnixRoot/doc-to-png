package com.antigravity.doctopng

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.core.content.FileProvider
import com.antigravity.doctopng.ui.EditorScreen
import com.antigravity.doctopng.viewmodel.EditorViewModel
import java.io.File

class MainActivity : ComponentActivity() {

    private val viewModel: EditorViewModel by viewModels()
    private var pendingCameraUri: Uri? = null

    // Camera Capture Launcher
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && pendingCameraUri != null) {
            val bitmap = decodeBitmapFromUri(pendingCameraUri!!)
            if (bitmap != null) {
                viewModel.setBackgroundPhoto(bitmap)
            }
        }
    }

    // Gallery Photo Picker Launcher (Background Photo)
    private val pickGalleryPhotoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val bitmap = decodeBitmapFromUri(uri)
            if (bitmap != null) {
                viewModel.setBackgroundPhoto(bitmap)
            }
        }
    }

    // TTF / OTF Font File Picker Launcher
    private val pickTtfFontLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            viewModel.importTtfFont(applicationContext, uri)
        }
    }

    // SVG File Picker Launcher
    private val pickSvgFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            viewModel.importSvgFile(applicationContext, uri)
        }
    }

    // PNG Image / Sticker Layer File Picker Launcher
    private val pickPngImageLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            viewModel.importPngImage(applicationContext, uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF0F1117),
                    surface = Color(0xFF181B24),
                    primary = Color(0xFF6366F1),
                    secondary = Color(0xFF38BDF8)
                )
            ) {
                EditorScreen(
                    viewModel = viewModel,
                    onRequestCameraPhoto = { launchCamera() },
                    onRequestGalleryPhoto = { pickGalleryPhotoLauncher.launch("image/*") },
                    onRequestTtfFont = { pickTtfFontLauncher.launch(arrayOf("*/*")) },
                    onRequestSvgFile = { pickSvgFileLauncher.launch(arrayOf("image/svg+xml", "*/*")) },
                    onRequestPngImage = { pickPngImageLauncher.launch(arrayOf("image/png", "image/*")) }
                )
            }
        }
    }

    private fun launchCamera() {
        try {
            val photoFile = File(cacheDir, "camera_capture_${System.currentTimeMillis()}.jpg")
            val photoUri = FileProvider.getUriForFile(
                this,
                "com.antigravity.doctopng.fileprovider",
                photoFile
            )
            pendingCameraUri = photoUri
            takePictureLauncher.launch(photoUri)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun decodeBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                }
            } else {
                contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
