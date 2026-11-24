/**
 * DocToPNG Studio - Interactive Canvas Typography & SVG Engine
 * Supports custom .TTF font loading, vector SVGs, live camera photo capture,
 * and high-resolution transparent PNG export in 4:3, 1:1, and 3:4.
 */

(function () {
  'use strict';

  // --- Configuration & Constants ---
  const RATIOS = {
    '3:4': { name: '3:4', width: 1080, height: 1440, label: '3:4 (Retrato / Instagram / Móvil)' },
    '1:1': { name: '1:1', width: 1080, height: 1080, label: '1:1 (Cuadrado / Post)' },
    '4:3': { name: '4:3', width: 1440, height: 1080, label: '4:3 (Paisaje / Cámara Clásica)' }
  };

  // Built-in SVG Presets (Vector watermarks, frames, camera seals)
  const SVG_PRESETS = [
    {
      id: 'cam-seal',
      name: 'Sello Cámara',
      svg: `<svg viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">
        <circle cx="50" cy="50" r="46" fill="none" stroke="currentColor" stroke-width="2.5" stroke-dasharray="2 3"/>
        <circle cx="50" cy="50" r="41" fill="none" stroke="currentColor" stroke-width="1.5"/>
        <path d="M35 42h30v26H35z" fill="none" stroke="currentColor" stroke-width="2"/>
        <circle cx="50" cy="55" r="7" fill="none" stroke="currentColor" stroke-width="2"/>
        <path d="M42 37h16l2 5H40z" fill="none" stroke="currentColor" stroke-width="1.5"/>
        <circle cx="60" cy="46" r="1.5" fill="currentColor"/>
        <text x="50" y="28" font-size="6.5" font-weight="bold" text-anchor="middle" fill="currentColor" letter-spacing="1">ORIGINAL SHOT</text>
        <text x="50" y="80" font-size="6.5" font-weight="bold" text-anchor="middle" fill="currentColor" letter-spacing="1.5">LIMITED EDITION</text>
      </svg>`
    },
    {
      id: 'frame-corners',
      name: 'Marco Esquinas',
      svg: `<svg viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">
        <path d="M10 25 V10 H25" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round"/>
        <path d="M75 10 H90 V25" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round"/>
        <path d="M90 75 V90 H75" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round"/>
        <path d="M25 90 H10 V75" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round"/>
        <circle cx="50" cy="50" r="2" fill="currentColor"/>
        <line x1="44" y1="50" x2="56" y2="50" stroke="currentColor" stroke-width="1"/>
        <line x1="50" y1="44" x2="50" y2="56" stroke="currentColor" stroke-width="1"/>
      </svg>`
    },
    {
      id: 'date-badge',
      name: 'Badge Fecha',
      svg: `<svg viewBox="0 0 160 50" xmlns="http://www.w3.org/2000/svg">
        <rect x="2" y="2" width="156" height="46" rx="8" fill="none" stroke="currentColor" stroke-width="2"/>
        <circle cx="22" cy="25" r="10" fill="none" stroke="currentColor" stroke-width="1.5"/>
        <polyline points="22,19 22,25 26,27" fill="none" stroke="currentColor" stroke-width="1.5"/>
        <text x="40" y="24" font-size="10" font-weight="bold" fill="currentColor" letter-spacing="1">RAW • 24MM</text>
        <text x="40" y="37" font-size="9" fill="currentColor" opacity="0.85">ISO 100  f/1.8  1/500s</text>
      </svg>`
    },
    {
      id: 'minimal-star',
      name: 'Estrella Brutalista',
      svg: `<svg viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">
        <polygon points="50,5 62,38 95,50 62,62 50,95 38,62 5,50 38,38" fill="currentColor"/>
      </svg>`
    },
    {
      id: 'aperture',
      name: 'Diafragma Lente',
      svg: `<svg viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">
        <circle cx="50" cy="50" r="46" fill="none" stroke="currentColor" stroke-width="3"/>
        <line x1="50" y1="4" x2="80" y2="70" stroke="currentColor" stroke-width="2"/>
        <line x1="96" y1="50" x2="30" y2="80" stroke="currentColor" stroke-width="2"/>
        <line x1="50" y1="96" x2="20" y2="30" stroke="currentColor" stroke-width="2"/>
        <line x1="4" y1="50" x2="70" y2="20" stroke="currentColor" stroke-width="2"/>
      </svg>`
    },
    {
      id: 'quote-marks',
      name: 'Comillas Elegantes',
      svg: `<svg viewBox="0 0 100 80" xmlns="http://www.w3.org/2000/svg">
        <path d="M15 45 c0-15 10-35 30-40 l5 8 c-12 5-18 15-18 24 h13 v35 H15 Z" fill="currentColor"/>
        <path d="M55 45 c0-15 10-35 30-40 l5 8 c-12 5-18 15-18 24 h13 v35 H55 Z" fill="currentColor"/>
      </svg>`
    }
  ];

  // --- App State ---
  const state = {
    currentRatio: '3:4',
    exportScale: 2,
    transparentMode: true,
    showRuleOfThirds: true,
    showCenterGuides: true,
    zoom: 1,
    activeLayerId: null,
    layers: [],
    customFonts: [],
    bgPhoto: null, // HTMLImageElement or null
    cameraStream: null
  };

  // DOM Elements
  const canvas = document.getElementById('studio-canvas');
  const ctx = canvas.getContext('2d');
  const canvasContainer = document.getElementById('canvas-container');
  const stageWrapper = document.getElementById('stage-wrapper');
  const selectionOverlay = document.getElementById('selection-overlay');
  const zoomBadge = document.getElementById('zoom-badge');
  const toastEl = document.getElementById('toast');

  // Input Controls
  const textInput = document.getElementById('text-input');
  const fontFamilySelect = document.getElementById('font-family-select');
  const customFontsOptgroup = document.getElementById('custom-fonts-optgroup');
  const fontUpload = document.getElementById('font-upload');
  const loadedFontIndicator = document.getElementById('loaded-font-indicator');
  const loadedFontName = document.getElementById('loaded-font-name');

  const fontSize = document.getElementById('font-size');
  const fontSizeVal = document.getElementById('font-size-val');
  const letterSpacing = document.getElementById('letter-spacing');
  const letterSpacingVal = document.getElementById('letter-spacing-val');
  const lineHeight = document.getElementById('line-height');
  const lineHeightVal = document.getElementById('line-height-val');
  const textRotation = document.getElementById('text-rotation');
  const rotationVal = document.getElementById('rotation-val');

  const btnAlignLeft = document.getElementById('btn-align-left');
  const btnAlignCenter = document.getElementById('btn-align-center');
  const btnAlignRight = document.getElementById('btn-align-right');
  const btnAlignJustify = document.getElementById('btn-align-justify');
  const btnFontBold = document.getElementById('btn-font-bold');
  const btnFontItalic = document.getElementById('btn-font-italic');
  const btnFontUppercase = document.getElementById('btn-font-uppercase');

  const textColor = document.getElementById('text-color');
  const textColorHex = document.getElementById('text-color-hex');
  const textOpacity = document.getElementById('text-opacity');
  const opacityVal = document.getElementById('opacity-val');

  const enableStroke = document.getElementById('enable-stroke');
  const strokeControls = document.getElementById('stroke-controls');
  const strokeColor = document.getElementById('stroke-color');
  const strokeWidth = document.getElementById('stroke-width');
  const strokeWidthVal = document.getElementById('stroke-width-val');

  const enableShadow = document.getElementById('enable-shadow');
  const shadowControls = document.getElementById('shadow-controls');
  const shadowColor = document.getElementById('shadow-color');
  const shadowBlur = document.getElementById('shadow-blur');
  const shadowBlurVal = document.getElementById('shadow-blur-val');
  const shadowOffsetX = document.getElementById('shadow-offset-x');
  const shadowOffsetXVal = document.getElementById('shadow-offset-x-val');
  const shadowOffsetY = document.getElementById('shadow-offset-y');
  const shadowOffsetYVal = document.getElementById('shadow-offset-y-val');

  const enableBgBox = document.getElementById('enable-bg-box');
  const bgBoxControls = document.getElementById('bg-box-controls');
  const bgBoxColor = document.getElementById('bg-box-color');
  const bgBoxOpacity = document.getElementById('bg-box-opacity');
  const bgBoxOpacityVal = document.getElementById('bg-box-opacity-val');
  const bgBoxPad = document.getElementById('bg-box-pad');
  const bgBoxPadVal = document.getElementById('bg-box-pad-val');
  const bgBoxRadius = document.getElementById('bg-box-radius');
  const bgBoxRadiusVal = document.getElementById('bg-box-radius-val');

  // SVG Controls
  const svgDropzone = document.getElementById('svg-dropzone');
  const svgUpload = document.getElementById('svg-upload');
  const btnPasteSvg = document.getElementById('btn-paste-svg');
  const activeSvgControls = document.getElementById('active-svg-controls');
  const svgScale = document.getElementById('svg-scale');
  const svgScaleVal = document.getElementById('svg-scale-val');
  const svgRotation = document.getElementById('svg-rotation');
  const svgRotationVal = document.getElementById('svg-rotation-val');
  const svgOpacity = document.getElementById('svg-opacity');
  const svgOpacityVal = document.getElementById('svg-opacity-val');
  const svgTintColor = document.getElementById('svg-tint-color');
  const svgEnableTint = document.getElementById('svg-enable-tint');
  const btnDeleteSvg = document.getElementById('btn-delete-svg');
  const svgPresetsContainer = document.getElementById('svg-presets-container');

  // Layers Tab
  const layersList = document.getElementById('layers-list');
  const layerCount = document.getElementById('layer-count');
  const btnLayerUp = document.getElementById('btn-layer-up');
  const btnLayerDown = document.getElementById('btn-layer-down');
  const btnLayerDel = document.getElementById('btn-layer-del');

  // Settings & Ratio
  const exportScaleSelect = document.getElementById('export-scale');
  const outputDimLabel = document.getElementById('output-dim-label');
  const showRuleOfThirdsCheck = document.getElementById('show-rule-of-thirds');
  const showCenterGuidesCheck = document.getElementById('show-center-guides');
  const transparentModeCheck = document.getElementById('transparent-mode');

  // Modals
  const cameraModal = document.getElementById('camera-modal');
  const btnCloseCamera = document.getElementById('btn-close-camera');
  const btnCameraToggle = document.getElementById('btn-camera-toggle');
  const btnStartCamera = document.getElementById('btn-start-camera');
  const btnCapturePhoto = document.getElementById('btn-capture-photo');
  const btnSwitchCamera = document.getElementById('btn-switch-camera');
  const liveCameraVideo = document.getElementById('live-camera-video');
  const bgPhotoUpload = document.getElementById('bg-photo-upload');
  const btnRemoveBgPhoto = document.getElementById('btn-remove-bg-photo');

  const svgPasteModal = document.getElementById('svg-paste-modal');
  const btnCloseSvgPaste = document.getElementById('btn-close-svg-paste');
  const btnCancelSvgPaste = document.getElementById('btn-cancel-svg-paste');
  const btnConfirmSvgPaste = document.getElementById('btn-confirm-svg-paste');
  const rawSvgTextarea = document.getElementById('raw-svg-textarea');

  const exportModal = document.getElementById('export-modal');
  const btnCloseExport = document.getElementById('btn-close-export');
  const btnExportPng = document.getElementById('btn-export-png');
  const exportPreviewImg = document.getElementById('export-preview-img');
  const exportMetaInfo = document.getElementById('export-meta-info');
  const btnModalDownload = document.getElementById('btn-modal-download');
  const btnModalCopy = document.getElementById('btn-modal-copy');
  const btnModalShare = document.getElementById('btn-modal-share');

  // --- Helper: Toast Notification ---
  function showToast(message, duration = 2800) {
    toastEl.textContent = message;
    toastEl.classList.add('show');
    clearTimeout(toastEl._timer);
    toastEl._timer = setTimeout(() => {
      toastEl.classList.remove('show');
    }, duration);
  }

  // --- Unique ID Generator ---
  function uid(prefix = 'item') {
    return prefix + '_' + Math.random().toString(36).substr(2, 9);
  }

  // --- Canvas Dimensions & Aspect Ratio Setup ---
  function updateCanvasDimensions() {
    const config = RATIOS[state.currentRatio];
    canvas.width = config.width;
    canvas.height = config.height;

    // Update Output Dimension label in settings
    const scale = parseInt(state.exportScale, 10);
    const finalW = config.width * scale;
    const finalH = config.height * scale;
    outputDimLabel.textContent = `${finalW} × ${finalH} px (${state.currentRatio} @ ${scale}x)`;

    fitCanvasToStage();
    render();
  }

  function fitCanvasToStage() {
    const stageWidth = stageWrapper.clientWidth - 48;
    const stageHeight = stageWrapper.clientHeight - 48;
    const config = RATIOS[state.currentRatio];

    const stageAspect = stageWidth / stageHeight;
    const canvasAspect = config.width / config.height;

    let displayW, displayH;
    if (stageAspect > canvasAspect) {
      displayH = stageHeight;
      displayW = displayH * canvasAspect;
    } else {
      displayW = stageWidth;
      displayH = displayW / canvasAspect;
    }

    displayW = Math.max(200, displayW * state.zoom);
    displayH = Math.max(200, displayH * state.zoom);

    canvasContainer.style.width = `${Math.round(displayW)}px`;
    canvasContainer.style.height = `${Math.round(displayH)}px`;
  }

  // --- Font Management (.TTF / .OTF / WebFonts) ---
  async function loadTTFFont(file) {
    try {
      showToast(`Cargando fuente "${file.name}"...`);
      const buffer = await file.arrayBuffer();
      // Generate clean font-family name
      const fontName = file.name.replace(/\.[^/.]+$/, "").replace(/[^a-zA-Z0-9_-]/g, "_");
      
      const fontFace = new FontFace(fontName, buffer);
      await fontFace.load();
      document.fonts.add(fontFace);

      // Check if already in list
      if (!state.customFonts.includes(fontName)) {
        state.customFonts.push(fontName);

        const option = document.createElement('option');
        option.value = `'${fontName}', sans-serif`;
        option.textContent = `★ ${file.name}`;
        customFontsOptgroup.appendChild(option);
      }

      // Automatically apply to active layer if text
      const activeLayer = getActiveLayer();
      if (activeLayer && activeLayer.type === 'text') {
        activeLayer.fontFamily = `'${fontName}', sans-serif`;
        fontFamilySelect.value = activeLayer.fontFamily;
      }

      loadedFontIndicator.style.display = 'block';
      loadedFontName.textContent = file.name;
      showToast(`¡Fuente .TTF "${file.name}" lista!`);
      render();
    } catch (err) {
      console.error('Error al cargar fuente:', err);
      showToast(`Error al procesar la fuente: ${err.message}`, 4000);
    }
  }

  // --- SVG Loading & Processing ---
  function parseAndAddSVG(svgString, title = 'Gráfico SVG') {
    if (!svgString || !svgString.includes('<svg')) {
      showToast('Código SVG no válido');
      return;
    }

    // Clean up / normalize
    const cleanSvg = svgString.trim();
    const blob = new Blob([cleanSvg], { type: 'image/svg+xml;charset=utf-8' });
    const blobUrl = URL.createObjectURL(blob);

    const img = new Image();
    img.onload = () => {
      // Calculate sensible initial size
      const maxDim = Math.min(canvas.width, canvas.height) * 0.4;
      let w = img.naturalWidth || 200;
      let h = img.naturalHeight || 200;
      if (w > maxDim || h > maxDim) {
        const factor = maxDim / Math.max(w, h);
        w *= factor;
        h *= factor;
      }

      const newLayer = {
        id: uid('svg'),
        type: 'svg',
        title: title,
        rawSvg: cleanSvg,
        img: img,
        blobUrl: blobUrl,
        x: canvas.width / 2,
        y: canvas.height / 2,
        width: w,
        height: h,
        naturalWidth: img.naturalWidth || w,
        naturalHeight: img.naturalHeight || h,
        scale: 1,
        rotation: 0,
        opacity: 1,
        tint: {
          enabled: false,
          color: '#FFFFFF'
        },
        visible: true
      };

      state.layers.push(newLayer);
      setActiveLayer(newLayer.id);
      switchTab('tab-svg');
      updateLayersUI();
      render();
      showToast(`SVG "${title}" añadido`);
    };

    img.onerror = (e) => {
      console.error('Error al procesar SVG:', e);
      showToast('No se pudo procesar la imagen SVG');
    };

    img.src = blobUrl;
  }

  // --- Layer Management ---
  function createDefaultTextLayer() {
    return {
      id: uid('text'),
      type: 'text',
      title: 'Texto Principal',
      text: 'FOTOGRAFÍA\nURBANA',
      x: canvas.width / 2,
      y: canvas.height * 0.42,
      fontFamily: "'Bebas Neue', sans-serif",
      fontSize: 84,
      lineHeight: 1.05,
      letterSpacing: 3,
      rotation: 0,
      align: 'center',
      color: '#FFFFFF',
      opacity: 1,
      isBold: false,
      isItalic: false,
      isUppercase: true,
      stroke: {
        enabled: true,
        color: '#000000',
        width: 4
      },
      shadow: {
        enabled: true,
        color: '#000000',
        blur: 16,
        offsetX: 0,
        offsetY: 4
      },
      bgBox: {
        enabled: false,
        color: '#000000',
        opacity: 0.75,
        padding: 16,
        radius: 12
      },
      visible: true
    };
  }

  function getActiveLayer() {
    return state.layers.find(l => l.id === state.activeLayerId) || null;
  }

  function setActiveLayer(layerId) {
    state.activeLayerId = layerId;
    syncControlsFromActiveLayer();
    updateLayersUI();
    render();
  }

  function syncControlsFromActiveLayer() {
    const layer = getActiveLayer();
    if (!layer) return;

    if (layer.type === 'text') {
      textInput.value = layer.text;
      fontFamilySelect.value = layer.fontFamily;
      fontSize.value = layer.fontSize;
      fontSizeVal.textContent = layer.fontSize;
      letterSpacing.value = layer.letterSpacing;
      letterSpacingVal.textContent = layer.letterSpacing;
      lineHeight.value = layer.lineHeight;
      lineHeightVal.textContent = layer.lineHeight;
      textRotation.value = layer.rotation;
      rotationVal.textContent = layer.rotation;

      btnAlignLeft.classList.toggle('active', layer.align === 'left');
      btnAlignCenter.classList.toggle('active', layer.align === 'center');
      btnAlignRight.classList.toggle('active', layer.align === 'right');
      if (btnAlignJustify) btnAlignJustify.classList.toggle('active', layer.align === 'justify');
      btnFontBold.classList.toggle('active', !!layer.isBold);
      btnFontItalic.classList.toggle('active', !!layer.isItalic);
      btnFontUppercase.classList.toggle('active', !!layer.isUppercase);

      textColor.value = layer.color;
      textColorHex.textContent = layer.color.toUpperCase();
      textOpacity.value = Math.round(layer.opacity * 100);
      opacityVal.textContent = textOpacity.value;

      enableStroke.checked = layer.stroke.enabled;
      strokeControls.style.display = layer.stroke.enabled ? 'flex' : 'none';
      strokeColor.value = layer.stroke.color;
      strokeWidth.value = layer.stroke.width;
      strokeWidthVal.textContent = layer.stroke.width;

      enableShadow.checked = layer.shadow.enabled;
      shadowControls.style.display = layer.shadow.enabled ? 'flex' : 'none';
      shadowColor.value = layer.shadow.color;
      shadowBlur.value = layer.shadow.blur;
      shadowBlurVal.textContent = layer.shadow.blur;
      shadowOffsetX.value = layer.shadow.offsetX;
      shadowOffsetXVal.textContent = layer.shadow.offsetX;
      shadowOffsetY.value = layer.shadow.offsetY;
      shadowOffsetYVal.textContent = layer.shadow.offsetY;

      enableBgBox.checked = layer.bgBox.enabled;
      bgBoxControls.style.display = layer.bgBox.enabled ? 'flex' : 'none';
      bgBoxColor.value = layer.bgBox.color;
      bgBoxOpacity.value = Math.round(layer.bgBox.opacity * 100);
      bgBoxOpacityVal.textContent = bgBoxOpacity.value;
      bgBoxPad.value = layer.bgBox.padding;
      bgBoxPadVal.textContent = layer.bgBox.padding;
      bgBoxRadius.value = layer.bgBox.radius;
      bgBoxRadiusVal.textContent = layer.bgBox.radius;

      activeSvgControls.style.display = 'none';
    } else if (layer.type === 'svg') {
      activeSvgControls.style.display = 'block';
      svgScale.value = Math.round(layer.scale * 100);
      svgScaleVal.textContent = svgScale.value;
      svgRotation.value = layer.rotation;
      svgRotationVal.textContent = layer.rotation;
      svgOpacity.value = Math.round(layer.opacity * 100);
      svgOpacityVal.textContent = svgOpacity.value;
      svgEnableTint.checked = layer.tint.enabled;
      svgTintColor.value = layer.tint.color;
    }
  }

  function updateLayersUI() {
    layersList.innerHTML = '';
    layerCount.textContent = state.layers.length;

    // Render layers in reverse order (top to bottom list)
    [...state.layers].reverse().forEach((layer) => {
      const item = document.createElement('div');
      item.className = `layer-item ${layer.id === state.activeLayerId ? 'active' : ''}`;
      item.onclick = () => setActiveLayer(layer.id);

      const typeBadge = layer.type === 'text' ? 'TXT' : 'SVG';
      const labelText = layer.type === 'text' ? (layer.text.split('\n')[0] || 'Texto') : layer.title;

      item.innerHTML = `
        <div class="layer-info">
          <span class="layer-type-badge">${typeBadge}</span>
          <span style="max-width: 170px; overflow: hidden; text-overflow: ellipsis;">${labelText}</span>
        </div>
        <div class="layer-actions">
          <button class="layer-visibility-btn" title="Alternar visibilidad">
            ${layer.visible ? '👁' : '🚫'}
          </button>
        </div>
      `;

      item.querySelector('.layer-visibility-btn').onclick = (e) => {
        e.stopPropagation();
        layer.visible = !layer.visible;
        updateLayersUI();
        render();
      };

      layersList.appendChild(item);
    });
  }

  // --- Rendering Pipeline ---
  function render(targetCtx = ctx, targetScale = 1, isExport = false) {
    const w = targetCtx.canvas.width;
    const h = targetCtx.canvas.height;

    // Clear canvas
    targetCtx.clearRect(0, 0, w, h);

    // 1. Render Background Photo (if present and transparent mode is OFF or when user chose with photo)
    if (state.bgPhoto && (!state.transparentMode || !isExport)) {
      renderCoverImage(targetCtx, state.bgPhoto, w, h);
    }

    // 2. Render all visible layers in stack order
    state.layers.forEach((layer) => {
      if (!layer.visible) return;

      targetCtx.save();
      if (layer.type === 'text') {
        renderTextLayer(targetCtx, layer, targetScale);
      } else if (layer.type === 'svg') {
        renderSvgLayer(targetCtx, layer, targetScale);
      }
      targetCtx.restore();
    });

    // 3. Render Composition Guides (only on editor preview canvas, NEVER on export)
    if (!isExport) {
      if (state.showRuleOfThirds) renderThirdsGuide(targetCtx, w, h);
      if (state.showCenterGuides) renderCenterGuide(targetCtx, w, h);
      renderSelectionOverlay();
    }
  }

  function renderCoverImage(context, img, destW, destH) {
    const imgAspect = img.naturalWidth / img.naturalHeight;
    const destAspect = destW / destH;
    let sW, sH, sX, sY;

    if (imgAspect > destAspect) {
      sH = img.naturalHeight;
      sW = sH * destAspect;
      sX = (img.naturalWidth - sW) / 2;
      sY = 0;
    } else {
      sW = img.naturalWidth;
      sH = sW / destAspect;
      sX = 0;
      sY = (img.naturalHeight - sH) / 2;
    }

    context.drawImage(img, sX, sY, sW, sH, 0, 0, destW, destH);
  }

  function renderTextLayer(context, layer, scale = 1) {
    const x = layer.x * scale;
    const y = layer.y * scale;
    const fontSize = layer.fontSize * scale;
    const lineHeightPx = fontSize * layer.lineHeight;
    const letterSpacing = layer.letterSpacing * scale;

    let fontStyle = '';
    if (layer.isItalic) fontStyle += 'italic ';
    if (layer.isBold) fontStyle += 'bold ';
    context.font = `${fontStyle}${fontSize}px ${layer.fontFamily}`;
    context.textAlign = layer.align;
    context.textBaseline = 'middle';

    const rawText = layer.isUppercase ? layer.text.toUpperCase() : layer.text;
    const lines = rawText.split('\n');

    context.translate(x, y);
    if (layer.rotation !== 0) {
      context.rotate((layer.rotation * Math.PI) / 180);
    }

    // Compute bounding block height for vertical centering
    const totalBlockH = lines.length * lineHeightPx;
    const startY = -(totalBlockH / 2) + (lineHeightPx / 2);

    // Optional Background Box / Pill
    if (layer.bgBox && layer.bgBox.enabled) {
      let maxLineW = 0;
      lines.forEach(line => {
        const w = measureTextWidth(context, line, letterSpacing);
        if (w > maxLineW) maxLineW = w;
      });

      const pad = layer.bgBox.padding * scale;
      const boxW = maxLineW + pad * 2;
      const boxH = totalBlockH + pad * 2;
      const radius = layer.bgBox.radius * scale;

      let boxX = -boxW / 2;
      if (layer.align === 'left') boxX = -pad;
      if (layer.align === 'right') boxX = -maxLineW - pad;

      const boxY = -(totalBlockH / 2) - pad;

      context.save();
      context.globalAlpha = layer.bgBox.opacity * layer.opacity;
      context.fillStyle = layer.bgBox.color;
      drawRoundedRect(context, boxX, boxY, boxW, boxH, radius);
      context.fill();
      context.restore();
    }

    context.globalAlpha = layer.opacity;

    lines.forEach((line, index) => {
      const lineY = startY + index * lineHeightPx;

      // Draw Shadow
      if (layer.shadow && layer.shadow.enabled) {
        context.save();
        context.shadowColor = layer.shadow.color;
        context.shadowBlur = layer.shadow.blur * scale;
        context.shadowOffsetX = layer.shadow.offsetX * scale;
        context.shadowOffsetY = layer.shadow.offsetY * scale;
        drawSpacedText(context, line, 0, lineY, letterSpacing, 'fill', layer.color);
        context.restore();
      }

      // Draw Stroke / Outline
      if (layer.stroke && layer.stroke.enabled && layer.stroke.width > 0) {
        context.save();
        context.strokeStyle = layer.stroke.color;
        context.lineWidth = layer.stroke.width * scale;
        context.lineJoin = 'round';
        context.miterLimit = 2;
        drawSpacedText(context, line, 0, lineY, letterSpacing, 'stroke');
        context.restore();
      }

      // Draw Fill Text
      drawSpacedText(context, line, 0, lineY, letterSpacing, 'fill', layer.color);
    });
  }

  function measureTextWidth(context, text, letterSpacing = 0) {
    if (!text) return 0;
    const baseW = context.measureText(text).width;
    return baseW + (text.length - 1) * letterSpacing;
  }

  function drawSpacedText(context, text, startX, y, spacing, mode = 'fill', color = '#FFF') {
    if (color && mode === 'fill') context.fillStyle = color;

    if (!spacing || spacing === 0) {
      if (mode === 'fill') context.fillText(text, startX, y);
      else context.strokeText(text, startX, y);
      return;
    }

    // Measure total width for text alignment
    const totalW = measureTextWidth(context, text, spacing);
    let currentX = startX;
    if (context.textAlign === 'center') {
      currentX = startX - totalW / 2;
    } else if (context.textAlign === 'right') {
      currentX = startX - totalW;
    }

    const prevAlign = context.textAlign;
    context.textAlign = 'left';

    for (let i = 0; i < text.length; i++) {
      const char = text[i];
      if (mode === 'fill') {
        context.fillText(char, currentX, y);
      } else {
        context.strokeText(char, currentX, y);
      }
      currentX += context.measureText(char).width + spacing;
    }

    context.textAlign = prevAlign;
  }

  function drawRoundedRect(context, x, y, width, height, radius) {
    radius = Math.min(radius, width / 2, height / 2);
    context.beginPath();
    context.moveTo(x + radius, y);
    context.lineTo(x + width - radius, y);
    context.quadraticCurveTo(x + width, y, x + width, y + radius);
    context.lineTo(x + width, y + height - radius);
    context.quadraticCurveTo(x + width, y + height, x + width - radius, y + height);
    context.lineTo(x + radius, y + height);
    context.quadraticCurveTo(x, y + height, x, y + height - radius);
    context.lineTo(x, y + radius);
    context.quadraticCurveTo(x, y, x + radius, y);
    context.closePath();
  }

  function renderSvgLayer(context, layer, scale = 1) {
    const x = layer.x * scale;
    const y = layer.y * scale;
    const w = layer.width * layer.scale * scale;
    const h = layer.height * layer.scale * scale;

    context.translate(x, y);
    if (layer.rotation !== 0) {
      context.rotate((layer.rotation * Math.PI) / 180);
    }
    context.globalAlpha = layer.opacity;

    if (layer.tint && layer.tint.enabled) {
      // Offscreen tinting buffer
      const tintCanvas = document.createElement('canvas');
      tintCanvas.width = Math.max(1, Math.round(w));
      tintCanvas.height = Math.max(1, Math.round(h));
      const tCtx = tintCanvas.getContext('2d');

      tCtx.drawImage(layer.img, 0, 0, tintCanvas.width, tintCanvas.height);
      tCtx.globalCompositeOperation = 'source-in';
      tCtx.fillStyle = layer.tint.color;
      tCtx.fillRect(0, 0, tintCanvas.width, tintCanvas.height);

      context.drawImage(tintCanvas, -w / 2, -h / 2, w, h);
    } else {
      context.drawImage(layer.img, -w / 2, -h / 2, w, h);
    }
  }

  function renderThirdsGuide(context, w, h) {
    context.save();
    context.strokeStyle = 'rgba(255, 255, 255, 0.15)';
    context.lineWidth = 1;
    context.setLineDash([6, 6]);

    // Vertical lines
    context.beginPath();
    context.moveTo(w / 3, 0);
    context.lineTo(w / 3, h);
    context.moveTo((w * 2) / 3, 0);
    context.lineTo((w * 2) / 3, h);

    // Horizontal lines
    context.moveTo(0, h / 3);
    context.lineTo(w, h / 3);
    context.moveTo(0, (h * 2) / 3);
    context.lineTo(w, (h * 2) / 3);
    context.stroke();
    context.restore();
  }

  function renderCenterGuide(context, w, h) {
    context.save();
    context.strokeStyle = 'rgba(56, 189, 248, 0.25)';
    context.lineWidth = 1;

    context.beginPath();
    context.moveTo(w / 2, 0);
    context.lineTo(w / 2, h);
    context.moveTo(0, h / 2);
    context.lineTo(w, h / 2);
    context.stroke();
    context.restore();
  }

  function renderSelectionOverlay() {
    const layer = getActiveLayer();
    if (!layer || !layer.visible) {
      selectionOverlay.style.display = 'none';
      return;
    }

    const config = RATIOS[state.currentRatio];
    const displayW = canvasContainer.clientWidth;
    const displayH = canvasContainer.clientHeight;
    const scaleX = displayW / config.width;
    const scaleY = displayH / config.height;

    let bW = 100, bH = 50;
    if (layer.type === 'text') {
      bW = (layer.fontSize * 4) * scaleX;
      bH = (layer.fontSize * 1.5 * layer.text.split('\n').length) * scaleY;
    } else if (layer.type === 'svg') {
      bW = (layer.width * layer.scale) * scaleX;
      bH = (layer.height * layer.scale) * scaleY;
    }

    const screenX = layer.x * scaleX;
    const screenY = layer.y * scaleY;

    selectionOverlay.style.display = 'block';
    selectionOverlay.style.width = `${Math.round(bW)}px`;
    selectionOverlay.style.height = `${Math.round(bH)}px`;
    selectionOverlay.style.left = `${Math.round(screenX - bW / 2)}px`;
    selectionOverlay.style.top = `${Math.round(screenY - bH / 2)}px`;
    selectionOverlay.style.transform = `rotate(${layer.rotation}deg)`;
  }

  // --- Interactive Dragging & Canvas Panning ---
  let isDragging = false;
  let dragStartX = 0;
  let dragStartY = 0;
  let initialLayerX = 0;
  let initialLayerY = 0;

  function getCanvasCoordinates(e) {
    const rect = canvas.getBoundingClientRect();
    const clientX = e.touches ? e.touches[0].clientX : e.clientX;
    const clientY = e.touches ? e.touches[0].clientY : e.clientY;
    const scaleX = canvas.width / rect.width;
    const scaleY = canvas.height / rect.height;

    return {
      x: (clientX - rect.left) * scaleX,
      y: (clientY - rect.top) * scaleY
    };
  }

  function hitTest(x, y) {
    // Check layers in reverse order (topmost first)
    for (let i = state.layers.length - 1; i >= 0; i--) {
      const l = state.layers[i];
      if (!l.visible) continue;

      let w = 150, h = 100;
      if (l.type === 'text') {
        w = Math.max(120, l.fontSize * 3);
        h = Math.max(60, l.fontSize * l.lineHeight * l.text.split('\n').length);
      } else if (l.type === 'svg') {
        w = l.width * l.scale;
        h = l.height * l.scale;
      }

      if (Math.abs(x - l.x) <= w / 2 && Math.abs(y - l.y) <= h / 2) {
        return l;
      }
    }
    return null;
  }

  function onPointerDown(e) {
    const coords = getCanvasCoordinates(e);
    const hit = hitTest(coords.x, coords.y);

    if (hit) {
      setActiveLayer(hit.id);
      isDragging = true;
      dragStartX = coords.x;
      dragStartY = coords.y;
      initialLayerX = hit.x;
      initialLayerY = hit.y;
    }
  }

  function onPointerMove(e) {
    if (!isDragging) return;
    const layer = getActiveLayer();
    if (!layer) return;

    const coords = getCanvasCoordinates(e);
    const dx = coords.x - dragStartX;
    const dy = coords.y - dragStartY;

    layer.x = Math.round(initialLayerX + dx);
    layer.y = Math.round(initialLayerY + dy);

    // Snap to center horizontal / vertical if close
    if (Math.abs(layer.x - canvas.width / 2) < 15) {
      layer.x = canvas.width / 2;
    }
    if (Math.abs(layer.y - canvas.height / 2) < 15) {
      layer.y = canvas.height / 2;
    }

    render();
  }

  function onPointerUp() {
    isDragging = false;
  }

  canvas.addEventListener('mousedown', onPointerDown);
  window.addEventListener('mousemove', onPointerMove);
  window.addEventListener('mouseup', onPointerUp);

  canvas.addEventListener('touchstart', onPointerDown, { passive: false });
  window.addEventListener('touchmove', onPointerMove, { passive: false });
  window.addEventListener('touchend', onPointerUp);

  // --- UI Event Listeners ---

  // Aspect Ratio Switching
  document.querySelectorAll('.aspect-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.aspect-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      state.currentRatio = btn.getAttribute('data-ratio');
      updateCanvasDimensions();
      showToast(`Formato cambiado a ${state.currentRatio}`);
    });
  });

  // Tab Navigation
  function switchTab(tabId) {
    document.querySelectorAll('.tab-btn').forEach(b => {
      b.classList.toggle('active', b.getAttribute('data-tab') === tabId);
    });
    document.querySelectorAll('.tab-panel').forEach(p => {
      p.classList.toggle('active', p.id === tabId);
    });
  }

  document.querySelectorAll('.tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      switchTab(btn.getAttribute('data-tab'));
    });
  });

  // Mobile quick buttons
  document.getElementById('m-btn-text').onclick = () => switchTab('tab-text');
  document.getElementById('m-btn-ttf').onclick = () => {
    switchTab('tab-text');
    fontUpload.click();
  };
  document.getElementById('m-btn-svg').onclick = () => switchTab('tab-svg');
  document.getElementById('m-btn-camera').onclick = () => openCameraModal();
  document.getElementById('m-btn-export').onclick = () => triggerExport();

  // Text Property Listeners
  textInput.addEventListener('input', (e) => {
    const layer = getActiveLayer();
    if (layer && layer.type === 'text') {
      layer.text = e.target.value;
      updateLayersUI();
      render();
    }
  });

  document.getElementById('btn-add-text').addEventListener('click', () => {
    const newLayer = createDefaultTextLayer();
    newLayer.text = 'NUEVO TEXTO';
    newLayer.y = canvas.height * 0.6;
    state.layers.push(newLayer);
    setActiveLayer(newLayer.id);
    switchTab('tab-text');
    showToast('Nueva capa de texto añadida');
  });

  fontFamilySelect.addEventListener('change', (e) => {
    const layer = getActiveLayer();
    if (layer && layer.type === 'text') {
      layer.fontFamily = e.target.value;
      render();
    }
  });

  fontUpload.addEventListener('change', (e) => {
    const file = e.target.files[0];
    if (file) loadTTFFont(file);
  });

  fontSize.addEventListener('input', (e) => {
    fontSizeVal.textContent = e.target.value;
    const layer = getActiveLayer();
    if (layer && layer.type === 'text') {
      layer.fontSize = parseInt(e.target.value, 10);
      render();
    }
  });

  letterSpacing.addEventListener('input', (e) => {
    letterSpacingVal.textContent = e.target.value;
    const layer = getActiveLayer();
    if (layer && layer.type === 'text') {
      layer.letterSpacing = parseInt(e.target.value, 10);
      render();
    }
  });

  lineHeight.addEventListener('input', (e) => {
    lineHeightVal.textContent = e.target.value;
    const layer = getActiveLayer();
    if (layer && layer.type === 'text') {
      layer.lineHeight = parseFloat(e.target.value);
      render();
    }
  });

  textRotation.addEventListener('input', (e) => {
    rotationVal.textContent = e.target.value;
    const layer = getActiveLayer();
    if (layer) {
      layer.rotation = parseInt(e.target.value, 10);
      render();
    }
  });

  // Alignment & Styles
  btnAlignLeft.onclick = () => setTextAlign('left');
  btnAlignCenter.onclick = () => setTextAlign('center');
  btnAlignRight.onclick = () => setTextAlign('right');
  if (btnAlignJustify) btnAlignJustify.onclick = () => setTextAlign('justify');

  function setTextAlign(alignment) {
    const layer = getActiveLayer();
    if (layer && layer.type === 'text') {
      layer.align = alignment;
      btnAlignLeft.classList.toggle('active', alignment === 'left');
      btnAlignCenter.classList.toggle('active', alignment === 'center');
      btnAlignRight.classList.toggle('active', alignment === 'right');
      if (btnAlignJustify) btnAlignJustify.classList.toggle('active', alignment === 'justify');
      render();
    }
  }

  btnFontBold.onclick = () => {
    const layer = getActiveLayer();
    if (layer && layer.type === 'text') {
      layer.isBold = !layer.isBold;
      btnFontBold.classList.toggle('active', layer.isBold);
      render();
    }
  };

  btnFontItalic.onclick = () => {
    const layer = getActiveLayer();
    if (layer && layer.type === 'text') {
      layer.isItalic = !layer.isItalic;
      btnFontItalic.classList.toggle('active', layer.isItalic);
      render();
    }
  };

  btnFontUppercase.onclick = () => {
    const layer = getActiveLayer();
    if (layer && layer.type === 'text') {
      layer.isUppercase = !layer.isUppercase;
      btnFontUppercase.classList.toggle('active', layer.isUppercase);
      render();
    }
  };

  textColor.addEventListener('input', (e) => {
    textColorHex.textContent = e.target.value.toUpperCase();
    const layer = getActiveLayer();
    if (layer && layer.type === 'text') {
      layer.color = e.target.value;
      render();
    }
  });

  textOpacity.addEventListener('input', (e) => {
    opacityVal.textContent = e.target.value;
    const layer = getActiveLayer();
    if (layer) {
      layer.opacity = parseInt(e.target.value, 10) / 100;
      render();
    }
  });

  // Stroke
  enableStroke.addEventListener('change', (e) => {
    strokeControls.style.display = e.target.checked ? 'flex' : 'none';
    const layer = getActiveLayer();
    if (layer && layer.type === 'text') {
      layer.stroke.enabled = e.target.checked;
      render();
    }
  });

  strokeColor.addEventListener('input', (e) => {
    const layer = getActiveLayer();
    if (layer && layer.type === 'text') {
      layer.stroke.color = e.target.value;
      render();
    }
  });

  strokeWidth.addEventListener('input', (e) => {
    strokeWidthVal.textContent = e.target.value;
    const layer = getActiveLayer();
    if (layer && layer.type === 'text') {
      layer.stroke.width = parseInt(e.target.value, 10);
      render();
    }
  });

  // Shadow
  enableShadow.addEventListener('change', (e) => {
    shadowControls.style.display = e.target.checked ? 'flex' : 'none';
    const layer = getActiveLayer();
    if (layer && layer.type === 'text') {
      layer.shadow.enabled = e.target.checked;
      render();
    }
  });

  shadowColor.addEventListener('input', (e) => {
    const layer = getActiveLayer();
    if (layer && layer.type === 'text') {
      layer.shadow.color = e.target.value;
      render();
    }
  });

  shadowBlur.addEventListener('input', (e) => {
    shadowBlurVal.textContent = e.target.value;
    const layer = getActiveLayer();
    if (layer && layer.type === 'text') {
      layer.shadow.blur = parseInt(e.target.value, 10);
      render();
    }
  });

  shadowOffsetX.addEventListener('input', (e) => {
    shadowOffsetXVal.textContent = e.target.value;
    const layer = getActiveLayer();
    if (layer && layer.type === 'text') {
      layer.shadow.offsetX = parseInt(e.target.value, 10);
      render();
    }
  });

  shadowOffsetY.addEventListener('input', (e) => {
    shadowOffsetYVal.textContent = e.target.value;
    const layer = getActiveLayer();
    if (layer && layer.type === 'text') {
      layer.shadow.offsetY = parseInt(e.target.value, 10);
      render();
    }
  });

  // Background Box / Pill
  enableBgBox.addEventListener('change', (e) => {
    bgBoxControls.style.display = e.target.checked ? 'flex' : 'none';
    const layer = getActiveLayer();
    if (layer && layer.type === 'text') {
      layer.bgBox.enabled = e.target.checked;
      render();
    }
  });

  bgBoxColor.addEventListener('input', (e) => {
    const layer = getActiveLayer();
    if (layer && layer.type === 'text') {
      layer.bgBox.color = e.target.value;
      render();
    }
  });

  bgBoxOpacity.addEventListener('input', (e) => {
    bgBoxOpacityVal.textContent = e.target.value;
    const layer = getActiveLayer();
    if (layer && layer.type === 'text') {
      layer.bgBox.opacity = parseInt(e.target.value, 10) / 100;
      render();
    }
  });

  bgBoxPad.addEventListener('input', (e) => {
    bgBoxPadVal.textContent = e.target.value;
    const layer = getActiveLayer();
    if (layer && layer.type === 'text') {
      layer.bgBox.padding = parseInt(e.target.value, 10);
      render();
    }
  });

  bgBoxRadius.addEventListener('input', (e) => {
    bgBoxRadiusVal.textContent = e.target.value;
    const layer = getActiveLayer();
    if (layer && layer.type === 'text') {
      layer.bgBox.radius = parseInt(e.target.value, 10);
      render();
    }
  });

  // SVG Dropzone & Presets
  svgDropzone.onclick = () => svgUpload.click();
  svgUpload.onchange = (e) => {
    const file = e.target.files[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (event) => parseAndAddSVG(event.target.result, file.name.replace('.svg', ''));
      reader.readAsText(file);
    }
  };

  svgDropzone.addEventListener('dragover', (e) => {
    e.preventDefault();
    svgDropzone.classList.add('dragover');
  });

  svgDropzone.addEventListener('dragleave', () => svgDropzone.classList.remove('dragover'));
  svgDropzone.addEventListener('drop', (e) => {
    e.preventDefault();
    svgDropzone.classList.remove('dragover');
    const file = e.dataTransfer.files[0];
    if (file && (file.type.includes('svg') || file.name.endsWith('.svg'))) {
      const reader = new FileReader();
      reader.onload = (event) => parseAndAddSVG(event.target.result, file.name.replace('.svg', ''));
      reader.readAsText(file);
    }
  });

  // Build SVG Presets
  SVG_PRESETS.forEach(preset => {
    const item = document.createElement('div');
    item.className = 'svg-preset-item';
    item.innerHTML = `${preset.svg}<span>${preset.name}</span>`;
    item.onclick = () => parseAndAddSVG(preset.svg, preset.name);
    svgPresetsContainer.appendChild(item);
  });

  // Raw SVG Paste Modal
  btnPasteSvg.onclick = () => {
    rawSvgTextarea.value = '';
    svgPasteModal.classList.add('active');
  };
  btnCloseSvgPaste.onclick = btnCancelSvgPaste.onclick = () => svgPasteModal.classList.remove('active');
  btnConfirmSvgPaste.onclick = () => {
    parseAndAddSVG(rawSvgTextarea.value, 'SVG Pegado');
    svgPasteModal.classList.remove('active');
  };

  // Active SVG Controls
  svgScale.addEventListener('input', (e) => {
    svgScaleVal.textContent = e.target.value;
    const layer = getActiveLayer();
    if (layer && layer.type === 'svg') {
      layer.scale = parseInt(e.target.value, 10) / 100;
      render();
    }
  });

  svgRotation.addEventListener('input', (e) => {
    svgRotationVal.textContent = e.target.value;
    const layer = getActiveLayer();
    if (layer && layer.type === 'svg') {
      layer.rotation = parseInt(e.target.value, 10);
      render();
    }
  });

  svgOpacity.addEventListener('input', (e) => {
    svgOpacityVal.textContent = e.target.value;
    const layer = getActiveLayer();
    if (layer && layer.type === 'svg') {
      layer.opacity = parseInt(e.target.value, 10) / 100;
      render();
    }
  });

  svgEnableTint.addEventListener('change', (e) => {
    const layer = getActiveLayer();
    if (layer && layer.type === 'svg') {
      layer.tint.enabled = e.target.checked;
      render();
    }
  });

  svgTintColor.addEventListener('input', (e) => {
    const layer = getActiveLayer();
    if (layer && layer.type === 'svg') {
      layer.tint.color = e.target.value;
      render();
    }
  });

  btnDeleteSvg.onclick = () => deleteActiveLayer();

  // Layers Toolbar
  btnLayerUp.onclick = () => {
    const idx = state.layers.findIndex(l => l.id === state.activeLayerId);
    if (idx < state.layers.length - 1) {
      const temp = state.layers[idx];
      state.layers[idx] = state.layers[idx + 1];
      state.layers[idx + 1] = temp;
      updateLayersUI();
      render();
    }
  };

  btnLayerDown.onclick = () => {
    const idx = state.layers.findIndex(l => l.id === state.activeLayerId);
    if (idx > 0) {
      const temp = state.layers[idx];
      state.layers[idx] = state.layers[idx - 1];
      state.layers[idx - 1] = temp;
      updateLayersUI();
      render();
    }
  };

  btnLayerDel.onclick = () => deleteActiveLayer();

  function deleteActiveLayer() {
    if (state.layers.length <= 1) {
      showToast('Debe haber al menos una capa en el diseño');
      return;
    }
    const idx = state.layers.findIndex(l => l.id === state.activeLayerId);
    if (idx !== -1) {
      state.layers.splice(idx, 1);
      state.activeLayerId = state.layers[Math.max(0, idx - 1)].id;
      syncControlsFromActiveLayer();
      updateLayersUI();
      render();
      showToast('Capa eliminada');
    }
  }

  // Floating Toolbar Controls
  document.getElementById('btn-center-element').onclick = () => {
    const layer = getActiveLayer();
    if (layer) {
      layer.x = canvas.width / 2;
      layer.y = canvas.height / 2;
      render();
      showToast('Elemento centrado');
    }
  };

  document.getElementById('btn-zoom-in').onclick = () => {
    state.zoom = Math.min(3, state.zoom + 0.15);
    zoomBadge.textContent = `${Math.round(state.zoom * 100)}%`;
    fitCanvasToStage();
  };

  document.getElementById('btn-zoom-out').onclick = () => {
    state.zoom = Math.max(0.4, state.zoom - 0.15);
    zoomBadge.textContent = `${Math.round(state.zoom * 100)}%`;
    fitCanvasToStage();
  };

  document.getElementById('btn-zoom-fit').onclick = () => {
    state.zoom = 1;
    zoomBadge.textContent = '100%';
    fitCanvasToStage();
  };

  document.getElementById('btn-toggle-grid').onclick = () => {
    state.showRuleOfThirds = !state.showRuleOfThirds;
    state.showCenterGuides = state.showRuleOfThirds;
    showRuleOfThirdsCheck.checked = state.showRuleOfThirds;
    showCenterGuidesCheck.checked = state.showCenterGuides;
    render();
    showToast(state.showRuleOfThirds ? 'Guías activadas' : 'Guías ocultas');
  };

  // Settings
  exportScaleSelect.onchange = (e) => {
    state.exportScale = parseInt(e.target.value, 10);
    updateCanvasDimensions();
  };

  showRuleOfThirdsCheck.onchange = (e) => {
    state.showRuleOfThirds = e.target.checked;
    render();
  };

  showCenterGuidesCheck.onchange = (e) => {
    state.showCenterGuides = e.target.checked;
    render();
  };

  transparentModeCheck.onchange = (e) => {
    state.transparentMode = e.target.checked;
    render();
    showToast(state.transparentMode ? 'Modo transparente activado' : 'Fondo visible activado');
  };

  // --- Camera & Photo Capture Module ---
  let currentFacingMode = 'environment';

  function openCameraModal() {
    cameraModal.classList.add('active');
  }

  btnCameraToggle.onclick = openCameraModal;
  btnCloseCamera.onclick = () => {
    stopCameraStream();
    cameraModal.classList.remove('active');
  };

  async function startCameraStream() {
    try {
      if (state.cameraStream) stopCameraStream();

      const constraints = {
        video: {
          facingMode: currentFacingMode,
          width: { ideal: 1920 },
          height: { ideal: 1080 }
        },
        audio: false
      };

      state.cameraStream = await navigator.mediaDevices.getUserMedia(constraints);
      liveCameraVideo.srcObject = state.cameraStream;
      btnCapturePhoto.disabled = false;
      btnStartCamera.textContent = 'Cámara Activa';
      btnSwitchCamera.style.display = 'inline-flex';
      showToast('Cámara conectada');
    } catch (err) {
      console.error('Error al acceder a la cámara:', err);
      showToast('No se pudo acceder a la cámara: ' + err.message, 4000);
    }
  }

  function stopCameraStream() {
    if (state.cameraStream) {
      state.cameraStream.getTracks().forEach(track => track.stop());
      state.cameraStream = null;
      liveCameraVideo.srcObject = null;
      btnCapturePhoto.disabled = true;
      btnStartCamera.textContent = 'Activar Cámara';
      btnSwitchCamera.style.display = 'none';
    }
  }

  btnStartCamera.onclick = startCameraStream;

  btnSwitchCamera.onclick = () => {
    currentFacingMode = currentFacingMode === 'environment' ? 'user' : 'environment';
    startCameraStream();
  };

  btnCapturePhoto.onclick = () => {
    if (!liveCameraVideo.videoWidth) return;

    const snapCanvas = document.createElement('canvas');
    snapCanvas.width = liveCameraVideo.videoWidth;
    snapCanvas.height = liveCameraVideo.videoHeight;
    const snapCtx = snapCanvas.getContext('2d');
    snapCtx.drawImage(liveCameraVideo, 0, 0);

    const img = new Image();
    img.onload = () => {
      state.bgPhoto = img;
      state.transparentMode = false;
      transparentModeCheck.checked = false;
      btnRemoveBgPhoto.style.display = 'block';
      stopCameraStream();
      cameraModal.classList.remove('active');
      render();
      showToast('¡Foto capturada como fondo del lienzo!');
    };
    img.src = snapCanvas.toDataURL('image/jpeg', 0.95);
  };

  bgPhotoUpload.onchange = (e) => {
    const file = e.target.files[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (event) => {
        const img = new Image();
        img.onload = () => {
          state.bgPhoto = img;
          state.transparentMode = false;
          transparentModeCheck.checked = false;
          btnRemoveBgPhoto.style.display = 'block';
          cameraModal.classList.remove('active');
          render();
          showToast('Foto cargada como fondo');
        };
        img.src = event.target.result;
      };
      reader.readAsDataURL(file);
    }
  };

  btnRemoveBgPhoto.onclick = () => {
    state.bgPhoto = null;
    state.transparentMode = true;
    transparentModeCheck.checked = true;
    btnRemoveBgPhoto.style.display = 'none';
    render();
    showToast('Foto de fondo eliminada (Modo transparente activo)');
  };

  // --- High-Resolution PNG Export & Sharing ---
  let generatedBlob = null;
  let generatedFileName = '';

  function triggerExport() {
    const config = RATIOS[state.currentRatio];
    const scale = state.exportScale;
    const exportW = config.width * scale;
    const exportH = config.height * scale;

    showToast('Generando PNG de alta calidad...', 1500);

    // Create offscreen export canvas
    const offCanvas = document.createElement('canvas');
    offCanvas.width = exportW;
    offCanvas.height = exportH;
    const offCtx = offCanvas.getContext('2d');

    // Render with scale factor and true alpha transparency
    render(offCtx, scale, true);

    offCanvas.toBlob((blob) => {
      if (!blob) {
        showToast('Error al generar la imagen');
        return;
      }

      generatedBlob = blob;
      const url = URL.createObjectURL(blob);
      exportPreviewImg.src = url;

      const dateStr = new Date().toISOString().slice(0, 10).replace(/-/g, '');
      const ratioStr = state.currentRatio.replace(':', 'x');
      const bgType = state.transparentMode ? 'transparente' : 'foto';
      generatedFileName = `doctopng_${ratioStr}_${bgType}_${dateStr}.png`;

      exportMetaInfo.innerHTML = `
        <strong>${exportW} × ${exportH} px</strong> • 
        Proporción: <strong>${state.currentRatio}</strong> • 
        ${state.transparentMode ? '<span style="color:#38bdf8">PNG Transparente</span>' : 'Fondo Completo'} • 
        ${(blob.size / 1024).toFixed(1)} KB
      `;

      // Check if Web Share API is available for image sharing (mobile devices)
      if (navigator.share && navigator.canShare && navigator.canShare({ files: [new File([blob], generatedFileName, { type: 'image/png' })] })) {
        btnModalShare.style.display = 'inline-flex';
      } else {
        btnModalShare.style.display = 'none';
      }

      exportModal.classList.add('active');
    }, 'image/png');
  }

  btnExportPng.onclick = triggerExport;
  btnCloseExport.onclick = () => exportModal.classList.remove('active');

  btnModalDownload.onclick = () => {
    if (!generatedBlob) return;
    const url = URL.createObjectURL(generatedBlob);
    const a = document.createElement('a');
    a.href = url;
    a.download = generatedFileName;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    showToast(`Descargando ${generatedFileName}`);
  };

  btnModalCopy.onclick = async () => {
    if (!generatedBlob) return;
    try {
      if (navigator.clipboard && window.ClipboardItem) {
        const item = new ClipboardItem({ 'image/png': generatedBlob });
        await navigator.clipboard.write([item]);
        showToast('¡Imagen copiada al portapapeles!');
      } else {
        showToast('El portapapeles de imágenes no es soportado por este navegador.');
      }
    } catch (err) {
      console.error('Error al copiar:', err);
      showToast('No se pudo copiar: ' + err.message);
    }
  };

  btnModalShare.onclick = async () => {
    if (!generatedBlob) return;
    try {
      const file = new File([generatedBlob], generatedFileName, { type: 'image/png' });
      await navigator.share({
        title: 'DocToPNG Export',
        text: 'Imagen generada con DocToPNG Studio',
        files: [file]
      });
      showToast('Compartido con éxito');
    } catch (err) {
      if (err.name !== 'AbortError') {
        showToast('Error al compartir: ' + err.message);
      }
    }
  };

  // --- Initial Setup ---
  function init() {
    // Add initial default text layer
    const textLayer = createDefaultTextLayer();
    state.layers.push(textLayer);

    // Add initial sample SVG preset (Sello Cámara watermark)
    const initialSvg = SVG_PRESETS[0];
    const blob = new Blob([initialSvg.svg], { type: 'image/svg+xml;charset=utf-8' });
    const blobUrl = URL.createObjectURL(blob);
    const img = new Image();
    img.onload = () => {
      const svgLayer = {
        id: uid('svg'),
        type: 'svg',
        title: initialSvg.name,
        rawSvg: initialSvg.svg,
        img: img,
        blobUrl: blobUrl,
        x: canvas.width / 2,
        y: canvas.height * 0.72,
        width: 140,
        height: 140,
        naturalWidth: 140,
        naturalHeight: 140,
        scale: 1,
        rotation: 0,
        opacity: 0.9,
        tint: {
          enabled: false,
          color: '#38bdf8'
        },
        visible: true
      };
      state.layers.push(svgLayer);
      updateLayersUI();
      render();
    };
    img.src = blobUrl;

    state.activeLayerId = textLayer.id;
    updateCanvasDimensions();
    syncControlsFromActiveLayer();
    updateLayersUI();

    window.addEventListener('resize', fitCanvasToStage);

    // Ensure fonts are loaded
    if (document.fonts) {
      document.fonts.ready.then(() => {
        render();
      });
    }
  }

  // Run initialization when DOM is loaded
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
