package com.example.color

data class CmykColor(
    val cyan: Int,
    val magenta: Int,
    val yellow: Int,
    val key: Int
) {
    fun formatted(): String = "$cyan%, $magenta%, $yellow%, $key%"
}

enum class ColorFamily(val label: String) {
    RED("Reds"),
    ORANGE("Oranges"),
    YELLOW("Yellows"),
    GREEN("Greens"),
    TEAL("Teals"),
    BLUE("Blues"),
    PURPLE("Purples"),
    PINK("Pinks"),
    NEUTRAL("Neutrals")
}

data class IdentifiedColor(
    val argb: Int,
    val hex: String,
    val name: String,
    val family: ColorFamily,
    val red: Int,
    val green: Int,
    val blue: Int,
    val hue: Float,
    val saturation: Float,
    val lightness: Float,
    val cmyk: CmykColor,
    val whiteContrast: Float,
    val blackContrast: Float,
    val onSwatchArgb: Int
)

enum class SamplingRadius(val label: String, val pixelRadius: Int) {
    PINPOINT("1px", 0),
    BALANCED("9px", 4),
    AREA("25px", 12)
}

data class DetectedColorSpot(
    val color: IdentifiedColor,
    val normX: Float,
    val normY: Float,
    val dominancePercent: Int
)

enum class HarmonyType(
    val title: String,
    val subtitle: String,
    val hueRule: String
) {
    COMPLEMENTARY(
        title = "Complementary",
        subtitle = "High-contrast opposite hue balance",
        hueRule = "0° / +180°"
    ),
    SPLIT_COMPLEMENTARY(
        title = "Split-Complementary",
        subtitle = "Vibrant tension with softer harmony",
        hueRule = "0° / +150° / +210°"
    ),
    ANALOGOUS(
        title = "Analogous",
        subtitle = "Adjacent hues for cohesive mood",
        hueRule = "-30° to +30°"
    ),
    TRIADIC(
        title = "Triadic",
        subtitle = "Evenly spaced 3-point color wheel",
        hueRule = "0° / +120° / +240°"
    ),
    TETRADIC(
        title = "Tetradic Square",
        subtitle = "Four-way balanced accent spectrum",
        hueRule = "0° / +90° / +180° / +270°"
    ),
    MONOCHROMATIC(
        title = "Monochromatic Studio",
        subtitle = "Layered shades & tints of your hue",
        hueRule = "Same Hue"
    )
}

data class PaletteSwatch(
    val argb: Int,
    val hex: String,
    val name: String,
    val role: String,
    val onSwatchArgb: Int
)

data class RecommendedPalette(
    val type: HarmonyType,
    val swatches: List<PaletteSwatch>
)
