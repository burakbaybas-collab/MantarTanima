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

                BackHandler(enabled = state !is UiState.Home) { vm.reset() }

                when (val s = state) {
                    UiState.Home -> HomeScreen(isDemo = vm.isDemo, onImageSelected = vm::analyze)
                    is UiState.Analyzing -> ResultScreen(
                        imageUri = s.imageUri,
                        predictions = null,
                        isDemo = vm.isDemo,
                        onBack = vm::reset,
                    )
                    is UiState.Result -> ResultScreen(
                        imageUri = s.imageUri,
                        predictions = s.predictions,
                        isDemo = s.isDemo,
                        onBack = vm::reset,
                    )
                    is UiState.Error -> ErrorScreen(message = s.message, onBack = vm::reset)
                }
            }
        }
    }
}
