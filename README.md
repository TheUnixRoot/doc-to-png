# DocToPNG Studio 🎨📸

**DocToPNG** es una aplicación moderna para diseñar composiciones tipográficas y vectoriales con soporte para fuentes `.ttf`, gráficos `.svg` e imágenes `.png`, exportando en **PNG transparente de alta definición (Canal Alfa)** en proporciones **4:3**, **1:1** y **3:4**.

Ideal para crear sellos fotográficos, marcas de agua, stickers, firmas, subtítulos tipográficos y gráficos para superponer sobre fotos tomadas con la cámara o exportar de forma aislada.

---

## ✨ Características Principales

- **Editor Tipográfico Avanzado:**
  - Soporte dinámico para cargar cualquier fuente tipográfica externa **`.ttf`** y **`.otf`** desde el almacenamiento.
  - Tamaños desde microfuentes (**4 sp**) hasta títulos grandes (**180 sp**) con saltos de precisión `[-]` / `[+]` y chips rápidos.
  - Alineación completa: **Izquierda**, **Centrado**, **Derecha** y **Justificado** (distribución inter-palabra precisa en párrafos).
  - Controles de tracking (espaciado entre letras), interlineado, rotación, negrita, cursiva y mayúsculas.
  - Paleta de color, contorno/stroke exterior, sombra/resplandor difuminado y caja resaltada (pill box).

- **Gráficos Vectoriales `.svg` & Stickers `.png`:**
  - Importación de archivos `.svg` vectoriales manteniendo el **100% de sus colores originales, gradientes y trazos** (con opción voluntaria de teñir monocromático).
  - Importación de imágenes y sellos **`.png` transparentes** como capas interactivas.
  - Biblioteca integrada de sellos fotográficos, marcas de agua EXIF, marcos de esquinas y diafragmas.
  - Transformaciones táctiles: escalado, rotación y opacidad por capa.

- **Cámara & Fotos ("Escribir texto y sacar fotos"):**
  - Acceso directo a la cámara del dispositivo para capturar fotos de referencia y componer sobre ellas en vivo.
  - Carga de fotos desde la galería.

- **Proporciones de Salida y Exportación Transparente:**
  - **3:4** (1080 × 1440 px / Retrato y redes sociales)
  - **1:1** (1080 × 1080 px / Formato cuadrado)
  - **4:3** (1440 × 1080 px / Paisaje y cámara clásica)
  - **Modo PNG Transparente:** exporta exclusivamente las capas de texto, SVG y PNG con canal alfa puro (0 alpha), listo para usar en Photoshop, Lightroom o redes sociales.
  - Guardado en dispositivo (`Pictures/DocToPNG`) y diálogo de compartir inmediato.

---

## 🏗️ Arquitectura y Tecnologías

- **Android App:**
  - Kotlin + Jetpack Compose
  - Arquitectura MVVM (StateFlow, Coroutines)
  - Motor de renderizado Canvas en mapa de bits ARGB_8888 de alta resolución
  - [AndroidSVG](https://bigbadaboom.github.io/androidsvg/) + [Coil](https://coil-kt.github.io/coil/)
- **Web App Companion:**
  - HTML5 Canvas API + Web Typography + Touch drag controls

---

## 🚀 Compilación (Android / Gradle)

```bash
# Compilar APK debug
./gradlew assembleDebug

# El APK generado se ubica en:
app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 Licencia

MIT License.
