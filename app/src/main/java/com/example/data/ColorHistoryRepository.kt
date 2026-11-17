package com.example.data

import android.content.Context
import android.graphics.Bitmap
import com.example.color.IdentifiedColor
import com.example.color.ImageColorExtractor
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ColorHistoryRepository(
    private val dao: CapturedColorDao,
    private val appContext: Context
) {
    val recentCaptures: Flow<List<CapturedColorEntity>> = dao.observeAll()

    suspend fun saveCapture(color: IdentifiedColor, sourceBitmap: Bitmap?): Long {
        val thumbPath = withContext(Dispatchers.IO) {
            if (sourceBitmap == null) return@withContext null
            runCatching {
                val dir = File(appContext.filesDir, "swatch_thumbs").apply { mkdirs() }
                val file = File(dir, "swatch_${System.currentTimeMillis()}.jpg")
                file.writeBytes(ImageColorExtractor.compressThumbnail(sourceBitmap))
                file.absolutePath
            }.getOrNull()
        }

        val entity = CapturedColorEntity(
            argb = color.argb,
            hexCode = color.hex,
            colorName = color.name,
            familyName = color.family.name,
            red = color.red,
            green = color.green,
            blue = color.blue,
            hue = color.hue,
            saturation = color.saturation,
            lightness = color.lightness,
            thumbnailPath = thumbPath,
            capturedAt = System.currentTimeMillis()
        )
        return dao.insert(entity)
    }

    suspend fun deleteCapture(entity: CapturedColorEntity) {
        withContext(Dispatchers.IO) {
            entity.thumbnailPath?.let { path ->
                runCatching { File(path).delete() }
            }
        }
        dao.deleteById(entity.id)
    }

    suspend fun clearHistory() {
        withContext(Dispatchers.IO) {
            runCatching {
                File(appContext.filesDir, "swatch_thumbs").deleteRecursively()
            }
        }
        dao.clearAll()
    }
}
