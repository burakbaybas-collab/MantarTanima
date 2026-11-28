package com.burakbaybas.mantartanima

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.burakbaybas.mantartanima.classifier.ClassifierFactory
import com.burakbaybas.mantartanima.classifier.MushroomDetector
import com.burakbaybas.mantartanima.classifier.Prediction
import com.burakbaybas.mantartanima.data.HistoryEntry
import com.burakbaybas.mantartanima.data.HistoryStore
import com.burakbaybas.mantartanima.data.PlaceFinder
import com.burakbaybas.mantartanima.data.Season
import com.burakbaybas.mantartanima.data.SpeciesCatalog
import java.util.Calendar
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
    /** [found]: tarama sonunda gösterilecek bulunan tür (sonuç sayfasına geçmeden hemen önce). */
    data class Analyzing(val found: String? = null) : UiState
    data class Result(val predictions: List<Prediction>, val isDemo: Boolean) : UiState

    /** Fotoğraflar farklı mantarlara ait görünüyor; her fotoğrafın kendi tahmini gösterilir. */
    data class Mismatch(val guesses: List<Prediction>) : UiState

    /** Fotoğrafların çoğunda mantar algılanamadı; tür tahmini gösterilmez. */
    data class NoMushroom(val mushroomPhotos: Int, val totalPhotos: Int) : UiState
    data class Error(val message: String) : UiState

    /** Geçmiş tanımalar listesi */
    data object History : UiState

    /** Geçmişteki bir tanımanın ayrıntısı */
    data class HistoryDetail(val entry: HistoryEntry) : UiState
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    init {
        SpeciesCatalog.load(app)
    }

    private val classifier = ClassifierFactory.create(app)
    private val detector = MushroomDetector.create(app)

    val isDemo: Boolean get() = classifier.isDemo

    private val _photos = MutableStateFlow<Map<PhotoAngle, Uri>>(emptyMap())
    val photos: StateFlow<Map<PhotoAngle, Uri>> = _photos.asStateFlow()

    private val _state = MutableStateFlow<UiState>(UiState.Home)
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _history = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val history: StateFlow<List<HistoryEntry>> = _history.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) { _history.value = HistoryStore.load(app) }
    }

    fun openHistory() {
        _state.value = UiState.History
    }

    fun openHistoryEntry(entry: HistoryEntry) {
        _state.value = UiState.HistoryDetail(entry)
    }

    fun deleteHistoryEntry(entry: HistoryEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            HistoryStore.delete(getApplication(), entry.id)
            _history.value = HistoryStore.load(getApplication())
        }
    }

    fun deleteHistoryEntries(entries: List<HistoryEntry>) {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            if (entries.size == _history.value.size) HistoryStore.clear(app)
            else entries.forEach { HistoryStore.delete(app, it.id) }
            _history.value = HistoryStore.load(app)
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            HistoryStore.clear(getApplication())
            _history.value = emptyList()
        }
    }

    /** Geri tuşu: ayrıntıdan listeye, diğer her yerden ana sayfaya. */
    fun back() {
        _state.value = if (_state.value is UiState.HistoryDetail) UiState.History else UiState.Home
    }

    /** Sonucu, fotoğrafları ve (izin varsa) konumu geçmişe kaydeder; arka planda çalışır. */
    private fun saveToHistory(predictions: List<Prediction>, uris: List<Uri>, bitmaps: List<Bitmap>) {
        val app = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val place = PlaceFinder.find(app, uris)
                HistoryStore.add(app, predictions, PhotoAngle.entries.zip(bitmaps).toMap(), place)
                _history.value = HistoryStore.load(app)
            } catch (e: Exception) {
                // Geçmişe kayıt başarısız olsa da tanıma etkilenmez
            }
        }
    }

    fun setPhoto(angle: PhotoAngle, uri: Uri) = _photos.update { it + (angle to uri) }

    fun removePhoto(angle: PhotoAngle) = _photos.update { it - angle }

    /** Bugünün ayına göre mevsim dışı türlerin olasılığını azaltır ve yeniden normalleştirir. */
    private fun withSeason(all: List<Prediction>): List<Prediction> {
        if (all.isEmpty()) return all
        val month = Calendar.getInstance().get(Calendar.MONTH) + 1
        val weighted = all.map { it.copy(confidence = it.confidence * Season.weight(SpeciesCatalog.find(it.label), month)) }
        val sum = weighted.sumOf { it.confidence.toDouble() }.toFloat().takeIf { it > 0f } ?: return all
        return weighted.map { it.copy(confidence = it.confidence / sum) }.sortedByDescending { it.confidence }
    }

    private val MIN_SCAN_MS = 4000L

    fun analyze() {
        val uris = PhotoAngle.entries.mapNotNull { _photos.value[it] }
        if (uris.size < PhotoAngle.entries.size) return
        _state.value = UiState.Analyzing()
        viewModelScope.launch {
            val t0 = android.os.SystemClock.elapsedRealtime()
            try {
                val result = withContext(Dispatchers.Default) {
                    val bitmaps = uris.map { decodeBitmap(it) ?: error("Fotoğraf açılamadı") }
                    val predictions = withSeason(classifier.classify(bitmaps, topK = Int.MAX_VALUE)).take(3)
                    val mushroomPhotos = detector?.let { d -> bitmaps.count { d.looksLikeMushroom(it) } }
                    // Genel algılayıcı kâse, kulak ve jöle mantarlarını tanımayabilir. Tür modeli yeterince
                    // eminse sonucu yine gösteririz; ikisi de emin değilse "mantar algılanamadı" deriz.
                    val classifierSure = (predictions.firstOrNull()?.confidence ?: 0f) >= CLASSIFIER_OVERRIDE
                    if (mushroomPhotos != null && mushroomPhotos < MIN_MUSHROOM_PHOTOS && !classifierSure) {
                        UiState.NoMushroom(mushroomPhotos, bitmaps.size)
                    } else if (classifier.photosDisagree) {
                        UiState.Mismatch(classifier.lastPhotoGuesses)
                    } else {
                        if (!classifier.isDemo && predictions.isNotEmpty()) saveToHistory(predictions, uris, bitmaps)
                        UiState.Result(predictions, classifier.isDemo)
                    }
                }
                // Tarama ekranı en az ~4 sn görünsün
                val wait = MIN_SCAN_MS - (android.os.SystemClock.elapsedRealtime() - t0)
                if (wait > 0) kotlinx.coroutines.delay(wait)
                // Bulunan türü tarama ekranında kısaca göster, sonra sonuca geç
                val top = (result as? UiState.Result)?.predictions?.firstOrNull()
                if (top != null && !result.isDemo && top.confidence >= 0.25f) {
                    _state.value = UiState.Analyzing(found = top.label)
                    kotlinx.coroutines.delay(1600)
                }
                _state.value = result
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
        detector?.close()
    }

    companion object {
        /** Üç fotoğraftan en az bu kadarında mantar algılanmalı. */
        const val MIN_MUSHROOM_PHOTOS = 2

        /** Algılayıcı mantar görmese de tür modeli bu güvenin üstündeyse sonuç gösterilir. */
        const val CLASSIFIER_OVERRIDE = 0.40f
    }
}
