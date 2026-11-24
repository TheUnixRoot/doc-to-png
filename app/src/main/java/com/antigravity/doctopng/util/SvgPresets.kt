package com.antigravity.doctopng.util

object SvgPresets {

    data class Preset(val id: String, val title: String, val svgContent: String)

    val list: List<Preset> = listOf(
        Preset(
            id = "camera_seal",
            title = "Sello Fotográfico",
            svgContent = """
                <svg viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">
                  <circle cx="50" cy="50" r="46" fill="none" stroke="#FFFFFF" stroke-width="2.5" stroke-dasharray="2 3"/>
                  <circle cx="50" cy="50" r="41" fill="none" stroke="#FFFFFF" stroke-width="1.5"/>
                  <path d="M35 42h30v26H35z" fill="none" stroke="#FFFFFF" stroke-width="2"/>
                  <circle cx="50" cy="55" r="7" fill="none" stroke="#FFFFFF" stroke-width="2"/>
                  <path d="M42 37h16l2 5H40z" fill="none" stroke="#FFFFFF" stroke-width="1.5"/>
                  <circle cx="60" cy="46" r="1.5" fill="#FFFFFF"/>
                  <text x="50" y="28" font-size="6.5" font-weight="bold" text-anchor="middle" fill="#FFFFFF" letter-spacing="1">ORIGINAL SHOT</text>
                  <text x="50" y="80" font-size="6.5" font-weight="bold" text-anchor="middle" fill="#FFFFFF" letter-spacing="1.5">LIMITED EDITION</text>
                </svg>
            """.trimIndent()
        ),
        Preset(
            id = "corners_frame",
            title = "Marco Esquinas",
            svgContent = """
                <svg viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">
                  <path d="M10 25 V10 H25" fill="none" stroke="#FFFFFF" stroke-width="3" stroke-linecap="round"/>
                  <path d="M75 10 H90 V25" fill="none" stroke="#FFFFFF" stroke-width="3" stroke-linecap="round"/>
                  <path d="M90 75 V90 H75" fill="none" stroke="#FFFFFF" stroke-width="3" stroke-linecap="round"/>
                  <path d="M25 90 H10 V75" fill="none" stroke="#FFFFFF" stroke-width="3" stroke-linecap="round"/>
                  <circle cx="50" cy="50" r="2" fill="#FFFFFF"/>
                  <line x1="44" y1="50" x2="56" y2="50" stroke="#FFFFFF" stroke-width="1"/>
                  <line x1="50" y1="44" x2="50" y2="56" stroke="#FFFFFF" stroke-width="1"/>
                </svg>
            """.trimIndent()
        ),
        Preset(
            id = "date_badge",
            title = "Marca de Agua EXIF",
            svgContent = """
                <svg viewBox="0 0 160 50" xmlns="http://www.w3.org/2000/svg">
                  <rect x="2" y="2" width="156" height="46" rx="8" fill="none" stroke="#FFFFFF" stroke-width="2"/>
                  <circle cx="22" cy="25" r="10" fill="none" stroke="#FFFFFF" stroke-width="1.5"/>
                  <polyline points="22,19 22,25 26,27" fill="none" stroke="#FFFFFF" stroke-width="1.5"/>
                  <text x="40" y="24" font-size="10" font-weight="bold" fill="#FFFFFF" letter-spacing="1">RAW • 24MM</text>
                  <text x="40" y="37" font-size="9" fill="#FFFFFF" opacity="0.85">ISO 100  f/1.8  1/500s</text>
                </svg>
            """.trimIndent()
        ),
        Preset(
            id = "aperture_lens",
            title = "Diafragma Lente",
            svgContent = """
                <svg viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">
                  <circle cx="50" cy="50" r="46" fill="none" stroke="#FFFFFF" stroke-width="3"/>
                  <line x1="50" y1="4" x2="80" y2="70" stroke="#FFFFFF" stroke-width="2"/>
                  <line x1="96" y1="50" x2="30" y2="80" stroke="#FFFFFF" stroke-width="2"/>
                  <line x1="50" y1="96" x2="20" y2="30" stroke="#FFFFFF" stroke-width="2"/>
                  <line x1="4" y1="50" x2="70" y2="20" stroke="#FFFFFF" stroke-width="2"/>
                </svg>
            """.trimIndent()
        ),
        Preset(
            id = "brutalist_star",
            title = "Estrella Brutalista",
            svgContent = """
                <svg viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">
                  <polygon points="50,5 62,38 95,50 62,62 50,95 38,62 5,50 38,38" fill="#FFFFFF"/>
                </svg>
            """.trimIndent()
        ),
        Preset(
            id = "quote_mark",
            title = "Comillas Editoriales",
            svgContent = """
                <svg viewBox="0 0 100 80" xmlns="http://www.w3.org/2000/svg">
                  <path d="M15 45 c0-15 10-35 30-40 l5 8 c-12 5-18 15-18 24 h13 v35 H15 Z" fill="#FFFFFF"/>
                  <path d="M55 45 c0-15 10-35 30-40 l5 8 c-12 5-18 15-18 24 h13 v35 H55 Z" fill="#FFFFFF"/>
                </svg>
            """.trimIndent()
        )
    )
}
