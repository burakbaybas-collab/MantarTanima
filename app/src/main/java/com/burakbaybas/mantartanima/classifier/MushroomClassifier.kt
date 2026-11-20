package com.burakbaybas.mantartanima.classifier

import android.content.Context
import android.graphics.Bitmap

data class Prediction(
    val label: String,
    val confidence: Float, // 0..1
)

/** Tüm sınıflandırıcıların uyduğu ortak arayüz. Model değişse de ekranlar aynı kalır. */
interface MushroomClassifier {
    /** Gerçek bir model yüklü değilse true. */
    val isDemo: Boolean

    fun classify(bitmap: Bitmap, topK: Int = 3): List<Prediction>

    fun close() {}
}

/** Model dosyası yokken kullanılan yer tutucu. Tahmin üretmez. */
class DemoClassifier : MushroomClassifier {
    override val isDemo = true
    override fun classify(bitmap: Bitmap, topK: Int): List<Prediction> = emptyList()
}

object ClassifierFactory {
    const val MODEL_FILE = "model.tflite"
    const val LABELS_FILE = "labels.txt"

    fun create(context: Context): MushroomClassifier {
        val assets = context.assets.list("")?.toSet().orEmpty()
        return if (MODEL_FILE in assets && LABELS_FILE in assets) {
            try {
                TfliteClassifier(context, MODEL_FILE, LABELS_FILE)
            } catch (e: Exception) {
                DemoClassifier()
            }
        } else {
            DemoClassifier()
        }
    }
}
