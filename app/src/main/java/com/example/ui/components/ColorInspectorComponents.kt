package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.color.DetectedColorSpot
import com.example.color.IdentifiedColor
import com.example.color.PaletteSwatch
import com.example.color.RecommendedPalette
import com.example.color.SamplingRadius
import com.example.ui.theme.JetBrainsMonoFamily
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun InteractivePhotoEyedropper(
    bitmap: Bitmap,
    normX: Float,
    normY: Float,
    sampledColor: IdentifiedColor?,
    samplingRadius: SamplingRadius,
    onSamplePointChange: (Float, Float) -> Unit,
    onRadiusChange: (SamplingRadius) -> Unit,
    onRetakeClick: () -> Unit,
    onSaveSwatchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    val reticleColor by animateColorAsState(
        targetValue = sampledColor?.let { Color(it.argb) } ?: MaterialTheme.colorScheme.primary,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "reticleColor"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = Color(0xFF0D0E11),
            tonalElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(310.dp)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val density = LocalDensity.current
                val boxW = with(density) { maxWidth.toPx() }
                val boxH = with(density) { maxHeight.toPx() }

                val imgWOrig = bitmap.width.toFloat().coerceAtLeast(1f)
                val imgHOrig = bitmap.height.toFloat().coerceAtLeast(1f)
                val fitScale = min(boxW / imgWOrig, boxH / imgHOrig)
                val renderedW = imgWOrig * fitScale
                val renderedH = imgHOrig * fitScale
                val leftOffset = (boxW - renderedW) / 2f
                val topOffset = (boxH - renderedH) / 2f

                fun pointerToNorm(pos: Offset): Pair<Float, Float> {
                    val nx = ((pos.x - leftOffset) / renderedW).coerceIn(0f, 1f)
                    val ny = ((pos.y - topOffset) / renderedH).coerceIn(0f, 1f)
                    return nx to ny
                }

                Image(
                    bitmap = imageBitmap,
                    contentDescription = "Captured photo for color sampling",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("captured_photo_canvas")
                        .pointerInput(renderedW, renderedH, leftOffset, topOffset) {
                            detectTapGestures { offset ->
                                val (nx, ny) = pointerToNorm(offset)
                                onSamplePointChange(nx, ny)
                            }
                        }
                        .pointerInput(renderedW, renderedH, leftOffset, topOffset) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                val (nx, ny) = pointerToNorm(change.position)
                                onSamplePointChange(nx, ny)
                            }
                        }
                )

                // Precision eyedropper reticle overlay
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = leftOffset + normX * renderedW
                    val cy = topOffset + normY * renderedH
                    val outerRadius = 22.dp.toPx()
                    val innerRadius = 11.dp.toPx()

                    // Soft dark shadow ring for contrast on bright photos
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.55f),
                        radius = outerRadius + 2.dp.toPx(),
                        center = Offset(cx, cy),
                        style = Stroke(width = 4.dp.toPx())
                    )
                    // Outer color preview halo
                    drawCircle(
                        color = reticleColor,
                        radius = outerRadius,
                        center = Offset(cx, cy),
                        style = Stroke(width = 6.dp.toPx())
                    )
                    // Crisp white inner ring
                    drawCircle(
                        color = Color.White,
                        radius = innerRadius,
                        center = Offset(cx, cy),
                        style = Stroke(width = 2.dp.toPx())
                    )
                    // Center target dot
                    drawCircle(
                        color = Color.White,
                        radius = 2.5.dp.toPx(),
                        center = Offset(cx, cy)
                    )
                }

                // Floating instruction & top controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.62f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Colorize,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Drag or tap object",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = onRetakeClick,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("new_photo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("New Photo")
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Sampling precision & save bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sample:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SamplingRadius.entries.forEach { radiusOption ->
                    FilterChip(
                        selected = samplingRadius == radiusOption,
                        onClick = { onRadiusChange(radiusOption) },
                        label = {
                            Text(
                                text = radiusOption.label,
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 11.sp
                            )
                        },
                        modifier = Modifier.testTag("radius_${radiusOption.name.lowercase()}")
                    )
                }
            }

            FilledTonalButton(
                onClick = onSaveSwatchClick,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("save_swatch_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.BookmarkAdd,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Save")
            }
        }
    }
}

@Composable
fun DetectedColorsStrip(
    spots: List<DetectedColorSpot>,
    selectedHex: String?,
    onSpotClick: (DetectedColorSpot) -> Unit,
    modifier: Modifier = Modifier
) {
    if (spots.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Detected in Photo",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(spots, key = { it.color.hex }) { spot ->
                val isSelected = spot.color.hex == selectedHex
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = if (isSelected) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { onSpotClick(spot) }
                        .testTag("detected_spot_${spot.color.hex}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color(spot.color.argb))
                                .border(1.dp, Color.Black.copy(alpha = 0.14f), CircleShape)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = spot.color.hex,
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = spot.color.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IdentifiedColorHeroCard(
    color: IdentifiedColor,
    onCopyHex: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedBg by animateColorAsState(
        targetValue = Color(color.argb),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "heroSwatch"
    )
    val onSwatch = Color(color.onSwatchArgb)

    ElevatedCard(
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            // Luminous Pantone-inspired color showcase block
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(animatedBg)
                    .padding(horizontal = 20.dp, vertical = 22.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = onSwatch.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = color.family.label.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = onSwatch,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = onSwatch.copy(alpha = 0.18f),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onCopyHex(color.hex) }
                                .minimumInteractiveComponentSize()
                                .testTag("copy_primary_hex_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = "Copy hex code",
                                    tint = onSwatch,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Copy Hex",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = onSwatch
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = color.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = onSwatch,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = color.hex,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                        letterSpacing = 1.sp,
                        color = onSwatch,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onCopyHex(color.hex) }
                            .testTag("identified_hex_text")
                    )
                }
            }

            // Technical specs & WCAG contrast footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ColorSpecBadge(
                        label = "RGB",
                        value = "${color.red}, ${color.green}, ${color.blue}"
                    )
                    ColorSpecBadge(
                        label = "HSL",
                        value = "${color.hue.roundToInt()}° ${color.saturation.roundToInt()}% ${color.lightness.roundToInt()}%"
                    )
                    ColorSpecBadge(
                        label = "CMYK",
                        value = color.cmyk.formatted()
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ContrastChip(
                        label = "On Light",
                        ratio = color.whiteContrast,
                        previewBg = Color.White,
                        previewFg = Color(color.argb),
                        modifier = Modifier.weight(1f)
                    )
                    ContrastChip(
                        label = "On Dark",
                        ratio = color.blackContrast,
                        previewBg = Color(0xFF111215),
                        previewFg = Color(color.argb),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorSpecBadge(label: String, value: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = value,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ContrastChip(
    label: String,
    ratio: Float,
    previewBg: Color,
    previewFg: Color,
    modifier: Modifier = Modifier
) {
    val grade = when {
        ratio >= 7.0f -> "AAA"
        ratio >= 4.5f -> "AA"
        ratio >= 3.0f -> "AA Large"
        else -> "Low"
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(previewBg)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                ) {
                    Text(
                        text = "Aa",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = previewFg
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format("%.1f:1", ratio),
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            AssistChip(
                onClick = {},
                enabled = false,
                label = { Text(grade, fontSize = 10.sp) }
            )
        }
    }
}

@Composable
fun PaletteRecommendationCard(
    palette: RecommendedPalette,
    onCopySwatchHex: (PaletteSwatch) -> Unit,
    onInspectSwatch: (PaletteSwatch) -> Unit,
    onCopyEntirePalette: (RecommendedPalette) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = palette.type.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = palette.type.hueRule,
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = palette.type.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { onCopyEntirePalette(palette) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("copy_palette_${palette.type.name.lowercase()}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "Copy ${palette.type.title} palette hex codes",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Unified color strip preview
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
            ) {
                palette.swatches.forEach { swatch ->
                    val swatchColor = Color(swatch.argb)
                    val onColor = Color(swatch.onSwatchArgb)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(swatchColor)
                            .clickable { onCopySwatchHex(swatch) }
                            .padding(6.dp)
                            .testTag("swatch_${palette.type.name.lowercase()}_${swatch.hex}")
                    ) {
                        if (swatch.role == "Captured") {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Captured base color",
                                tint = onColor.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .size(14.dp)
                                    .align(Alignment.TopEnd)
                            )
                        }
                        Column(modifier = Modifier.align(Alignment.BottomStart)) {
                            Text(
                                text = swatch.hex,
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                                color = onColor,
                                maxLines = 1
                            )
                            Text(
                                text = swatch.role,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 9.5.sp,
                                color = onColor.copy(alpha = 0.82f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Quick-inspect color name pills below the bar
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(palette.swatches, key = { "${palette.type.name}_${it.role}_${it.hex}" }) { swatch ->
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onInspectSwatch(swatch) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(swatch.argb))
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = swatch.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Outlined.Palette,
                                contentDescription = "Explore palettes from ${swatch.name}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
