package com.burakbaybas.mantartanima

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
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

@Composable
private fun AppContent() {
    val vm: MainViewModel = viewModel()
    val state by vm.state.collectAsState()
    val photos by vm.photos.collectAsState()

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
