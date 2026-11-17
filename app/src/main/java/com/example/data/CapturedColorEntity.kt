package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "captured_colors")
data class CapturedColorEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val argb: Int,
    val hexCode: String,
    val colorName: String,
    val familyName: String,
    val red: Int,
    val green: Int,
    val blue: Int,
    val hue: Float,
    val saturation: Float,
    val lightness: Float,
    val thumbnailPath: String? = null,
    val capturedAt: Long = System.currentTimeMillis()
)
