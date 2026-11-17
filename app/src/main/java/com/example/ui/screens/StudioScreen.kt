package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.color.DetectedColorSpot
import com.example.color.HarmonyType
import com.example.color.ImageColorExtractor
import com.example.color.PaletteSwatch
import com.example.color.RecommendedPalette
import com.example.color.SamplingRadius
import com.example.ui.ColorClueUiState
import com.example.ui.StudioMode
import com.example.ui.components.DetectedColorsStrip
import com.example.ui.components.IdentifiedColorHeroCard
import com.example.ui.components.InteractivePhotoEyedropper
import com.example.ui.components.PaletteRecommendationCard
import com.example.ui.theme.JetBrainsMonoFamily
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

@Composable
fun StudioScreen(
    uiState: ColorClueUiState,
    onLiveFrameColor: (com.example.color.IdentifiedColor) -> Unit,
    onCameraError: (String) -> Unit,
    onClearCameraError: () -> Unit,
    onPhotoCaptured: (android.graphics.Bitmap) -> Unit,
    onPhotoPicked: (Uri) -> Unit,
    onSamplePointChanged: (Float, Float) -> Unit,
    onSamplingRadiusChanged: (SamplingRadius) -> Unit,
    onDominantSpotSelected: (DetectedColorSpot) -> Unit,
    onInspectSwatch: (PaletteSwatch) -> Unit,
    onCopyHex: (String) -> Unit,
    onCopyEntirePalette: (RecommendedPalette) -> Unit,
    onSaveSwatch: () -> Unit,
    onRetakePhoto: () -> Unit,
    onToggleFlash: () -> Unit,
    onSwitchCamera: () -> Unit,
    onHarmonyFilterSelected: (HarmonyType?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionRequestedOnce by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        permissionRequestedOnce = true
        if (granted) onClearCameraError()
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { onPhotoPicked(it) }
    }

    fun launchPhotoPicker() {
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    // Back press from photo/swatch inspector returns smoothly to live camera
    BackHandler(enabled = uiState.studioMode != StudioMode.CAMERA) {
        onRetakePhoto()
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isExpandedLayout = maxWidth >= 760.dp

        if (uiState.isProcessingImage) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Analyzing photo colors…",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return@BoxWithConstraints
        }

        when (uiState.studioMode) {
            StudioMode.CAMERA -> {
                if (!hasCameraPermission) {
                    val permanentlyDenied = permissionRequestedOnce &&
                        (context as? Activity)?.let { activity ->
                            !ActivityCompat.shouldShowRequestPermissionRationale(
                                activity,
                                Manifest.permission.CAMERA
                            )
                        } == true

                    CameraPermissionWelcome(
                        permanentlyDenied = permanentlyDenied,
                        onRequestCamera = {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        onOpenSettings = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        },
                        onPickFromPhotos = ::launchPhotoPicker,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (uiState.cameraUnavailableError != null) {
                    CameraFallbackCard(
                        errorMessage = uiState.cameraUnavailableError,
                        onRetryCamera = onClearCameraError,
                        onPickFromPhotos = ::launchPhotoPicker,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LiveCameraViewfinder(
                        uiState = uiState,
                        onLiveFrameColor = onLiveFrameColor,
                        onCameraError = onCameraError,
                        onPhotoCaptured = { bmp ->
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onPhotoCaptured(bmp)
                        },
                        onPickFromPhotos = ::launchPhotoPicker,
                        onToggleFlash = onToggleFlash,
                        onSwitchCamera = onSwitchCamera,
                        onCopyLiveHex = { hex ->
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onCopyHex(hex)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            StudioMode.PHOTO_INSPECTOR,
            StudioMode.SWATCH_INSPECTOR -> {
                val filteredPalettes = remember(uiState.recommendedPalettes, uiState.selectedHarmonyFilter) {
                    val filter = uiState.selectedHarmonyFilter
                    if (filter == null) uiState.recommendedPalettes
                    else uiState.recommendedPalettes.filter { it.type == filter }
                }

                if (isExpandedLayout) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            if (uiState.capturedBitmap != null) {
                                InteractivePhotoEyedropper(
                                    bitmap = uiState.capturedBitmap,
                                    normX = uiState.sampleNormX,
                                    normY = uiState.sampleNormY,
                                    sampledColor = uiState.identifiedColor,
                                    samplingRadius = uiState.samplingRadius,
                                    onSamplePointChange = onSamplePointChanged,
                                    onRadiusChange = onSamplingRadiusChanged,
                                    onRetakeClick = onRetakePhoto,
                                    onSaveSwatchClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onSaveSwatch()
                                    }
                                )
                            } else {
                                SwatchOnlyActionsHeader(
                                    onRetakeClick = onRetakePhoto,
                                    onPickPhotoClick = ::launchPhotoPicker,
                                    onSaveSwatchClick = onSaveSwatch
                                )
                            }
                            Spacer(Modifier.height(14.dp))
                            DetectedColorsStrip(
                                spots = uiState.dominantSpots,
                                selectedHex = uiState.identifiedColor?.hex,
                                onSpotClick = onDominantSpotSelected
                            )
                        }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 24.dp),
                            modifier = Modifier
                                .weight(1.15f)
                                .fillMaxHeight()
                        ) {
                            uiState.identifiedColor?.let { color ->
                                item(key = "hero_color") {
                                    IdentifiedColorHeroCard(
                                        color = color,
                                        onCopyHex = { hex ->
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onCopyHex(hex)
                                        }
                                    )
                                }
                            }

                            item(key = "palette_header") {
                                PaletteSectionHeader(
                                    selectedFilter = uiState.selectedHarmonyFilter,
                                    onFilterSelected = onHarmonyFilterSelected
                                )
                            }

                            items(filteredPalettes, key = { it.type.name }) { palette ->
                                PaletteRecommendationCard(
                                    palette = palette,
                                    onCopySwatchHex = { swatch ->
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onCopyHex(swatch.hex)
                                    },
                                    onInspectSwatch = onInspectSwatch,
                                    onCopyEntirePalette = { p ->
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onCopyEntirePalette(p)
                                    }
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item(key = "photo_or_swatch_stage") {
                            if (uiState.capturedBitmap != null) {
                                InteractivePhotoEyedropper(
                                    bitmap = uiState.capturedBitmap,
                                    normX = uiState.sampleNormX,
                                    normY = uiState.sampleNormY,
                                    sampledColor = uiState.identifiedColor,
                                    samplingRadius = uiState.samplingRadius,
                                    onSamplePointChange = onSamplePointChanged,
                                    onRadiusChange = onSamplingRadiusChanged,
                                    onRetakeClick = onRetakePhoto,
                                    onSaveSwatchClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onSaveSwatch()
                                    }
                                )
                            } else {
                                SwatchOnlyActionsHeader(
                                    onRetakeClick = onRetakePhoto,
                                    onPickPhotoClick = ::launchPhotoPicker,
                                    onSaveSwatchClick = onSaveSwatch
                                )
                            }
                        }

                        if (uiState.dominantSpots.isNotEmpty()) {
                            item(key = "detected_spots") {
                                DetectedColorsStrip(
                                    spots = uiState.dominantSpots,
                                    selectedHex = uiState.identifiedColor?.hex,
                                    onSpotClick = onDominantSpotSelected
                                )
                            }
                        }

                        uiState.identifiedColor?.let { color ->
                            item(key = "hero_color") {
                                IdentifiedColorHeroCard(
                                    color = color,
                                    onCopyHex = { hex ->
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onCopyHex(hex)
                                    }
                                )
                            }
                        }

                        item(key = "palette_header") {
                            PaletteSectionHeader(
                                selectedFilter = uiState.selectedHarmonyFilter,
                                onFilterSelected = onHarmonyFilterSelected
                            )
                        }

                        items(filteredPalettes, key = { it.type.name }) { palette ->
                            PaletteRecommendationCard(
                                palette = palette,
                                onCopySwatchHex = { swatch ->
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onCopyHex(swatch.hex)
                                },
                                onInspectSwatch = onInspectSwatch,
                                onCopyEntirePalette = { p ->
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onCopyEntirePalette(p)
                                }
                            )
                        }

                        item(key = "bottom_spacer") {
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SwatchOnlyActionsHeader(
    onRetakeClick: () -> Unit,
    onPickPhotoClick: () -> Unit,
    onSaveSwatchClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalButton(
            onClick = onRetakeClick,
            modifier = Modifier
                .weight(1f)
                .minimumInteractiveComponentSize()
                .testTag("back_to_camera_button")
        ) {
            Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Camera")
        }
        OutlinedButton(
            onClick = onPickPhotoClick,
            modifier = Modifier
                .weight(1f)
                .minimumInteractiveComponentSize()
                .testTag("pick_photo_header_button")
        ) {
            Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Photos")
        }
        FilledTonalButton(
            onClick = onSaveSwatchClick,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("save_swatch_header_button")
        ) {
            Icon(Icons.Outlined.BookmarkAdd, contentDescription = "Save swatch", modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun PaletteSectionHeader(
    selectedFilter: HarmonyType?,
    onFilterSelected: (HarmonyType?) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.recommended_palettes),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Tap any swatch to copy its hex code, or tap a color pill below a strip to explore its harmonies.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item(key = "all_harmonies") {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { onFilterSelected(null) },
                    label = { Text("All") }
                )
            }
            items(HarmonyType.entries, key = { it.name }) { type ->
                FilterChip(
                    selected = selectedFilter == type,
                    onClick = { onFilterSelected(type) },
                    label = { Text(type.title) },
                    modifier = Modifier.testTag("filter_${type.name.lowercase()}")
                )
            }
        }
    }
}

@Composable
private fun LiveCameraViewfinder(
    uiState: ColorClueUiState,
    onLiveFrameColor: (com.example.color.IdentifiedColor) -> Unit,
    onCameraError: (String) -> Unit,
    onPhotoCaptured: (android.graphics.Bitmap) -> Unit,
    onPickFromPhotos: () -> Unit,
    onToggleFlash: () -> Unit,
    onSwitchCamera: () -> Unit,
    onCopyLiveHex: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val lastAnalyzedAt = remember { AtomicLong(0L) }

    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var isCapturing by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    LaunchedEffect(previewView, uiState.useFrontCamera, lifecycleOwner) {
        val pv = previewView ?: return@LaunchedEffect
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener(
            {
                try {
                    val cameraProvider = providerFuture.get()
                    val selector = if (uiState.useFrontCamera) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }

                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = pv.surfaceProvider
                    }

                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()

                    val analysis = ImageAnalysis.Builder()
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analyzerUseCase ->
                            analyzerUseCase.setAnalyzer(cameraExecutor) { proxy ->
                                try {
                                    val now = System.currentTimeMillis()
                                    val prev = lastAnalyzedAt.get()
                                    if (now - prev >= 110L && lastAnalyzedAt.compareAndSet(prev, now)) {
                                        ImageColorExtractor.sampleCenterFromRgbaProxy(proxy)?.let { color ->
                                            ContextCompat.getMainExecutor(context).execute {
                                                onLiveFrameColor(color)
                                            }
                                        }
                                    }
                                } finally {
                                    proxy.close()
                                }
                            }
                        }

                    cameraProvider.unbindAll()
                    val cam = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        selector,
                        preview,
                        capture,
                        analysis
                    )
                    imageCapture = capture
                    boundCamera = cam
                } catch (e: Exception) {
                    onCameraError(e.localizedMessage ?: "Camera preview unavailable on this device.")
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    LaunchedEffect(boundCamera, uiState.flashEnabled) {
        val cam = boundCamera ?: return@LaunchedEffect
        if (cam.cameraInfo.hasFlashUnit()) {
            cam.cameraControl.enableTorch(uiState.flashEnabled)
        }
    }

    val liveColor = uiState.livePreviewColor
    val animatedRingColor by animateColorAsState(
        targetValue = liveColor?.let { Color(it.argb) } ?: MaterialTheme.colorScheme.primary,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "liveRingColor"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Color(0xFF0C0D10))
    ) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }.also { previewView = it }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Optical center crosshair reticle
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerR = 30.dp.toPx()
            val innerR = 14.dp.toPx()

            drawCircle(
                color = Color.Black.copy(alpha = 0.45f),
                radius = outerR,
                center = center,
                style = Stroke(width = 6.dp.toPx())
            )
            drawCircle(
                color = animatedRingColor,
                radius = outerR,
                center = center,
                style = Stroke(width = 3.5.dp.toPx())
            )
            drawCircle(
                color = Color.White,
                radius = innerR,
                center = center,
                style = Stroke(width = 1.8.dp.toPx())
            )
            drawCircle(
                color = Color.White,
                radius = 2.5.dp.toPx(),
                center = center
            )
        }

        // Top studio HUD (Live Color Pill + Flash / Switch Camera)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.65f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color.White.copy(alpha = 0.16f)
                ),
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(enabled = liveColor != null) {
                        liveColor?.let { onCopyLiveHex(it.hex) }
                    }
                    .testTag("live_color_pill")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(animatedRingColor)
                            .border(1.5.dp, Color.White, CircleShape)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = liveColor?.name ?: "Point at an object",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                        Text(
                            text = liveColor?.hex ?: "#------",
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 11.5.sp,
                            color = Color.White.copy(alpha = 0.82f)
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!uiState.useFrontCamera && boundCamera?.cameraInfo?.hasFlashUnit() == true) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.6f)
                    ) {
                        IconButton(
                            onClick = onToggleFlash,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("toggle_flash_button")
                        ) {
                            Icon(
                                imageVector = if (uiState.flashEnabled) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff,
                                contentDescription = stringResource(R.string.toggle_flash_cd),
                                tint = if (uiState.flashEnabled) Color(0xFF22D3EE) else Color.White
                            )
                        }
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    IconButton(
                        onClick = onSwitchCamera,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("switch_camera_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Cameraswitch,
                            contentDescription = stringResource(R.string.switch_camera_cd),
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Bottom Camera Shutter Dock
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Photo Picker button
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.16f),
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onPickFromPhotos)
                    .testTag("gallery_picker_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.PhotoLibrary,
                        contentDescription = stringResource(R.string.pick_photo_cd),
                        tint = Color.White
                    )
                }
            }

            // Concentric Shutter Button tinted by live captured hue
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(78.dp)
                    .clip(CircleShape)
                    .border(4.dp, Color.White, CircleShape)
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(if (isCapturing) Color.Gray else Color.White)
                    .clickable(enabled = !isCapturing && imageCapture != null) {
                        val cap = imageCapture ?: return@clickable
                        isCapturing = true
                        cap.takePicture(
                            cameraExecutor,
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    val bmp = try {
                                        ImageColorExtractor.imageProxyToBitmap(
                                            image,
                                            mirrorHorizontally = uiState.useFrontCamera
                                        )
                                    } finally {
                                        image.close()
                                    }
                                    ContextCompat.getMainExecutor(context).execute {
                                        isCapturing = false
                                        if (bmp != null) {
                                            onPhotoCaptured(bmp)
                                        }
                                    }
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    ContextCompat.getMainExecutor(context).execute {
                                        isCapturing = false
                                        onCameraError(exception.localizedMessage ?: "Capture failed")
                                    }
                                }
                            }
                        )
                    }
                    .testTag("camera_shutter_button")
            ) {
                if (isCapturing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp,
                        color = Color(0xFF111215)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(animatedRingColor)
                    )
                }
            }

            // Placeholder symmetry / hint badge
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.14f)
            ) {
                Text(
                    text = "HEX",
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun CameraPermissionWelcome(
    permanentlyDenied: Boolean,
    onRequestCamera: () -> Unit,
    onOpenSettings: () -> Unit,
    onPickFromPhotos: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ElevatedCard(
            shape = MaterialTheme.shapes.extraLarge,
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Image(
                    painter = painterResource(id = R.drawable.img_prism_hero_1794628752610),
                    contentDescription = "Optical prism casting spectrum onto color swatch cards",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                )

                Column(modifier = Modifier.padding(22.dp)) {
                    Text(
                        text = stringResource(R.string.camera_permission_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.camera_permission_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(20.dp))

                    Button(
                        onClick = if (permanentlyDenied) onOpenSettings else onRequestCamera,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("enable_camera_button")
                    ) {
                        Icon(Icons.Rounded.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (permanentlyDenied) {
                                stringResource(R.string.open_settings)
                            } else {
                                stringResource(R.string.enable_camera)
                            }
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onPickFromPhotos,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("pick_photo_welcome_button")
                    ) {
                        Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.pick_from_library))
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraFallbackCard(
    errorMessage: String,
    onRetryCamera: () -> Unit,
    onPickFromPhotos: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.padding(24.dp)
    ) {
        ElevatedCard(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CameraAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Camera Unavailable",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = onPickFromPhotos,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("fallback_pick_photo_button")
                ) {
                    Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.pick_from_library))
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onRetryCamera,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                ) {
                    Text("Retry Camera")
                }
            }
        }
    }
}
