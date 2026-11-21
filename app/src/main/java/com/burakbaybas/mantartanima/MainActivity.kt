package com.burakbaybas.mantartanima

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.burakbaybas.mantartanima.ui.EmergencyScreen
import com.burakbaybas.mantartanima.ui.SpeciesGuideScreen
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.burakbaybas.mantartanima.ui.Disclaimer
import com.burakbaybas.mantartanima.ui.DisclaimerScreen
import com.burakbaybas.mantartanima.ui.ErrorScreen
import com.burakbaybas.mantartanima.ui.HomeScreen
import com.burakbaybas.mantartanima.ui.ResultScreen
import com.burakbaybas.mantartanima.ui.theme.MantarTanimaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MantarTanimaTheme {
                val context = LocalContext.current
                var accepted by remember { mutableStateOf(Disclaimer.isAccepted(context)) }
                if (!accepted) {
                    DisclaimerScreen(onAccept = {
                        Disclaimer.accept(context)
                        accepted = true
                    })
                } else {
                    AppContent()
                }
            }
        }
    }
}

private enum class Tab(val label: String) { IDENTIFY("Tanımla"), GUIDE("Rehber"), EMERGENCY("Acil") }

@Composable
private fun AppContent() {
    val vm: MainViewModel = viewModel()
    val state by vm.state.collectAsState()
    val photos by vm.photos.collectAsState()
    var tab by rememberSaveable { mutableStateOf(Tab.IDENTIFY) }
    val showBar = tab != Tab.IDENTIFY || state is UiState.Home

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = tab == t,
                            onClick = { tab = t },
                            label = { Text(t.label) },
                            icon = {
                                Icon(
                                    when (t) {
                                        Tab.IDENTIFY -> Icons.Filled.CenterFocusStrong
                                        Tab.GUIDE -> Icons.AutoMirrored.Filled.MenuBook
                                        Tab.EMERGENCY -> Icons.Filled.LocalHospital
                                    },
                                    contentDescription = null,
                                    tint = if (t == Tab.EMERGENCY && tab != t) Color(0xFFB3261E) else LocalContentColor.current,
                                )
                            },
                        )
                    }
                }
            }
        },
    ) { inner ->
        Box(Modifier.padding(inner).consumeWindowInsets(inner)) {
            when (tab) {
                Tab.GUIDE -> {
                    BackHandler { tab = Tab.IDENTIFY }
                    SpeciesGuideScreen(onOpenEmergency = { tab = Tab.EMERGENCY })
                }
                Tab.EMERGENCY -> {
                    BackHandler { tab = Tab.IDENTIFY }
                    EmergencyScreen()
                }
                Tab.IDENTIFY -> IdentifyFlow(vm, state, photos, onOpenEmergency = { tab = Tab.EMERGENCY })
            }
        }
    }
}

@Composable
private fun IdentifyFlow(
    vm: MainViewModel,
    state: UiState,
    photos: Map<PhotoAngle, android.net.Uri>,
    onOpenEmergency: () -> Unit,
) {
    BackHandler(enabled = state !is UiState.Home) { vm.backToHome() }

    when (val s = state) {
        UiState.Home -> HomeScreen(
            photos = photos,
            isDemo = vm.isDemo,
            onPhotoSelected = vm::setPhoto,
            onPhotoRemoved = vm::removePhoto,
            onAnalyze = vm::analyze,
        )
        UiState.Analyzing -> ResultScreen(
            photos = photos,
            predictions = null,
            isDemo = vm.isDemo,
            onBack = vm::backToHome,
            onStartOver = vm::startOver,
        )
        is UiState.Result -> ResultScreen(
            photos = photos,
            predictions = s.predictions,
            isDemo = s.isDemo,
            onBack = vm::backToHome,
            onStartOver = vm::startOver,
            onOpenEmergency = onOpenEmergency,
        )
        is UiState.NoMushroom -> ResultScreen(
            photos = photos,
            predictions = emptyList(),
            isDemo = false,
            onBack = vm::backToHome,
            onStartOver = vm::startOver,
            noMushroom = s,
        )
        is UiState.Error -> ErrorScreen(message = s.message, onBack = vm::backToHome)
    }
}
