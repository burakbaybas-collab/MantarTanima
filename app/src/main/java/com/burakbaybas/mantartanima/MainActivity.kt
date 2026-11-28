package com.burakbaybas.mantartanima

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
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
import com.burakbaybas.mantartanima.ui.HistoryInfoCard
import com.burakbaybas.mantartanima.ui.HistoryScreen
import com.burakbaybas.mantartanima.ui.HomeScreen
import com.burakbaybas.mantartanima.ui.TutorialScreen
import com.burakbaybas.mantartanima.ui.Tutorial
import com.burakbaybas.mantartanima.ui.ResultScreen
import com.burakbaybas.mantartanima.ui.ScanScreen
import com.burakbaybas.mantartanima.ui.theme.MantarTanimaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
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
                    var showTutorial by remember { mutableStateOf(!Tutorial.isSeen(context)) }
                    if (showTutorial) {
                        BackHandler { Tutorial.markSeen(context); showTutorial = false }
                        TutorialScreen(onDone = { Tutorial.markSeen(context); showTutorial = false })
                    } else {
                        AppContent(onOpenTutorial = { showTutorial = true })
                    }
                }
            }
        }
    }
}

@Composable
private fun AppContent(onOpenTutorial: () -> Unit = {}) {
    val vm: MainViewModel = viewModel()
    val state by vm.state.collectAsState()
    val photos by vm.photos.collectAsState()
    val history by vm.history.collectAsState()

    BackHandler(enabled = state !is UiState.Home) { vm.back() }

    when (val s = state) {
        UiState.Home -> HomeScreen(
            onOpenTutorial = onOpenTutorial,
            photos = photos,
            isDemo = vm.isDemo,
            onPhotoSelected = vm::setPhoto,
            onPhotoRemoved = vm::removePhoto,
            onAnalyze = vm::analyze,
            historyCount = history.size,
            onOpenHistory = vm::openHistory,
        )
        is UiState.Analyzing -> ScanScreen(photos, s.found)
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
        is UiState.Mismatch -> ResultScreen(
            photos = photos,
            predictions = emptyList(),
            isDemo = false,
            onBack = vm::backToHome,
            onStartOver = vm::startOver,
            mismatch = s.guesses,
        )
        is UiState.Error -> ErrorScreen(message = s.message, onBack = vm::backToHome)
        UiState.History -> HistoryScreen(
            entries = history,
            onBack = vm::backToHome,
            onOpen = vm::openHistoryEntry,
            onDelete = vm::deleteHistoryEntry,
            onDeleteMany = vm::deleteHistoryEntries,
        )
        is UiState.HistoryDetail -> ResultScreen(
            photos = s.entry.photos.mapValues { Uri.fromFile(it.value) },
            predictions = s.entry.predictions,
            isDemo = false,
            onBack = vm::openHistory,
            onStartOver = vm::startOver,
            infoCard = { HistoryInfoCard(s.entry) },
            fromHistory = true,
        )
    }
}
