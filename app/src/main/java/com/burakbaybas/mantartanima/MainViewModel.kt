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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Mantarın fotoğraflanacağı üç açı. */
enum class PhotoAngle(val title: String, val hint: String) {
    TOP("Üstten", "Şapkanın üst yüzeyi"),
    SIDE("Yandan", "Şapka, sap ve taban birlikte"),
    BOTTOM("Alttan", "Lameller / gözenekler"),
}

sealed interface UiState {
    data object Home : UiState
    data object Analyzing : UiState
    data class Result(val predictions: List<Prediction>, val isDemo: Boolean) : UiState
    data class Error(val message: String) : UiState
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val classifier = ClassifierFactory.create(app)

    val isDemo: Boolean get() = classifier.isDemo

    private val _photos = MutableStateFlow<Map<PhotoAngle, Uri>>(emptyMap())
    val photos: StateFlow<Map<PhotoAngle, Uri>> = _photos.asStateFlow()

    private val _state = MutableStateFlow<UiState>(UiState.Home)
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun setPhoto(angle: PhotoAngle, uri: Uri) = _photos.update { it + (angle to uri) }

    fun removePhoto(angle: PhotoAngle) = _photos.update { it - angle }

    fun analyze() {
        val uris = PhotoAngle.entries.mapNotNull { _photos.value[it] }
        if (uris.size < PhotoAngle.entries.size) return
        _state.value = UiState.Analyzing
        viewModelScope.launch {
            try {
                val predictions = withContext(Dispatchers.Default) {
                    val bitmaps = uris.map { decodeBitmap(it) ?: error("Fotoğraf açılamadı") }
                    classifier.classify(bitmaps)
                }
                _state.value = UiState.Result(predictions, classifier.isDemo)
            } catch (e: Exception) {
                _state.value = UiState.Error(e.message ?: "Bilinmeyen hata")
            }
        }
    }

    /** Sonuçtan ana sayfaya döner; fotoğraflar korunur. */
    fun backToHome() {
        _state.value = UiState.Home
    }

    /** Fotoğrafları temizleyip yeni tanıma başlatır. */
    fun startOver() {
        _photos.value = emptyMap()
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
