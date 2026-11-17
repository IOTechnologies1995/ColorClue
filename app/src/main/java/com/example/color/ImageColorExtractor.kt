package com.example.color

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object ImageColorExtractor {

    /**
     * Ultra-fast center pixel sampler for CameraX ImageAnalysis configured with
     * ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888.
     */
    fun sampleCenterFromRgbaProxy(imageProxy: ImageProxy): IdentifiedColor? {
        if (imageProxy.planes.isEmpty()) return null
        val plane = imageProxy.planes[0]
        val buffer: ByteBuffer = plane.buffer
        val width = imageProxy.width
        val height = imageProxy.height
        if (width <= 0 || height <= 0) return null

        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride

        val cx = width / 2
        val cy = height / 2
        val radius = 6

        var sumR = 0L
        var sumG = 0L
        var sumB = 0L
        var count = 0

        val startY = max(0, cy - radius)
        val endY = min(height - 1, cy + radius)
        val startX = max(0, cx - radius)
        val endX = min(width - 1, cx + radius)

        for (y in startY..endY step 2) {
            val rowOffset = y * rowStride
            for (x in startX..endX step 2) {
                val offset = rowOffset + x * pixelStride
                if (offset + 2 < buffer.limit()) {
                    val r = buffer.get(offset).toInt() and 0xFF
                    val g = buffer.get(offset + 1).toInt() and 0xFF
                    val b = buffer.get(offset + 2).toInt() and 0xFF
                    sumR += r
                    sumG += g
                    sumB += b
                    count++
                }
            }
        }

        if (count == 0) return null
        val avgR = (sumR / count).toInt().coerceIn(0, 255)
        val avgG = (sumG / count).toInt().coerceIn(0, 255)
        val avgB = (sumB / count).toInt().coerceIn(0, 255)
        val argb = (0xFF shl 24) or (avgR shl 16) or (avgG shl 8) or avgB
        return ColorDictionary.identifyColor(argb)
    }

    /**
     * Converts a captured CameraX ImageProxy (JPEG or RGBA_8888) into a rotated software ARGB_8888 Bitmap.
     */
    fun imageProxyToBitmap(imageProxy: ImageProxy, mirrorHorizontally: Boolean = false): Bitmap? {
        val rawBitmap = when (imageProxy.format) {
            ImageFormat.JPEG -> {
                val buffer = imageProxy.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                val opts = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            }
            else -> {
                // Convert to Bitmap via CameraX helper or YUV/RGBA fallback
                runCatching { imageProxy.toBitmap() }.getOrNull()
            }
        } ?: return null

        val scaled = downscaleBitmap(rawBitmap, 1440)
        val rotation = imageProxy.imageInfo.rotationDegrees.toFloat()
        if (rotation == 0f && !mirrorHorizontally && scaled.config == Bitmap.Config.ARGB_8888) {
            return scaled
        }

        val matrix = Matrix().apply {
            if (rotation != 0f) postRotate(rotation)
            if (mirrorHorizontally) postScale(-1f, 1f)
        }
        val transformed = Bitmap.createBitmap(
            scaled, 0, 0, scaled.width, scaled.height, matrix, true
        )
        if (transformed !== scaled) scaled.recycle()
        return ensureSoftwareArgb8888(transformed)
    }

    fun decodeBitmapFromUri(context: Context, uri: Uri, maxDimension: Int = 1440): Bitmap? {
        return try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val w = info.size.width
                    val h = info.size.height
                    val maxEdge = max(w, h)
                    if (maxEdge > maxDimension) {
                        val scale = maxDimension.toFloat() / maxEdge.toFloat()
                        decoder.setTargetSize(
                            (w * scale).roundToInt().coerceAtLeast(1),
                            (h * scale).roundToInt().coerceAtLeast(1)
                        )
                    }
                }
            } else {
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, boundsOpts)
                }
                val maxEdge = max(boundsOpts.outWidth, boundsOpts.outHeight)
                var sampleSize = 1
                while (maxEdge / sampleSize > maxDimension) {
                    sampleSize *= 2
                }
                val decodeOpts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, decodeOpts)
                }
            }
            bitmap?.let { downscaleBitmap(ensureSoftwareArgb8888(it), maxDimension) }
        } catch (_: Exception) {
            null
        }
    }

    fun sampleColorAtNormalizedPoint(
        bitmap: Bitmap,
        normX: Float,
        normY: Float,
        radius: SamplingRadius
    ): IdentifiedColor {
        val w = bitmap.width
        val h = bitmap.height
        val px = (normX.coerceIn(0f, 1f) * (w - 1)).roundToInt().coerceIn(0, w - 1)
        val py = (normY.coerceIn(0f, 1f) * (h - 1)).roundToInt().coerceIn(0, h - 1)

        val r = radius.pixelRadius
        if (r <= 0) {
            return ColorDictionary.identifyColor(bitmap.getPixel(px, py))
        }

        val minX = max(0, px - r)
        val maxX = min(w - 1, px + r)
        val minY = max(0, py - r)
        val maxY = min(h - 1, py + r)

        var sumR = 0L
        var sumG = 0L
        var sumB = 0L
        var count = 0

        for (y in minY..maxY) {
            for (x in minX..maxX) {
                val pixel = bitmap.getPixel(x, y)
                sumR += (pixel shr 16) and 0xFF
                sumG += (pixel shr 8) and 0xFF
                sumB += pixel and 0xFF
                count++
            }
        }

        val avgR = (sumR / count).toInt().coerceIn(0, 255)
        val avgG = (sumG / count).toInt().coerceIn(0, 255)
        val avgB = (sumB / count).toInt().coerceIn(0, 255)
        val argb = (0xFF shl 24) or (avgR shl 16) or (avgG shl 8) or avgB
        return ColorDictionary.identifyColor(argb)
    }

    /**
     * Extracts up to 5 distinct dominant object colors from the bitmap along with
     * a representative normalized (x, y) point for each so users can tap to snap the reticle.
     */
    fun extractDominantColors(bitmap: Bitmap, maxColors: Int = 5): List<DetectedColorSpot> {
        val sampleSize = 72
        val aspect = bitmap.width.toFloat() / bitmap.height.toFloat().coerceAtLeast(1f)
        val targetW = if (aspect >= 1f) sampleSize else (sampleSize * aspect).roundToInt().coerceAtLeast(16)
        val targetH = if (aspect < 1f) sampleSize else (sampleSize / aspect).roundToInt().coerceAtLeast(16)
        val thumb = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)

        data class Bucket(
            var count: Int = 0,
            var weightSum: Double = 0.0,
            var rSum: Long = 0,
            var gSum: Long = 0,
            var bSum: Long = 0,
            var bestWeight: Double = -1.0,
            var bestX: Int = 0,
            var bestY: Int = 0
        )

        // 5-bit per channel quantization (32x32x32)
        val buckets = HashMap<Int, Bucket>()
        var totalPixels = 0

        for (y in 0 until targetH) {
            for (x in 0 until targetW) {
                val pixel = thumb.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val (_, sat, light) = ColorDictionary.rgbToHsl(r, g, b)

                // Mildly weight center region & chromatic objects over plain shadows/blowouts
                val dx = (x.toFloat() / targetW) - 0.5f
                val dy = (y.toFloat() / targetH) - 0.5f
                val centerWeight = (1.35 - (dx * dx + dy * dy) * 1.1).coerceAtLeast(0.5)
                val chromaBoost = if (sat > 18f && light in 12f..90f) 1.35 else 0.75
                val weight = centerWeight * chromaBoost

                val key = ((r shr 3) shl 10) or ((g shr 3) shl 5) or (b shr 3)
                val bucket = buckets.getOrPut(key) { Bucket() }
                bucket.count++
                bucket.weightSum += weight
                bucket.rSum += r
                bucket.gSum += g
                bucket.bSum += b
                // Track most representative pixel closest to bucket's center
                val pixelScore = centerWeight * (1.0 + sat / 100.0)
                if (pixelScore > bucket.bestWeight) {
                    bucket.bestWeight = pixelScore
                    bucket.bestX = x
                    bucket.bestY = y
                }
                totalPixels++
            }
        }
        if (thumb !== bitmap) thumb.recycle()

        val sorted = buckets.values.sortedByDescending { it.weightSum }
        val selected = mutableListOf<DetectedColorSpot>()

        for (bucket in sorted) {
            if (selected.size >= maxColors) break
            val avgR = (bucket.rSum / bucket.count).toInt().coerceIn(0, 255)
            val avgG = (bucket.gSum / bucket.count).toInt().coerceIn(0, 255)
            val avgB = (bucket.bSum / bucket.count).toInt().coerceIn(0, 255)

            val tooClose = selected.any { existing ->
                val distSq = ColorDictionary.labDistanceSq(
                    avgR, avgG, avgB,
                    existing.color.red, existing.color.green, existing.color.blue
                )
                distSq < 320.0 // Delta E ~ 18 threshold so swatches feel distinct
            }
            if (!tooClose) {
                val argb = (0xFF shl 24) or (avgR shl 16) or (avgG shl 8) or avgB
                val percent = ((bucket.count.toFloat() / totalPixels.coerceAtLeast(1)) * 100f)
                    .roundToInt()
                    .coerceIn(1, 99)
                selected.add(
                    DetectedColorSpot(
                        color = ColorDictionary.identifyColor(argb),
                        normX = (bucket.bestX.toFloat() + 0.5f) / targetW.toFloat(),
                        normY = (bucket.bestY.toFloat() + 0.5f) / targetH.toFloat(),
                        dominancePercent = percent
                    )
                )
            }
        }
        return selected
    }

    fun compressThumbnail(bitmap: Bitmap, maxEdge: Int = 280): ByteArray {
        val scaled = downscaleBitmap(bitmap, maxEdge)
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 82, out)
        if (scaled !== bitmap) scaled.recycle()
        return out.toByteArray()
    }

    private fun downscaleBitmap(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val longest = max(w, h)
        if (longest <= maxEdge) return bitmap
        val scale = maxEdge.toFloat() / longest.toFloat()
        val newW = (w * scale).roundToInt().coerceAtLeast(1)
        val newH = (h * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }

    private fun ensureSoftwareArgb8888(bitmap: Bitmap): Bitmap {
        if (bitmap.config == Bitmap.Config.ARGB_8888) return bitmap
        val copy = bitmap.copy(Bitmap.Config.ARGB_8888, false)
        if (copy != null) {
            bitmap.recycle()
            return copy
        }
        return bitmap
    }
}
