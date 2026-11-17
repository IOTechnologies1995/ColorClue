package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.ColorClueViewModel
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.ColorClueTheme
import kotlinx.coroutines.launch

private const val ROUTE_STUDIO = "studio"
private const val ROUTE_HISTORY = "history"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ColorClueTheme {
                ColorClueApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorClueApp(
    viewModel: ColorClueViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val historyItems by viewModel.historyItems.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_STUDIO

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    fun copyToClipboard(label: String, text: String, feedbackMessage: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(feedbackMessage)
        }
    }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 760.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.sweepGradient(
                                            listOf(
                                                Color(0xFF22D3EE),
                                                Color(0xFFFFFFFF),
                                                Color(0xFF6B7280),
                                                Color(0xFF0A0B0D),
                                                Color(0xFF22D3EE)
                                            )
                                        )
                                    )
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = currentRoute == ROUTE_STUDIO,
                            onClick = { navigateToTab(ROUTE_STUDIO) },
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute == ROUTE_STUDIO) {
                                        Icons.Filled.Colorize
                                    } else {
                                        Icons.Outlined.Colorize
                                    },
                                    contentDescription = stringResource(R.string.studio_tab)
                                )
                            },
                            label = { Text(stringResource(R.string.studio_tab)) },
                            modifier = Modifier.testTag("nav_studio_tab")
                        )
                        NavigationBarItem(
                            selected = currentRoute == ROUTE_HISTORY,
                            onClick = { navigateToTab(ROUTE_HISTORY) },
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute == ROUTE_HISTORY) {
                                        Icons.Filled.Bookmarks
                                    } else {
                                        Icons.Outlined.Bookmarks
                                    },
                                    contentDescription = stringResource(R.string.history_tab)
                                )
                            },
                            label = { Text(stringResource(R.string.history_tab)) },
                            modifier = Modifier.testTag("nav_history_tab")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        NavigationRailItem(
                            selected = currentRoute == ROUTE_STUDIO,
                            onClick = { navigateToTab(ROUTE_STUDIO) },
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute == ROUTE_STUDIO) {
                                        Icons.Filled.Colorize
                                    } else {
                                        Icons.Outlined.Colorize
                                    },
                                    contentDescription = stringResource(R.string.studio_tab)
                                )
                            },
                            label = { Text(stringResource(R.string.studio_tab)) },
                            modifier = Modifier.testTag("rail_studio_tab")
                        )
                        NavigationRailItem(
                            selected = currentRoute == ROUTE_HISTORY,
                            onClick = { navigateToTab(ROUTE_HISTORY) },
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute == ROUTE_HISTORY) {
                                        Icons.Filled.Bookmarks
                                    } else {
                                        Icons.Outlined.Bookmarks
                                    },
                                    contentDescription = stringResource(R.string.history_tab)
                                )
                            },
                            label = { Text(stringResource(R.string.history_tab)) },
                            modifier = Modifier.testTag("rail_history_tab")
                        )
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_STUDIO,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    composable(ROUTE_STUDIO) {
                        StudioScreen(
                            uiState = uiState,
                            onLiveFrameColor = viewModel::onLiveFrameColor,
                            onCameraError = viewModel::onCameraError,
                            onClearCameraError = viewModel::clearCameraError,
                            onPhotoCaptured = viewModel::onPhotoCaptured,
                            onPhotoPicked = viewModel::onPhotoPicked,
                            onSamplePointChanged = viewModel::onSamplePointChanged,
                            onSamplingRadiusChanged = viewModel::onSamplingRadiusChanged,
                            onDominantSpotSelected = viewModel::onDominantSpotSelected,
                            onInspectSwatch = { swatch ->
                                viewModel.onInspectColorSwatch(swatch.argb)
                            },
                            onCopyHex = { hex ->
                                copyToClipboard(
                                    label = "ColorClue Hex",
                                    text = hex,
                                    feedbackMessage = context.getString(R.string.copied_hex_message, hex)
                                )
                            },
                            onCopyEntirePalette = { palette ->
                                val hexList = palette.swatches.joinToString(", ") { it.hex }
                                copyToClipboard(
                                    label = "${palette.type.title} Palette",
                                    text = hexList,
                                    feedbackMessage = context.getString(
                                        R.string.copied_palette_message,
                                        palette.type.title,
                                        hexList
                                    )
                                )
                            },
                            onSaveSwatch = {
                                val hex = uiState.identifiedColor?.hex ?: return@StudioScreen
                                viewModel.onSaveCurrentSwatch()
                                coroutineScope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    snackbarHostState.showSnackbar(
                                        context.getString(R.string.swatch_saved, hex)
                                    )
                                }
                            },
                            onRetakePhoto = viewModel::onRetakePhoto,
                            onToggleFlash = viewModel::onToggleFlash,
                            onSwitchCamera = viewModel::onSwitchCamera,
                            onHarmonyFilterSelected = viewModel::onHarmonyFilterSelected
                        )
                    }

                    composable(ROUTE_HISTORY) {
                        HistoryScreen(
                            items = historyItems,
                            selectedFamily = uiState.selectedHistoryFamily,
                            onFamilyFilterSelected = viewModel::onHistoryFamilyFilterSelected,
                            onSelectCapture = { entity ->
                                viewModel.onLoadFromHistory(entity)
                                navigateToTab(ROUTE_STUDIO)
                            },
                            onCopyHex = { hex ->
                                copyToClipboard(
                                    label = "ColorClue Hex",
                                    text = hex,
                                    feedbackMessage = context.getString(R.string.copied_hex_message, hex)
                                )
                            },
                            onDeleteCapture = viewModel::onDeleteHistoryItem,
                            onClearHistory = viewModel::onClearHistory
                        )
                    }
                }
            }
        }
    }
}
