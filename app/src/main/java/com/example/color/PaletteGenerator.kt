package com.example.color

object PaletteGenerator {

    fun generatePalettes(base: IdentifiedColor): List<RecommendedPalette> {
        val h = base.hue
        // For near-neutral captured colors, lift saturation slightly in harmonic accents
        // so complementary recommendations remain expressive and useful.
        val workingSat = if (base.saturation < 14f) 28f else base.saturation.coerceIn(22f, 92f)
        val baseL = base.lightness.coerceIn(18f, 82f)

        val capturedSwatch = makeSwatch(base.argb, "Captured")

        // 1. Complementary (0, +180 + warm/cool tonal bridge)
        val compHue = wrapHue(h + 180f)
        val complementary = RecommendedPalette(
            type = HarmonyType.COMPLEMENTARY,
            swatches = listOf(
                capturedSwatch,
                swatchFromHsl(h, (workingSat * 0.75f).coerceIn(15f, 85f), (baseL + 22f).coerceAtMost(90f), "Light Tint"),
                swatchFromHsl(compHue, workingSat, baseL, "Complement"),
                swatchFromHsl(compHue, (workingSat * 0.85f).coerceIn(18f, 88f), (baseL - 18f).coerceAtLeast(16f), "Deep Accent"),
                swatchFromHsl(compHue, (workingSat * 0.30f).coerceIn(8f, 32f), if (baseL > 50f) 18f else 88f, "Anchor")
            )
        )

        // 2. Split-Complementary (0, +150, +210)
        val split1 = wrapHue(h + 150f)
        val split2 = wrapHue(h + 210f)
        val splitComp = RecommendedPalette(
            type = HarmonyType.SPLIT_COMPLEMENTARY,
            swatches = listOf(
                capturedSwatch,
                swatchFromHsl(h, (workingSat * 0.45f).coerceIn(12f, 55f), (baseL + 24f).coerceAtMost(92f), "Soft Base"),
                swatchFromHsl(split1, workingSat, baseL.coerceIn(32f, 70f), "Split A"),
                swatchFromHsl(split2, workingSat, baseL.coerceIn(30f, 68f), "Split B"),
                swatchFromHsl(split2, (workingSat * 0.65f).coerceIn(14f, 65f), 20f, "Deep Shade")
            )
        )

        // 3. Analogous (-30, -15, 0, +15, +30)
        val analogous = RecommendedPalette(
            type = HarmonyType.ANALOGOUS,
            swatches = listOf(
                swatchFromHsl(wrapHue(h - 32f), workingSat, (baseL - 8f).coerceAtLeast(20f), "-32° Hue"),
                swatchFromHsl(wrapHue(h - 16f), workingSat, (baseL + 8f).coerceAtMost(84f), "-16° Hue"),
                capturedSwatch,
                swatchFromHsl(wrapHue(h + 16f), workingSat, (baseL + 6f).coerceAtMost(84f), "+16° Hue"),
                swatchFromHsl(wrapHue(h + 32f), workingSat, (baseL - 10f).coerceAtLeast(20f), "+32° Hue")
            )
        )

        // 4. Triadic (0, +120, +240)
        val tri1 = wrapHue(h + 120f)
        val tri2 = wrapHue(h + 240f)
        val triadic = RecommendedPalette(
            type = HarmonyType.TRIADIC,
            swatches = listOf(
                capturedSwatch,
                swatchFromHsl(tri1, workingSat, baseL.coerceIn(34f, 70f), "+120° Triad"),
                swatchFromHsl(tri1, (workingSat * 0.55f).coerceIn(15f, 60f), 84f, "Triad Tint"),
                swatchFromHsl(tri2, workingSat, baseL.coerceIn(32f, 68f), "+240° Triad"),
                swatchFromHsl(tri2, (workingSat * 0.70f).coerceIn(18f, 75f), 22f, "Triad Shade")
            )
        )

        // 5. Tetradic Square (0, +90, +180, +270 + neutral)
        val tetradic = RecommendedPalette(
            type = HarmonyType.TETRADIC,
            swatches = listOf(
                capturedSwatch,
                swatchFromHsl(wrapHue(h + 90f), workingSat, baseL.coerceIn(36f, 68f), "+90°"),
                swatchFromHsl(wrapHue(h + 180f), workingSat, baseL.coerceIn(34f, 68f), "+180°"),
                swatchFromHsl(wrapHue(h + 270f), workingSat, baseL.coerceIn(36f, 68f), "+270°"),
                swatchFromHsl(h, 10f, if (baseL > 52f) 16f else 92f, "Neutral")
            )
        )

        // 6. Monochromatic Studio
        val monoSat = base.saturation.coerceIn(6f, 92f)
        val monochromatic = RecommendedPalette(
            type = HarmonyType.MONOCHROMATIC,
            swatches = listOf(
                swatchFromHsl(h, (monoSat * 0.90f).coerceIn(4f, 90f), 16f, "Deep Shade"),
                swatchFromHsl(h, monoSat, 34f, "Mid Dark"),
                capturedSwatch,
                swatchFromHsl(h, (monoSat * 0.78f).coerceIn(4f, 80f), 72f, "Soft Tone"),
                swatchFromHsl(h, (monoSat * 0.55f).coerceIn(4f, 65f), 90f, "Highlight")
            )
        )

        return listOf(complementary, splitComp, analogous, triadic, tetradic, monochromatic)
    }

    private fun swatchFromHsl(h: Float, s: Float, l: Float, role: String): PaletteSwatch {
        val argb = ColorDictionary.hslToArgb(h, s, l)
        return makeSwatch(argb, role)
    }

    private fun makeSwatch(argb: Int, role: String): PaletteSwatch {
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        val opaque = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        return PaletteSwatch(
            argb = opaque,
            hex = ColorDictionary.formatHex(opaque),
            name = ColorDictionary.findClosestColorName(r, g, b),
            role = role,
            onSwatchArgb = ColorDictionary.contrastingTextArgb(opaque)
        )
    }

    private fun wrapHue(hue: Float): Float = ((hue % 360f) + 360f) % 360f
}
