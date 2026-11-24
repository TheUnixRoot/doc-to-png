package com.antigravity.doctopng.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.antigravity.doctopng.model.*
import com.antigravity.doctopng.util.SvgPresets
import com.antigravity.doctopng.viewmodel.EditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onRequestCameraPhoto: () -> Unit,
    onRequestGalleryPhoto: () -> Unit,
    onRequestTtfFont: () -> Unit,
    onRequestSvgFile: () -> Unit,
    onRequestPngImage: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    var showCameraSourceDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "DocToPNG",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF38BDF8)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        RatioSelector(
                            currentRatio = uiState.currentRatio,
                            onRatioSelected = { viewModel.setAspectRatio(it) }
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showCameraSourceDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Cámara",
                            tint = if (uiState.bgPhotoBitmap != null) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                        )
                    }
                    Button(
                        onClick = {
                            viewModel.exportPng(context) { _, _ -> }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Exportar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF181B24),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF0F1117)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Canvas Area (weight 1)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                CanvasPreview(
                    uiState = uiState,
                    onMoveLayer = { id, dx, dy -> viewModel.updateLayerPosition(id, dx, dy) },
                    onSelectLayer = { id -> viewModel.selectLayer(id) },
                    modifier = Modifier.fillMaxSize()
                )

                if (uiState.isExporting) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x99000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF38BDF8))
                    }
                }
            }

            // Bottom Control Center Tabs
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(315.dp),
                color = Color(0xFF181B24),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Navigation Tabs
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color(0xFF1E2230),
                        contentColor = Color(0xFF38BDF8)
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Texto", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                            icon = { Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Gráficos", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                            icon = { Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Capas (${uiState.layers.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                            icon = { Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            text = { Text("Ajustes", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                            icon = { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }

                    // Tab Content
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        when (selectedTab) {
                            0 -> TextControlsTab(
                                activeLayer = uiState.activeLayer as? TextLayer,
                                customFonts = uiState.customFonts,
                                onUpdateText = { transform ->
                                    uiState.activeLayerId?.let { id ->
                                        viewModel.updateTextLayer(id, transform)
                                    }
                                },
                                onAddText = { viewModel.addTextLayer() },
                                onRequestTtfFont = onRequestTtfFont
                            )
                            1 -> GraphicsControlsTab(
                                activeLayer = uiState.activeLayer,
                                onUpdateSvg = { transform ->
                                    uiState.activeLayerId?.let { id ->
                                        viewModel.updateSvgLayer(id, transform)
                                    }
                                },
                                onUpdateImage = { transform ->
                                    uiState.activeLayerId?.let { id ->
                                        viewModel.updateImageLayer(id, transform)
                                    }
                                },
                                onAddPreset = { preset -> viewModel.addSvgPreset(preset) },
                                onRequestSvgFile = onRequestSvgFile,
                                onRequestPngImage = onRequestPngImage,
                                onDeleteActiveLayer = {
                                    uiState.activeLayerId?.let { id -> viewModel.deleteLayer(id) }
                                }
                            )
                            2 -> LayersTab(
                                layers = uiState.layers,
                                activeId = uiState.activeLayerId,
                                onSelect = { viewModel.selectLayer(it) },
                                onMoveUp = { viewModel.moveLayerUp(it) },
                                onMoveDown = { viewModel.moveLayerDown(it) },
                                onDelete = { viewModel.deleteLayer(it) },
                                onAddText = { viewModel.addTextLayer() }
                            )
                            3 -> SettingsTab(
                                uiState = uiState,
                                onSetTransparent = { viewModel.setTransparentMode(it) },
                                onSetGuides = { viewModel.setShowGuides(it) },
                                onSetExportScale = { viewModel.setExportScale(it) },
                                onRemovePhoto = { viewModel.removeBackgroundPhoto() }
                            )
                        }
                    }
                }
            }
        }
    }

    // Camera Source Choice Dialog
    if (showCameraSourceDialog) {
        AlertDialog(
            onDismissRequest = { showCameraSourceDialog = false },
            title = { Text("Cámara / Fondo de Foto") },
            text = { Text("Puedes tomar una foto directamente con la cámara o seleccionar una de tu galería para encuadrar tu texto y SVG.") },
            confirmButton = {
                TextButton(onClick = {
                    showCameraSourceDialog = false
                    onRequestCameraPhoto()
                }) {
                    Text("Tomar Foto con Cámara")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCameraSourceDialog = false
                    onRequestGalleryPhoto()
                }) {
                    Text("Elegir de Galería")
                }
            }
        )
    }

    // Export Success Dialog
    uiState.lastExportedUri?.let { uri ->
        Dialog(onDismissRequest = { viewModel.clearExportResult() }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF181B24),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "¡PNG Guardado con Éxito!",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F1117)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Preview",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.lastExportedFileDetails ?: "Guardado en Pictures/DocToPNG",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/png"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Compartir PNG"))
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Compartir")
                        }
                        OutlinedButton(
                            onClick = { viewModel.clearExportResult() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cerrar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TextControlsTab(
    activeLayer: TextLayer?,
    customFonts: List<CustomFont>,
    onUpdateText: ((TextLayer) -> TextLayer) -> Unit,
    onAddText: () -> Unit,
    onRequestTtfFont: () -> Unit
) {
    if (activeLayer == null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("No hay texto seleccionado", color = Color(0xFF94A3B8), fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onAddText) {
                Text("+ Añadir Nuevo Texto")
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Text Input
        item {
            OutlinedTextField(
                value = activeLayer.text,
                onValueChange = { newText -> onUpdateText { it.copy(text = newText) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Texto a dibujar") },
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF6366F1),
                    unfocusedBorderColor = Color(0xFF2E3549)
                )
            )
        }

        // Alignment (Izq, Centro, Der, Justificado) & Format Controls
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E2230), RoundedCornerShape(10.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Alignment group
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    AlignmentButton(
                        icon = Icons.Default.FormatAlignLeft,
                        selected = activeLayer.textAlign == TextAlign.Left,
                        title = "Izquierda",
                        onClick = { onUpdateText { it.copy(textAlign = TextAlign.Left) } }
                    )
                    AlignmentButton(
                        icon = Icons.Default.FormatAlignCenter,
                        selected = activeLayer.textAlign == TextAlign.Center,
                        title = "Centrado",
                        onClick = { onUpdateText { it.copy(textAlign = TextAlign.Center) } }
                    )
                    AlignmentButton(
                        icon = Icons.Default.FormatAlignRight,
                        selected = activeLayer.textAlign == TextAlign.Right,
                        title = "Derecha",
                        onClick = { onUpdateText { it.copy(textAlign = TextAlign.Right) } }
                    )
                    AlignmentButton(
                        icon = Icons.Default.FormatAlignJustify,
                        selected = activeLayer.textAlign == TextAlign.Justify,
                        title = "Justificado",
                        onClick = { onUpdateText { it.copy(textAlign = TextAlign.Justify) } }
                    )
                }

                // Divider
                Box(
                    modifier = Modifier
                        .height(24.dp)
                        .width(1.dp)
                        .background(Color(0xFF2E3549))
                )

                // Format toggles (B, I, aA)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FormatToggleButton(
                        text = "B",
                        isBold = true,
                        selected = activeLayer.isBold,
                        onClick = { onUpdateText { it.copy(isBold = !it.isBold) } }
                    )
                    FormatToggleButton(
                        text = "I",
                        isItalic = true,
                        selected = activeLayer.isItalic,
                        onClick = { onUpdateText { it.copy(isItalic = !it.isItalic) } }
                    )
                    FormatToggleButton(
                        text = "aA",
                        selected = activeLayer.isUppercase,
                        onClick = { onUpdateText { it.copy(isUppercase = !it.isUppercase) } }
                    )
                }
            }
        }

        // TTF Font Selection Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Fuente: ${activeLayer.fontFamilyName}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Button(
                    onClick = onRequestTtfFont,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262C3E)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF38BDF8))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Subir .TTF", fontSize = 11.sp, color = Color(0xFF38BDF8))
                }
            }

            if (customFonts.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(customFonts) { font ->
                        val isSelected = activeLayer.fontFamilyName == font.name
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF6366F1) else Color(0xFF1E2230))
                                .clickable {
                                    onUpdateText { it.copy(fontFamilyName = font.name, customTypeface = font.typeface) }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(font.name, fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        // Font Size (Supports Tiny/Micro sizes 4sp - 180sp) + Quick Chips
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tamaño: ${activeLayer.fontSizeSp.toInt()} sp",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF38BDF8)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = {
                                val next = (activeLayer.fontSizeSp - 1f).coerceAtLeast(4f)
                                onUpdateText { it.copy(fontSizeSp = next) }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "-1", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = {
                                val next = (activeLayer.fontSizeSp + 1f).coerceAtMost(180f)
                                onUpdateText { it.copy(fontSizeSp = next) }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "+1", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Slider(
                    value = activeLayer.fontSizeSp,
                    onValueChange = { sz -> onUpdateText { it.copy(fontSizeSp = sz) } },
                    valueRange = 4f..180f,
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Size Presets (Micro to Display)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    val presets = listOf(4f, 6f, 8f, 10f, 14f, 20f, 32f, 48f, 72f, 96f)
                    items(presets) { presetSz ->
                        val isSel = activeLayer.fontSizeSp.toInt() == presetSz.toInt()
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) Color(0xFF6366F1) else Color(0xFF1E2230))
                                .border(1.dp, if (isSel) Color(0xFF38BDF8) else Color(0xFF2E3549), RoundedCornerShape(6.dp))
                                .clickable { onUpdateText { it.copy(fontSizeSp = presetSz) } }
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text("${presetSz.toInt()}sp", fontSize = 10.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        // Sliders: Spacing & Line Height
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Espaciado (${(activeLayer.letterSpacingEm * 100).toInt()})", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    Slider(
                        value = activeLayer.letterSpacingEm,
                        onValueChange = { sp -> onUpdateText { it.copy(letterSpacingEm = sp) } },
                        valueRange = -0.05f..0.3f
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Interlineado (${String.format("%.2f", activeLayer.lineHeightMultiplier)})", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    Slider(
                        value = activeLayer.lineHeightMultiplier,
                        onValueChange = { lh -> onUpdateText { it.copy(lineHeightMultiplier = lh) } },
                        valueRange = 0.7f..2.5f
                    )
                }
            }
        }

        // Color Palette
        item {
            Text("Color del Texto", fontSize = 11.sp, color = Color(0xFF94A3B8))
            ColorPaletteRow(
                selectedColorArgb = activeLayer.textColorArgb,
                onColorSelected = { col -> onUpdateText { it.copy(textColorArgb = col) } }
            )
        }

        // Toggles: Outline, Shadow, Pill Box
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = activeLayer.strokeEnabled,
                        onCheckedChange = { chk -> onUpdateText { it.copy(strokeEnabled = chk) } }
                    )
                    Text("Borde", fontSize = 11.sp, color = Color.White)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = activeLayer.shadowEnabled,
                        onCheckedChange = { chk -> onUpdateText { it.copy(shadowEnabled = chk) } }
                    )
                    Text("Sombra", fontSize = 11.sp, color = Color.White)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = activeLayer.bgBoxEnabled,
                        onCheckedChange = { chk -> onUpdateText { it.copy(bgBoxEnabled = chk) } }
                    )
                    Text("Caja", fontSize = 11.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun AlignmentButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    title: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0xFF6366F1) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (selected) Color.White else Color(0xFF94A3B8),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun FormatToggleButton(
    text: String,
    selected: Boolean,
    isBold: Boolean = false,
    isItalic: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0xFF6366F1) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontWeight = if (isBold) FontWeight.Black else FontWeight.Medium,
            fontStyle = if (isItalic) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
            color = if (selected) Color.White else Color(0xFF94A3B8),
            fontSize = 13.sp
        )
    }
}

@Composable
fun GraphicsControlsTab(
    activeLayer: CanvasLayer?,
    onUpdateSvg: ((SvgLayer) -> SvgLayer) -> Unit,
    onUpdateImage: ((ImageLayer) -> ImageLayer) -> Unit,
    onAddPreset: (SvgPresets.Preset) -> Unit,
    onRequestSvgFile: () -> Unit,
    onRequestPngImage: () -> Unit,
    onDeleteActiveLayer: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Upload Buttons (PNG / SVG)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRequestPngImage,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262C3E)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Subir PNG", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onRequestSvgFile,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262C3E)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Subir SVG", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Presets Library
        item {
            Text("Biblioteca de Sellos y Marcos Vectoriales:", fontSize = 11.sp, color = Color(0xFF94A3B8))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SvgPresets.list) { preset ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E2230))
                            .border(1.dp, Color(0xFF2E3549), RoundedCornerShape(8.dp))
                            .clickable { onAddPreset(preset) }
                            .padding(8.dp)
                    ) {
                        Text(preset.title, fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }

        // Active SVG Controls
        if (activeLayer is SvgLayer) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Ajustes del SVG (${activeLayer.title})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                    IconButton(onClick = onDeleteActiveLayer, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Escala (${(activeLayer.scale * 100).toInt()}%)", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        Slider(
                            value = activeLayer.scale,
                            onValueChange = { s -> onUpdateSvg { it.copy(scale = s) } },
                            valueRange = 0.2f..3.0f
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Rotación (${activeLayer.rotationDeg.toInt()}°)", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        Slider(
                            value = activeLayer.rotationDeg,
                            onValueChange = { r -> onUpdateSvg { it.copy(rotationDeg = r) } },
                            valueRange = -180f..180f
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (activeLayer.tintEnabled) "Teñir Color Monocromático" else "Color Original del SVG (Sin teñir)",
                        fontSize = 11.sp,
                        fontWeight = if (activeLayer.tintEnabled) FontWeight.Bold else FontWeight.Normal,
                        color = if (activeLayer.tintEnabled) Color(0xFF38BDF8) else Color.White
                    )
                    Switch(
                        checked = activeLayer.tintEnabled,
                        onCheckedChange = { en -> onUpdateSvg { it.copy(tintEnabled = en) } }
                    )
                }

                if (activeLayer.tintEnabled) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Selecciona el color del tinte:", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    ColorPaletteRow(
                        selectedColorArgb = activeLayer.tintColorArgb,
                        onColorSelected = { col -> onUpdateSvg { it.copy(tintEnabled = true, tintColorArgb = col) } }
                    )
                }
            }
        }

        // Active PNG Image Controls
        if (activeLayer is ImageLayer) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Ajustes Imagen PNG (${activeLayer.title})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                    IconButton(onClick = onDeleteActiveLayer, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Escala (${(activeLayer.scale * 100).toInt()}%)", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        Slider(
                            value = activeLayer.scale,
                            onValueChange = { s -> onUpdateImage { it.copy(scale = s) } },
                            valueRange = 0.1f..3.0f
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Rotación (${activeLayer.rotationDeg.toInt()}°)", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        Slider(
                            value = activeLayer.rotationDeg,
                            onValueChange = { r -> onUpdateImage { it.copy(rotationDeg = r) } },
                            valueRange = -180f..180f
                        )
                    }
                }
            }

            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Opacidad (${(activeLayer.opacity * 100).toInt()}%)", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    Slider(
                        value = activeLayer.opacity,
                        onValueChange = { op -> onUpdateImage { it.copy(opacity = op) } },
                        valueRange = 0.05f..1.0f
                    )
                }
            }
        }
    }
}

@Composable
fun LayersTab(
    layers: List<CanvasLayer>,
    activeId: String?,
    onSelect: (String) -> Unit,
    onMoveUp: (String) -> Unit,
    onMoveDown: (String) -> Unit,
    onDelete: (String) -> Unit,
    onAddText: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Capas del Diseño", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            TextButton(onClick = onAddText) {
                Text("+ Nuevo Texto", fontSize = 11.sp, color = Color(0xFF38BDF8))
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(layers.reversed()) { layer ->
                val isSelected = layer.id == activeId
                val typeTag = when (layer) {
                    is TextLayer -> "TXT"
                    is SvgLayer -> "SVG"
                    is ImageLayer -> "PNG"
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(layer.id) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) Color(0xFF262C3E) else Color(0xFF1E2230),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6366F1)) else null
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF0F1117))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(typeTag, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (layer is TextLayer) layer.text.take(16) else layer.title,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                        Row {
                            IconButton(onClick = { onMoveUp(layer.id) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { onMoveDown(layer.id) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { onDelete(layer.id) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsTab(
    uiState: EditorUiState,
    onSetTransparent: (Boolean) -> Unit,
    onSetGuides: (Boolean) -> Unit,
    onSetExportScale: (Int) -> Unit,
    onRemovePhoto: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Modo PNG Transparente (Canal Alfa)", fontSize = 12.sp, color = Color.White)
            Switch(
                checked = uiState.transparentMode,
                onCheckedChange = onSetTransparent
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Guías de Composición (Regla de Tercios)", fontSize = 12.sp, color = Color.White)
            Switch(
                checked = uiState.showGuides,
                onCheckedChange = onSetGuides
            )
        }

        if (uiState.bgPhotoBitmap != null) {
            Button(
                onClick = onRemovePhoto,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Quitar Foto de Fondo (Volver a Transparente)")
            }
        }
    }
}
