package com.burakbaybas.mantartanima

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
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
                    is UiState.Error -> ErrorScreen(message = s.message, onBack = vm::backToHome)
                }
            }
        }
    }
}
