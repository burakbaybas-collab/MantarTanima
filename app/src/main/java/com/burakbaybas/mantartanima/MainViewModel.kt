package com.burakbaybas.mantartanima

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.burakbaybas.mantartanima.classifier.ClassifierFactory
import com.burakbaybas.mantartanima.classifier.Prediction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface UiState {
    data object Home : UiState
    data class Analyzing(val imageUri: Uri) : UiState
    data class Result(
        val imageUri: Uri,
        val predictions: List<Prediction>,
        val isDemo: Boolean,
    ) : UiState
    data class Error(val message: String) : UiState
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val classifier = ClassifierFactory.create(app)

    val isDemo: Boolean get() = classifier.isDemo

    private val _state = MutableStateFlow<UiState>(UiState.Home)
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun analyze(uri: Uri) {
        _state.value = UiState.Analyzing(uri)
        viewModelScope.launch {
            try {
                val predictions = withContext(Dispatchers.Default) {
                    val bitmap = decodeBitmap(uri) ?: error("Fotoğraf açılamadı")
                    classifier.classify(bitmap)
                }
                _state.value = UiState.Result(uri, predictions, classifier.isDemo)
            } catch (e: Exception) {
                _state.value = UiState.Error(e.message ?: "Bilinmeyen hata")
            }
        }
    }

    fun reset() {
        _state.value = UiState.Home
    }

    /** Büyük fotoğrafları belleği doldurmadan küçülterek açar. */
    private fun decodeBitmap(uri: Uri, maxSide: Int = 1024): Bitmap? {
        val resolver = getApplication<Application>().contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > maxSide || bounds.outHeight / sample > maxSide) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }

    override fun onCleared() {
        classifier.close()
    }
}
