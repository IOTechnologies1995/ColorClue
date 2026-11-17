package com.example

import com.example.color.ColorDictionary
import com.example.color.ColorFamily
import com.example.color.HarmonyType
import com.example.color.PaletteGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorClueEngineTest {

    @Test
    fun identifyColor_exactCoral_formatsHexAndNameCorrectly() {
        val coralArgb = 0xFFFF6B5B.toInt()
        val identified = ColorDictionary.identifyColor(coralArgb)

        assertEquals("#FF6B5B", identified.hex)
        assertEquals(255, identified.red)
        assertEquals(107, identified.green)
        assertEquals(91, identified.blue)
        assertEquals("Coral Reef", identified.name)
        assertEquals(ColorFamily.RED, identified.family)
    }

    @Test
    fun rgbToHsl_and_hslToArgb_roundTripsPrimaryColors() {
        val cobalt = ColorDictionary.identifyColor(0xFF2563EB.toInt())
        val roundTrip = ColorDictionary.hslToArgb(cobalt.hue, cobalt.saturation, cobalt.lightness)
        val rDiff = kotlin.math.abs(((roundTrip shr 16) and 0xFF) - cobalt.red)
        val gDiff = kotlin.math.abs(((roundTrip shr 8) and 0xFF) - cobalt.green)
        val bDiff = kotlin.math.abs((roundTrip and 0xFF) - cobalt.blue)

        assertTrue(rDiff <= 2 && gDiff <= 2 && bDiff <= 2)
    }

    @Test
    fun paletteGenerator_producesAllSixHarmoniesWithFiveSwatchesEach() {
        val emerald = ColorDictionary.identifyColor(0xFF10B981.toInt())
        val palettes = PaletteGenerator.generatePalettes(emerald)

        assertEquals(HarmonyType.entries.size, palettes.size)
        palettes.forEach { palette ->
            assertEquals(5, palette.swatches.size)
            assertTrue(palette.swatches.any { it.hex == "#10B981" && it.role == "Captured" })
            palette.swatches.forEach { swatch ->
                assertTrue(swatch.hex.matches(Regex("^#[0-9A-F]{6}$")))
            }
        }
    }
}
