package com.example.color

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

object ColorDictionary {

    private data class NamedColor(
        val name: String,
        val r: Int,
        val g: Int,
        val b: Int,
        val l: Double,
        val a: Double,
        val labB: Double
    )

    private fun entry(name: String, hex: Int): NamedColor {
        val r = (hex shr 16) and 0xFF
        val g = (hex shr 8) and 0xFF
        val b = hex and 0xFF
        val (l, a, labB) = rgbToLab(r, g, b)
        return NamedColor(name, r, g, b, l, a, labB)
    }

    private val dictionary: List<NamedColor> = listOf(
        // Neutrals & Whites / Blacks
        entry("Pure White", 0xFFFFFF),
        entry("Alabaster", 0xF7F5EE),
        entry("Ivory Cream", 0xF5F1E3),
        entry("Warm Linen", 0xEDE6D6),
        entry("Silver Mist", 0xD8DCE2),
        entry("Cool Ash", 0xB8BDC6),
        entry("Pewter Gray", 0x8F959E),
        entry("Slate Gray", 0x676E79),
        entry("Graphite", 0x454952),
        entry("Obsidian", 0x22252A),
        entry("Jet Black", 0x0F1012),
        entry("Warm Taupe", 0xA89989),
        entry("Espresso", 0x3D2B24),
        entry("Sandstone", 0xD2BFA7),

        // Reds & Corals
        entry("Crimson", 0xDC143C),
        entry("Ruby Red", 0xC81D3A),
        entry("Scarlet", 0xEE2B2B),
        entry("Cherry Blossom", 0xF4B6C2),
        entry("Coral Reef", 0xFF6B5B),
        entry("Warm Coral", 0xF26A5A),
        entry("Terracotta", 0xD35E42),
        entry("Burnt Sienna", 0xB84A2E),
        entry("Brick Red", 0xA4372D),
        entry("Burgundy", 0x7A1C2E),
        entry("Maroon", 0x5C121E),
        entry("Salmon Pink", 0xFA8A76),
        entry("Vermilion", 0xE6482E),
        entry("Rosewood", 0x8B434B),

        // Oranges & Amber
        entry("Tangerine", 0xF97316),
        entry("Sunset Orange", 0xFF6B35),
        entry("Apricot", 0xFBB078),
        entry("Peach Fuzz", 0xFFBE98),
        entry("Rust Copper", 0xB85327),
        entry("Amber Gold", 0xF59E0B),
        entry("Marigold", 0xFBBF24),
        entry("Ochre", 0xCC8223),
        entry("Cinnamon", 0x9C5B2C),
        entry("Caramel", 0xC67D3E),
        entry("Honeycomb", 0xE5A93B),
        entry("Papaya", 0xFD8D5B),

        // Yellows
        entry("Sunburst Yellow", 0xFACC15),
        entry("Lemon Zest", 0xFDE047),
        entry("Canary", 0xFFEA4C),
        entry("Buttercream", 0xFEF3C7),
        entry("Mustard Seed", 0xCA8A04),
        entry("Chartreuse", 0xA3E635),
        entry("Saffron", 0xEAB308),
        entry("Champagne", 0xEFE0B9),

        // Greens
        entry("Lime Punch", 0x84CC16),
        entry("Meadow Green", 0x4ADE80),
        entry("Emerald", 0x10B981),
        entry("Kelly Green", 0x22C55E),
        entry("Forest Green", 0x1E6B43),
        entry("Pine Needle", 0x184E37),
        entry("Sage Green", 0x84A98C),
        entry("Eucalyptus", 0x6B9E8A),
        entry("Olive Grove", 0x6F7D3C),
        entry("Moss Green", 0x556B2F),
        entry("Mint Leaf", 0x6EE7B7),
        entry("Pistachio", 0xA7D489),
        entry("Jade", 0x00A878),
        entry("Matcha", 0x9AB973),

        // Teals & Cyans
        entry("Seafoam", 0x5EEAD4),
        entry("Aquamarine", 0x2DD4BF),
        entry("Turquoise", 0x06B6D4),
        entry("Deep Teal", 0x0F766E),
        entry("Lagoon", 0x0D9488),
        entry("Peacock Blue", 0x0E7490),
        entry("Cyan Spark", 0x22D3EE),
        entry("Ice Blue", 0xBAE6FD),

        // Blues
        entry("Sky Blue", 0x38BDF8),
        entry("Cerulean", 0x0284C7),
        entry("Azure", 0x2563EB),
        entry("Cobalt Blue", 0x1D4ED8),
        entry("Royal Blue", 0x2B4EE6),
        entry("Sapphire", 0x1E40AF),
        entry("Midnight Navy", 0x172554),
        entry("Denim Blue", 0x4B6B9B),
        entry("Cornflower", 0x6495ED),
        entry("Periwinkle", 0x8C9EFF),
        entry("Powder Blue", 0x93C5FD),
        entry("Steel Blue", 0x4682B4),

        // Purples & Violets
        entry("Indigo", 0x4F46E5),
        entry("Electric Violet", 0x7C3AED),
        entry("Amethyst", 0x9333EA),
        entry("Plum", 0x6B21A8),
        entry("Mulberry", 0x831843),
        entry("Lavender Mist", 0xC4B5FD),
        entry("Lilac", 0xD8B4FE),
        entry("Orchid", 0xC084FC),
        entry("Grapevine", 0x581C87),

        // Pinks & Magentas
        entry("Magenta", 0xD946EF),
        entry("Fuchsia", 0xEC4899),
        entry("Hot Pink", 0xF43F5E),
        entry("Raspberry", 0xBE185D),
        entry("Blush Rose", 0xFDA4AF),
        entry("Dusty Rose", 0xC07A86),
        entry("Mauve", 0xB3889E),
        entry("Flamingo", 0xFB7185)
    )

    fun identifyColor(argb: Int): IdentifiedColor {
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        val opaqueArgb = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        val (h, s, l) = rgbToHsl(r, g, b)
        val cmyk = rgbToCmyk(r, g, b)
        val whiteContrast = contrastRatio(opaqueArgb, 0xFFFFFFFF.toInt())
        val blackContrast = contrastRatio(opaqueArgb, 0xFF111215.toInt())
        val onSwatch = if (whiteContrast >= blackContrast) 0xFFFFFFFF.toInt() else 0xFF111215.toInt()

        return IdentifiedColor(
            argb = opaqueArgb,
            hex = formatHex(opaqueArgb),
            name = findClosestColorName(r, g, b),
            family = classifyColorFamily(h, s, l),
            red = r,
            green = g,
            blue = b,
            hue = h,
            saturation = s,
            lightness = l,
            cmyk = cmyk,
            whiteContrast = whiteContrast,
            blackContrast = blackContrast,
            onSwatchArgb = onSwatch
        )
    }

    fun formatHex(argb: Int): String {
        val rgb = argb and 0x00FFFFFF
        return String.format("#%06X", rgb)
    }

    fun findClosestColorName(r: Int, g: Int, b: Int): String {
        val (l, a, labB) = rgbToLab(r, g, b)
        var best = dictionary.first()
        var bestDist = Double.MAX_VALUE
        for (item in dictionary) {
            val dl = l - item.l
            val da = a - item.a
            val db = labB - item.labB
            val dist = dl * dl + da * da + db * db
            if (dist < bestDist) {
                bestDist = dist
                best = item
            }
        }
        return best.name
    }

    fun classifyColorFamily(hue: Float, saturation: Float, lightness: Float): ColorFamily {
        if (saturation < 12f || lightness < 9f || lightness > 95f) {
            return ColorFamily.NEUTRAL
        }
        val h = ((hue % 360f) + 360f) % 360f
        return when {
            h < 15f || h >= 340f -> ColorFamily.RED
            h < 42f -> ColorFamily.ORANGE
            h < 68f -> ColorFamily.YELLOW
            h < 155f -> ColorFamily.GREEN
            h < 192f -> ColorFamily.TEAL
            h < 255f -> ColorFamily.BLUE
            h < 295f -> ColorFamily.PURPLE
            else -> ColorFamily.PINK
        }
    }

    fun rgbToHsl(r: Int, g: Int, b: Int): Triple<Float, Float, Float> {
        val rf = r / 255f
        val gf = g / 255f
        val bf = b / 255f
        val maxC = max(rf, max(gf, bf))
        val minC = min(rf, min(gf, bf))
        val delta = maxC - minC

        val l = (maxC + minC) / 2f
        val s = if (delta == 0f) 0f else delta / (1f - abs(2f * l - 1f))
        val h = when {
            delta == 0f -> 0f
            maxC == rf -> 60f * (((gf - bf) / delta) % 6f)
            maxC == gf -> 60f * (((bf - rf) / delta) + 2f)
            else -> 60f * (((rf - gf) / delta) + 4f)
        }.let { ((it % 360f) + 360f) % 360f }

        return Triple(h, (s * 100f).coerceIn(0f, 100f), (l * 100f).coerceIn(0f, 100f))
    }

    fun hslToArgb(hue: Float, saturationPercent: Float, lightnessPercent: Float): Int {
        val h = ((hue % 360f) + 360f) % 360f
        val s = (saturationPercent / 100f).coerceIn(0f, 1f)
        val l = (lightnessPercent / 100f).coerceIn(0f, 1f)

        val c = (1f - abs(2f * l - 1f)) * s
        val x = c * (1f - abs((h / 60f) % 2f - 1f))
        val m = l - c / 2f

        val (rf, gf, bf) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        val r = ((rf + m) * 255f).roundToInt().coerceIn(0, 255)
        val g = ((gf + m) * 255f).roundToInt().coerceIn(0, 255)
        val b = ((bf + m) * 255f).roundToInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    fun rgbToCmyk(r: Int, g: Int, b: Int): CmykColor {
        if (r == 0 && g == 0 && b == 0) return CmykColor(0, 0, 0, 100)
        val rf = r / 255f
        val gf = g / 255f
        val bf = b / 255f
        val k = 1f - max(rf, max(gf, bf))
        val denom = 1f - k
        if (denom <= 0.0001f) return CmykColor(0, 0, 0, 100)
        val c = ((1f - rf - k) / denom * 100f).roundToInt().coerceIn(0, 100)
        val m = ((1f - gf - k) / denom * 100f).roundToInt().coerceIn(0, 100)
        val y = ((1f - bf - k) / denom * 100f).roundToInt().coerceIn(0, 100)
        val kp = (k * 100f).roundToInt().coerceIn(0, 100)
        return CmykColor(c, m, y, kp)
    }

    fun rgbToLab(r: Int, g: Int, b: Int): Triple<Double, Double, Double> {
        fun linearize(channel: Int): Double {
            val v = channel / 255.0
            return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        }
        val rl = linearize(r)
        val gl = linearize(g)
        val bl = linearize(b)

        // sRGB to XYZ (D65)
        val x = (rl * 0.4124564 + gl * 0.3575761 + bl * 0.1804375) / 0.95047
        val y = (rl * 0.2126729 + gl * 0.7151522 + bl * 0.0721750) / 1.00000
        val z = (rl * 0.0193339 + gl * 0.1191920 + bl * 0.9503041) / 1.08883

        fun f(t: Double): Double = if (t > 0.008856) t.pow(1.0 / 3.0) else (7.787 * t) + (16.0 / 116.0)
        val fx = f(x)
        val fy = f(y)
        val fz = f(z)

        val l = 116.0 * fy - 16.0
        val a = 500.0 * (fx - fy)
        val labB = 200.0 * (fy - fz)
        return Triple(l, a, labB)
    }

    fun labDistanceSq(r1: Int, g1: Int, b1: Int, r2: Int, g2: Int, b2: Int): Double {
        val (l1, a1, lb1) = rgbToLab(r1, g1, b1)
        val (l2, a2, lb2) = rgbToLab(r2, g2, b2)
        val dl = l1 - l2
        val da = a1 - a2
        val db = lb1 - lb2
        return dl * dl + da * da + db * db
    }

    fun relativeLuminance(argb: Int): Double {
        fun lin(c: Int): Double {
            val s = c / 255.0
            return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        val r = lin((argb shr 16) and 0xFF)
        val g = lin((argb shr 8) and 0xFF)
        val b = lin(argb and 0xFF)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    fun contrastRatio(argb1: Int, argb2: Int): Float {
        val l1 = relativeLuminance(argb1)
        val l2 = relativeLuminance(argb2)
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return ((lighter + 0.05) / (darker + 0.05)).toFloat()
    }

    fun contrastingTextArgb(backgroundArgb: Int): Int {
        val whiteC = contrastRatio(backgroundArgb, 0xFFFFFFFF.toInt())
        val darkC = contrastRatio(backgroundArgb, 0xFF111215.toInt())
        return if (whiteC >= darkC) 0xFFFFFFFF.toInt() else 0xFF111215.toInt()
    }

    fun sqrtDist(dSq: Double): Double = sqrt(dSq)
}
